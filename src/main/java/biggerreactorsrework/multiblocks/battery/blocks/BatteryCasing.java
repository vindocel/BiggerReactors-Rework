package biggerreactorsrework.multiblocks.battery.blocks;

import biggerreactorsrework.core.multiblock.IAssemblyStateBlock;
import biggerreactorsrework.core.registry.RegisterBlock;
import biggerreactorsrework.multiblocks.battery.tiles.BatteryCasingTile;
//? if =26.1.2 || =26.3 {
import biggerreactorsrework.core.multiblock.rectangular.IAxisPositionBlock;
import net.minecraft.core.BlockPos;
//?} else if =1.21.1 {
/*import biggerreactorsrework.core.multiblock.rectangular.IAxisPositionBlock;
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
public class BatteryCasing extends BatteryBaseBlock implements IAssemblyStateBlock, IAxisPositionBlock {
//?} else if =1.21.1 {
/*@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BatteryCasing extends BatteryBaseBlock implements IAssemblyStateBlock, IAxisPositionBlock {
*/
//?}

//? if =26.1.2 || =26.3 {
    @RegisterBlock(name = "battery_casing", tileEntityClass = BatteryCasingTile.class)
    public static final BatteryCasing INSTANCE = biggerreactorsrework.core.registry.RegistryIds.named("battery_casing", () -> new BatteryCasing());

    @Override
//?} else if =1.21.1 {
/*    @RegisterBlock(name = "battery_casing", tileEntityClass = BatteryCasingTile.class)
    public static final BatteryCasing INSTANCE = new BatteryCasing();

    @Override
*/
//?}
    public boolean isGoodForFrame() {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return BatteryCasingTile.SUPPLIER.create(pos, state);
    }
}
