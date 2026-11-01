package biggerreactorsrework.multiblocks.turbine.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import biggerreactorsrework.multiblocks.turbine.state.TurbineShaftRotationState;
import biggerreactorsrework.multiblocks.turbine.tiles.TurbineRotorBladeTile;
import biggerreactorsrework.core.multiblock.IAssemblyStateBlock;
import biggerreactorsrework.core.registry.RegisterBlock;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import static biggerreactorsrework.multiblocks.turbine.state.TurbineShaftRotationState.Y;

@ParametersAreNonnullByDefault
public class TurbineRotorBlade extends TurbineBaseBlock implements IAssemblyStateBlock {
    // Modified 2026-10-31: redirect ordinary blade clicks and orient new real blocks before assembly.
    @Override public net.minecraft.world.InteractionResult onUse(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        var result = biggerreactorsrework.multiblocks.turbine.TurbineRotorLayout.use(level,pos,player,hand);
        return result == net.minecraft.world.InteractionResult.PASS ? super.onUse(state,level,pos,player,hand,hit) : result;
    }
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return biggerreactorsrework.multiblocks.turbine.TurbineRotorLayout.placementState(context,defaultBlockState());
    }
    
    public static final IntegerProperty BLADE_POSITION = IntegerProperty.create("blade_position", 0, 3);
    
    @RegisterBlock(name = "turbine_rotor_blade", tileEntityClass = TurbineRotorBladeTile.class)
    public static final TurbineRotorBlade INSTANCE = biggerreactorsrework.core.registry.RegistryIds.named("turbine_rotor_blade", () -> new TurbineRotorBlade());
    
    public TurbineRotorBlade() {
        super(false);
        registerDefaultState(defaultBlockState().setValue(TurbineShaftRotationState.TURBINE_SHAFT_ROTATION_STATE_ENUM_PROPERTY, Y));
    }
    
    @Override
    protected void buildStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TurbineShaftRotationState.TURBINE_SHAFT_ROTATION_STATE_ENUM_PROPERTY);
        builder.add(BLADE_POSITION);
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return TurbineRotorBladeTile.SUPPLIER.create(pos, state);
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
