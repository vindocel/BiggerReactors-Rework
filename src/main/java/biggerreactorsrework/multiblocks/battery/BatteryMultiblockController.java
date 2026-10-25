package biggerreactorsrework.multiblocks.battery;

import biggerreactorsrework.Config;
import biggerreactorsrework.core.CoreEvents;
import biggerreactorsrework.core.debug.DebugInfo;
import biggerreactorsrework.core.energy.PowerDistribution;
import biggerreactorsrework.core.modular.block.IConnectedTexture;
import biggerreactorsrework.core.multiblock.MultiblockController;
import biggerreactorsrework.core.multiblock.ValidationException;
import biggerreactorsrework.core.multiblock.common.IEventMultiblock;
import biggerreactorsrework.core.multiblock.common.IPersistentMultiblock;
import biggerreactorsrework.core.multiblock.rectangular.IRectangularMultiblock;
import biggerreactorsrework.core.multiblock.touching.ITouchingMultiblock;
import biggerreactorsrework.multiblocks.battery.blocks.BatteryBaseBlock;
import biggerreactorsrework.multiblocks.battery.blocks.BatteryCell;
import biggerreactorsrework.multiblocks.battery.blocks.BatteryEnergyPort.PortDirection;
import biggerreactorsrework.multiblocks.battery.state.BatteryActivity;
import biggerreactorsrework.multiblocks.battery.state.BatteryState;
import biggerreactorsrework.multiblocks.battery.tiles.BatteryBaseTile;
import biggerreactorsrework.multiblocks.battery.tiles.BatteryCellTile;
import biggerreactorsrework.multiblocks.battery.tiles.BatteryEnergyPortTile;
import biggerreactorsrework.multiblocks.battery.tiles.BatteryTerminalTile;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.joml.Vector3i;
import org.joml.Vector3ic;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Battery multiblock. Every cell adds {@code CellCapacity} FE of storage and {@code CellTransferRate} FE/t to both the
 * input and the output limit.
 * <p>
 * The running total is kept here and split evenly across the cells before they are saved or dropped and whenever a
 * block joins or leaves, so a broken cell keeps its share and brings it into the next battery.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class BatteryMultiblockController extends MultiblockController<BatteryBaseTile, BatteryBaseBlock, BatteryMultiblockController> implements
        IRectangularMultiblock<BatteryBaseTile, BatteryBaseBlock, BatteryMultiblockController>,
        IPersistentMultiblock<BatteryBaseTile, BatteryBaseBlock, BatteryMultiblockController>,
        ITouchingMultiblock<BatteryBaseTile, BatteryBaseBlock, BatteryMultiblockController>,
        IEventMultiblock<BatteryBaseTile, BatteryBaseBlock, BatteryMultiblockController> {

    /**
     * Ticks without transfer before the terminals go idle, so bursty loads do not flicker them.
     */
    private static final int IDLE_DELAY = 20;

    private final Set<BatteryCellTile> cells = new LinkedHashSet<>();
    private final Set<BatteryEnergyPortTile> energyPorts = new LinkedHashSet<>();
    private final Set<BatteryTerminalTile> terminals = new LinkedHashSet<>();
    private final List<BatteryEnergyPortTile> outlets = new ArrayList<>();

    public final BatteryItemHandler items = new BatteryItemHandler(this::dirty);
    
    private long stored = 0;
    /**
     * Set when the total changed since the cells last received their shares.
     */
    private boolean cellsDirty = false;
    private long capacity = 0;
    private long transferLimit = 0;

    private long inputThisTick = 0;
    private long outputThisTick = 0;
    private long lastInput = 0;
    private long lastOutput = 0;

    private int ticksSinceTransfer = IDLE_DELAY;
    private BatteryActivity activity = BatteryActivity.IDLE;
    private boolean charged = false;
    private boolean updateBlockStates = false;

    public BatteryMultiblockController(Level level) {
        super(level, BatteryBaseTile.class, BatteryBaseBlock.class);
    }

    @Nullable
    @Override
    public Vector3ic minSize() {
        return new Vector3i(3, 3, 3);
    }

    @Nullable
    @Override
    public Vector3ic maxSize() {
        return new Vector3i(Config.CONFIG.Battery.MaxLength, Config.CONFIG.Battery.MaxHeight, Config.CONFIG.Battery.MaxWidth);
    }

    @Override
    public boolean allowedInteriorBlock(Block block) {
        return block.defaultBlockState().isAir();
    }

    @Override
    public void validateStage1() throws ValidationException {
        if (terminals.isEmpty()) {
            throw new ValidationException("multiblock.error.biggerreactorsrework.battery.missing_terminal");
        }
        if (energyPorts.size() < 2) {
            throw new ValidationException("multiblock.error.biggerreactorsrework.battery.missing_ports");
        }
        if (cells.isEmpty()) {
            throw new ValidationException("multiblock.error.biggerreactorsrework.battery.missing_cell");
        }
    }

    @Override
    protected void onPartAdded(BatteryBaseTile tile) {
        if (tile instanceof BatteryCellTile cell) {
            syncCells();
            cells.add(cell);
            stored = saturatedAdd(stored, cell.energy());
        }
        if (tile instanceof BatteryEnergyPortTile port) {
            energyPorts.add(port);
        }
        if (tile instanceof BatteryTerminalTile terminal) {
            terminals.add(terminal);
        }
    }

    @Override
    protected void onPartRemoved(BatteryBaseTile tile) {
        if (tile instanceof BatteryCellTile cell) {
            syncCells();
            cells.remove(cell);
            stored = Math.max(0, stored - cell.energy());
        }
        if (tile instanceof BatteryEnergyPortTile port) {
            energyPorts.remove(port);
        }
        if (tile instanceof BatteryTerminalTile terminal) {
            terminals.remove(terminal);
        }
    }

    @Override
    protected void onPartBroken(BatteryBaseTile tile) {
        // The slots have no block of their own, so breaking any part drops their items.
        for (int slot = 0; slot < items.getSlots(); slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, tile.getBlockPos().getX(), tile.getBlockPos().getY(), tile.getBlockPos().getZ(), stack.copy());
                items.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }
    
    @Override
    public void onAssembled() {
        validationPassed();
    }

    @Override
    public void onReassembled() {
        validationPassed();
    }

    @Override
    public void onDisassembled() {
        syncCells();
        capacity = 0;
        transferLimit = 0;
        lastInput = 0;
        lastOutput = 0;
        outlets.clear();
        activity = BatteryActivity.IDLE;
    }

    private void validationPassed() {
        capacity = saturatedMultiply(cells.size(), Config.CONFIG.Battery.CellCapacity);
        transferLimit = saturatedMultiply(cells.size(), Config.CONFIG.Battery.CellTransferRate);
        syncCells();
        long total = 0;
        for (BatteryCellTile cell : cells) {
            total = saturatedAdd(total, cell.energy());
        }
        stored = Math.min(total, capacity);
        cellsDirty = stored != total;

        // A new battery needs an inlet and an outlet: if every port points the same way, flip one.
        boolean anyInlet = false;
        boolean anyOutlet = false;
        for (BatteryEnergyPortTile port : energyPorts) {
            anyInlet |= port.direction() == PortDirection.INLET;
            anyOutlet |= port.direction() == PortDirection.OUTLET;
        }
        if (!anyInlet || !anyOutlet) {
            BatteryEnergyPortTile first = energyPorts.iterator().next();
            first.setDirection(first.direction().opposite());
        }
        refreshOutlets();
        refreshCellConnections();

        charged = stored > 0;
        updateBlockStates = true;
    }

    private static long saturatedAdd(long a, long b) {
        long sum = a + b;
        return sum < 0 ? Long.MAX_VALUE : sum;
    }

    /**
     * Splits the total evenly across the cells if it changed since the last split.
     */
    public void syncCells() {
        if (!cellsDirty || cells.isEmpty()) {
            return;
        }
        cellsDirty = false;
        long share = stored / cells.size();
        long remainder = stored % cells.size();
        for (BatteryCellTile cell : cells) {
            cell.setEnergy(share + (remainder-- > 0 ? 1 : 0));
        }
    }

    /**
     * A cell of this battery was placed from an item that carried energy.
     */
    public void addCellEnergy(BatteryCellTile cell, long energy) {
        syncCells();
        stored = saturatedAdd(Math.max(0, stored - cell.energy()), energy);
        cell.setEnergy(energy);
        if (assemblyState() == AssemblyState.ASSEMBLED && stored > capacity) {
            stored = capacity;
            cellsDirty = true;
        }
        dirty();
    }

    /**
     * Keeps configs with huge per-cell values from wrapping the totals past a long.
     */
    private static long saturatedMultiply(long a, long b) {
        long high = Math.multiplyHigh(a, b);
        long low = a * b;
        if ((high == 0 && low >= 0) || (high == -1 && low < 0)) {
            return low;
        }
        return Long.MAX_VALUE;
    }

    public void refreshOutlets() {
        outlets.clear();
        for (BatteryEnergyPortTile port : energyPorts) {
            if (port.direction() == PortDirection.OUTLET) {
                outlets.add(port);
            }
        }
    }

    public long insert(long amount, boolean simulate) {
        if (amount <= 0 || assemblyState() != AssemblyState.ASSEMBLED) {
            return 0;
        }
        long accepted = Math.min(amount, Math.min(transferLimit - inputThisTick, capacity - stored));
        if (accepted <= 0) {
            return 0;
        }
        if (!simulate) {
            stored += accepted;
            inputThisTick += accepted;
            cellsDirty = true;
        }
        return accepted;
    }

    public long extract(long amount, boolean simulate) {
        if (amount <= 0 || assemblyState() != AssemblyState.ASSEMBLED) {
            return 0;
        }
        long extracted = Math.min(amount, Math.min(transferLimit - outputThisTick, stored));
        if (extracted <= 0) {
            return 0;
        }
        if (!simulate) {
            stored -= extracted;
            outputThisTick += extracted;
            cellsDirty = true;
        }
        return extracted;
    }

    public long stored() {
        return stored;
    }

    public long capacity() {
        return capacity;
    }

    @Override
    public void tick() {
        if (stored > capacity) {
            stored = capacity;
            cellsDirty = true;
        }

        chargeItems();
        
        long available = Math.min(stored, transferLimit - outputThisTick);
        PowerDistribution.distribute(available, outlets, BatteryEnergyPortTile::distributePower, amount -> extract(amount, false));

        lastInput = inputThisTick;
        lastOutput = outputThisTick;
        inputThisTick = 0;
        outputThisTick = 0;

        if (lastInput > 0 || lastOutput > 0) {
            ticksSinceTransfer = 0;
        } else if (ticksSinceTransfer < IDLE_DELAY) {
            ticksSinceTransfer++;
        }
        BatteryActivity newActivity = ticksSinceTransfer < IDLE_DELAY ? BatteryActivity.ACTIVE : BatteryActivity.IDLE;
        if (newActivity != activity) {
            activity = newActivity;
            updateTerminalStates();
        }

        boolean nowCharged = stored > 0;
        if (nowCharged != charged) {
            charged = nowCharged;
            updateCellStates();
        }

        if (updateBlockStates) {
            updateBlockStates = false;
            updateTerminalStates();
            updateCellStates();
        }

        if (CoreEvents.tickNumber() % 2 == 0) {
            dirty();
        }
    }

    /**
     * Item energy is counted in ints, so each move is capped.
     */
    private void chargeItems() {
        IEnergyStorage source = BatteryItemHandler.energy(items.getStackInSlot(BatteryItemHandler.DISCHARGE_SLOT));
        if (source != null && source.canExtract()) {
            int room = (int) Math.min(Integer.MAX_VALUE, insert(Long.MAX_VALUE, true));
            if (room > 0) {
                insert(source.extractEnergy(room, false), false);
            }
        }
        IEnergyStorage target = BatteryItemHandler.energy(items.getStackInSlot(BatteryItemHandler.CHARGE_SLOT));
        if (target != null && target.canReceive()) {
            int available = (int) Math.min(Integer.MAX_VALUE, extract(Long.MAX_VALUE, true));
            if (available > 0) {
                extract(target.receiveEnergy(available, false), false);
            }
        }
    }
    
    private void updateTerminalStates() {
        for (BatteryTerminalTile terminal : terminals) {
            BlockState state = terminal.getBlockState();
            if (state.getValue(BatteryActivity.BATTERY_STATE_ENUM_PROPERTY) != activity) {
                level.setBlockAndUpdate(terminal.getBlockPos(), state.setValue(BatteryActivity.BATTERY_STATE_ENUM_PROPERTY, activity));
            }
        }
    }

    /**
     * Connections only update when a cell or its neighbour changes; this catches cells changed while unloaded.
     */
    private void refreshCellConnections() {
        for (BatteryCellTile cell : cells) {
            BlockPos pos = cell.getBlockPos();
            BlockState state = cell.getBlockState();
            BlockState connected = state;
            for (Direction direction : Direction.values()) {
                connected = connected.setValue(connectedProperty(direction), level.getBlockState(pos.relative(direction)).is(BatteryCell.INSTANCE));
            }
            if (connected != state) {
                level.setBlock(pos, connected, Block.UPDATE_CLIENTS);
            }
        }
    }

    private static BooleanProperty connectedProperty(Direction direction) {
        return switch (direction) {
            case UP -> IConnectedTexture.Module.TOP_CONNECTED_PROPERTY;
            case DOWN -> IConnectedTexture.Module.BOTTOM_CONNECTED_PROPERTY;
            case NORTH -> IConnectedTexture.Module.NORTH_CONNECTED_PROPERTY;
            case SOUTH -> IConnectedTexture.Module.SOUTH_CONNECTED_PROPERTY;
            case EAST -> IConnectedTexture.Module.EAST_CONNECTED_PROPERTY;
            case WEST -> IConnectedTexture.Module.WEST_CONNECTED_PROPERTY;
        };
    }

    private void updateCellStates() {
        for (BatteryCellTile cell : cells) {
            BlockState state = cell.getBlockState();
            if (state.getValue(BatteryCell.CHARGED) != charged) {
                level.setBlock(cell.getBlockPos(), state.setValue(BatteryCell.CHARGED, charged), Block.UPDATE_CLIENTS);
            }
        }
    }

    public void updateDataPacket(BatteryState state) {
        state.energyStored = stored;
        state.energyCapacity = capacity;
        state.lastInput = lastInput;
        state.lastOutput = lastOutput;
        state.transferLimit = transferLimit;
        state.cellCount = cells.size();
        int inlets = 0;
        for (BatteryEnergyPortTile port : energyPorts) {
            if (port.direction() == PortDirection.INLET) {
                inlets++;
            }
        }
        state.inletCount = inlets;
        state.outletCount = energyPorts.size() - inlets;
    }

    public void runRequest(String requestName, @Nullable Object requestData) {
    }

    @Override
    public CompoundTag mergeNBTs(CompoundTag nbtA, CompoundTag nbtB) {
        // Energy is saved in the cells; only the item slots are saved here.
        return nbtA.copy();
    }

    @Override
    public void read(CompoundTag nbt) {
        if (nbt.contains("items")) {
            items.deserializeNBT(level.registryAccess(), nbt.getCompound("items"));
        }
        updateBlockStates = true;
    }

    @Nonnull
    @Override
    public CompoundTag write() {
        CompoundTag nbt = new CompoundTag();
        nbt.put("items", items.serializeNBT(level.registryAccess()));
        return nbt;
    }

    @Nonnull
    @Override
    public DebugInfo getControllerDebugInfo() {
        return new DebugInfo("Battery")
                .add("Cells: " + cells.size())
                .add("Stored: " + stored + " / " + capacity + " FE")
                .add("Transfer limit: " + transferLimit + " FE/t")
                .add("Last input: " + lastInput + " FE/t")
                .add("Last output: " + lastOutput + " FE/t");
    }
}
