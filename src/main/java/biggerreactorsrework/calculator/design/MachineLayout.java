package biggerreactorsrework.calculator.design;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;

/** One machine of a calculated plant: non-air blocks by local position, exterior size and its fluid ports. */
public record MachineLayout(Kind kind, int width, int height, int depth, Map<BlockPos, BlockState> blocks, List<Port> ports) {
    public enum Kind { REACTOR, EXCHANGER, TURBINE }

    public enum Role {
        REACTOR_HOT_OUT, REACTOR_COLD_IN,
        EXCHANGER_HOT_IN, EXCHANGER_COLD_OUT, EXCHANGER_WATER_IN, EXCHANGER_STEAM_OUT,
        TURBINE_STEAM_IN, TURBINE_WATER_OUT;

        public boolean inlet() { return name().endsWith("_IN"); }
    }

    public record Port(BlockPos pos, Role role) { }

    public MachineLayout {
        blocks = Map.copyOf(blocks);
        ports = List.copyOf(ports);
    }

    public Port port(Role role) {
        return ports.stream().filter(p -> p.role() == role).findFirst().orElseThrow();
    }
}
