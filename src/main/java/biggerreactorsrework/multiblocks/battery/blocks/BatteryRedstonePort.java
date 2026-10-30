package biggerreactorsrework.multiblocks.battery.blocks;

import biggerreactorsrework.core.multiblock.IAssemblyStateBlock;
import biggerreactorsrework.core.multiblock.rectangular.IFaceDirectionBlock;
import biggerreactorsrework.core.registry.RegisterBlock;
import biggerreactorsrework.multiblocks.battery.tiles.BatteryRedstonePortTile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Added 2026-10-30: outward-facing, configurable battery charge sensor. */
public final class BatteryRedstonePort extends BatteryBaseBlock implements IAssemblyStateBlock, IFaceDirectionBlock {
    public static final BooleanProperty IS_LIT = BooleanProperty.create("is_lit");
    @RegisterBlock(name = "battery_redstone_port", tileEntityClass = BatteryRedstonePortTile.class)
    public static final BatteryRedstonePort INSTANCE = new BatteryRedstonePort();

    public BatteryRedstonePort() { registerDefaultState(defaultBlockState().setValue(IS_LIT, false)); }
    @Override protected void buildStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.buildStateDefinition(builder);
        builder.add(IS_LIT);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return BatteryRedstonePortTile.SUPPLIER.create(pos, state); }
    @Override public boolean isSignalSource(BlockState state) { return true; }
    @Override public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return level.getBlockEntity(pos) instanceof BatteryRedstonePortTile tile && tile.emitting(side) ? 15 : 0;
    }
    @Override public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) { return getSignal(state, level, pos, side); }
}
