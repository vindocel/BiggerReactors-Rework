package biggerreactorsrework.multiblocks.battery.tiles;

import biggerreactorsrework.core.capability.CachedWrappedBlockCapability;
import biggerreactorsrework.core.client.gui.api.IHasUpdatableState;
import biggerreactorsrework.core.energy.IEnergyHandler;
import biggerreactorsrework.core.energy.IEnergyTile;
import biggerreactorsrework.core.multiblock.common.IEventMultiblock;
import biggerreactorsrework.core.multiblock.validated.IValidatedMultiblock;
import biggerreactorsrework.core.registry.RegisterTile;
import biggerreactorsrework.core.util.BlockStates;
import biggerreactorsrework.multiblocks.battery.blocks.BatteryEnergyPort;
import biggerreactorsrework.multiblocks.battery.blocks.BatteryEnergyPort.PortDirection;
import biggerreactorsrework.multiblocks.battery.containers.BatteryEnergyPortContainer;
import biggerreactorsrework.multiblocks.battery.state.BatteryEnergyPortState;
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

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import static biggerreactorsrework.multiblocks.battery.blocks.BatteryEnergyPort.PortDirection.*;

@ParametersAreNonnullByDefault
public class BatteryEnergyPortTile extends BatteryBaseTile implements IEnergyTile, IEnergyHandler, IEventMultiblock.AssemblyStateTransition, MenuProvider, IHasUpdatableState<BatteryEnergyPortState> {

    @RegisterTile("battery_energy_port")
    public static final BlockEntityType.BlockEntitySupplier<BatteryEnergyPortTile> SUPPLIER = new RegisterTile.Producer<>(BatteryEnergyPortTile::new);

    public BatteryEnergyPortTile(BlockEntityType<?> TYPE, BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }

    private PortDirection direction = INLET;
    @Nullable
    private Direction outputDirection = null;
    @Nullable
    private CachedWrappedBlockCapability<IEnergyHandler, Direction> output = null;

    public PortDirection direction() {
        return direction;
    }

    public void setDirection(PortDirection direction) {
        this.direction = direction;
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            if (state.getValue(PortDirection.PORT_DIRECTION_ENUM_PROPERTY) != direction) {
                level.setBlockAndUpdate(worldPosition, state.setValue(PortDirection.PORT_DIRECTION_ENUM_PROPERTY, direction));
            }
        }
        if (nullableController() != null) {
            controller().refreshOutlets();
        }
    }

    private boolean assembled() {
        return nullableController() != null && controller().assemblyState() == IValidatedMultiblock.AssemblyState.ASSEMBLED;
    }

    @Override
    public IEnergyHandler energyHandler(Direction direction) {
        return this;
    }

    @Override
    public Object transferSnapshot() {
        return nullableController() == null ? null : controller().transferState();
    }

    @Override
    public void restoreTransferSnapshot(Object snapshot) {
        if (nullableController() != null && snapshot instanceof long[] state) {
            controller().restoreTransferState(state);
        }
    }

    @Override
    public long insertEnergy(long maxInsert, boolean simulate) {
        if (direction != INLET || !assembled()) {
            return 0;
        }
        return controller().insert(maxInsert, simulate);
    }

    @Override
    public long extractEnergy(long maxExtract, boolean simulate) {
        if (direction != OUTLET || !assembled()) {
            return 0;
        }
        return controller().extract(maxExtract, simulate);
    }

    @Override
    public long energyStored() {
        return nullableController() != null ? Math.max(0, controller().stored()) : 0;
    }

    @Override
    public long maxEnergyStored() {
        return nullableController() != null ? Math.max(0, controller().capacity()) : 0;
    }

    public long distributePower(long toDistribute, boolean simulate) {
        if (output == null) {
            return 0;
        }
        @Nullable
        final var outputCap = output.getCapability();
        if (outputCap == null) {
            return 0;
        }
        return Math.max(0, outputCap.insertEnergy(toDistribute, simulate));
    }

    public boolean isConnected() {
        return output != null && output.getCapability() != null;
    }

    public void neighborChanged() {
        output = null;
        if (outputDirection == null || level == null || level.isClientSide()) {
            return;
        }
        output = findEnergyCapability(outputDirection);
    }

    @Override
    public void onAssemblyStateTransition(IValidatedMultiblock.AssemblyState oldState, IValidatedMultiblock.AssemblyState newState) {
        if (newState == IValidatedMultiblock.AssemblyState.ASSEMBLED) {
            outputDirection = getBlockState().getValue(BlockStates.FACING);
        } else {
            outputDirection = null;
        }
        neighborChanged();
    }

    @Override
    protected void readNBT(CompoundTag compound) {
        super.readNBT(compound);
        if (compound.contains("direction")) {
            // Modified 2026-10-30: tolerate malformed direction metadata.
            direction = biggerreactorsrework.core.util.SavedValues.enumValue(PortDirection.class, compound.getStringOr("direction", ""), PortDirection.INLET);
        }
    }

    @Override
    protected CompoundTag writeNBT() {
        CompoundTag nbt = super.writeNBT();
        nbt.putString("direction", direction.name());
        return nbt;
    }

    private final BatteryEnergyPortState portState = new BatteryEnergyPortState(this);

    @Override
    public BatteryEnergyPortState getState() {
        return portState;
    }

    @Override
    public void updateState() {
        portState.direction = direction == INLET;
        portState.connected = isConnected();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(BatteryEnergyPort.INSTANCE.getDescriptionId());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
        return new BatteryEnergyPortContainer(windowId, this.worldPosition, player);
    }

    @Override
    public void runRequest(String requestName, Object requestData) {
        if (requestName.equals("setDirection") && requestData instanceof Integer value && value >= 0 && value <= 1) {
            setDirection(value != 0 ? OUTLET : INLET);
            return;
        }
        super.runRequest(requestName, requestData);
    }
}
