package biggerreactorsrework.multiblocks.battery.tiles;

//? if =26.1.2 || =26.3 {
import biggerreactorsrework.core.registry.RegisterTile;
import net.minecraft.core.BlockPos;
//?} else if =1.21.1 {
/*import biggerreactorsrework.core.registry.RegisterTile;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
*/
//?}
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;

//? if =26.1.2 || =26.3 {
@ParametersAreNonnullByDefault
public class BatteryCasingTile extends BatteryBaseTile {
//?} else if =1.21.1 {
/*@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BatteryCasingTile extends BatteryBaseTile {
*/
//?}

    @RegisterTile("battery_casing")
    public static final BlockEntityType.BlockEntitySupplier<BatteryCasingTile> SUPPLIER = new RegisterTile.Producer<>(BatteryCasingTile::new);

    public BatteryCasingTile(BlockEntityType<?> TYPE, BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }
}
