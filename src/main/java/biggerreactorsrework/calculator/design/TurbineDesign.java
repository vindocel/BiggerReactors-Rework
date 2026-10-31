package biggerreactorsrework.calculator.design;

import biggerreactorsrework.Config;
import biggerreactorsrework.multiblocks.turbine.blocks.*;
import biggerreactorsrework.multiblocks.turbine.simulation.modern.ModernTurbineSimulation;
import biggerreactorsrework.multiblocks.turbine.state.VentState;
import biggerreactorsrework.registries.TurbineCoilRegistry;
import biggerreactorsrework.util.GuideStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector4i;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * A vertical turbine: blades in the lower layers, coils in the upper layers. Each coil layer fills from the ring
 * right around the shaft outwards; the last coil layer may be partial. Spacer layers hold only the shaft, to make
 * the turbine tall enough for the exchanger's ports.
 */
public record TurbineDesign(int arm, int bladeLayers, int spacer, int coils, Block coil, long flow) {
    public int side() { return 2 * arm + 1; }
    private int perLayer() { return side() * side() - 1; }
    public int coilLayers() { return (coils + perLayer() - 1) / perLayer(); }
    public int shaft() { return bladeLayers + spacer + coilLayers(); }
    public int width() { return side() + 2; }
    public int height() { return shaft() + 2; }
    public int depth() { return side() + 2; }
    public long blades() { return 4L * arm * bladeLayers; }

    /** Steam the blades take at full efficiency at the given speed. */
    public double bladeCapacity(double rpm) {
        double metres = bladeLayers * 4.0 * arm * (arm + 1) / 2;
        return metres * Config.CONFIG.Turbine.FluidPerBladeLinerKilometre / 1000 * 2 * Math.PI * rpm;
    }

    public long flowLimit() { return ((long) side() * side() - 1) * Config.CONFIG.Turbine.FlowRatePerBlock; }

    public TurbineDesign withFlow(long value) { return new TurbineDesign(arm, bladeLayers, spacer, coils, coil, value); }
    public TurbineDesign withCoils(int value) { return new TurbineDesign(arm, bladeLayers, spacer, value, coil, flow); }

    /** Positions of a coil layer around the shaft, nearest ring first, in a fixed order. */
    private List<int[]> ring() {
        var positions = new ArrayList<int[]>();
        for (int dx = -arm; dx <= arm; dx++) for (int dz = -arm; dz <= arm; dz++) if (dx != 0 || dz != 0) positions.add(new int[]{dx, dz});
        positions.sort(java.util.Comparator.<int[]>comparingInt(p -> Math.max(Math.abs(p[0]), Math.abs(p[1]))).thenComparingInt(p -> p[0]).thenComparingInt(p -> p[1]));
        return positions;
    }

    private boolean coilAt(int coilLayer, int index) {
        return coilLayer * perLayer() + index < coils;
    }

    /** Fewest coils, filled in layout order, whose distance factors add up to at least {@code factor}. */
    public int coilsForFactor(double factor, int maxCoils) {
        var ring = ring();
        double sum = 0;
        for (int n = 0; n < maxCoils; n++) {
            var p = ring.get(n % ring.size());
            int d = Math.max(Math.abs(p[0]), Math.abs(p[1]));
            sum += d < 1 ? 1 : 2.0 / (d + 1);
            if (sum >= factor) return n + 1;
        }
        return maxCoils;
    }

    /** Sum of the controller's distance factor over every coil: coil torque is proportional to it. */
    public double coilFactor() {
        var ring = ring();
        double factor = 0;
        for (int layer = 0; layer < coilLayers(); layer++)
            for (int i = 0; i < ring.size(); i++) if (coilAt(layer, i)) {
                int d = Math.max(Math.abs(ring.get(i)[0]), Math.abs(ring.get(i)[1]));
                factor += d < 1 ? 1 : 2.0 / (d + 1);
            }
        return factor;
    }

    /** Same configuration the controller builds from the assembled rotor. */
    public ModernTurbineSimulation simulation(TurbineCoilRegistry.CoilData coilData) {
        var sim = new ModernTurbineSimulation();
        sim.resize(side(), shaft(), side());
        var rotor = new ArrayList<Vector4i>();
        for (int layer = shaft() - 1; layer >= 0; layer--)
            rotor.add(layer < bladeLayers ? new Vector4i(arm, arm, arm, arm) : new Vector4i());
        sim.setRotorConfiguration(rotor);
        var ring = ring();
        for (int layer = 0; layer < coilLayers(); layer++)
            for (int i = 0; i < ring.size(); i++) if (coilAt(layer, i)) sim.setCoilData(ring.get(i)[0], ring.get(i)[1], coilData);
        sim.updateInternalValues();
        sim.setNominalFlowRate(flow);
        sim.setVentState(VentState.CLOSED);
        sim.setCoilEngaged(true);
        sim.setActive(true);
        return sim;
    }

    /** Blocks of the turbine. Fluid ports on the west face, power tap on the east face, terminal on the south face. */
    public MachineLayout layout(List<MachineLayout.Port> fluidPorts) {
        int w = width(), h = height(), d = depth(), c = arm + 1;
        var parts = new LinkedHashMap<BlockPos, BlockState>();
        for (int px = 0; px < w; px++) for (int py = 0; py < h; py++) for (int pz = 0; pz < d; pz++)
            if (px == 0 || px == w - 1 || py == 0 || py == h - 1 || pz == 0 || pz == d - 1)
                parts.put(new BlockPos(px, py, pz), TurbineCasing.INSTANCE.defaultBlockState());
        parts.put(new BlockPos(c, 0, c), TurbineRotorBearing.INSTANCE.defaultBlockState());
        parts.put(new BlockPos(c, h - 1, c), TurbineRotorBearing.INSTANCE.defaultBlockState());
        for (int layer = 0; layer < shaft(); layer++) {
            int py = layer + 1;
            parts.put(new BlockPos(c, py, c), TurbineRotorShaft.INSTANCE.defaultBlockState());
            if (layer < bladeLayers) {
                for (int d1 = 1; d1 <= arm; d1++) for (var dir : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}})
                    parts.put(new BlockPos(c + dir[0] * d1, py, c + dir[1] * d1), TurbineRotorBlade.INSTANCE.defaultBlockState());
            } else if (layer >= bladeLayers + spacer) {
                var ring = ring();
                for (int i = 0; i < ring.size(); i++) if (coilAt(layer - bladeLayers - spacer, i))
                    parts.put(new BlockPos(c + ring.get(i)[0], py, c + ring.get(i)[1]), coil.defaultBlockState());
            }
        }
        for (var port : fluidPorts) parts.put(port.pos(), TurbineFluidPort.INSTANCE.defaultBlockState()
                .setValue(TurbineFluidPort.PortDirection.PORT_DIRECTION_ENUM_PROPERTY,
                        port.role().inlet() ? TurbineFluidPort.PortDirection.INLET : TurbineFluidPort.PortDirection.OUTLET));
        parts.put(new BlockPos(w - 1, 1, c), TurbinePowerTap.INSTANCE.defaultBlockState());
        parts.put(new BlockPos(c, 1, d - 1), TurbineTerminal.INSTANCE.defaultBlockState());
        GuideStructures.connect(parts);
        return new MachineLayout(MachineLayout.Kind.TURBINE, w, h, d, parts, fluidPorts);
    }
}
