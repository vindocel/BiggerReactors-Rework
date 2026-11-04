package biggerreactorsrework.fluids;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.registries.CoolantComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.neoforge.transfer.ItemAccessResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidType;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

/** Added 2026-11-03: heated coolant keeps its cold origin in buckets and in placed source blocks. */
public final class HeatedCoolantBucket {
    private HeatedCoolantBucket() { }

    public static void register(IEventBus modBus) {
        PlacedCoolantOrigins.register(modBus);
        modBus.addListener(RegisterCapabilitiesEvent.class, event -> event.registerItem(Capabilities.Fluid.ITEM,
                (stack, access) -> new Wrapper(access), HeatedCoolant.INSTANCE.getBucket()));
    }

    @Nullable
    public static String origin(ItemStack stack) {
        return stack.get(CoolantComponents.ORIGIN.get());
    }

    public static ItemStack filled(FluidStack fluid) {
        var bucket = new ItemStack(HeatedCoolant.INSTANCE.getBucket());
        String origin = fluid.get(CoolantComponents.ORIGIN.get());
        if (origin != null) bucket.set(CoolantComponents.ORIGIN.get(), origin);
        return bucket;
    }

    public static FluidStack contents(ItemStack bucket) {
        var fluid = new FluidStack(HeatedCoolant.INSTANCE, net.neoforged.neoforge.fluids.FluidType.BUCKET_VOLUME);
        String origin = origin(bucket);
        if (origin != null) fluid.set(CoolantComponents.ORIGIN.get(), origin);
        return fluid;
    }

    /** Called by the placed block when an empty bucket collects a source. */
    public static ItemStack pickup(@Nullable String origin, ItemStack bucket) {
        if (!bucket.isEmpty() && origin != null) bucket.set(CoolantComponents.ORIGIN.get(), origin);
        return bucket;
    }

    public static class BucketWithOrigin extends BucketItem {
        public BucketWithOrigin(Fluid fluid, net.minecraft.world.item.Item.Properties properties) {
            super(fluid, properties);
        }

        @Override
        public Component getName(ItemStack stack) {
            return Component.translatable("item.biggerreactorsrework.heated_coolant_bucket.named", CoolantComponents.name(contents(stack)));
        }

        @Override
        public boolean emptyContents(@Nullable LivingEntity player, Level level, BlockPos pos, @Nullable BlockHitResult hit, @Nullable ItemStack container) {
            String origin = container == null ? null : origin(container);
            if (origin == null) return false;
            boolean placed = super.emptyContents(player, level, pos, hit, container);
            if (placed && level instanceof ServerLevel server && level.getFluidState(pos).isSourceOfType(HeatedCoolant.INSTANCE))
                PlacedCoolantOrigins.put(server, pos, origin);
            return placed;
        }
    }

    // Ported 2026-11-03: the native transactional resource handler keeps the origin on both sides.
    private static final class Wrapper extends ItemAccessResourceHandler<FluidResource> {
        Wrapper(ItemAccess access) { super(access, 1); }

        @Override protected FluidResource getResourceFrom(ItemResource resource, int slot) {
            return FluidResource.of(contents(resource.toStack(1)));
        }

        @Override protected int getAmountFrom(ItemResource resource, int slot) {
            return FluidType.BUCKET_VOLUME;
        }

        @Override protected ItemResource update(ItemResource resource, int slot, FluidResource fluid, int amount) {
            return amount == 0 ? ItemResource.of(net.minecraft.world.item.Items.BUCKET) : ItemResource.EMPTY;
        }

        @Override protected int getCapacity(int slot, FluidResource fluid) { return FluidType.BUCKET_VOLUME; }
    }

    static final class Origins extends SavedData {
        private static final Codec<Origins> CODEC = Codec.unboundedMap(Codec.STRING, Codec.STRING)
                .xmap(Origins::new, value -> value.origins);
        private static final SavedDataType<Origins> TYPE = new SavedDataType<>(
                Identifier.fromNamespaceAndPath(BiggerReactors.modid, "heated_coolant_origins"), Origins::new, CODEC);
        private final Map<String, String> origins;

        private Origins() { this(Map.of()); }
        private Origins(Map<String, String> origins) { this.origins = new HashMap<>(origins); }

        static Origins get(ServerLevel level) { return level.getDataStorage().computeIfAbsent(TYPE); }

        void migrate(net.minecraft.world.level.chunk.LevelChunk chunk) {
            var iterator = origins.entrySet().iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                var pos = BlockPos.of(Long.parseLong(entry.getKey()));
                if (chunk.getPos().x() != (pos.getX() >> 4) || chunk.getPos().z() != (pos.getZ() >> 4)) continue;
                if (chunk.getFluidState(pos).isSourceOfType(HeatedCoolant.INSTANCE)) {
                    PlacedCoolantOrigins.put(chunk, pos, entry.getValue());
                }
                iterator.remove();
                setDirty();
            }
        }
    }
}
