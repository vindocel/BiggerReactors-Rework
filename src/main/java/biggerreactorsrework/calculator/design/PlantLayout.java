package biggerreactorsrework.calculator.design;

import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * Machines placed side by side, wall against wall, with every connected pair of ports touching directly.
 * Offsets are the minimum corner of each machine in plant coordinates.
 */
public record PlantLayout(List<Placed> machines, List<Connection> connections) {
    public record Placed(MachineLayout machine, BlockPos offset) { }

    /** Output port of one machine against the input port of another; both positions in plant coordinates. */
    public record Connection(int from, MachineLayout.Role fromRole, int to, MachineLayout.Role toRole) { }

    public PlantLayout {
        machines = List.copyOf(machines);
        connections = List.copyOf(connections);
    }

    public BlockPos portPosition(int machine, MachineLayout.Role role) {
        var placed = machines.get(machine);
        return placed.machine().port(role).pos().offset(placed.offset());
    }

    public int width() { return machines.stream().mapToInt(p -> p.offset().getX() + p.machine().width()).max().orElse(0) - minX(); }
    public int height() { return machines.stream().mapToInt(p -> p.offset().getY() + p.machine().height()).max().orElse(0) - minY(); }
    public int depth() { return machines.stream().mapToInt(p -> p.offset().getZ() + p.machine().depth()).max().orElse(0) - minZ(); }
    public int minX() { return machines.stream().mapToInt(p -> p.offset().getX()).min().orElse(0); }
    public int minY() { return machines.stream().mapToInt(p -> p.offset().getY()).min().orElse(0); }
    public int minZ() { return machines.stream().mapToInt(p -> p.offset().getZ()).min().orElse(0); }
}
