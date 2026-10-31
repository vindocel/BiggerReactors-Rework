package biggerreactorsrework.multiblocks.battery.tiles;

import biggerreactorsrework.core.modular.tile.ModularTile;
import biggerreactorsrework.core.multiblock.IMultiblockTile;
import biggerreactorsrework.core.multiblock.common.IPersistentMultiblockTile;
import biggerreactorsrework.core.multiblock.rectangular.IRectangularMultiblockTile;
import biggerreactorsrework.core.multiblock.touching.ITouchingMultiblockTile;
import biggerreactorsrework.multiblocks.battery.BatteryMultiblockController;
import biggerreactorsrework.multiblocks.battery.blocks.BatteryBaseBlock;
import biggerreactorsrework.multiblocks.battery.blocks.BatteryTerminal;
import biggerreactorsrework.multiblocks.battery.containers.BatteryTerminalContainer;
import biggerreactorsrework.multiblocks.battery.state.BatteryState;
import biggerreactorsrework.core.multiblock.validated.IValidatedMultiblock;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class BatteryBaseTile extends ModularTile implements IMultiblockTile<BatteryBaseTile, BatteryBaseBlock, BatteryMultiblockController>,
        IRectangularMultiblockTile<BatteryBaseTile, BatteryBaseBlock, BatteryMultiblockController>,
        IPersistentMultiblockTile<BatteryBaseTile, BatteryBaseBlock, BatteryMultiblockController>,
        ITouchingMultiblockTile<BatteryBaseTile, BatteryBaseBlock, BatteryMultiblockController> {
    public BatteryBaseTile(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state);
    }
    
    @Override
    public final BatteryMultiblockController createController() {
        if (level == null) {
            throw new IllegalStateException("Attempt to create controller with null world");
        }
        return new BatteryMultiblockController(level);
    }
    
    private BatteryState batteryState;
    
    public BatteryState batteryState() {
        if (batteryState == null) {
            batteryState = new BatteryState(this);
        }
        return batteryState;
    }
    
    public void updateBatteryState() {
        if (nullableController() != null) {
            controller().updateDataPacket(batteryState());
        }
    }
    
    public boolean isBatteryAssembled() {
        return nullableController() != null && controller().assemblyState() == IValidatedMultiblock.AssemblyState.ASSEMBLED;
    }
    
    public void openTerminal(Player player) {
        player.openMenu(new SimpleMenuProvider((windowId, inventory, menuPlayer) -> new BatteryTerminalContainer(windowId, worldPosition, menuPlayer),
                Component.translatable(BatteryTerminal.INSTANCE.getDescriptionId())), worldPosition);
    }
    
    public void runRequest(String requestName, Object requestData) {
        if (nullableController() != null) {
            controller().runRequest(requestName, requestData);
        }
    }
}
