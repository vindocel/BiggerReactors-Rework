//? if =26.1.2 || =26.3 {
package biggerreactorsrework.multiblocks.turbine.tiles;

import net.minecraft.core.BlockPos;
//?} else if =1.21.1 {
/*package biggerreactorsrework.multiblocks.turbine.tiles;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
*/
//?}
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import biggerreactorsrework.core.registry.RegisterTile;

import javax.annotation.ParametersAreNonnullByDefault;

//? if =26.1.2 || =26.3 {
@ParametersAreNonnullByDefault
public class TurbineCasingTile extends TurbineBaseTile {
//?} else if =1.21.1 {
/*@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TurbineCasingTile extends TurbineBaseTile {
*/
//?}

    @RegisterTile("turbine_casing")
    public static final BlockEntityType.BlockEntitySupplier<TurbineCasingTile> SUPPLIER = new RegisterTile.Producer<>(TurbineCasingTile::new);

    public TurbineCasingTile(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state);
    }
}
