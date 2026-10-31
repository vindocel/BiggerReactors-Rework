package biggerreactorsrework.multiblocks.heatexchanger.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import biggerreactorsrework.multiblocks.heatexchanger.blocks.HeatExchangerTerminalBlock;
import biggerreactorsrework.multiblocks.heatexchanger.containers.HeatExchangerTerminalContainer;
import biggerreactorsrework.multiblocks.heatexchanger.state.HeatExchangerState;
import biggerreactorsrework.core.client.gui.api.IHasUpdatableState;
import biggerreactorsrework.core.registry.RegisterTile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class HeatExchangerTerminalTile extends HeatExchangerBaseTile implements MenuProvider, IHasUpdatableState<HeatExchangerState> {
    
    @RegisterTile("heat_exchanger_terminal")
    public static final BlockEntityType.BlockEntitySupplier<HeatExchangerTerminalTile> SUPPLIER = new RegisterTile.Producer<>(HeatExchangerTerminalTile::new);
    
    public HeatExchangerTerminalTile(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state);
    }
    
    HeatExchangerState state = new HeatExchangerState(this);
    public biggerreactorsrework.core.fluids.FluidRenderSnapshot primaryView=biggerreactorsrework.core.fluids.FluidRenderSnapshot.EMPTY;
    public biggerreactorsrework.core.fluids.FluidRenderSnapshot secondaryView=biggerreactorsrework.core.fluids.FluidRenderSnapshot.EMPTY;
    @Override protected net.minecraft.nbt.CompoundTag writeNBT(){var tag=super.writeNBT();tag.put("primaryView",primaryView.save());tag.put("secondaryView",secondaryView.save());return tag;}
    @Override protected void readNBT(net.minecraft.nbt.CompoundTag tag){super.readNBT(tag);primaryView=biggerreactorsrework.core.fluids.FluidRenderSnapshot.load(tag.getCompoundOrEmpty("primaryView"));secondaryView=biggerreactorsrework.core.fluids.FluidRenderSnapshot.load(tag.getCompoundOrEmpty("secondaryView"));}
    @Override protected net.minecraft.nbt.CompoundTag getUpdateNBT(){return writeNBT();}
    @Override protected void handleUpdateNBT(net.minecraft.nbt.CompoundTag tag){readNBT(tag);}
    
    @Nonnull
    @Override
    public HeatExchangerState getState() {
        return state;
    }
    
    @Override
    public void updateState() {
        // TODO: trigger an actual update
        if (nullableController() == null) {
            return;
        }
        
        state.condenserIntakeOrigin=biggerreactorsrework.registries.CoolantComponents.origin(controller().condenserTank.fluidComponentsInTank(0));
        state.evaporatorExhaustOrigin=biggerreactorsrework.registries.CoolantComponents.origin(controller().evaporatorTank.fluidComponentsInTank(1));
        state.condenserTankSize = controller().condenserTank.perSideCapacity;
        
        state.condenserIntakeFluid = BuiltInRegistries.FLUID.getKey(controller().condenserTank.fluidTypeInTank(0)).toString();
        state.condenserIntakeFluidAmount = controller().condenserTank.fluidAmountInTank(0);
        
        state.condenserExhaustFluid = BuiltInRegistries.FLUID.getKey(controller().condenserTank.fluidTypeInTank(1)).toString();
        state.condenserExhaustFluidAmount = controller().condenserTank.fluidAmountInTank(1);
        
        
        state.evaporatorTankSize = controller().evaporatorTank.perSideCapacity;
        
        state.evaporatorIntakeFluid = BuiltInRegistries.FLUID.getKey(controller().evaporatorTank.fluidTypeInTank(0)).toString();
        state.evaporatorIntakeFluidAmount = controller().evaporatorTank.fluidAmountInTank(0);
        
        state.evaporatorExhaustFluid = BuiltInRegistries.FLUID.getKey(controller().evaporatorTank.fluidTypeInTank(1)).toString();
        state.evaporatorExhaustFluidAmount = controller().evaporatorTank.fluidAmountInTank(1);
        
        
        state.condenserChannelTemperature = controller().condenserHeatBody.temperature();
        state.condenserChannelFlowRate = controller().condenserTank.transitionedLastTick();
        
        state.evaporatorChannelTemperature = controller().evaporatorHeatBody.temperature();
        state.evaporatorChannelFlowRate = controller().evaporatorTank.transitionedLastTick();
    }
    
    @Override
    public Component getDisplayName() {
        return Component.translatable(HeatExchangerTerminalBlock.INSTANCE.getDescriptionId());
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, @Nonnull Inventory playerInventory, @Nonnull Player player) {
        return new HeatExchangerTerminalContainer(windowId, this.worldPosition, player);
    }
}
