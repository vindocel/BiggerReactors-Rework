//? if =26.1.2 || =26.3 {
package biggerreactorsrework.multiblocks.reactor.blocks;

import net.minecraft.core.BlockPos;
//?} else if =1.21.1 {
/*package biggerreactorsrework.multiblocks.reactor.blocks;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
*/
//?}
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import biggerreactorsrework.multiblocks.reactor.tiles.ReactorCasingTile;
import biggerreactorsrework.core.multiblock.IAssemblyStateBlock;
import biggerreactorsrework.core.multiblock.rectangular.IAxisPositionBlock;
import biggerreactorsrework.core.registry.RegisterBlock;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

//? if =26.1.2 || =26.3 {
@ParametersAreNonnullByDefault
public class ReactorCasing extends ReactorBaseBlock implements IAssemblyStateBlock, IAxisPositionBlock {
//?} else if =1.21.1 {
/*@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ReactorCasing extends ReactorBaseBlock implements IAssemblyStateBlock, IAxisPositionBlock {
*/
//?}

//? if =26.1.2 || =26.3 {
    @RegisterBlock(name = "reactor_casing", tileEntityClass = ReactorCasingTile.class)
    public static final ReactorCasing INSTANCE = biggerreactorsrework.core.registry.RegistryIds.named("reactor_casing", () -> new ReactorCasing());

    public ReactorCasing() {
//?} else if =1.21.1 {
/*    @RegisterBlock(name = "reactor_casing", tileEntityClass = ReactorCasingTile.class)
    public static final ReactorCasing INSTANCE = new ReactorCasing();

    public ReactorCasing() {
*/
//?}
        super();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ReactorCasingTile.SUPPLIER.create(pos, state);
    }

    @Override
    public boolean isGoodForFrame() {
        return true;
    }
}
