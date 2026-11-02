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
    // Modified 2026-11-01: orient the first real shaft before the multiblock's delayed topology update.
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return defaultBlockState().setValue(TurbineShaftRotationState.TURBINE_SHAFT_ROTATION_STATE_ENUM_PROPERTY,
                TurbineShaftRotationState.values()[context.getClickedFace().getAxis().ordinal()]);
    }
    // Modified 2026-10-31: real blade blocks can be managed through the shaft.
    @Override public net.minecraft.world.InteractionResult onUse(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        var result = biggerreactorsrework.multiblocks.turbine.TurbineRotorLayout.use(level,pos,player,hand);
        return result == net.minecraft.world.InteractionResult.PASS ? super.onUse(state,level,pos,player,hand,hit) : result;
    }
    @Override public BlockState playerWillDestroy(net.minecraft.world.level.Level level, BlockPos pos, BlockState state,
            net.minecraft.world.entity.player.Player player) {
        biggerreactorsrework.multiblocks.turbine.TurbineRotorLayout.breakShaft(level,pos,player);
        return super.playerWillDestroy(level,pos,state,player);
    }
    
    // Modified 2026-11-01: slim aiming outline; retain full-block collision.
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        return RotorShapes.shaft(state.getValue(TurbineShaftRotationState.TURBINE_SHAFT_ROTATION_STATE_ENUM_PROPERTY).ordinal());
    }
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        return net.minecraft.world.phys.shapes.Shapes.block();
    }

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
