package biggerreactorsrework.multiblocks.battery.tiles;

import biggerreactorsrework.core.client.gui.GuiSync;
import biggerreactorsrework.core.multiblock.common.IEventMultiblock;
import biggerreactorsrework.core.multiblock.common.ITickablePartsMultiblock;
import biggerreactorsrework.core.multiblock.validated.IValidatedMultiblock.AssemblyState;
import biggerreactorsrework.core.registry.RegisterTile;
import biggerreactorsrework.core.util.BlockStates;
import biggerreactorsrework.multiblocks.battery.blocks.BatteryRedstonePort;
import biggerreactorsrework.multiblocks.battery.containers.BatteryRedstonePortContainer;
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

import java.math.BigDecimal;
import java.util.Map;

/** Added 2026-10-30: server-authoritative output, evaluated without an open menu. */
public final class BatteryRedstonePortTile extends BatteryBaseTile implements MenuProvider, GuiSync.IGUIPacket,
        ITickablePartsMultiblock.Tickable, IEventMultiblock.AssemblyStateTransition {
    @RegisterTile("battery_redstone_port")
    public static final RegisterTile.Producer<BatteryRedstonePortTile> SUPPLIER = new RegisterTile.Producer<>(BatteryRedstonePortTile::new);
    public enum Mode { DISABLED, ABOVE, BELOW, FULL, EMPTY }
    private Mode mode = Mode.DISABLED;
    private double threshold = 90;
    public double charge;
    public boolean assembled, output;
    public boolean settingsReady;

    public BatteryRedstonePortTile(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }
    public Mode mode() { return mode; }
    public double threshold() { return threshold; }

    public static boolean valid(int mode, double threshold) {
        return mode >= 0 && mode < Mode.values().length && Double.isFinite(threshold) && threshold >= 0 && threshold <= 100;
    }

    /** Decimal comparison avoids overflowing long storage and rounding a nearly full battery to 100%. */
    public static boolean matches(Mode mode, double threshold, long stored, long capacity) {
        if (capacity <= 0 || stored < 0 || !valid(mode.ordinal(), threshold)) return false;
        return switch (mode) {
            case DISABLED -> false;
            case FULL -> stored >= capacity;
            case EMPTY -> stored == 0;
            case ABOVE, BELOW -> {
                int comparison = BigDecimal.valueOf(stored).multiply(BigDecimal.valueOf(100))
                        .compareTo(BigDecimal.valueOf(capacity).multiply(BigDecimal.valueOf(threshold)));
                yield mode == Mode.ABOVE ? comparison >= 0 : comparison <= 0;
            }
        };
    }

    public boolean configure(Object data) {
        if (!(data instanceof Map<?, ?> map) || !(map.get("mode") instanceof Integer m)
                || !(map.get("threshold") instanceof Number n) || !valid(m, n.doubleValue())) return false;
        mode = Mode.values()[m];
        threshold = n.doubleValue();
        setChanged();
        refreshOutput();
        return true;
    }

    public boolean emitting(Direction side) {
        return isBatteryAssembled() && getBlockState().getValue(BatteryRedstonePort.IS_LIT)
                && side.getOpposite() == getBlockState().getValue(BlockStates.FACING);
    }

    private void setOutput(boolean value) {
        output = value;
        if (level == null || level.isClientSide()) return;
        var state = level.getBlockState(worldPosition);
        if (!state.is(BatteryRedstonePort.INSTANCE) || state.getValue(BatteryRedstonePort.IS_LIT) == value) return;
        level.setBlockAndUpdate(worldPosition, state.setValue(BatteryRedstonePort.IS_LIT, value));
        level.updateNeighborsAt(worldPosition, state.getBlock());
        level.updateNeighborsAt(worldPosition.relative(state.getValue(BlockStates.FACING)), state.getBlock());
    }

    private void refreshOutput() {
        if (level == null || level.isClientSide()) return;
        assembled = isBatteryAssembled();
        long capacity = assembled ? controller().capacity() : 0;
        long stored = assembled ? controller().stored() : 0;
        charge = capacity > 0 ? 100.0 * stored / capacity : 0;
        setOutput(assembled && matches(mode, threshold, stored, capacity));
    }

    @Override public void preTick() { }
    @Override public void postTick() { refreshOutput(); }
    @Override public void onAssemblyStateTransition(AssemblyState oldState, AssemblyState newState) {
        // Clear persisted light until the assembled controller has computed the current capacity.
        assembled = false;
        charge = 0;
        setOutput(false);
    }
    @Override public void runRequest(String request, Object data) { if (request.equals("configure")) configure(data); }
    @Override public Component getDisplayName() { return Component.translatable(BatteryRedstonePort.INSTANCE.getDescriptionId()); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new BatteryRedstonePortContainer(id, worldPosition, player); }
    @Override public Map<?, ?> write() {
        // Modified 2026-10-30: sensor appearance follows the same canonical terminal as the battery body.
        updateBatteryState();
        return Map.of("mode", mode.ordinal(), "threshold", threshold, "charge", charge, "assembled", assembled, "output", output,
                "appearancePos", batteryState().appearancePos.asLong());
    }
    @Override public void read(Map<?, ?> data) {
        if (data.get("appearancePos") instanceof Long position) {
            batteryState().appearancePos = BlockPos.of(position);
            batteryState().appearanceReady = true;
        }
        if (data.get("mode") instanceof Integer m && data.get("threshold") instanceof Number n && valid(m, n.doubleValue())) {
            mode = Mode.values()[m]; threshold = n.doubleValue();
            settingsReady = true;
        }
        if (data.get("charge") instanceof Number n && Double.isFinite(n.doubleValue())) charge = Math.clamp(n.doubleValue(), 0, 100);
        if (data.get("assembled") instanceof Boolean value) assembled = value;
        if (data.get("output") instanceof Boolean value) output = value;
    }
    @Override protected CompoundTag writeNBT() {
        var tag = super.writeNBT();
        tag.putInt("sensorMode", mode.ordinal()); tag.putDouble("threshold", threshold);
        return tag;
    }
    @Override protected void readNBT(CompoundTag tag) {
        super.readNBT(tag);
        int m = tag.getIntOr("sensorMode", 0);
        double value = tag.contains("threshold") ? tag.getDoubleOr("threshold", 0D) : 90;
        mode = valid(m, value) ? Mode.values()[m] : Mode.DISABLED;
        threshold = valid(m, value) ? value : 90;
    }
}
