package biggerreactorsrework.multiblocks.reactor.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import biggerreactorsrework.multiblocks.reactor.blocks.ReactorTerminal;
import biggerreactorsrework.multiblocks.reactor.containers.ReactorTerminalContainer;
import biggerreactorsrework.multiblocks.reactor.state.ReactorState;
import biggerreactorsrework.core.client.gui.api.IHasUpdatableState;
import biggerreactorsrework.core.registry.RegisterTile;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class ReactorTerminalTile extends ReactorBaseTile implements MenuProvider, IHasUpdatableState<ReactorState> {
    
    @RegisterTile("reactor_terminal")
    public static final BlockEntityType.BlockEntitySupplier<ReactorTerminalTile> SUPPLIER = new RegisterTile.Producer<>(ReactorTerminalTile::new);
    
    public ReactorTerminalTile(BlockEntityType<?> TYPE, BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }
    
    public final ReactorState reactorState = new ReactorState(this);
    public String renderCold="minecraft:empty",renderHot="minecraft:empty",renderOrigin="";
    public double renderColdFill,renderHotFill;
    @Override protected net.minecraft.nbt.CompoundTag writeNBT(){
        var tag=super.writeNBT();tag.putString("renderCold",renderCold);tag.putString("renderHot",renderHot);tag.putString("renderOrigin",renderOrigin);
        tag.putDouble("renderColdFill",renderColdFill);tag.putDouble("renderHotFill",renderHotFill);return tag;
    }
    @Override protected void readNBT(net.minecraft.nbt.CompoundTag tag){
        super.readNBT(tag);renderCold=tag.getStringOr("renderCold", "");renderHot=tag.getStringOr("renderHot", "");renderOrigin=tag.getStringOr("renderOrigin", "");
        renderColdFill=tag.getDoubleOr("renderColdFill", 0D);renderHotFill=tag.getDoubleOr("renderHotFill", 0D);
    }
    @Override protected net.minecraft.nbt.CompoundTag getUpdateNBT(){return writeNBT();}
    @Override protected void handleUpdateNBT(net.minecraft.nbt.CompoundTag tag){readNBT(tag);}
    
    @Override
    public ReactorState getState() {
        this.updateState();
        return this.reactorState;
    }
    
    @Override
    public void updateState() {
        if (nullableController() != null) {
            controller().updateReactorState(reactorState);
        }
    }
    
    @Override
    public Component getDisplayName() {
        return Component.translatable(ReactorTerminal.INSTANCE.getDescriptionId());
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
        return new ReactorTerminalContainer(windowId, this.worldPosition, player);
    }
}
