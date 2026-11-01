package biggerreactorsrework.multiblocks.turbine.blocks;

import biggerreactorsrework.multiblocks.turbine.TurbineRotorLayout;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Added 2026-11-01: slim targeting outlines, so parts behind a rotor can be reached. Collision stays a full block. */
final class RotorShapes {
    private static final double CORE_MIN = 4, CORE_MAX = 12, THIN_MIN = 4.5, THIN_MAX = 11.5;
    private static final VoxelShape[] SHAFT = new VoxelShape[3];
    private static final VoxelShape[][] BLADE = new VoxelShape[3][4];

    static {
        for (var axis : Direction.Axis.values()) {
            var other = TurbineRotorLayout.arms(axis).stream().map(Direction::getAxis).distinct().toList();
            SHAFT[axis.ordinal()] = Shapes.or(bar(axis, CORE_MIN, CORE_MAX), bar(other.get(0), THIN_MIN, THIN_MAX), bar(other.get(1), THIN_MIN, THIN_MAX));
            for (int position = 0; position < 4; position++) {
                BLADE[axis.ordinal()][position] = bar(TurbineRotorLayout.arms(axis).get(position).getAxis(), THIN_MIN, THIN_MAX);
            }
        }
    }

    private RotorShapes() {}

    /** A full-length bar along one axis, with the given square cross-section. */
    private static VoxelShape bar(Direction.Axis along, double min, double max) {
        return Block.box(along == Direction.Axis.X ? 0 : min, along == Direction.Axis.Y ? 0 : min, along == Direction.Axis.Z ? 0 : min,
                along == Direction.Axis.X ? 16 : max, along == Direction.Axis.Y ? 16 : max, along == Direction.Axis.Z ? 16 : max);
    }

    static VoxelShape shaft(int axis) { return SHAFT[axis]; }
    static VoxelShape blade(int axis, int position) { return BLADE[axis][position]; }
}
