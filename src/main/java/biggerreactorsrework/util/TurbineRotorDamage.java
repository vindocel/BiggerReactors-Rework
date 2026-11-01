package biggerreactorsrework.util;

/** Added 2026-11-01: two hits per second produce 1–10 hearts/second before normal mitigation. */
public final class TurbineRotorDamage {
    private TurbineRotorDamage() {}
    public static float perHit(double rpm, double maximumRpm) {
        if (!Double.isFinite(rpm) || rpm <= 0 || !Double.isFinite(maximumRpm) || maximumRpm <= 0) return 0;
        return (float)(1 + 9 * Math.min(1, rpm / maximumRpm));
    }
}
