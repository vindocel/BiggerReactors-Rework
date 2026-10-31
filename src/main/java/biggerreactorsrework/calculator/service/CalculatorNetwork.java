package biggerreactorsrework.calculator.service;

import biggerreactorsrework.BiggerReactors;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * One bidirectional payload for the calculator: a compound with a {@code type} key. Reading is bounded by the
 * vanilla NBT quota; every field is validated by the receiver.
 */
public final class CalculatorNetwork {
    public static final String CATALOG = "catalog", START = "start", CANCEL = "cancel", PROGRESS = "progress", RESULT = "result";
    private static final String PROTOCOL = "1";

    public record Payload(CompoundTag tag) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(BiggerReactors.modid, "calculator"));
        public static final StreamCodec<FriendlyByteBuf, Payload> CODEC = StreamCodec.of(
                (buf, payload) -> buf.writeNbt(payload.tag),
                buf -> {
                    var tag = buf.readNbt();
                    return new Payload(tag == null ? new CompoundTag() : tag);
                });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private CalculatorNetwork() { }

    public static void register(RegisterPayloadHandlersEvent event) {
        net.neoforged.neoforge.network.handling.IPayloadHandler<Payload> handler = (payload, context) -> context.enqueueWork(() -> {
            if (context.flow().isClientbound()) {
                if (FMLEnvironment.getDist().isClient()) biggerreactorsrework.client.calculator.CalculatorClient.receive(payload.tag());
            } else if (context.player() instanceof ServerPlayer player) {
                CalculatorService.receive(player, payload.tag());
            }
        });
        event.registrar(PROTOCOL).playBidirectional(Payload.TYPE, Payload.CODEC, handler, handler);
    }

    public static void toPlayer(ServerPlayer player, CompoundTag tag) {
        PacketDistributor.sendToPlayer(player, new Payload(tag));
    }

    public static void toServer(CompoundTag tag) {
        net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new Payload(tag));
    }
}
