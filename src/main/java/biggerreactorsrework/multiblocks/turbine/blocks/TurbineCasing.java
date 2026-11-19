//? if =26.1.2 || =26.3 {
package biggerreactorsrework.multiblocks.turbine.blocks;

import net.minecraft.core.BlockPos;
//?} else if =1.21.1 {
/*package biggerreactorsrework.multiblocks.turbine.blocks;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
*/
//?}
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import biggerreactorsrework.multiblocks.turbine.tiles.TurbineCasingTile;
import biggerreactorsrework.core.multiblock.IAssemblyStateBlock;
import biggerreactorsrework.core.multiblock.rectangular.IAxisPositionBlock;
import biggerreactorsrework.core.registry.RegisterBlock;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

//? if =26.1.2 || =26.3 {
@ParametersAreNonnullByDefault
public class TurbineCasing extends TurbineBaseBlock implements IAssemblyStateBlock, IAxisPositionBlock {
//?} else if =1.21.1 {
/*@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TurbineCasing extends TurbineBaseBlock implements IAssemblyStateBlock, IAxisPositionBlock {
*/
//?}

//? if =26.1.2 || =26.3 {
    @RegisterBlock(name = "turbine_casing", tileEntityClass = TurbineCasingTile.class)
    public static final TurbineCasing INSTANCE = biggerreactorsrework.core.registry.RegistryIds.named("turbine_casing", () -> new TurbineCasing());

    public TurbineCasing() {
//?} else if =1.21.1 {
/*    @RegisterBlock(name = "turbine_casing", tileEntityClass = TurbineCasingTile.class)
    public static final TurbineCasing INSTANCE = new TurbineCasing();

    public TurbineCasing() {
*/
//?}
        super();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return TurbineCasingTile.SUPPLIER.create(pos, state);
    }

    @Override
    public boolean isGoodForFrame() {
        return true;
    }
}
