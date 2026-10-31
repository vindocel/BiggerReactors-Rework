package biggerreactorsrework.multiblocks.battery.tiles;

import biggerreactorsrework.core.registry.RegisterTile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class BatteryCasingTile extends BatteryBaseTile {
    
    @RegisterTile("battery_casing")
    public static final BlockEntityType.BlockEntitySupplier<BatteryCasingTile> SUPPLIER = new RegisterTile.Producer<>(BatteryCasingTile::new);
    
    public BatteryCasingTile(BlockEntityType<?> TYPE, BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }
}
