package biggerreactorsrework.calculator.design;

import biggerreactorsrework.Config;
import biggerreactorsrework.multiblocks.heatexchanger.blocks.*;
import biggerreactorsrework.util.GuideStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Paired serpentines folded through neighbouring planes. Separate side corridors join successive planes without
 * crossing the other circuit or branching. The two-argument form preserves the original single-plane fixtures.
 */
// Modified 2026-10-29: allow the heat paths to grow in all three dimensions.
public record ExchangerDesign(int length, int rows, int layers) {
    public ExchangerDesign(int length, int rows) { this(length, rows, 1); }

    public ExchangerDesign {
        if (length < 2 || rows < 2 || rows % 2 != 0 || layers < 1) throw new IllegalArgumentException("invalid exchanger " + length + "x" + rows + "x" + layers);
    }

    private int inset() { return layers == 1 ? 1 : 2; }
    public int width() { return length + 2 * inset(); }
    public int height() { return 2 * rows + 1; }
    public int depth() { return 3 * layers + 1; }
    public int topRow() { return 2 * rows - 1; }
    public int portHeight() { return Math.max(coldOut().getY(), steamOut().getY()); }
    public int portDepth() { return 3 * (layers - 1) + 1; }

    public BlockPos hotIn() { return new BlockPos(0, 1, 1); }
    public BlockPos coldOut() { return new BlockPos(0, layers % 2 == 0 ? 1 : topRow(), portDepth()); }
    public BlockPos waterIn() { return new BlockPos(width() - 1, 1, 2); }
    public BlockPos steamOut() { return new BlockPos(width() - 1, coldOut().getY(), portDepth() + 1); }

    /** Heat paths counted from the blocks exactly as the controller counts them on assembly. */
    public record Geometry(int condenser, int evaporator, int contact, int condenserAir, int evaporatorAir, int airVolume, int ambientArea) {
        public double channelRFKT() { return contact * Config.CONFIG.HeatExchanger.ChannelFEPerKelvinMetreSquared; }
        public double condenserAirRFKT() { return condenserAir * Config.CONFIG.HeatExchanger.AirFEPerKelvinMetreSquared; }
        public double evaporatorAirRFKT() { return evaporatorAir * Config.CONFIG.HeatExchanger.AirFEPerKelvinMetreSquared; }
        public double airRFK() { return airVolume * Config.CONFIG.HeatExchanger.AirFEPerKelvinUnitVolume; }
        public double ambientRFKT() { return ambientArea * Config.CONFIG.HeatExchanger.AmbientFEPerKelvinMetreSquared; }
    }

    public Geometry geometry() {
        var parts = channels();
        int condenser = 0, evaporator = 0, contact = 0, condenserAir = 0, evaporatorAir = 0;
        for (var entry : parts.entrySet()) {
            boolean hot = entry.getValue() == HeatExchangerCondenserChannelBlock.INSTANCE;
            if (hot) condenser++;
            else evaporator++;
            for (var direction : Direction.values()) {
                var neighbour = parts.get(entry.getKey().relative(direction));
                if (hot) {
                    if (neighbour == HeatExchangerEvaporatorChannelBlock.INSTANCE) contact++;
                    else if (neighbour == null) condenserAir++;
                } else if (neighbour == null) evaporatorAir++;
            }
        }
        int w = width(), h = height(), d = depth();
        return new Geometry(condenser, evaporator, contact, condenserAir, evaporatorAir, w * h * d - condenser - evaporator, 2 * (w * h + w * d + h * d));
    }

