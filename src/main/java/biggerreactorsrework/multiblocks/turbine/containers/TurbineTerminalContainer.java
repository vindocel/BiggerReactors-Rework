package biggerreactorsrework.multiblocks.turbine.containers;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import biggerreactorsrework.multiblocks.turbine.blocks.TurbineTerminal;
import biggerreactorsrework.multiblocks.turbine.tiles.TurbineTerminalTile;
import biggerreactorsrework.core.client.gui.GuiSync;
import biggerreactorsrework.core.registry.ContainerSupplier;
import biggerreactorsrework.core.registry.RegisterContainer;

import javax.annotation.Nullable;

@RegisterContainer(name = "turbine_terminal")
public class TurbineTerminalContainer extends AbstractContainerMenu implements GuiSync.IGUIPacketProvider {
    
    @RegisterContainer.Type
    public static MenuType<TurbineTerminalContainer> INSTANCE;
    @RegisterContainer.Supplier
    public static final ContainerSupplier SUPPLIER = TurbineTerminalContainer::new;
    
    private Player player;
    private TurbineTerminalTile tileEntity;
    
    public TurbineTerminalContainer(int windowId, BlockPos blockPos, Player player) {
        super(INSTANCE, windowId);
        this.player = player;
        this.tileEntity = (TurbineTerminalTile) player.level().getBlockEntity(blockPos);
        this.getGuiPacket();
    }
    
    /**
     * @return The current state of the machine.
     */
    @Nullable
    @Override
    public GuiSync.IGUIPacket getGuiPacket() {
        return this.tileEntity.turbineState;
    }
    
    @Override
    public boolean stillValid(Player player) {
        assert tileEntity.getLevel() != null;
        return stillValid(ContainerLevelAccess.create(tileEntity.getLevel(), tileEntity.getBlockPos()),
                player, TurbineTerminal.INSTANCE);
    }
    
    @Override
    public void executeRequest(String requestName, Object requestData) {
        assert tileEntity.getLevel() != null;
        if (tileEntity.getLevel().isClientSide()) {
            runRequest(requestName, requestData);
            return;
        }
        
        tileEntity.runRequest(requestName, requestData);
    }
    @Override
    public ItemStack quickMoveStack(Player p_38941_, int p_38942_) {
        return ItemStack.EMPTY;
    }
    @Override
    public BlockPos machinePos() {
        return this.tileEntity.getBlockPos();
    }
    @Override public java.util.List<java.util.Map<String,Object>> statistics() {
        return tileEntity.nullableController()==null?java.util.List.of():tileEntity.controller().statistics();
    }
}
