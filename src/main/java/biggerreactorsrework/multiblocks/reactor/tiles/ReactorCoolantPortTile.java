package biggerreactorsrework.multiblocks.reactor.tiles;

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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import biggerreactorsrework.multiblocks.reactor.blocks.ReactorAccessPort;
import biggerreactorsrework.multiblocks.reactor.blocks.ReactorCoolantPort;
import biggerreactorsrework.multiblocks.reactor.containers.ReactorCoolantPortContainer;
import biggerreactorsrework.multiblocks.reactor.state.ReactorCoolantPortState;
import biggerreactorsrework.multiblocks.reactor.util.ReactorTransitionTank;
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

import static biggerreactorsrework.multiblocks.reactor.blocks.ReactorAccessPort.PortDirection.*;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ReactorCoolantPortTile extends ReactorBaseTile implements IFluidTankHandler, MenuProvider, IHasUpdatableState<ReactorCoolantPortState>, IEventMultiblock.AssemblyStateTransition {
    
    @RegisterTile("reactor_coolant_port")
    public static final BlockEntityType.BlockEntitySupplier<ReactorCoolantPortTile> SUPPLIER = new RegisterTile.Producer<>(ReactorCoolantPortTile::new);
    
    @RegisterCapability
    private static final CapabilityRegistration FLUID_HANDLER_CAP_REGISTRATION = CapabilityRegistration.tileCap(Capabilities.FluidHandler.BLOCK, ReactorCoolantPortTile.class);
    
    public ReactorCoolantPortTile(BlockEntityType<?> TYPE, BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }

    
    @Nullable
    private ReactorTransitionTank transitionTank;
    
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
    @Override public boolean fluidValidForTank(int tank,Fluid fluid,DataComponentPatch components){return transitionTank!=null && transitionTank.fluidValidForTank(tank,fluid,components);}
    
    @Override
    public long fill(Fluid fluid, DataComponentPatch components, long amount, boolean simulate) {
        if (transitionTank == null || direction != INLET) {
            return 0;
        }
        return transitionTank.fill(fluid, components, amount, simulate);
    }
    
    @Override
    public long drain(Fluid fluid, DataComponentPatch components, long amount, boolean simulate) {
        if (transitionTank == null || direction == INLET) {
            return 0;
        }
        return transitionTank.drain(fluid, components, amount, simulate);
    }
    
    public long pushFluid() {
        if (!connected || direction == INLET) {
            return 0;
        }
        if (handler != null && transitionTank != null) {
            Fluid fluid = transitionTank.vaporType();
            var components = transitionTank.fluidComponentsInTank(1);
            long amount = transitionTank.vaporAmount();
            amount = transitionTank.drain(fluid, components, amount, true);
            amount = handler.fill(fluid, components, amount, false);
            amount = transitionTank.drain(fluid, components, amount, false);
            return amount;
        } else {
            handler = null;
            connected = false;
        }
        return 0;
    }
    
    private boolean connected = false;
    Direction steamOutputDirection = null;
    
    IFluidTankHandler handler = null;
    private ReactorAccessPort.PortDirection direction = INLET;
    public final ReactorCoolantPortState reactorCoolantPortState = new ReactorCoolantPortState(this);
    
    @SuppressWarnings("DuplicatedCode")
    public void neighborChanged() {
        handler = null;
        if (steamOutputDirection == null) {
            connected = false;
            return;
        }
        assert level != null;
        final var capHandler = level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.relative(steamOutputDirection), steamOutputDirection.getOpposite());
        connected = false;
        if (capHandler != null) {
            connected = true;
            handler = FluidHandlerWrapper.wrap(capHandler);
        }
    }
    
    public void setDirection(ReactorAccessPort.PortDirection direction) {
        this.direction = direction;
        this.setChanged();
    }
    
    @Override
    protected void readNBT(CompoundTag compound) {
        if (compound.contains("direction")) {
            direction = ReactorAccessPort.PortDirection.valueOf(compound.getString("direction"));
        }
    }
    
    @Override
    
    protected CompoundTag writeNBT() {
        CompoundTag NBT = new CompoundTag();
        NBT.putString("direction", String.valueOf(direction));
        return NBT;
    }
    
    @Override
    public void runRequest(String requestName, Object requestData) {
        // Change IO direction.
        if (requestName.equals("setDirection")) {
            this.setDirection(((Integer) requestData != 0) ? OUTLET : INLET);
            assert level != null;
            level.setBlock(this.worldPosition, this.getBlockState().setValue(PORT_DIRECTION_ENUM_PROPERTY, direction), 3);
        }
        
        super.runRequest(requestName, requestData);
    }
    
    @Override
    public Component getDisplayName() {
        return Component.translatable(ReactorCoolantPort.INSTANCE.getDescriptionId());
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
        return new ReactorCoolantPortContainer(windowId, this.worldPosition, player);
    }
    
    @Override
    
    public ReactorCoolantPortState getState() {
        this.updateState();
        return this.reactorCoolantPortState;
    }
    
    @Override
    public void updateState() {
        reactorCoolantPortState.direction = (this.direction == INLET);
    }
    
    @Override
    public void onAssemblyStateTransition(IValidatedMultiblock.AssemblyState oldState, IValidatedMultiblock.AssemblyState newState) {
        assert level != null;
        if (newState == IValidatedMultiblock.AssemblyState.ASSEMBLED) {
            level.setBlock(worldPosition, level.getBlockState(worldPosition).setValue(PORT_DIRECTION_ENUM_PROPERTY, direction), 3);
            steamOutputDirection = getBlockState().getValue(BlockStates.FACING);
            transitionTank = controller().coolantTank();
        } else {
            steamOutputDirection = null;
            transitionTank = null;
        }
        neighborChanged();
    }
}
