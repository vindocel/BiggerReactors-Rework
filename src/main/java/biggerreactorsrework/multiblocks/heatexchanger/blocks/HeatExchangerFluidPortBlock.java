package biggerreactorsrework.multiblocks.heatexchanger.blocks;


import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import biggerreactorsrework.multiblocks.heatexchanger.tiles.HeatExchangerFluidPortTile;
import biggerreactorsrework.core.multiblock.IAssemblyStateBlock;
import biggerreactorsrework.core.multiblock.rectangular.IFaceDirectionBlock;
import biggerreactorsrework.core.registry.RegisterBlock;
import biggerreactorsrework.core.util.Util;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import static biggerreactorsrework.core.util.BlockStates.PORT_DIRECTION;

@ParametersAreNonnullByDefault
public class HeatExchangerFluidPortBlock extends HeatExchangerBaseBlock implements IAssemblyStateBlock, IFaceDirectionBlock {
    
    public static final BooleanProperty CONDENSER = BooleanProperty.create("condenser");
    
    
    public HeatExchangerFluidPortBlock() {
        super();
        registerDefaultState(defaultBlockState().setValue(PORT_DIRECTION, false));
        registerDefaultState(defaultBlockState().setValue(CONDENSER, false));
    }
    
    @RegisterBlock(name = "heat_exchanger_fluid_port", tileEntityClass = HeatExchangerFluidPortTile.class)
    public static final HeatExchangerFluidPortBlock INSTANCE = biggerreactorsrework.core.registry.RegistryIds.named("heat_exchanger_fluid_port", () -> new HeatExchangerFluidPortBlock());
    
    @Override
    protected void buildStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PORT_DIRECTION);
        builder.add(CONDENSER);
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return HeatExchangerFluidPortTile.SUPPLIER.create(pos, state);
    }
    
    @Override
    public InteractionResult onUse(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
        if (handIn == InteractionHand.MAIN_HAND) {
            if (Util.isWrench(player.getMainHandItem().getItem())) {
                boolean direction = !state.getValue(PORT_DIRECTION);
                state = state.setValue(PORT_DIRECTION, direction);
                worldIn.setBlock(pos, state, 3);
                if (!worldIn.isClientSide()) {
                    BlockEntity te = worldIn.getBlockEntity(pos);
                    if (te instanceof HeatExchangerFluidPortTile) {
                        ((HeatExchangerFluidPortTile) te).setInletOtherOutlet(direction);
                    }
                }
                return InteractionResult.SUCCESS;
            }
        }
        return super.onUse(state, worldIn, pos, player, handIn, hit);
    }
    
    @Override
    public void onNeighborChange(BlockState state, Level worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
        super.onNeighborChange(state, worldIn, pos, blockIn, fromPos, isMoving);
        BlockEntity te = worldIn.getBlockEntity(pos);
        if (te instanceof HeatExchangerFluidPortTile) {
            ((HeatExchangerFluidPortTile) te).neighborChanged();
        }
    }
}