    private Map<BlockPos, Block> channels() {
        var parts = new LinkedHashMap<BlockPos, Block>();
        for (int layer = 0; layer < layers; layer++) {
            int z = 3 * layer + 1;
            for (int r = 0; r < rows; r++) {
                int y = layer % 2 == 0 ? 2 * r + 1 : topRow() - 2 * r;
                for (int x = inset(); x < inset() + length; x++) {
                    parts.put(new BlockPos(x, y, z), HeatExchangerCondenserChannelBlock.INSTANCE);
                    parts.put(new BlockPos(x, y, z + 1), HeatExchangerEvaporatorChannelBlock.INSTANCE);
                }
                if (r < rows - 1) {
                    int turnY = y + (layer % 2 == 0 ? 1 : -1);
                    parts.put(new BlockPos(r % 2 == 0 ? inset() + length - 1 : inset(), turnY, z), HeatExchangerCondenserChannelBlock.INSTANCE);
                    parts.put(new BlockPos(r % 2 == 0 ? inset() : inset() + length - 1, turnY, z + 1), HeatExchangerEvaporatorChannelBlock.INSTANCE);
                }
            }
            if (layer < layers - 1) {
                int y = layer % 2 == 0 ? topRow() : 1;
                for (int step = 0; step <= 3; step++) {
                    parts.put(new BlockPos(1, y, z + step), HeatExchangerCondenserChannelBlock.INSTANCE);
                    parts.put(new BlockPos(width() - 2, y, z + 1 + step), HeatExchangerEvaporatorChannelBlock.INSTANCE);
                }
            }
        }
        if (layers > 1) {
            parts.put(hotIn().east(), HeatExchangerCondenserChannelBlock.INSTANCE);
            parts.put(coldOut().east(), HeatExchangerCondenserChannelBlock.INSTANCE);
            parts.put(waterIn().west(), HeatExchangerEvaporatorChannelBlock.INSTANCE);
            parts.put(steamOut().west(), HeatExchangerEvaporatorChannelBlock.INSTANCE);
        }
        return parts;
    }

    public long channelVolume() {
        long count = (long) layers * (rows * length + rows - 1) + (layers == 1 ? 0 : 4L * (layers - 1) + 2);
        return count * Config.CONFIG.HeatExchanger.ChannelTankVolumePerBlock;
    }

    public MachineLayout layout() {
        int w = width(), h = height(), d = depth();
        var parts = new LinkedHashMap<BlockPos, BlockState>();
        for (int px = 0; px < w; px++) for (int py = 0; py < h; py++) for (int pz = 0; pz < d; pz++)
            if (px == 0 || px == w - 1 || py == 0 || py == h - 1 || pz == 0 || pz == d - 1)
                parts.put(new BlockPos(px, py, pz), HeatExchangerCasingBlock.INSTANCE.defaultBlockState());
        channels().forEach((pos, block) -> parts.put(pos, block.defaultBlockState()));
        var ports = List.of(new MachineLayout.Port(hotIn(), MachineLayout.Role.EXCHANGER_HOT_IN),
                new MachineLayout.Port(coldOut(), MachineLayout.Role.EXCHANGER_COLD_OUT),
                new MachineLayout.Port(waterIn(), MachineLayout.Role.EXCHANGER_WATER_IN),
                new MachineLayout.Port(steamOut(), MachineLayout.Role.EXCHANGER_STEAM_OUT));
        for (var port : ports) {
            boolean condenser = port.role() == MachineLayout.Role.EXCHANGER_HOT_IN || port.role() == MachineLayout.Role.EXCHANGER_COLD_OUT;
            parts.put(port.pos(), HeatExchangerFluidPortBlock.INSTANCE.defaultBlockState()
                    .setValue(HeatExchangerFluidPortBlock.CONDENSER, condenser)
                    .setValue(biggerreactorsrework.core.util.BlockStates.PORT_DIRECTION, port.role().inlet()));
        }
        parts.put(new BlockPos(1, 1, d - 1), HeatExchangerTerminalBlock.INSTANCE.defaultBlockState());
        GuideStructures.connect(parts);
        return new MachineLayout(MachineLayout.Kind.EXCHANGER, w, h, d, parts, ports);
    }
}
