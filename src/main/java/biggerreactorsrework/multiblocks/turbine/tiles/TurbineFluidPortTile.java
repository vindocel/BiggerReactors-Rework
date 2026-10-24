package biggerreactorsrework.multiblocks.turbine.tiles;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.MethodsReturnNonnullByDefault;
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
import biggerreactorsrework.multiblocks.turbine.blocks.TurbineFluidPort;
import biggerreactorsrework.multiblocks.turbine.containers.TurbineFluidPortContainer;
import biggerreactorsrework.multiblocks.turbine.simulation.ITurbineFluidTank;
import biggerreactorsrework.multiblocks.turbine.state.TurbineFluidPortState;
import biggerreactorsrework.core.fluids.FluidHandlerWrapper;
import biggerreactorsrework.core.fluids.IFluidTankHandler;
import biggerreactorsrework.core.client.gui.api.IHasUpdatableState;
import biggerreactorsrework.core.multiblock.common.IEventMultiblock;
import biggerreactorsrework.core.multiblock.validated.IValidatedMultiblock;
import biggerreactorsrework.core.registry.CapabilityRegistration;
import biggerreactorsrework.core.registry.RegisterCapability;
import biggerreactorsrework.core.registry.RegisterTile;
import biggerreactorsrework.core.util.BlockStates;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import static biggerreactorsrework.multiblocks.turbine.blocks.TurbineFluidPort.PortDirection.*;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TurbineFluidPortTile extends TurbineBaseTile implements IFluidTankHandler, MenuProvider, IHasUpdatableState<TurbineFluidPortState>, IEventMultiblock.AssemblyStateTransition.OnAssembly, IEventMultiblock.AssemblyStateTransition.OnDisassembly {
    
    @RegisterTile("turbine_fluid_port")
    public static final BlockEntityType.BlockEntitySupplier<TurbineFluidPortTile> SUPPLIER = new RegisterTile.Producer<>(TurbineFluidPortTile::new);
    
    @RegisterCapability
    private static final CapabilityRegistration FLUID_HANDLER_CAP_REGISTRATION = CapabilityRegistration.tileCap(Capabilities.FluidHandler.BLOCK, TurbineFluidPortTile.class);
    
    public TurbineFluidPortTile(BlockEntityType<?> TYPE, BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }

    // TODO: mek gas
//    private static final Capability<IGasHandler> GAS_HANDLER_CAPABILITY = CapabilityManager.get(new CapabilityToken<>(){});
//
//    @Override
//    public <T> LazyOptional<T> capability(Capability<T> cap, @Nullable Direction side) {
//        if (cap == ForgeCapabilities.FLUID_HANDLER) {
//            return LazyOptional.of(() -> this).cast();
//        }
//        if (cap == GAS_HANDLER_CAPABILITY) {
//            return LazyOptional.of(() -> MekanismGasWrappers.wrap(this)).cast();
//        }
//        return super.capability(cap, side);
//    }
    
    private ITurbineFluidTank transitionTank;
    
    @Override
    public int tankCount() {
        if (transitionTank == null) {
            return 0;
        }
        return transitionTank.tankCount();
    }
    
    @Override
    public long tankCapacity(int tank) {
        if (transitionTank == null) {
            return 0;
        }
        return transitionTank.tankCapacity(tank);
    }
    
    @Override
    public Fluid fluidTypeInTank(int tank) {
        if (transitionTank == null) {
            return Fluids.EMPTY;
        }
        return transitionTank.fluidTypeInTank(tank);
    }
    
    @Nullable
    @Override
    public DataComponentPatch fluidComponentsInTank(int tank) {
        if (transitionTank == null) {
            return DataComponentPatch.EMPTY;
        }
        return transitionTank.fluidComponentsInTank(tank);
    }
    
    @Override
    public long fluidAmountInTank(int tank) {
        if (transitionTank == null) {
            return 0;
        }
        return transitionTank.fluidAmountInTank(tank);
    }
    
    @Override
    public boolean fluidValidForTank(int tank, Fluid fluid) {
        if (transitionTank == null) {
            return false;
        }
        return transitionTank.fluidValidForTank(tank, fluid);
    }
    
    @Override
    public long fill(Fluid fluid, DataComponentPatch components, long amount, boolean simulate) {
        if (transitionTank == null || direction != INLET) {
            return 0;
        }
        return transitionTank.fill(fluid, DataComponentPatch.EMPTY, amount, simulate);
    }
    
    @Override
    public long drain(Fluid fluid, DataComponentPatch components, long amount, boolean simulate) {
        if (transitionTank == null || direction == INLET) {
            return 0;
        }
        return transitionTank.drain(fluid, DataComponentPatch.EMPTY, amount, simulate);
    }
    
    public long pushFluid() {
        if (!connected || direction == INLET) {
            return 0;
        }
        if (handler != null) {
            Fluid fluid = transitionTank.liquidType();
            long amount = transitionTank.liquidAmount();
            amount = transitionTank.drain(fluid, DataComponentPatch.EMPTY, amount, true);
            amount = handler.fill(fluid, DataComponentPatch.EMPTY, amount, false);
            amount = transitionTank.drain(fluid, DataComponentPatch.EMPTY, amount, false);
            return amount;
        } else {
            handler = null;
            connected = false;
        }
        return 0;
    }
    
    private boolean connected = false;
    Direction waterOutputDirection = null;
    
    IFluidTankHandler handler = null;
    private TurbineFluidPort.PortDirection direction = INLET;
    public final TurbineFluidPortState fluidPortState = new TurbineFluidPortState(this);
    
    @SuppressWarnings("DuplicatedCode")
    public void neighborChanged() {
        handler = null;
        if (waterOutputDirection == null) {
            connected = false;
            return;
        }
        assert level != null;
        final var outputCap = level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.relative(waterOutputDirection), waterOutputDirection.getOpposite());
        if (outputCap == null) {
            connected = false;
            return;
        }
        connected = false;
        if (outputCap != null) {
            connected = true;
            handler = FluidHandlerWrapper.wrap(outputCap);
            // todo: mek gas
//        } else if (GAS_HANDLER_CAPABILITY != null) {
//            LazyOptional<IGasHandler> gasOptional = te.getCapability(GAS_HANDLER_CAPABILITY, waterOutputDirection.getOpposite());
//            if (gasOptional.isPresent()) {
//                IGasHandler gasHandler = gasOptional.orElse(MekanismGasWrappers.EMPTY_TANK);
//                connected = true;
//                handlerOptional = gasOptional;
//                handler = MekanismGasWrappers.wrap(gasHandler);
//            }
        }
    }
    
    public void setDirection(TurbineFluidPort.PortDirection direction) {
        this.direction = direction;
        this.setChanged();
    }
    
    @Override
    protected void readNBT(CompoundTag compound) {
        if (compound.contains("direction")) {
            direction = TurbineFluidPort.PortDirection.valueOf(compound.getString("direction"));
        }
    }
    
    @Override
    protected CompoundTag writeNBT() {
        CompoundTag NBT = new CompoundTag();
        NBT.putString("direction", String.valueOf(direction));
        return NBT;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void runRequest(String requestName, Object requestData) {
        // Change IO direction.
        if (requestName.equals("setDirection")) {
            this.setDirection(((Integer) requestData != 0) ? OUTLET : INLET);
            level.setBlockAndUpdate(this.worldPosition, this.getBlockState().setValue(PORT_DIRECTION_ENUM_PROPERTY, direction));
        }
        super.runRequest(requestName, requestData);
    }
    
    @Override
    public Component getDisplayName() {
        return Component.translatable(TurbineFluidPort.INSTANCE.getDescriptionId());
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
        return new TurbineFluidPortContainer(windowId, this.worldPosition, player);
    }
    
    @Nullable
    @Override
    public TurbineFluidPortState getState() {
        this.updateState();
        return this.fluidPortState;
    }
    
    @Override
    public void updateState() {
        fluidPortState.direction = (this.direction == INLET);
    }
    
    
    @Override
    public void onAssemblyStateTransition(IValidatedMultiblock.AssemblyState oldState, IValidatedMultiblock.AssemblyState newState) {
        OnAssembly.super.onAssemblyStateTransition(oldState, newState);
        OnDisassembly.super.onAssemblyStateTransition(oldState, newState);
    }
    
    @Override
    public void onAssembly() {
        this.transitionTank = controller().simulation().fluidTank();
        waterOutputDirection = getBlockState().getValue(BlockStates.FACING);
        level.setBlockAndUpdate(worldPosition, level.getBlockState(worldPosition).setValue(PORT_DIRECTION_ENUM_PROPERTY, direction));
        neighborChanged();
    }
    
    @Override
    public void onDisassembly() {
        waterOutputDirection = null;
        transitionTank = null;
        neighborChanged();
    }
}
