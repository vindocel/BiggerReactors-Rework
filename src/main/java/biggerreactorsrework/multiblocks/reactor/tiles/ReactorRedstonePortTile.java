package biggerreactorsrework.multiblocks.reactor.tiles;

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
import biggerreactorsrework.multiblocks.reactor.blocks.ReactorRedstonePort;
import biggerreactorsrework.multiblocks.reactor.containers.ReactorRedstonePortContainer;
import biggerreactorsrework.multiblocks.reactor.state.ReactorActivity;
import biggerreactorsrework.multiblocks.reactor.state.ReactorRedstonePortSelection;
import biggerreactorsrework.multiblocks.reactor.state.ReactorRedstonePortState;
import biggerreactorsrework.multiblocks.reactor.state.ReactorRedstonePortTriggers;
import biggerreactorsrework.core.client.gui.api.IHasUpdatableState;
import biggerreactorsrework.core.multiblock.common.IEventMultiblock;
import biggerreactorsrework.core.multiblock.common.ITickablePartsMultiblock;
import biggerreactorsrework.core.multiblock.validated.IValidatedMultiblock;
import biggerreactorsrework.core.registry.RegisterTile;
import biggerreactorsrework.core.util.BlockStates;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ReactorRedstonePortTile extends ReactorBaseTile implements MenuProvider, ITickablePartsMultiblock.Tickable, IHasUpdatableState<ReactorRedstonePortState>, IEventMultiblock.AssemblyStateTransition {
    
    @RegisterTile("reactor_redstone_port")
    public static final BlockEntityType.BlockEntitySupplier<ReactorRedstonePortTile> SUPPLIER = new RegisterTile.Producer<>(ReactorRedstonePortTile::new);
    
    public final ReactorRedstonePortState reactorRedstonePortState = new ReactorRedstonePortState(this);
    
    public ReactorRedstonePortTile(BlockEntityType<?> TYPE, BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }
    
    private boolean isEmitting;
    double mainVal = 0;
    double secondaryVal = 0;
    Direction powerOutputDirection = null;
    
    public boolean isEmitting(Direction side) {
        if (side.getOpposite() != powerOutputDirection) {
            return false;
        }
        return isEmitting;
    }
    
    private boolean isPowered = false;
    private boolean wasPowered = false;
    
    public void updatePowered() {
        if (powerOutputDirection == null) {
            return;
        }
        assert level != null;
        isPowered = level.hasSignal(worldPosition.relative(powerOutputDirection), powerOutputDirection);
    }
    
    private boolean isLit = false;
    
    @Override
    public void preTick() {
        boolean shouldBeEmitting = false;
        boolean shouldLight = false;
        switch (reactorRedstonePortState.selectedTab) {
            case INPUT_ACTIVITY:
                shouldLight = isPowered;
                if (reactorRedstonePortState.triggerPS.toBool()) {
                    // signal
                    if (wasPowered != isPowered) {
                        controller().setActive(isPowered ? ReactorActivity.ACTIVE : ReactorActivity.INACTIVE);
                    }
                } else if (!wasPowered && isPowered) {
                    // not signal, so, pulse
                    controller().toggleActive();
                }
                break;
            case INPUT_CONTROL_ROD_INSERTION: {
                shouldLight = isPowered;
                if (reactorRedstonePortState.triggerPS.toBool()) {
                    if (wasPowered == isPowered) {
                        break;
                    }
                    if (isPowered) {
                        controller().setAllControlRodLevels(mainVal);
                    } else {
                        controller().setAllControlRodLevels(secondaryVal);
                    }
                } else {
                    if (!wasPowered && isPowered) {
                        switch (reactorRedstonePortState.triggerMode) {
                            case 0: {
                                controller().setAllControlRodLevels(controller().controlRodLevel(0) + mainVal);
                                break;
                            }
                            case 1: {
                                controller().setAllControlRodLevels(controller().controlRodLevel(0) - mainVal);
                                break;
                            }
                            case 2: {
                                controller().setAllControlRodLevels(mainVal);
                                break;
                            }
                            default:
                                break;
                        }
                    }
                }
            }
            break;
            case INPUT_EJECT_WASTE: {
                shouldLight = isPowered;
                if (!wasPowered && isPowered) {
                    controller().ejectWaste();
                }
                break;
            }
            case OUTPUT_FUEL_TEMP: {
                var reactorSim = controller().simulation();
                if (reactorSim == null) {
                    break;
                }
                double fuelTemp = reactorSim.fuelHeat();
                if ((fuelTemp < mainVal) == reactorRedstonePortState.triggerAB.toBool()) {
                    shouldBeEmitting = true;
                }
            }
            break;
            case OUTPUT_CASING_TEMP: {
                var reactorSim = controller().simulation();
                if (reactorSim == null) {
                    break;
                }
                double casingTemperature = reactorSim.stackHeat();
                if ((casingTemperature < mainVal) == reactorRedstonePortState.triggerAB.toBool()) {
                    shouldBeEmitting = true;
                }
            }
            break;
            case OUTPUT_FUEL_ENRICHMENT: {
                var reactorSim = controller().simulation();
                if (reactorSim == null) {
                    break;
                }
                double fuelPercent = reactorSim.fuelTank().fuel();
                fuelPercent /= reactorSim.fuelTank().totalStored();
                fuelPercent *= 100;
                if ((fuelPercent < mainVal) == reactorRedstonePortState.triggerAB.toBool()) {
                    shouldBeEmitting = true;
                }
            }
            break;
            case OUTPUT_FUEL_AMOUNT: {
                var reactorSim = controller().simulation();
                if (reactorSim == null) {
                    break;
                }
                double fuelAmount = reactorSim.fuelTank().fuel();
                if ((fuelAmount < mainVal) == reactorRedstonePortState.triggerAB.toBool()) {
                    shouldBeEmitting = true;
                }
            }
            break;
            case OUTPUT_WASTE_AMOUNT: {
                var reactorSim = controller().simulation();
                if (reactorSim == null) {
                    break;
                }
                double wasteAmount = reactorSim.fuelTank().waste();
                if ((wasteAmount < mainVal) == reactorRedstonePortState.triggerAB.toBool()) {
                    shouldBeEmitting = true;
                }
            }
            break;
            case OUTPUT_ENERGY_AMOUNT: {
                var reactorSim = controller().simulation();
                if (reactorSim == null) {
                    break;
                }
                var battery = reactorSim.battery();
                if (battery == null) {
                    break;
                }
                double energyAmount = battery.stored();
                energyAmount /= (double) battery.capacity();
                energyAmount *= 100;
                if ((energyAmount < mainVal) == reactorRedstonePortState.triggerAB.toBool()) {
                    shouldBeEmitting = true;
                }
            }
            break;
        }
        shouldLight |= shouldBeEmitting;
        if (shouldBeEmitting != isEmitting || wasPowered != isPowered) {
            if(powerOutputDirection == null) {
                return;
            }
            isEmitting = shouldBeEmitting;
            wasPowered = isPowered;
            assert level != null;
            BlockPos updatePos = worldPosition.relative(powerOutputDirection);
            level.blockUpdated(this.getBlockPos(), this.getBlockState().getBlock());
            level.blockUpdated(updatePos, level.getBlockState(updatePos).getBlock());
        }
        if (isLit != shouldLight) {
            isLit = shouldLight;
            assert level != null;
            level.setBlockAndUpdate(worldPosition, getBlockState().setValue(ReactorRedstonePort.IS_LIT_BOOLEAN_PROPERTY, isLit));
        }
        // TODO: 7/22/21 only mark changed when it actually changed 
        this.setChanged();
    }
    
    @Override
    public void postTick() {
    
    }
    
    @Override
    public Component getDisplayName() {
        return Component.translatable(ReactorRedstonePort.INSTANCE.getDescriptionId());
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
        return new ReactorRedstonePortContainer(windowId, this.worldPosition, player);
    }
    
    // Current changes/non-active. See reactorRedstonePortState to see what's actually being used for operations.
    private final ReactorRedstonePortState currentChanges = new ReactorRedstonePortState(this);
    
    public ReactorRedstonePortState getCurrentChanges() {
        return this.currentChanges;
    }
    
    @Override
    public ReactorRedstonePortState getState() {
        this.updateState();
        return this.reactorRedstonePortState;
    }
    
    @Override
    public void updateState() {
        // Update committed/active values.
    }
    
    public void applyChanges() {
        final double nextMain;
        final double nextSecondary;
        try {
            nextMain = parseSetting(currentChanges.textBufferA);
            nextSecondary = parseSetting(currentChanges.textBufferB);
        } catch (IllegalArgumentException invalid) {
            return;
        }
        this.reactorRedstonePortState.selectedTab = currentChanges.selectedTab;
        this.reactorRedstonePortState.triggerPS = currentChanges.triggerPS;
        this.reactorRedstonePortState.triggerAB = currentChanges.triggerAB;
        this.reactorRedstonePortState.triggerMode = currentChanges.triggerMode;
        this.reactorRedstonePortState.textBufferA = currentChanges.textBufferA;
        this.reactorRedstonePortState.textBufferB = currentChanges.textBufferB;
        mainVal = nextMain;
        secondaryVal = nextSecondary;
        setChanged();
    }

    private static double parseSetting(String value) {
        if (value.length() > 64) throw new IllegalArgumentException("Setting too long");
        double number = value.isBlank() ? 0 : Double.parseDouble(value);
        if (!Double.isFinite(number) || number < 0) throw new IllegalArgumentException("Invalid setting");
        return number;
    }
    
    public void revertChanges() {
        this.currentChanges.selectedTab = this.reactorRedstonePortState.selectedTab;
        this.currentChanges.triggerPS = this.reactorRedstonePortState.triggerPS;
        this.currentChanges.triggerAB = this.reactorRedstonePortState.triggerAB;
        this.currentChanges.triggerMode = this.reactorRedstonePortState.triggerMode;
        this.currentChanges.textBufferA = this.reactorRedstonePortState.textBufferA;
        this.currentChanges.textBufferB = this.reactorRedstonePortState.textBufferB;
    }
    
    @Override
    public void runRequest(String requestName, Object requestData) {
        switch (requestName) {
            case "setSelectedTab" -> {
                if (requestData instanceof Integer value && value >= 0 && value < ReactorRedstonePortSelection.values().length)
                    currentChanges.selectedTab = ReactorRedstonePortSelection.fromInt(value);
            }
            case "setTriggerPS" -> { if (requestData instanceof Boolean value) currentChanges.triggerPS = ReactorRedstonePortTriggers.fromBool(value); }
            case "setTriggerAB" -> { if (requestData instanceof Boolean value) currentChanges.triggerAB = ReactorRedstonePortTriggers.fromBool(value); }
            case "setTriggerMode" -> { if (requestData instanceof Integer value && value >= 0 && value <= 2) currentChanges.triggerMode = value; }
            case "setTextBufferA" -> { if (requestData instanceof String value && value.length() <= 64) currentChanges.textBufferA = value; }
            case "setTextBufferB" -> { if (requestData instanceof String value && value.length() <= 64) currentChanges.textBufferB = value; }
            case "revertChanges" -> revertChanges();
            case "applyChanges" -> applyChanges();
            default -> super.runRequest(requestName, requestData);
        }
    }
    
    @Override
    protected CompoundTag writeNBT() {
        CompoundTag compound = super.writeNBT();
        compound.putInt("settingId", reactorRedstonePortState.selectedTab.toInt());
        compound.putBoolean("triggerPulseOrSignal", reactorRedstonePortState.triggerPS.toBool());
        compound.putBoolean("triggerAboveOrBelow", reactorRedstonePortState.triggerAB.toBool());
        compound.putInt("mode", reactorRedstonePortState.triggerMode);
        compound.putString("mainBuffer", reactorRedstonePortState.textBufferA);
        compound.putString("secondBuffer", reactorRedstonePortState.textBufferB);
        compound.putBoolean("isPowered", isPowered);
        compound.putBoolean("isEmitting", isEmitting);
        return compound;
    }
    
    @Override
    protected void readNBT(CompoundTag compound) {
        super.readNBT(compound);
        if (compound.contains("settingId")) {
            reactorRedstonePortState.selectedTab = ReactorRedstonePortSelection.fromInt(compound.getInt("settingId"));
        }
        if (compound.contains("triggerPulseOrSignal")) {
            reactorRedstonePortState.triggerPS = ReactorRedstonePortTriggers.fromBool(compound.getBoolean("triggerPulseOrSignal"));
        }
        if (compound.contains("triggerAboveOrBelow")) {
            reactorRedstonePortState.triggerAB = ReactorRedstonePortTriggers.fromBool(compound.getBoolean("triggerAboveOrBelow"));
        }
        if (compound.contains("mode")) {
            reactorRedstonePortState.triggerMode = compound.getInt("mode");
        }
        if (compound.contains("mainBuffer")) {
            reactorRedstonePortState.textBufferA = compound.getString("mainBuffer");
        }
        if (compound.contains("secondBuffer")) {
            reactorRedstonePortState.textBufferB = compound.getString("secondBuffer");
        }
        if (compound.contains("isPowered")) {
            wasPowered = isPowered = compound.getBoolean("isPowered");
        }
        // Call reverted changes to align uncommitted settings to the active ones.
        revertChanges();
        applyChanges();
    }
    
    @Override
    public void onAssemblyStateTransition(IValidatedMultiblock.AssemblyState oldState, IValidatedMultiblock.AssemblyState newState) {
        if (newState == IValidatedMultiblock.AssemblyState.ASSEMBLED) {
            powerOutputDirection = getBlockState().getValue(BlockStates.FACING);
        } else {
            powerOutputDirection = null;
        }
        updatePowered();
    }
}
