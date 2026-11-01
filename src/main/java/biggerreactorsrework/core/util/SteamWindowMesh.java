package biggerreactorsrework.core.util;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/** Added 2026-10-31: outward-facing steam films behind actual windows, merged within seamless texture tiles. */
public final class SteamWindowMesh {
    public static final int TILE_SIZE = 4;
    public static final int FRAMES = 64;
    public static final float INSET = .002F;
    private record Cell(int u, int v) implements Comparable<Cell> {
        @Override public int compareTo(Cell other) {
            int row = Integer.compare(v, other.v);
            return row != 0 ? row : Integer.compare(u, other.u);
        }
    }

    public static List<FluidSurfaceMesh.Face> faces(FluidSurfaceMesh.Box interior, int[] glass) {
        if (!interior.valid() || glass.length % 3 != 0) return List.of();
        var result = new ArrayList<FluidSurfaceMesh.Face>();
        for (int axis = 0; axis < 3; axis++) for (int side = 0; side < 2; side++) {
            boolean positive = side == 1;
            int u = (axis + 1) % 3, v = (axis + 2) % 3;
            float wall = positive ? interior.max(axis) : interior.min(axis) - 1;
            var cells = new TreeSet<Cell>();
            for (int i = 0; i < glass.length; i += 3) {
                if (glass[i + axis] == wall && glass[i + u] >= interior.min(u) && glass[i + u] + 1 <= interior.max(u) &&
                        glass[i + v] >= interior.min(v) && glass[i + v] + 1 <= interior.max(v))
                    cells.add(new Cell(glass[i + u], glass[i + v]));
            }
            float plane = positive ? interior.max(axis) - INSET : interior.min(axis) + INSET;
            while (!cells.isEmpty()) {
                var start = cells.first();
                int width = 1, height = 1;
                int maxWidth = TILE_SIZE - Math.floorMod(start.u, TILE_SIZE);
                int maxHeight = TILE_SIZE - Math.floorMod(start.v, TILE_SIZE);
                while (width < maxWidth && cells.contains(new Cell(start.u + width, start.v))) width++;
                while (height < maxHeight) {
                    boolean full = true;
                    for (int x = 0; x < width; x++) if (!cells.contains(new Cell(start.u + x, start.v + height))) { full = false; break; }
                    if (!full) break;
                    height++;
                }
                for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) cells.remove(new Cell(start.u + x, start.v + y));
                result.add(new FluidSurfaceMesh.Face(axis, positive, plane, start.u, start.v, start.u + width, start.v + height));
            }
        }
        return List.copyOf(result);
    }

    public static int frame(double ticks) { return Math.floorMod((int) Math.floor(ticks / 4), FRAMES); }
    private SteamWindowMesh() { }
}
