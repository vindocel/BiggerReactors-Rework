package biggerreactorsrework.multiblocks.heatexchanger.tiles;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import biggerreactorsrework.core.transfer.SimpleTank;
import biggerreactorsrework.multiblocks.heatexchanger.blocks.HeatExchangerFluidPortBlock;
import biggerreactorsrework.multiblocks.heatexchanger.containers.HeatExchangerFluidPortContainer;
import biggerreactorsrework.multiblocks.heatexchanger.state.HeatExchangerFluidPortState;
import biggerreactorsrework.core.client.gui.api.IHasUpdatableState;
import biggerreactorsrework.core.fluids.FluidHandlerWrapper;
import biggerreactorsrework.core.fluids.IFluidTankHandler;
import biggerreactorsrework.core.multiblock.common.IEventMultiblock;
import biggerreactorsrework.core.multiblock.validated.IValidatedMultiblock;
import biggerreactorsrework.core.registry.CapabilityRegistration;
import biggerreactorsrework.core.registry.RegisterCapability;
import biggerreactorsrework.core.registry.RegisterTile;
import biggerreactorsrework.core.util.BlockStates;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import static biggerreactorsrework.multiblocks.heatexchanger.blocks.HeatExchangerFluidPortBlock.CONDENSER;
import static biggerreactorsrework.core.util.BlockStates.PORT_DIRECTION;


@ParametersAreNonnullByDefault
public class HeatExchangerFluidPortTile extends HeatExchangerBaseTile implements IFluidTankHandler, IEventMultiblock.AssemblyStateTransition.OnAssembly, IEventMultiblock.AssemblyStateTransition.OnDisassembly, MenuProvider, IHasUpdatableState<HeatExchangerFluidPortState> {
    // Modified 2026-10-30: refresh output handlers after neighbor capability changes.
    private final biggerreactorsrework.core.capability.NeighborCapability<biggerreactorsrework.core.transfer.FluidTanks, ?> outputCapability =
            biggerreactorsrework.core.capability.NeighborCapability.fluids(this);
    
    public long lastCheckedTick;
    
    @RegisterTile("heat_exchanger_fluid_port")
    public static final BlockEntityType.BlockEntitySupplier<HeatExchangerFluidPortTile> SUPPLIER = new RegisterTile.Producer<>(HeatExchangerFluidPortTile::new);
    
    @RegisterCapability
    private static final CapabilityRegistration FLUID_HANDLER_CAP_REGISTRATION = CapabilityRegistration.tileFluids(HeatExchangerFluidPortTile.class);
    
