package biggerreactorsrework.multiblocks.battery.blocks;

import biggerreactorsrework.core.multiblock.IAssemblyStateBlock;
import biggerreactorsrework.core.registry.RegisterBlock;
import biggerreactorsrework.multiblocks.battery.tiles.BatteryTerminalTile;
//? if =26.1.2 || =26.3 {
import biggerreactorsrework.core.multiblock.rectangular.IFaceDirectionBlock;
import net.minecraft.core.BlockPos;
//?} else if =1.21.1 {
/*import biggerreactorsrework.core.multiblock.rectangular.IFaceDirectionBlock;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
*/
//?}
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

//? if =26.1.2 || =26.3 {
@ParametersAreNonnullByDefault
public class BatteryTerminal extends BatteryBaseBlock implements IAssemblyStateBlock, IFaceDirectionBlock {
//?} else if =1.21.1 {
/*@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BatteryTerminal extends BatteryBaseBlock implements IAssemblyStateBlock, IFaceDirectionBlock {
*/
//?}

//? if =26.1.2 || =26.3 {
    @RegisterBlock(name = "battery_terminal", tileEntityClass = BatteryTerminalTile.class)
    public static final BatteryTerminal INSTANCE = biggerreactorsrework.core.registry.RegistryIds.named("battery_terminal", () -> new BatteryTerminal());

    @Override
//?} else if =1.21.1 {
/*    @RegisterBlock(name = "battery_terminal", tileEntityClass = BatteryTerminalTile.class)
    public static final BatteryTerminal INSTANCE = new BatteryTerminal();

    @Override
*/
//?}
    public boolean usesBatteryState() {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return BatteryTerminalTile.SUPPLIER.create(pos, state);
    }
}
