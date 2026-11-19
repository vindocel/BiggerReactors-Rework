//? if =26.1.2 || =26.3 {
package biggerreactorsrework.multiblocks.heatexchanger.tiles;

import net.minecraft.core.BlockPos;
//?} else if =1.21.1 {
/*package biggerreactorsrework.multiblocks.heatexchanger.tiles;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
*/
//?}
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import biggerreactorsrework.core.registry.RegisterTile;

//? if =26.1.2 || =26.3 {
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
//?} else if =1.21.1 {
/*import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
*/
//?}
public class HeatExchangerCasingTile extends HeatExchangerBaseTile {

    @RegisterTile("heat_exchanger_casing")
    public static final BlockEntityType.BlockEntitySupplier<HeatExchangerCasingTile> SUPPLIER = new RegisterTile.Producer<>(HeatExchangerCasingTile::new);

    public HeatExchangerCasingTile(BlockEntityType<HeatExchangerCasingTile> TYPE, BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }
}
