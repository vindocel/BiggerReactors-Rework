package biggerreactorsrework.multiblocks.battery.tiles;

import biggerreactorsrework.core.registry.RegisterTile;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BatteryGlassTile extends BatteryBaseTile {
    
    @RegisterTile("battery_glass")
    public static final BlockEntityType.BlockEntitySupplier<BatteryGlassTile> SUPPLIER = new RegisterTile.Producer<>(BatteryGlassTile::new);
    
    public BatteryGlassTile(BlockEntityType<?> TYPE, BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }
}
