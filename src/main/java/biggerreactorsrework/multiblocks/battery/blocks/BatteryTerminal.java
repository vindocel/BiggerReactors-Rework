package biggerreactorsrework.multiblocks.battery.blocks;

import biggerreactorsrework.core.multiblock.IAssemblyStateBlock;
import biggerreactorsrework.core.registry.RegisterBlock;
import biggerreactorsrework.multiblocks.battery.tiles.BatteryTerminalTile;
import biggerreactorsrework.core.multiblock.rectangular.IFaceDirectionBlock;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BatteryTerminal extends BatteryBaseBlock implements IAssemblyStateBlock, IFaceDirectionBlock {
    
    @RegisterBlock(name = "battery_terminal", tileEntityClass = BatteryTerminalTile.class)
    public static final BatteryTerminal INSTANCE = new BatteryTerminal();
    
    @Override
    public boolean usesBatteryState() {
        return true;
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return BatteryTerminalTile.SUPPLIER.create(pos, state);
    }
}
