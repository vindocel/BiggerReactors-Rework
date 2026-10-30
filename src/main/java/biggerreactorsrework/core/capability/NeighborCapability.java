package biggerreactorsrework.core.capability;

// Modified 2026-10-30: observe neighbor capability invalidation without loading chunks.

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import org.jetbrains.annotations.Nullable;
import java.util.EnumMap;

public final class NeighborCapability<T> {
    private final BlockEntity owner;
    private final BlockCapability<T, Direction> capability;
    private final EnumMap<Direction, BlockCapabilityCache<T, Direction>> caches = new EnumMap<>(Direction.class);

    public NeighborCapability(BlockEntity owner, BlockCapability<T, Direction> capability) {
        this.owner = owner;
        this.capability = capability;
    }

    @Nullable
    public T get(@Nullable Direction direction) {
        if (direction == null || owner.isRemoved() || !(owner.getLevel() instanceof ServerLevel level)) return null;
        var cache = caches.computeIfAbsent(direction, side -> BlockCapabilityCache.create(capability, level,
                owner.getBlockPos().relative(side).immutable(), side.getOpposite(), () -> !owner.isRemoved(), () -> { }));
        return cache.getCapability();
    }
}
