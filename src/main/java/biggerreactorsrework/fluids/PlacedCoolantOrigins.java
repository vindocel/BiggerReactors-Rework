package biggerreactorsrework.fluids;

import biggerreactorsrework.BiggerReactors;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.attachment.AttachmentSyncHandler;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/** Origin data shared by a bucket source and the blocks it flows into. */
public final class PlacedCoolantOrigins {
    private static final DeferredRegister<AttachmentType<?>> TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, BiggerReactors.modid);
    private static final Supplier<AttachmentType<Map<String, String>>> ORIGINS = TYPES.register("placed_coolant_origins",
            () -> AttachmentType.<Map<String, String>>builder(() -> Map.of())
                    .serialize(Codec.unboundedMap(Codec.STRING, Codec.STRING), origins -> !origins.isEmpty())
                    .sync(new AttachmentSyncHandler<>() {
                        @Override public void write(RegistryFriendlyByteBuf buf, Map<String, String> origins, boolean initialSync) {
                            buf.writeVarInt(origins.size());
                            origins.forEach((pos, origin) -> {
                                buf.writeLong(Long.parseLong(pos));
                                buf.writeUtf(origin);
                            });
                        }

                        @Override public Map<String, String> read(IAttachmentHolder holder, RegistryFriendlyByteBuf buf,
                                                                  @Nullable Map<String, String> previous) {
                            int size = buf.readVarInt();
                            var origins = new HashMap<String, String>();
                            for (int i = 0; i < size; i++) origins.put(Long.toString(buf.readLong()), buf.readUtf());
                            if (FMLEnvironment.dist.isClient()) {
                                biggerreactorsrework.BiggerReactorsClient.refreshPlacedCoolant(holder, previous, origins);
                            }
                            return Map.copyOf(origins);
                        }
                    }).build());

    private PlacedCoolantOrigins() {}

    static void register(IEventBus bus) {
        TYPES.register(bus);
        NeoForge.EVENT_BUS.addListener(PlacedCoolantOrigins::migrate);
    }

    @Nullable
    public static String get(@Nullable LevelChunk chunk, BlockPos pos) {
        return chunk == null ? null : chunk.getExistingData(ORIGINS).map(data -> data.get(key(pos))).orElse(null);
    }

    public static void put(ServerLevel level, BlockPos pos, @Nullable String origin) {
        put(level.getChunkAt(pos), pos, origin);
    }

    static void put(LevelChunk chunk, BlockPos pos, @Nullable String origin) {
        var old = chunk.getExistingData(ORIGINS).orElse(Map.of());
        String key = key(pos);
        if (Objects.equals(old.get(key), origin)) return;
        var updated = new HashMap<>(old);
        if (origin == null) updated.remove(key);
        else updated.put(key, origin);
        chunk.setData(ORIGINS, Map.copyOf(updated));
        chunk.syncData(ORIGINS.get());
    }

    private static String key(BlockPos pos) {
        return Long.toString(pos.asLong());
    }

    private static void migrate(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
            HeatedCoolantBucket.Origins.get(level).migrate(chunk);
        }
    }
}
