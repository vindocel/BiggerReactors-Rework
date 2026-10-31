package biggerreactorsrework.core.capability;

// Modified 2026-10-30: observe neighbor capability invalidation without loading chunks.

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import biggerreactorsrework.core.transfer.FluidTanks;
import biggerreactorsrework.core.transfer.ItemSlots;
import biggerreactorsrework.core.transfer.Transfer;
import org.jetbrains.annotations.Nullable;
import java.util.EnumMap;
import java.util.function.Function;

public final class NeighborCapability<T, H> {
    private final BlockEntity owner;
    private final BlockCapability<H, Direction> capability;
    private final Function<H, T> view;
    private final EnumMap<Direction, BlockCapabilityCache<H, Direction>> caches = new EnumMap<>(Direction.class);

    public NeighborCapability(BlockEntity owner, BlockCapability<H, Direction> capability, Function<H, T> view) {
        this.owner = owner;
        this.capability = capability;
        this.view = view;
    }

    public static NeighborCapability<ItemSlots, ?> items(BlockEntity owner) {
        return new NeighborCapability<>(owner, Capabilities.Item.BLOCK, Transfer::items);
    }

    public static NeighborCapability<FluidTanks, ?> fluids(BlockEntity owner) {
        return new NeighborCapability<>(owner, Capabilities.Fluid.BLOCK, Transfer::fluids);
    }

    @Nullable
    public T get(@Nullable Direction direction) {
        if (direction == null || owner.isRemoved() || !(owner.getLevel() instanceof ServerLevel level)) return null;
        var cache = caches.computeIfAbsent(direction, side -> BlockCapabilityCache.create(capability, level,
                owner.getBlockPos().relative(side).immutable(), side.getOpposite(), () -> !owner.isRemoved(), () -> { }));
        H handler = cache.getCapability();
        return handler == null ? null : view.apply(handler);
    }
}
