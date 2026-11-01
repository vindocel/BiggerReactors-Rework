package biggerreactorsrework.multiblocks.turbine.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import biggerreactorsrework.multiblocks.turbine.blocks.TurbineRotorBlade;
import biggerreactorsrework.multiblocks.turbine.blocks.TurbineRotorShaft;
import biggerreactorsrework.multiblocks.turbine.state.TurbineShaftRotationState;
import biggerreactorsrework.core.multiblock.common.IEventMultiblock;
import biggerreactorsrework.core.multiblock.validated.IValidatedMultiblock;
import biggerreactorsrework.core.registry.RegisterTile;

import javax.annotation.ParametersAreNonnullByDefault;

import static biggerreactorsrework.multiblocks.turbine.blocks.TurbineRotorBlade.BLADE_POSITION;
import static biggerreactorsrework.multiblocks.turbine.state.TurbineShaftRotationState.*;

@ParametersAreNonnullByDefault
public class TurbineRotorShaftTile extends TurbineBaseTile implements IEventMultiblock.AssemblyStateTransition {
    
    @RegisterTile("turbine_rotor_shaft")
    public static final BlockEntityType.BlockEntitySupplier<TurbineRotorShaftTile> SUPPLIER = new RegisterTile.Producer<>(TurbineRotorShaftTile::new);
    
    public TurbineRotorShaftTile(BlockEntityType<?> TYPE, BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }
    
    @Override
    public void onAssemblyStateTransition(IValidatedMultiblock.AssemblyState oldState, IValidatedMultiblock.AssemblyState newState) {
        TurbineShaftRotationState newRotation = getBlockState().getValue(TurbineShaftRotationState.TURBINE_SHAFT_ROTATION_STATE_ENUM_PROPERTY);
        // Modified 2026-10-31: share topology inference with placement, including a lone shaft beside a bearing.
        assert level != null;
        if(!level.hasChunkAt(worldPosition)||!level.getBlockState(worldPosition).is(TurbineRotorShaft.INSTANCE))return;
        var axis = biggerreactorsrework.multiblocks.turbine.TurbineRotorLayout.shaftAxis(level,getBlockPos());
        if (axis != null) newRotation = TurbineShaftRotationState.values()[axis.ordinal()];
        if(level.hasChunkAt(worldPosition)) level.setBlockAndUpdate(worldPosition,
                getBlockState().setValue(TURBINE_SHAFT_ROTATION_STATE_ENUM_PROPERTY,newRotation));
        
        // propagate it out to the blades
        int i = 0;
        for (Direction value : biggerreactorsrework.multiblocks.turbine.TurbineRotorLayout.arms(Direction.Axis.values()[newRotation.ordinal()])) {
            
            BlockPos pos = getBlockPos();
            while (true) {
                pos = pos.relative(value);
                if(!level.hasChunkAt(pos)) break;
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() != TurbineRotorBlade.INSTANCE) {
                    break;
                }
                if(level.isLoaded(worldPosition)) {
                    level.setBlockAndUpdate(pos, state.setValue(TURBINE_SHAFT_ROTATION_STATE_ENUM_PROPERTY, newRotation).setValue(BLADE_POSITION, i));
                }
            }
            i++;
        }
    }
}
