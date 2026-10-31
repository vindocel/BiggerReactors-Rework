package biggerreactorsrework.multiblocks.reactor.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import biggerreactorsrework.multiblocks.reactor.tiles.ReactorCoolantPortTile;
import biggerreactorsrework.core.multiblock.IAssemblyStateBlock;
import biggerreactorsrework.core.multiblock.rectangular.IFaceDirectionBlock;
import biggerreactorsrework.core.registry.RegisterBlock;
import biggerreactorsrework.core.util.Util;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import static biggerreactorsrework.multiblocks.reactor.blocks.ReactorAccessPort.PortDirection.*;

@ParametersAreNonnullByDefault
public class ReactorCoolantPort extends ReactorBaseBlock implements IAssemblyStateBlock, IFaceDirectionBlock {

    @RegisterBlock(name = "reactor_coolant_port", tileEntityClass = ReactorCoolantPortTile.class)
    public static final ReactorCoolantPort INSTANCE = biggerreactorsrework.core.registry.RegistryIds.named("reactor_coolant_port", () -> new ReactorCoolantPort());
    
    public ReactorCoolantPort() {
        super();
        registerDefaultState(defaultBlockState().setValue(PORT_DIRECTION_ENUM_PROPERTY, INLET));
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ReactorCoolantPortTile.SUPPLIER.create(pos, state);
    }
    
    @Override
    protected void buildStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PORT_DIRECTION_ENUM_PROPERTY);
    }
    
    @Override
    public InteractionResult onUse(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
        if (handIn == InteractionHand.MAIN_HAND) {
            if (Util.isWrench(player.getMainHandItem().getItem())) {
                ReactorAccessPort.PortDirection direction = state.getValue(PORT_DIRECTION_ENUM_PROPERTY);
                direction = direction == INLET ? OUTLET : INLET;
                state = state.setValue(PORT_DIRECTION_ENUM_PROPERTY, direction);
                worldIn.setBlockAndUpdate(pos, state);
                if (!worldIn.isClientSide()) {
                    BlockEntity te = worldIn.getBlockEntity(pos);
                    if (te instanceof ReactorCoolantPortTile) {
                        ((ReactorCoolantPortTile) te).setDirection(direction);
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
        if (te instanceof ReactorCoolantPortTile) {
            ((ReactorCoolantPortTile) te).neighborChanged();
        }
    }
}
