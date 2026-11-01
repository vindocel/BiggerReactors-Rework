package biggerreactorsrework.machine.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import biggerreactorsrework.machine.tiles.CyaniteReprocessorTile;
import biggerreactorsrework.core.registry.RegisterBlock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CyaniteReprocessor extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;

    @RegisterBlock(name = "cyanite_reprocessor", tileEntityClass = CyaniteReprocessorTile.class)
    public static final CyaniteReprocessor INSTANCE = new CyaniteReprocessor();

    public CyaniteReprocessor() {
        super(Properties.of()
                .mapColor(net.minecraft.world.level.material.MapColor.COLOR_GRAY)
                // Modified 2026-10-27: light follows the existing processing animation.
                .lightLevel(biggerreactorsrework.multiblocks.MachineLighting::emission)
                .sound(SoundType.STONE)
                .destroyTime(1.0F)
                .explosionResistance(1.0F)
                .requiresCorrectToolForDrops());
        this.registerDefaultState(this.getStateDefinition().any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ENABLED, Boolean.FALSE));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return CyaniteReprocessorTile.SUPPLIER.create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return (a, b, c, tile) -> {
            if (tile instanceof CyaniteReprocessorTile reprocessor) {
                reprocessor.tick();
            }
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING).add(ENABLED);
    }

    /**
     * Faces the player that places it.
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Nonnull
    @Override
    protected ItemInteractionResult useItemOn(@Nonnull ItemStack stack, @Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof CyaniteReprocessorTile tile) {
            return tile.onItemUse(stack, level, player, hand);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Nonnull
    @Override
    protected InteractionResult useWithoutItem(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof CyaniteReprocessorTile tile) {
            if (!level.isClientSide) {
                player.openMenu(tile, pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CyaniteReprocessorTile tile) {
            tile.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
