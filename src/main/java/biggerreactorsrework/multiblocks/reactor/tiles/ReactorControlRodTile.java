package biggerreactorsrework.multiblocks.reactor.tiles;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import biggerreactorsrework.multiblocks.reactor.blocks.ReactorControlRod;
import biggerreactorsrework.multiblocks.reactor.containers.ReactorControlRodContainer;
import biggerreactorsrework.multiblocks.reactor.state.ReactorControlRodState;
import biggerreactorsrework.core.client.gui.api.IHasUpdatableState;
import biggerreactorsrework.core.registry.RegisterTile;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class ReactorControlRodTile extends ReactorBaseTile implements MenuProvider, IHasUpdatableState<ReactorControlRodState> {
    
    @RegisterTile("reactor_control_rod")
    public static final BlockEntityType.BlockEntitySupplier<ReactorControlRodTile> SUPPLIER = new RegisterTile.Producer<>(ReactorControlRodTile::new);
    
    public ReactorControlRodTile(BlockEntityType<?> TYPE, BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }
    
    public final ReactorControlRodState reactorControlRodState = new ReactorControlRodState(this);
    
    @Override
    public ReactorControlRodState getState() {
        this.updateState();
        return this.reactorControlRodState;
    }
    
    @Override
    public void updateState() {
        reactorControlRodState.name = name;
        reactorControlRodState.insertionLevel = insertion;
    }
    
    @Override
    
    public Component getDisplayName() {
        return Component.translatable(ReactorControlRod.INSTANCE.getDescriptionId());
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
        return new ReactorControlRodContainer(windowId, this.worldPosition, player);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void runRequest(String requestName, Object requestData) {
        if (nullableController() == null) {
            return;
        }
        
        // Change the insertion level of the rod.
        if (requestName.equals("changeInsertionLevel")) {
            // Modified 2026-10-26: reject malformed/nonfinite client commands before mutation.
            if (!(requestData instanceof Pair<?, ?> dataPair) || !(dataPair.getFirst() instanceof Double delta)
                    || !Double.isFinite(delta) || !(dataPair.getSecond() instanceof Boolean all)) return;
            double newLevel = this.insertion + delta;
            newLevel = Math.max(0, Math.min(100, newLevel));
            if (all) {
                controller().setAllControlRodLevels(newLevel);
            } else {
                setInsertion(newLevel);
                controller().updateControlRodLevels();
            }
        }
        
        // Set the name for the control rod.
        if (requestName.equals("setName")) {
            if (requestData instanceof String name && name.length() <= 128) this.setName(name);
        }
        
        super.runRequest(requestName, requestData);
    }
    
    private double insertion = 0;
    
    public void setInsertion(double newLevel) {
        // Modified 2026-10-30: persist executed control changes and validate saved settings.
        if (!Double.isFinite(newLevel)) {
            return;
        }
        if (newLevel < 0) {
            newLevel = 0;
        }
        if (newLevel > 100) {
            newLevel = 100;
        }
        if (insertion != newLevel) { insertion = newLevel; setChanged(); }
    }
    
    public double getInsertion() {
        return insertion;
    }
    
    // TODO: What should the default control rod name be? I think it should be Chris Houlihan...
    private String name = "";
    
    public void setName(String name) {
        if (name.length() <= 128 && !this.name.equals(name)) { this.name = name; setChanged(); }
    }
    
    public String getName() {
        return name;
    }
    
    @Override
    protected CompoundTag writeNBT() {
        CompoundTag compound = super.writeNBT();
        compound.putDouble("insertion", insertion);
        compound.putString("name", name);
        return compound;
    }
    
    @Override
    protected void readNBT(CompoundTag compound) {
        super.readNBT(compound);
        if (compound.contains("insertion")) {
            insertion = Math.clamp(biggerreactorsrework.core.util.SavedValues.nonNegative(compound.getDoubleOr("insertion", 0D), 0), 0, 100);
        }
        if (compound.contains("name")) {
            name = compound.getStringOr("name", "");
            if (name.length() > 128) name = name.substring(0, 128);
        }
    }
}
