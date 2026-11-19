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
public class TurbineRotorBladeTile extends TurbineBaseTile {
//?} else if =1.21.1 {
/*@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TurbineRotorBladeTile extends TurbineBaseTile {
*/
//?}

    @RegisterTile("turbine_rotor_blade")
    public static final BlockEntityType.BlockEntitySupplier<TurbineRotorBladeTile> SUPPLIER = new RegisterTile.Producer<>(TurbineRotorBladeTile::new);

    public TurbineRotorBladeTile(BlockEntityType<?> TYPE, BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }
}
