package biggerreactorsrework.core.client.gui;

import biggerreactorsrework.core.networking.SimpleChannel;
import biggerreactorsrework.core.registry.OnModLoad;
import biggerreactorsrework.core.serialization.DataCompound;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

import static biggerreactorsrework.BiggerReactors.modid;

/**
 * Menu state synchronisation.
 * <p>
 * Every {@link #SYNC_INTERVAL_TICKS} server ticks, each player whose open menu is an {@link IGUIPacketProvider}
 * receives that menu's current state. Requests from the client are only accepted for the menu the player
 * currently has open on the server; vanilla closes the menu once {@code stillValid} fails, so a request can
 * never reach a machine the player is no longer allowed to use.
 */
public class GuiSync {

    /**
     * 4 ticks = 200 ms, the interval the menus have always been refreshed at.
     */
    private static final int SYNC_INTERVAL_TICKS = 4;
    private static final int MAX_REQUEST_NAME_LENGTH = 128;

    public interface IGUIPacketProvider {

        /** Position used to distinguish this machine's local appearance preferences. */
        net.minecraft.core.BlockPos machinePos();

        @Nullable
        IGUIPacket getGuiPacket();

        /**
         * Sends a request to the server side of this menu.
         *
         * @param requestName The request to make.
         * @param requestData The payload to send.
         */
        default void runRequest(@Nonnull String requestName, @Nullable Object requestData) {
            var compound = new DataCompound();

            compound.put("request", requestName);
            if (requestData != null) {
                compound.put("data", requestData);
            }

            INSTANCE.sendToServer(compound);
        }

        /**
         * Runs a request on the server. Implementations must validate {@code requestData}: it comes from the client.
         */
        default void executeRequest(String requestName, @Nullable Object requestData) {
        }
    }

    public interface IGUIPacket {
        void read(@Nonnull Map<?, ?> data);

        @Nullable
        Map<?, ?> write();
    }

    public static final SimpleChannel INSTANCE = new SimpleChannel(
            ResourceLocation.fromNamespaceAndPath(modid, "menu_sync"),
            GuiSync::clientHandler, GuiSync::serverHandler
    );

    private static int ticksUntilSync = 0;

    @OnModLoad
    public static void onModLoad() {
        NeoForge.EVENT_BUS.addListener(GuiSync::onServerTick);
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        if (--ticksUntilSync > 0) {
            return;
        }
        ticksUntilSync = SYNC_INTERVAL_TICKS;
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (!(player.containerMenu instanceof IGUIPacketProvider provider)) {
                continue;
            }
            IGUIPacket packet = provider.getGuiPacket();
            if (packet == null) {
                continue;
            }
            Map<?, ?> packetMap = packet.write();
            if (packetMap == null) {
                continue;
            }
            //noinspection unchecked
            INSTANCE.sendToPlayer(player, new DataCompound((Map<String, Object>) packetMap));
        }
    }

    private static void clientHandler(DataCompound compound, IPayloadContext ctx) {
        if (ctx.player().containerMenu instanceof IGUIPacketProvider provider) {
            IGUIPacket guiPacket = provider.getGuiPacket();
            if (guiPacket != null) {
                guiPacket.read(compound.toROBNMap());
            }
        }
    }

    private static void serverHandler(DataCompound compound, IPayloadContext ctx) {
        if (!(compound.get("request") instanceof String request) || request.isEmpty() || request.length() > MAX_REQUEST_NAME_LENGTH) {
            return;
        }
        final var player = ctx.player();
        if (!(player.containerMenu instanceof IGUIPacketProvider provider) || !player.containerMenu.stillValid(player)) {
            return;
        }
        provider.executeRequest(request, compound.get("data"));
    }
}
