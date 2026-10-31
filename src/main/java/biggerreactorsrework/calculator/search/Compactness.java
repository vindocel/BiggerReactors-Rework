package biggerreactorsrework.calculator.search;

/** Modified 2026-10-29: balance matters at every size, without forbidding any dimension or aspect ratio. */
public final class Compactness {
    private Compactness() { }

    public static long volume(int x, int y, int z) { return (long) x * y * z; }

    public static long surface(int x, int y, int z) { return 2L * ((long) x * y + (long) x * z + (long) y * z); }

    /** Mean of the actual volume and the volume of a cube with the same surface area. Equal for a cube. */
    public static double cost(int x, int y, int z) {
        return (volume(x, y, z) + Math.pow(surface(x, y, z) / 6.0, 1.5)) / 2;
    }

    public static int compare(int ax, int ay, int az, int bx, int by, int bz) {
        int balanced = Double.compare(cost(ax, ay, az), cost(bx, by, bz));
        if (balanced != 0) return balanced;
        int size = Long.compare(volume(ax, ay, az), volume(bx, by, bz));
        if (size != 0) return size;
        int shape = Long.compare(surface(ax, ay, az), surface(bx, by, bz));
        return shape != 0 ? shape : Integer.compare(Math.max(ax, Math.max(ay, az)), Math.max(bx, Math.max(by, bz)));
    }
}
