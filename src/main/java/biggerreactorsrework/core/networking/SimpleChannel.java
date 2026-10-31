package biggerreactorsrework.core.networking;

// Modified 2026-10-27: require protocol 4 for industrial item/fluid recipe and tank synchronization.

import biggerreactorsrework.core.registry.OnModLoad;
import biggerreactorsrework.core.serialization.DataCompound;
import biggerreactorsrework.core.util.NonnullDefault;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * A named, bidirectional payload carrying a {@link DataCompound} encoded in the internal binary format.
 * Callbacks always run on the main thread of the receiving side.
 */
@NonnullDefault
public class SimpleChannel {

    /**
     * Upper bound for a single encoded compound, keeps a malformed or hostile packet from allocating without limit.
     */
    private static final int MAX_PAYLOAD_BYTES = 1 << 20;
    // Modified 2026-10-30: protocol 5 includes authoritative server configuration snapshots.
    private static final String PROTOCOL_VERSION = "5";

    private static final List<SimpleChannel> channelsToRegister = new ObjectArrayList<>();

    private static void register(final RegisterPayloadHandlersEvent event) {
        final var registrar = event.registrar(PROTOCOL_VERSION);
        for (var channel : channelsToRegister) {
            registrar.playBidirectional(channel.type, channel.codec, channel::handle, channel::handle);
        }
    }

    @OnModLoad
    private static void onModLoad() {
        ModLoadingContext.get().getActiveContainer().getEventBus().addListener(SimpleChannel::register);
    }

    private final CustomPacketPayload.Type<CompoundPacket> type;
    private final StreamCodec<RegistryFriendlyByteBuf, CompoundPacket> codec;
    @Nullable
    private final BiConsumer<DataCompound, IPayloadContext> clientCallback;
    @Nullable
    private final BiConsumer<DataCompound, IPayloadContext> serverCallback;

    public SimpleChannel(Identifier id, @Nullable BiConsumer<DataCompound, IPayloadContext> clientCallback, @Nullable BiConsumer<DataCompound, IPayloadContext> serverCallback) {
        this.type = new CustomPacketPayload.Type<>(id);
        this.codec = StreamCodec.of(
                (buf, packet) -> buf.writeByteArray(packet.compound.toROBN().toByteArray()),
                buf -> new CompoundPacket(type, new DataCompound(buf.readByteArray(MAX_PAYLOAD_BYTES)))
        );
        this.clientCallback = clientCallback;
        this.serverCallback = serverCallback;
        channelsToRegister.add(this);
    }

    private void handle(CompoundPacket packet, IPayloadContext ctx) {
        @Nullable final var callbackToUse = ctx.flow().isClientbound() ? clientCallback : serverCallback;
        if (callbackToUse == null) {
            return;
        }
        ctx.enqueueWork(() -> callbackToUse.accept(packet.compound, ctx));
    }

    public void sendToServer(DataCompound compound) {
        net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new CompoundPacket(type, compound));
    }

    public void sendToPlayer(ServerPlayer serverPlayer, DataCompound compound) {
        PacketDistributor.sendToPlayer(serverPlayer, new CompoundPacket(type, compound));
    }

    public void forceClassLoad() {
    }

    private record CompoundPacket(Type<CompoundPacket> type, DataCompound compound) implements CustomPacketPayload {
    }
}
