package biggerreactorsrework.calculator.design;

import biggerreactorsrework.multiblocks.reactor.blocks.*;
import biggerreactorsrework.multiblocks.reactor.simulation.SimulationDescription;
import biggerreactorsrework.registries.ReactorModeratorRegistry;
import biggerreactorsrework.util.GuideStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * A reactor whose fuel columns and manifold columns run the whole interior height. Sizes are interior sizes.
 * Without a moderator the reactor is liquid cooled: manifolds beside the rods, air elsewhere.
 */
public record ReactorDesign(int x, int y, int z, Pattern pattern, @Nullable Block moderator, double insertion) {
    public enum Pattern {
        CHECKER, GRID2, GRID3;

        boolean rod(int i, int k, int sx, int sz) {
            return switch (this) {
                case CHECKER -> (i + k) % 2 == 0;
                case GRID2 -> i % 2 == 0 && k % 2 == 0;
                case GRID3 -> i % 3 == offset(sx) && k % 3 == offset(sz);
            };
        }

        private static int offset(int size) {
            return ((size - 1) / 2) % 3;
        }
    }

    public boolean liquid() { return moderator == null; }

    public boolean rod(int i, int k) { return pattern.rod(i, k, x, z); }

    public boolean manifold(int i, int k) {
        if (!liquid() || rod(i, k)) return false;
        return i > 0 && rod(i - 1, k) || i < x - 1 && rod(i + 1, k) || k > 0 && rod(i, k - 1) || k < z - 1 && rod(i, k + 1);
    }

    public int columns() {
        int n = 0;
        for (int i = 0; i < x; i++) for (int k = 0; k < z; k++) if (rod(i, k)) n++;
        return n;
    }

    public int manifoldColumns() {
        int n = 0;
        for (int i = 0; i < x; i++) for (int k = 0; k < z; k++) if (manifold(i, k)) n++;
        return n;
    }

    public long fuelRodBlocks() { return (long) columns() * y; }
    public long manifoldBlocks() { return (long) manifoldColumns() * y; }
    public long moderatorBlocks() { return liquid() ? 0 : ((long) x * z - columns()) * y; }

    /** Rod faces touching a manifold, the contact the simulation uses for liquid cooling. */
    public long contacts() {
        long faces = 0;
        for (int i = 0; i < x; i++) for (int k = 0; k < z; k++) {
            if (!rod(i, k)) continue;
            if (i > 0 && manifold(i - 1, k)) faces++;
            if (i < x - 1 && manifold(i + 1, k)) faces++;
            if (k > 0 && manifold(i, k - 1)) faces++;
            if (k < z - 1 && manifold(i, k + 1)) faces++;
        }
        return faces * y;
    }

    public ReactorDesign withInsertion(double value) {
        return new ReactorDesign(x, y, z, pattern, moderator, value);
    }

    public int width() { return x + 2; }
    public int height() { return y + 2; }
    public int depth() { return z + 2; }

    /** Same description the controller builds from the assembled blocks. */
    public SimulationDescription description(@Nullable ReactorModeratorRegistry.ModeratorProperties moderatorProperties) {
        var description = new SimulationDescription();
        description.setSize(x, y, z);
        var air = ReactorModeratorRegistry.blockModeratorProperties(net.minecraft.world.level.block.Blocks.AIR);
        description.setDefaultIModeratorProperties(air == null ? ReactorModeratorRegistry.ModeratorProperties.EMPTY_MODERATOR : air);
        for (int i = 0; i < x; i++) for (int k = 0; k < z; k++) {
            if (rod(i, k)) {
                description.setControlRod(i, k, true);
                continue;
            }
            boolean manifold = manifold(i, k);
            for (int j = 0; j < y; j++) {
                if (manifold) description.setManifold(i, j, k, true);
                else if (!liquid()) description.setModeratorProperties(i, j, k, moderatorProperties);
                else description.setModeratorProperties(i, j, k, air);
            }
        }
        return description;
    }

    /**
     * Blocks of the reactor. Coolant ports (liquid) or a power tap (solid) go on the east face, the terminal on the
     * west face and the access port on the north face.
     */
    public MachineLayout layout(List<MachineLayout.Port> coolantPorts) {
        int w = width(), h = height(), d = depth();
        var parts = new LinkedHashMap<BlockPos, BlockState>();
        for (int px = 0; px < w; px++) for (int py = 0; py < h; py++) for (int pz = 0; pz < d; pz++) {
            boolean wall = px == 0 || px == w - 1 || py == 0 || py == h - 1 || pz == 0 || pz == d - 1;
            if (wall) {
                parts.put(new BlockPos(px, py, pz), ReactorCasing.INSTANCE.defaultBlockState());
                continue;
            }
            int i = px - 1, k = pz - 1;
            Block block = rod(i, k) ? ReactorFuelRod.INSTANCE : manifold(i, k) ? ReactorManifold.INSTANCE : moderator;
            if (block != null) parts.put(new BlockPos(px, py, pz), block.defaultBlockState());
        }
        for (int i = 0; i < x; i++) for (int k = 0; k < z; k++)
            if (rod(i, k)) parts.put(new BlockPos(i + 1, h - 1, k + 1), ReactorControlRod.INSTANCE.defaultBlockState());
        parts.put(new BlockPos(0, 1, d / 2), ReactorTerminal.INSTANCE.defaultBlockState());
        // Fuel goes in through the north port; waste leaves through the south port.
        parts.put(new BlockPos(w / 2, 1, 0), ReactorAccessPort.INSTANCE.defaultBlockState()
                .setValue(ReactorAccessPort.PortDirection.PORT_DIRECTION_ENUM_PROPERTY, ReactorAccessPort.PortDirection.INLET));
        parts.put(new BlockPos(w / 2, 1, d - 1), ReactorAccessPort.INSTANCE.defaultBlockState()
                .setValue(ReactorAccessPort.PortDirection.PORT_DIRECTION_ENUM_PROPERTY, ReactorAccessPort.PortDirection.OUTLET));
        if (liquid()) {
            for (var port : coolantPorts) parts.put(port.pos(), ReactorCoolantPort.INSTANCE.defaultBlockState()
                    .setValue(ReactorAccessPort.PortDirection.PORT_DIRECTION_ENUM_PROPERTY,
                            port.role().inlet() ? ReactorAccessPort.PortDirection.INLET : ReactorAccessPort.PortDirection.OUTLET));
        } else {
            parts.put(new BlockPos(w - 1, 1, d / 2), ReactorPowerTap.INSTANCE.defaultBlockState());
        }
        GuideStructures.connect(parts);
        return new MachineLayout(MachineLayout.Kind.REACTOR, w, h, d, parts, liquid() ? coolantPorts : List.of());
    }
}
