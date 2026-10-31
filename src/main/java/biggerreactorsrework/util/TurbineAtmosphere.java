package biggerreactorsrework.util;

/** Added 2026-10-31: bounded cosmetic steam density, independent of rotor speed and energy. */
public final class TurbineAtmosphere {
    private TurbineAtmosphere() {}

    public static float density(long stored, long capacity) {
        if (stored <= 0 || capacity <= 0) return 0;
        double fraction = Math.clamp((double) stored / capacity, 0, 1);
        return (float) Math.sqrt(fraction);
    }

    /** Modified 2026-10-31: real through-flow remains visible even when conversion empties the input tank. */
    public static float density(long stored, long capacity, boolean active, long actualFlow, long selectedFlow) {
        float reserve = density(stored, capacity);
        float flowing = active ? density(actualFlow, selectedFlow) : 0;
        return Math.max(reserve, flowing);
    }

    public static int opacity(float density) {
        return Float.isFinite(density) ? Math.round(32 * Math.clamp(density, 0, 1)) : 0;
    }

    /** Consecutive server ticks of real operation; idle steam and coast-down cannot warm condensation. */
    public static final class CondensationTimer {
        public static final int WARMUP_TICKS = 60;
        private int ticks;
        public boolean tick(boolean active, long flow, double rpm) {
            if (!active || flow <= 0 || !Double.isFinite(rpm) || rpm < 1) { reset(); return false; }
            ticks = Math.min(WARMUP_TICKS, ticks + 1);
            return ready();
        }
        public boolean ready() { return ticks >= WARMUP_TICKS; }
        public void reset() { ticks = 0; }
    }

    public static boolean condensationSurface(boolean ceiling, boolean coil, boolean freeBelow, int y, int minY, int maxY) {
        return freeBelow && (ceiling || coil) && y >= minY + (maxY - minY + 1) / 2 && y <= maxY;
    }
}
