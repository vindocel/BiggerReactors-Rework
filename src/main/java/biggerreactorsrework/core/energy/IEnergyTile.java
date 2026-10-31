package biggerreactorsrework.core.energy;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import biggerreactorsrework.core.capability.CachedWrappedBlockCapability;
import biggerreactorsrework.core.modular.api.IModularTile;
import biggerreactorsrework.core.registry.CapabilityRegistration;
import biggerreactorsrework.core.registry.RegisterCapability;
import biggerreactorsrework.core.util.API;
import biggerreactorsrework.core.util.NonnullDefault;

import javax.annotation.Nullable;

@API
@NonnullDefault
public interface IEnergyTile extends IModularTile {
    
    @RegisterCapability
    CapabilityRegistration ENERGY_HANDLER_CAP_REGISTRATION = CapabilityRegistration.lazyTileCaps(IEnergyTile.class, () -> EnergyHandlerWrappers.wrap(IEnergyTile::energyHandler));
    
    @API
    IEnergyHandler energyHandler(Direction direction);
    
    @API
    default CachedWrappedBlockCapability<IEnergyHandler, Direction> findEnergyCapability(Direction direction) {
        final var thisTile = this.as(BlockEntity.class);
        @Nullable
        final var level = thisTile.getLevel();
        assert level != null;
        return new CachedWrappedBlockCapability<>(EnergyHandlerWrappers::findCapability, (ServerLevel) level, thisTile.getBlockPos().offset(direction.getUnitVec3i()), direction.getOpposite(), () -> !thisTile.isRemoved(), () -> onNeighborEnergyCapabilityInvalidated(direction));
    }
    
    @API
    default void onNeighborEnergyCapabilityInvalidated(Direction direction) {
    }
}