    public HeatExchangerFluidPortTile(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state);
    }
    
    
    
    private IFluidTankHandler HETank;
    
    public void setHETank(IFluidTankHandler HETank) {
        this.HETank = HETank;
    }
    
    private boolean inlet = true;
    private boolean condenser = true;
    
    public void setInlet(boolean inlet) {
        assert level != null;
        this.inlet = inlet;
        level.setBlock(this.getBlockPos(), this.getBlockState().setValue(PORT_DIRECTION, inlet), 3);
        setChanged();
    }
    
    public void setInletOtherOutlet(boolean inlet) {
        controller().setInletPort(this, inlet);
    }
    
    public boolean isInlet() {
        return inlet;
    }
    
    public void setCondenser(boolean condenser) {
        this.condenser = condenser;
        assert level != null;
        level.setBlock(this.getBlockPos(), this.getBlockState().setValue(CONDENSER, condenser), 3);
    }
    
    public boolean isCondenser() {
        return condenser;
    }
    
    @Override
    public Object transferSnapshot() {
        return HETank == null ? null : HETank.transferSnapshot();
    }

    @Override
    public void restoreTransferSnapshot(Object snapshot) {
        if (HETank != null) {
            HETank.restoreTransferSnapshot(snapshot);
        }
    }

    @Override
    public int tankCount() {
        if (HETank == null) {
            return 0;
        }
        return HETank.tankCount();
    }
    
    @Override
    public long tankCapacity(int tank) {
        if (HETank == null) {
            return 0;
        }
        return HETank.tankCapacity(tank);
    }
    
    
    @Override
    public Fluid fluidTypeInTank(int tank) {
        if (HETank == null) {
            return Fluids.EMPTY;
        }
        return HETank.fluidTypeInTank(tank);
    }
    
    @Nullable
    @Override
    public DataComponentPatch fluidComponentsInTank(int tank) {
        if (HETank == null) {
            return DataComponentPatch.EMPTY;
        }
        return HETank.fluidComponentsInTank(tank);
    }
    
    @Override
    public long fluidAmountInTank(int tank) {
        if (HETank == null) {
            return 0;
        }
        return HETank.fluidAmountInTank(tank);
    }
    
    @Override
    public boolean fluidValidForTank(int tank, Fluid fluid) {
        if (HETank == null) {
            return false;
        }
        return HETank.fluidValidForTank(tank, fluid);
    }
    @Override public boolean fluidValidForTank(int tank,Fluid fluid,DataComponentPatch components){return HETank!=null && HETank.fluidValidForTank(tank,fluid,components);}
    
    @Override
    public long fill(Fluid fluid, DataComponentPatch components, long amount, boolean simulate) {
        if (HETank == null || !inlet) {
            return 0;
        }
        return HETank.fill(fluid, components, amount, simulate);
    }
    
    @Override
    public long drain(Fluid fluid, DataComponentPatch components, long amount, boolean simulate) {
        if (HETank == null || inlet) {
            return 0;
        }
        return HETank.drain(fluid, components, amount, simulate);
    }
    
    
    public long pushFluid() {
        neighborChanged();
        if (!connected || inlet) {
            return 0;
        }
        if (handler != null) {
            Fluid fluid = HETank.fluidTypeInTank(1);
            var components = HETank.fluidComponentsInTank(1);
            long amount = HETank.fluidAmountInTank(1);
            amount = HETank.drain(fluid, components, amount, true);
            amount = handler.fill(fluid, components, amount, false);
            amount = HETank.drain(fluid, components, amount, false);
            return amount;
        } else {
            handler = null;
            connected = false;
        }
        return 0;
    }
    
    private boolean connected = false;
    Direction outputDirection = null;
    IFluidTankHandler handler = null;
    SimpleTank EMPTY_TANK = new SimpleTank(0);
    
    @SuppressWarnings("DuplicatedCode")
    public void neighborChanged() {
        handler = null;
        if (outputDirection == null) {
            connected = false;
            return;
        }
        assert level != null;
        final var outputCap = outputCapability.get(outputDirection);
        connected = false;
        if (outputCap != null) {
            connected = true;
            handler = FluidHandlerWrapper.wrap(outputCap);
        }
    }
    
    @Override
    protected void readNBT(CompoundTag compound) {
        super.readNBT(compound);
        inlet = compound.getBooleanOr("inlet", false);
    }
    
    
    @Override
    protected CompoundTag writeNBT() {
        CompoundTag nbt = super.writeNBT();
        nbt.putBoolean("inlet", inlet);
        return nbt;
    }
    
    @Override
    public void onAssemblyStateTransition(IValidatedMultiblock.AssemblyState oldState, IValidatedMultiblock.AssemblyState newState) {
        OnAssembly.super.onAssemblyStateTransition(oldState, newState);
        OnDisassembly.super.onAssemblyStateTransition(oldState, newState);
    }
    
    @Override
    public void onAssembly() {
        outputDirection = getBlockState().getValue(BlockStates.FACING);
        neighborChanged();
    }
    
    @Override
    public void onDisassembly() {
        outputDirection = null;
        HETank = null;
        neighborChanged();
    }
    
    private final HeatExchangerFluidPortState state = new HeatExchangerFluidPortState(this);
    
    @Override
    public HeatExchangerFluidPortState getState() {
        return state;
    }
    
    @Override
    public void updateState() {
        state.direction = isInlet();
        state.condenser = isCondenser();
    }
    
    @Override
    public Component getDisplayName() {
        return Component.translatable(HeatExchangerFluidPortBlock.INSTANCE.getDescriptionId());
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
        return new HeatExchangerFluidPortContainer(windowId, this.worldPosition, player);
    }

    @Override
    public void runRequest(String requestName, Object requestData) {
        if (requestName.equals("setDirection")) {
            // Modified 2026-10-30: reject invalid command types and domains before mutation.
            if (!(requestData instanceof Integer value) || value < 0 || value > 1) return;
            int direction = (Integer) requestData;
            setInletOtherOutlet(direction == 0);
        }

        super.runRequest(requestName, requestData);
    }
}
