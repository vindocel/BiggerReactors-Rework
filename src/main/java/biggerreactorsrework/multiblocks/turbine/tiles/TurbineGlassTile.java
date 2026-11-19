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
public class TurbineGlassTile extends TurbineBaseTile {
//?} else if =1.21.1 {
/*@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TurbineGlassTile extends TurbineBaseTile {
*/
//?}

    @RegisterTile("turbine_glass")
    public static final BlockEntityType.BlockEntitySupplier<TurbineGlassTile> SUPPLIER = new RegisterTile.Producer<>(TurbineGlassTile::new);

    public TurbineGlassTile(BlockEntityType<?> TYPE, BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }
}
