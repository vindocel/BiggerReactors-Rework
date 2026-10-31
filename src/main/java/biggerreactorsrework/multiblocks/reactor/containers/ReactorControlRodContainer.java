package biggerreactorsrework.multiblocks.reactor.containers;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import biggerreactorsrework.multiblocks.reactor.blocks.ReactorControlRod;
import biggerreactorsrework.multiblocks.reactor.tiles.ReactorControlRodTile;
import biggerreactorsrework.core.client.gui.GuiSync;
import biggerreactorsrework.core.registry.ContainerSupplier;
import biggerreactorsrework.core.registry.RegisterContainer;

import javax.annotation.Nonnull;

@RegisterContainer(name = "reactor_control_rod")
public class ReactorControlRodContainer extends AbstractContainerMenu implements GuiSync.IGUIPacketProvider {

    @RegisterContainer.Type
    public static MenuType<ReactorControlRodContainer> INSTANCE;
    @RegisterContainer.Supplier
    public static final ContainerSupplier SUPPLIER = ReactorControlRodContainer::new;

    private Player player;
    private ReactorControlRodTile tileEntity;

    public ReactorControlRodContainer(int windowId, BlockPos blockPos, Player player) {
        super(INSTANCE, windowId);
        this.player = player;
        this.tileEntity = (ReactorControlRodTile) player.level().getBlockEntity(blockPos);
        this.getGuiPacket();
    }

    /**
     * @return The current state of the machine.
     */
    @Override
    public GuiSync.IGUIPacket getGuiPacket() {
        return this.tileEntity.reactorControlRodState;
    }

    @Override
    public boolean stillValid(@Nonnull Player player) {
        assert tileEntity.getLevel() != null;
        return stillValid(ContainerLevelAccess.create(tileEntity.getLevel(), tileEntity.getBlockPos()),
                player, ReactorControlRod.INSTANCE);
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
}
