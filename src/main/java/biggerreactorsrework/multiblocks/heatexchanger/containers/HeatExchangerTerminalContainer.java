package biggerreactorsrework.multiblocks.heatexchanger.containers;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import biggerreactorsrework.multiblocks.heatexchanger.blocks.HeatExchangerTerminalBlock;
import biggerreactorsrework.multiblocks.heatexchanger.tiles.HeatExchangerTerminalTile;
import biggerreactorsrework.core.client.gui.GuiSync;
import biggerreactorsrework.core.registry.ContainerSupplier;
import biggerreactorsrework.core.registry.RegisterContainer;

import javax.annotation.Nonnull;

@RegisterContainer(name = "heat_exchanger_terminal")
public class HeatExchangerTerminalContainer extends AbstractContainerMenu implements GuiSync.IGUIPacketProvider {

    @RegisterContainer.Type
    public static MenuType<HeatExchangerTerminalContainer> INSTANCE;
    @RegisterContainer.Supplier
    public static final ContainerSupplier SUPPLIER = HeatExchangerTerminalContainer::new;

    private Player player;
    private HeatExchangerTerminalTile tileEntity;

    public HeatExchangerTerminalContainer(int windowId, BlockPos blockPos, Player player) {
        super(INSTANCE, windowId);
        this.player = player;
        this.tileEntity = (HeatExchangerTerminalTile) player.level().getBlockEntity(blockPos);
        this.getGuiPacket();
    }

    /**
     * @return The current state of the machine.
     */
    @Override
    public GuiSync.IGUIPacket getGuiPacket() {
        return this.tileEntity.getState();
    }

    @Override
    public boolean stillValid(@Nonnull Player player) {
        assert tileEntity.getLevel() != null;
        return stillValid(ContainerLevelAccess.create(tileEntity.getLevel(), tileEntity.getBlockPos()),
                player, HeatExchangerTerminalBlock.INSTANCE);
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
