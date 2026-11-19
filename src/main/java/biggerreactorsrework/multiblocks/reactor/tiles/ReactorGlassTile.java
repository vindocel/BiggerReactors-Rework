//? if =26.1.2 || =26.3 {
package biggerreactorsrework.multiblocks.reactor.tiles;

import net.minecraft.core.BlockPos;
//?} else if =1.21.1 {
/*package biggerreactorsrework.multiblocks.reactor.tiles;

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
public class ReactorGlassTile extends ReactorBaseTile {

    @RegisterTile("reactor_glass")
    public static final BlockEntityType.BlockEntitySupplier<ReactorGlassTile> SUPPLIER = new RegisterTile.Producer<>(ReactorGlassTile::new);

    public ReactorGlassTile(BlockEntityType<?> TYPE, BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }
}
