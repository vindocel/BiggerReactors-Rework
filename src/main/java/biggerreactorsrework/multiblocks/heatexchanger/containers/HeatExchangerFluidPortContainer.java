package biggerreactorsrework.multiblocks.heatexchanger.containers;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import biggerreactorsrework.multiblocks.heatexchanger.blocks.HeatExchangerFluidPortBlock;
import biggerreactorsrework.multiblocks.heatexchanger.tiles.HeatExchangerFluidPortTile;
import biggerreactorsrework.core.client.gui.GuiSync;
import biggerreactorsrework.core.registry.ContainerSupplier;
import biggerreactorsrework.core.registry.RegisterContainer;

import javax.annotation.Nonnull;

@RegisterContainer(name = "heat_exchanger_fluid_port")
public class HeatExchangerFluidPortContainer extends AbstractContainerMenu implements GuiSync.IGUIPacketProvider {
    
    @RegisterContainer.Type
    public static MenuType<HeatExchangerFluidPortContainer> INSTANCE;
    @RegisterContainer.Supplier
    public static final ContainerSupplier SUPPLIER = HeatExchangerFluidPortContainer::new;
    
    private Player player;
    private HeatExchangerFluidPortTile tileEntity;
    
    public HeatExchangerFluidPortContainer(int windowId, BlockPos blockPos, Player player) {
        super(INSTANCE, windowId);
        this.player = player;
        this.tileEntity = (HeatExchangerFluidPortTile) player.level().getBlockEntity(blockPos);
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
                player, HeatExchangerFluidPortBlock.INSTANCE);
    }
    
    @Override
    public void executeRequest(String requestName, Object requestData) {
        assert tileEntity.getLevel() != null;
        if (tileEntity.getLevel().isClientSide) {
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
}
