package biggerreactorsrework.core;

import biggerreactorsrework.core.event.ReloadDataEvent;
import biggerreactorsrework.core.multiblock.MultiblockRegistry;
import biggerreactorsrework.core.threading.Queues;
import biggerreactorsrework.core.util.Util;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Game-bus hooks shared by the internal framework: data reload notification, the server tick counter,
 * main-thread work queues and deferred block state updates.
 */
@ParametersAreNonnullByDefault
public final class CoreEvents {

    public static final Logger LOGGER = LogManager.getLogger("BiggerReactors-Rework/Core");
    // used to ensure things are not ticked twice
    private static long tick = 0;

    private static MinecraftServer server;
    public static ResourceManager serverResourceManager;

    private CoreEvents() {
    }

    public static void init() {
        NeoForge.EVENT_BUS.register(CoreEvents.class);
        if (FMLLoader.getCurrent().getDist().isClient()) {
            NeoForge.EVENT_BUS.addListener(CoreEvents::onClientTick);
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent serverStartedEvent) {
        server = serverStartedEvent.getServer();
        updateRegistries();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent serverStoppedEvent) {
        serverResourceManager = null;
        server = null;
    }

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent tagsUpdatedEvent) {
        updateRegistries();
    }

    private static void updateRegistries() {
        if (server == null) {
            return;
        }
        if (FMLLoader.getCurrent().getDist().isClient()) {
            // ignore client thread
            // prevents double reloads, and reaching across sides
            if (RenderSystem.isOnRenderThread()) {
                return;
            }
        }
        serverResourceManager = server.getResourceManager();
        NeoForge.EVENT_BUS.post(new ReloadDataEvent());
        MultiblockRegistry.revalidateAll();
    }

    public static long tickNumber() {
        return tick;
    }

    @SubscribeEvent
    public static void advanceTick(ServerTickEvent.Post e) {
        tick++;

        // prevents deadlock
        final boolean[] run = {true};
        Queues.serverThread.enqueue(() -> run[0] = false);
        while (run[0]) {
            Queues.serverThread.runOne();
        }

        Util.serverTick();
    }

    private static void onClientTick(ClientTickEvent.Pre e) {
        // prevents deadlock
        final boolean[] run = {true};
        Queues.clientThread.enqueue(() -> run[0] = false);
        while (run[0]) {
            Queues.clientThread.runOne();
        }

        Util.clientTick();
    }

    @SubscribeEvent
    public static void tickWorld(LevelTickEvent.Post e) {
        if (!(e.getLevel() instanceof ServerLevel)) {
            return;
        }
        Util.updateBlockStates(e.getLevel());
        Util.worldTickEndEvent(e.getLevel());
    }
}
