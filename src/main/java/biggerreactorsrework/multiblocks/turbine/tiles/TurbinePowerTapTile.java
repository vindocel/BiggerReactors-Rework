package biggerreactorsrework.multiblocks.turbine.tiles;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import biggerreactorsrework.core.capability.CachedWrappedBlockCapability;
import biggerreactorsrework.core.energy.IEnergyTile;
import biggerreactorsrework.core.energy.IEnergyHandler;
import biggerreactorsrework.core.multiblock.common.IEventMultiblock;
import biggerreactorsrework.core.multiblock.validated.IValidatedMultiblock;
import biggerreactorsrework.core.registry.RegisterTile;
import biggerreactorsrework.core.util.BlockStates;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import static biggerreactorsrework.multiblocks.turbine.blocks.TurbinePowerTap.ConnectionState.*;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TurbinePowerTapTile extends TurbineBaseTile implements IEnergyTile, IEnergyHandler, IEventMultiblock.AssemblyStateTransition {
    
    @RegisterTile("turbine_power_tap")
    public static final BlockEntityType.BlockEntitySupplier<TurbinePowerTapTile> SUPPLIER = new RegisterTile.Producer<>(TurbinePowerTapTile::new);
    
    public TurbinePowerTapTile(BlockEntityType<?> TYPE, BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }
    
    private boolean connected = false;
    Direction powerOutputDirection = null;
    
    @Override
    public IEnergyHandler energyHandler(Direction direction) {
        return this;
    }
    
    private void setConnected(boolean newState) {
        if (newState != connected) {
            connected = newState;
            assert level != null;
            level.setBlockAndUpdate(worldPosition, getBlockState().setValue(CONNECTION_STATE_ENUM_PROPERTY, connected ? CONNECTED : DISCONNECTED));
        }
    }
    
    @Nullable
    CachedWrappedBlockCapability<IEnergyHandler, Direction> output = null;;
    
    public long distributePower(long toDistribute, boolean simulate) {
        if(output == null) {
            return 0;
        }
        @Nullable
        final var outputCap = output.getCapability();
        if (outputCap == null) {
            return 0;
        }
        return Math.max(0, outputCap.insertEnergy(toDistribute, simulate));
    }
    
    @Override
    public long insertEnergy(long maxInsert, boolean simulate) {
        return 0;
    }
    
    @Override
    public long extractEnergy(long maxExtract, boolean simulate) {
        if (maxExtract <= 0 || nullableController() == null || controller().assemblyState() != IValidatedMultiblock.AssemblyState.ASSEMBLED) {
            return 0;
        }
        long toExtract = controller().simulation().battery().stored();
        toExtract = Math.min(maxExtract, toExtract);
        if (!simulate) {
            toExtract = controller().simulation().battery().extract(toExtract);
        }
        return toExtract;
    }
    
    @Override
    public long energyStored() {
        if (nullableController() != null) {
            return Math.max(0, controller().simulation().battery().stored());
        }
        return 0;
    }
    
    @Override
    public long maxEnergyStored() {
        if (nullableController() != null) {
            return Math.max(0, controller().simulation().battery().capacity());
        }
        return 0;
    }
    
    
    @SuppressWarnings("DuplicatedCode")
    public void neighborChanged() {
        output = null;
        if (powerOutputDirection == null) {
            setConnected(false);
            return;
        }
        
        output = findEnergyCapability(powerOutputDirection);
        setConnected(output.getCapability() != null);
    }
    
    @Override
    public void onAssemblyStateTransition(IValidatedMultiblock.AssemblyState oldState, IValidatedMultiblock.AssemblyState newState) {
        if (newState == IValidatedMultiblock.AssemblyState.ASSEMBLED) {
            powerOutputDirection = getBlockState().getValue(BlockStates.FACING);
        } else {
            powerOutputDirection = null;
        }
        neighborChanged();
    }
}
