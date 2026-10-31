package biggerreactorsrework.multiblocks.turbine.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import biggerreactorsrework.multiblocks.turbine.state.TurbineShaftRotationState;
import biggerreactorsrework.multiblocks.turbine.tiles.TurbineRotorShaftTile;
import biggerreactorsrework.core.multiblock.IAssemblyStateBlock;
import biggerreactorsrework.core.registry.RegisterBlock;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import static biggerreactorsrework.multiblocks.turbine.state.TurbineShaftRotationState.Y;

@ParametersAreNonnullByDefault
public class TurbineRotorShaft extends TurbineBaseBlock implements IAssemblyStateBlock {
    
    @RegisterBlock(name = "turbine_rotor_shaft", tileEntityClass = TurbineRotorShaftTile.class)
    public static final TurbineRotorShaft INSTANCE = biggerreactorsrework.core.registry.RegistryIds.named("turbine_rotor_shaft", () -> new TurbineRotorShaft());
    
    public TurbineRotorShaft() {
        super(false);
        registerDefaultState(defaultBlockState().setValue(TurbineShaftRotationState.TURBINE_SHAFT_ROTATION_STATE_ENUM_PROPERTY, Y));
    }
    
    @Override
    protected void buildStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TurbineShaftRotationState.TURBINE_SHAFT_ROTATION_STATE_ENUM_PROPERTY);
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return TurbineRotorShaftTile.SUPPLIER.create(pos, state);
    }
    
    @SuppressWarnings("deprecation")
    public float getShadeBrightness(BlockState state, BlockGetter worldIn, BlockPos pos) {
        return 1.0F;
    }
    
    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
        return true;
    }
    
    @Override
    public boolean isGoodForInterior() {
        return true;
    }
    
    @Override
    public boolean isGoodForExterior() {
        return false;
    }
}
