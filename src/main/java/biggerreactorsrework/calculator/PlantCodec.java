package biggerreactorsrework.calculator;

import biggerreactorsrework.calculator.design.MachineLayout;
import biggerreactorsrework.calculator.design.PlantLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Stores a plant block by block: per machine a palette of block states and run-length encoded cells, so a saved
 * layout never depends on the generator that produced it. Every size is checked before anything is allocated.
 */
public final class PlantCodec {
    private static final int MAX_SIDE = 256, MAX_CELLS = 4_000_000, MAX_PALETTE = 256, MAX_MACHINES = 3, MAX_PORTS = 8;

    private PlantCodec() { }

    public static CompoundTag encode(PlantLayout plant) {
        var machines = new ListTag();
        for (var placed : plant.machines()) {
            var m = placed.machine();
            var tag = new CompoundTag();
            tag.putString("kind", m.kind().name());
            tag.putIntArray("size", new int[]{m.width(), m.height(), m.depth()});
            tag.putIntArray("offset", new int[]{placed.offset().getX(), placed.offset().getY(), placed.offset().getZ()});
            var palette = new ArrayList<BlockState>();
            var index = new HashMap<BlockState, Integer>();
            var runs = new ArrayList<Integer>();
            int current = -1, count = 0;
            for (int x = 0; x < m.width(); x++) for (int y = 0; y < m.height(); y++) for (int z = 0; z < m.depth(); z++) {
                var state = m.blocks().get(new BlockPos(x, y, z));
                int value = state == null ? 0 : index.computeIfAbsent(state, s -> { palette.add(s); return palette.size(); });
                if (value == current) count++;
                else {
                    if (count > 0) { runs.add(count); runs.add(current); }
                    current = value;
                    count = 1;
                }
            }
            if (count > 0) { runs.add(count); runs.add(current); }
            var paletteTag = new ListTag();
            palette.forEach(s -> paletteTag.add(NbtUtils.writeBlockState(s)));
            tag.put("palette", paletteTag);
            tag.putIntArray("cells", runs.stream().mapToInt(Integer::intValue).toArray());
            var ports = new ListTag();
            for (var port : m.ports()) {
                var p = new CompoundTag();
                p.putIntArray("pos", new int[]{port.pos().getX(), port.pos().getY(), port.pos().getZ()});
                p.putString("role", port.role().name());
                ports.add(p);
            }
            tag.put("ports", ports);
            machines.add(tag);
        }
        var connections = new ListTag();
        for (var c : plant.connections()) {
            var tag = new CompoundTag();
            tag.putInt("from", c.from());
            tag.putString("fromRole", c.fromRole().name());
            tag.putInt("to", c.to());
            tag.putString("toRole", c.toRole().name());
            connections.add(tag);
        }
        var tag = new CompoundTag();
        tag.put("machines", machines);
        tag.put("connections", connections);
        return tag;
    }

    /** Null when the data is malformed. Block names this game does not know are collected in {@code missing}. */
    @Nullable
    public static PlantLayout decode(CompoundTag tag, Set<String> missing) {
        try {
            var machinesTag = tag.getListOrEmpty("machines");
            if (machinesTag.isEmpty() || machinesTag.size() > MAX_MACHINES) return null;
            var machines = new ArrayList<PlantLayout.Placed>();
            for (var element : machinesTag) {
                var m = (CompoundTag) element;
                var kind = MachineLayout.Kind.valueOf(m.getStringOr("kind", ""));
                int[] size = m.getIntArray("size").orElseGet(() -> new int[0]), offset = m.getIntArray("offset").orElseGet(() -> new int[0]);
                if (size.length != 3 || offset.length != 3) return null;
                for (int s : size) if (s < 1 || s > MAX_SIDE) return null;
                long cells = (long) size[0] * size[1] * size[2];
                if (cells > MAX_CELLS) return null;
                var paletteTag = m.getListOrEmpty("palette");
                if (paletteTag.size() > MAX_PALETTE) return null;
                var palette = new ArrayList<BlockState>();
                for (var p : paletteTag) {
                    var stateTag = (CompoundTag) p;
                    var state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK, upgradedState(stateTag));
                    var name = Identifier.tryParse(stateTag.getStringOr("Name", ""));
                    if (state.is(Blocks.AIR) && (name == null || !name.equals(BuiltInRegistries.BLOCK.getKey(Blocks.AIR)))) missing.add(stateTag.getStringOr("Name", ""));
                    palette.add(state);
                }
                int[] runs = m.getIntArray("cells").orElseGet(() -> new int[0]);
                if (runs.length % 2 != 0) return null;
                long total = 0;
                for (int i = 0; i < runs.length; i += 2) {
                    if (runs[i] <= 0 || runs[i + 1] < 0 || runs[i + 1] > palette.size()) return null;
                    total += runs[i];
                }
                if (total != cells) return null;
                var blocks = new LinkedHashMap<BlockPos, BlockState>();
                int cell = 0;
                for (int i = 0; i < runs.length; i += 2)
                    for (int n = 0; n < runs[i]; n++, cell++) {
                        if (runs[i + 1] == 0) continue;
                        int x = cell / (size[1] * size[2]), y = cell / size[2] % size[1], z = cell % size[2];
                        blocks.put(new BlockPos(x, y, z), palette.get(runs[i + 1] - 1));
                    }
                var portsTag = m.getListOrEmpty("ports");
                if (portsTag.size() > MAX_PORTS) return null;
                var ports = new ArrayList<MachineLayout.Port>();
                for (var p : portsTag) {
                    var portTag = (CompoundTag) p;
                    int[] pos = portTag.getIntArray("pos").orElseGet(() -> new int[0]);
                    if (pos.length != 3) return null;
                    ports.add(new MachineLayout.Port(new BlockPos(pos[0], pos[1], pos[2]), MachineLayout.Role.valueOf(portTag.getStringOr("role", ""))));
                }
                machines.add(new PlantLayout.Placed(new MachineLayout(kind, size[0], size[1], size[2], blocks, ports), new BlockPos(offset[0], offset[1], offset[2])));
            }
            var connections = new ArrayList<PlantLayout.Connection>();
            for (var element : tag.getListOrEmpty("connections")) {
                var c = (CompoundTag) element;
                int from = c.getIntOr("from", 0), to = c.getIntOr("to", 0);
                if (from < 0 || to < 0 || from >= machines.size() || to >= machines.size()) return null;
                connections.add(new PlantLayout.Connection(from, MachineLayout.Role.valueOf(c.getStringOr("fromRole", "")), to, MachineLayout.Role.valueOf(c.getStringOr("toRole", ""))));
            }
            if (connections.size() > 4) return null;
            return new PlantLayout(machines, connections);
        } catch (IllegalArgumentException | ClassCastException e) {
            return null;
        }
    }

    /** Block states saved by the 1.21.1 line use the keys {@code Name} and {@code Properties}. */
    public static net.minecraft.nbt.CompoundTag upgradedState(net.minecraft.nbt.CompoundTag tag) {
        if (!tag.contains("Name")) {
            return tag;
        }
        var upgraded = new net.minecraft.nbt.CompoundTag();
        upgraded.putString("id", tag.getStringOr("Name", ""));
        if (tag.contains("Properties")) {
            upgraded.put("properties", tag.getCompoundOrEmpty("Properties"));
        }
        return upgraded;
    }
}
