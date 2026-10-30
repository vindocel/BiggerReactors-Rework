package biggerreactorsrework.core.client.gui;

// Modified 2026-10-26: bind screen messages to the originating menu and position.

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
        default java.util.List<java.util.Map<String,Object>> statistics() { return java.util.List.of(); }

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

            if (!(this instanceof net.minecraft.world.inventory.AbstractContainerMenu menu)) return;
            compound.put("menu", menu.containerId);
            compound.put("position", machinePos().asLong());
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
    private static final java.util.Map<IGUIPacketProvider,java.util.List<java.util.Map<String,Object>>> CLIENT_STATISTICS = new java.util.WeakHashMap<>();
    public static java.util.List<java.util.Map<String,Object>> statistics(IGUIPacketProvider provider) { return CLIENT_STATISTICS.getOrDefault(provider,java.util.List.of()); }

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
            var snapshot=new java.util.HashMap<String,Object>((Map<String,Object>)packetMap);
            snapshot.put("menu",player.containerMenu.containerId);
            snapshot.put("position",provider.machinePos().asLong());
            snapshot.put("statistics",provider.statistics());
            INSTANCE.sendToPlayer(player, new DataCompound(snapshot));
        }
    }

    private static void clientHandler(DataCompound compound, IPayloadContext ctx) {
        if (ctx.player().containerMenu instanceof IGUIPacketProvider provider
                && matches(compound, ctx.player().containerMenu, provider)) {
            if(compound.get("statistics") instanceof java.util.List<?> list) {
                var rows=new java.util.ArrayList<java.util.Map<String,Object>>();
                for(var row:list)if(row instanceof java.util.Map<?,?> map && map.get("key") instanceof String key && map.get("value") instanceof String value)
                    rows.add(java.util.Map.of("key",key,"value",value));
                CLIENT_STATISTICS.put(provider,java.util.List.copyOf(rows));
            }
            IGUIPacket guiPacket = provider.getGuiPacket();
            if (guiPacket != null) {
                guiPacket.read(compound.toROBNMap());
            }
        }
    }

    // Modified 2026-10-26: bind requests and snapshots to the originating menu and machine.
    private static boolean matches(DataCompound data, net.minecraft.world.inventory.AbstractContainerMenu menu, IGUIPacketProvider provider) {
        return data.get("menu") instanceof Integer id && id == menu.containerId
                && data.get("position") instanceof Long position && position == provider.machinePos().asLong();
    }

    private static void serverHandler(DataCompound compound, IPayloadContext ctx) {
        handleServerRequest(compound, ctx.player());
    }

    // Modified 2026-10-30: share the exact validated menu dispatch with server-side callers.
    public static void handleServerRequest(DataCompound compound, net.minecraft.world.entity.player.Player player) {
        if (!(compound.get("request") instanceof String request) || request.isEmpty() || request.length() > MAX_REQUEST_NAME_LENGTH) {
            return;
        }
        if (!(player.containerMenu instanceof IGUIPacketProvider provider) || !player.containerMenu.stillValid(player) || !matches(compound, player.containerMenu, provider)) {
            return;
        }
        provider.executeRequest(request, compound.get("data"));
    }
}
