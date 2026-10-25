package biggerreactorsrework.multiblocks.turbine.containers;

import biggerreactorsrework.core.client.gui.GuiSync;
import biggerreactorsrework.core.registry.ContainerSupplier;
import biggerreactorsrework.core.registry.RegisterContainer;
import biggerreactorsrework.multiblocks.turbine.tiles.TurbineRedstonePortTile;
import biggerreactorsrework.multiblocks.turbine.blocks.TurbineRedstonePort;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

@RegisterContainer(name="turbine_redstone_port")
public final class TurbineRedstonePortContainer extends AbstractContainerMenu implements GuiSync.IGUIPacketProvider {
    @RegisterContainer.Type public static MenuType<TurbineRedstonePortContainer> INSTANCE;
    @RegisterContainer.Supplier public static final ContainerSupplier SUPPLIER=TurbineRedstonePortContainer::new;
    public final TurbineRedstonePortTile tile;
    public TurbineRedstonePortContainer(int id,BlockPos pos,Player player){super(INSTANCE,id);tile=(TurbineRedstonePortTile)player.level().getBlockEntity(pos);}
    @Override public boolean stillValid(Player player){return tile!=null&&tile.getLevel()!=null&&stillValid(ContainerLevelAccess.create(tile.getLevel(),tile.getBlockPos()),player,TurbineRedstonePort.INSTANCE);}
    @Override public GuiSync.IGUIPacket getGuiPacket(){return tile;}
    @Override public void executeRequest(String name,Object data){tile.runRequest(name,data);}
    @Override public ItemStack quickMoveStack(Player player,int slot){return ItemStack.EMPTY;}
    @Override
    public BlockPos machinePos() {
        return this.tile.getBlockPos();
    }
}
