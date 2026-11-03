package biggerreactorsrework.fluids;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.registries.CoolantComponents;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper;

import javax.annotation.Nullable;

/** Added 2026-11-03: heated coolant keeps its cold origin in buckets and in placed source blocks. */
public final class HeatedCoolantBucket {
    private HeatedCoolantBucket() { }

    public static void register(IEventBus modBus) {
        modBus.addListener(RegisterCapabilitiesEvent.class, event -> event.registerItem(Capabilities.FluidHandler.ITEM,
                (stack, context) -> new Wrapper(stack), HeatedCoolant.INSTANCE.getBucket()));
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
    public static ItemStack pickup(LevelAccessor level, BlockPos pos, ItemStack bucket) {
        if (!bucket.isEmpty() && level instanceof ServerLevel server) {
            String origin = Origins.get(server).take(pos);
            if (origin != null) bucket.set(CoolantComponents.ORIGIN.get(), origin);
        }
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
        public boolean emptyContents(@Nullable Player player, Level level, BlockPos pos, @Nullable BlockHitResult hit, @Nullable ItemStack container) {
            String origin = container == null ? null : origin(container);
            if (origin == null) return false;
            boolean placed = super.emptyContents(player, level, pos, hit, container);
            if (placed && level instanceof ServerLevel server && level.getFluidState(pos).isSourceOfType(HeatedCoolant.INSTANCE))
                Origins.get(server).put(pos, origin);
            return placed;
        }
    }

    private static final class Wrapper extends FluidBucketWrapper {
        Wrapper(ItemStack container) {
            super(container);
        }

        @Override
        public FluidStack getFluid() {
            return contents(container);
        }
    }

    /** Origins of placed heated coolant sources, per dimension. */
    static final class Origins extends SavedData {
        private static final String NAME = BiggerReactors.modid + "_heated_coolant_origins";
        private final Long2ObjectOpenHashMap<String> origins = new Long2ObjectOpenHashMap<>();

        static Origins get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Origins::new, Origins::load), NAME);
        }

        private static Origins load(CompoundTag tag, HolderLookup.Provider registries) {
            var origins = new Origins();
            for (String key : tag.getAllKeys()) {
                try { origins.origins.put(Long.parseLong(key), tag.getString(key)); } catch (NumberFormatException ignored) { }
            }
            return origins;
        }

        void put(BlockPos pos, String origin) {
            origins.put(pos.asLong(), origin);
            setDirty();
        }

        @Nullable
        String take(BlockPos pos) {
            String origin = origins.remove(pos.asLong());
            if (origin != null) setDirty();
            return origin;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            origins.forEach((pos, origin) -> tag.putString(Long.toString(pos), origin));
            return tag;
        }
    }
}
