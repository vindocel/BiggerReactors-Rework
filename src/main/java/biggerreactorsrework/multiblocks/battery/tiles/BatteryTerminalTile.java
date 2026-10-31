package biggerreactorsrework.multiblocks.battery.tiles;

import biggerreactorsrework.core.registry.RegisterTile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class BatteryTerminalTile extends BatteryBaseTile {
    
    @RegisterTile("battery_terminal")
    public static final BlockEntityType.BlockEntitySupplier<BatteryTerminalTile> SUPPLIER = new RegisterTile.Producer<>(BatteryTerminalTile::new);
    
    public BatteryTerminalTile(BlockEntityType<?> TYPE, BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }
}
