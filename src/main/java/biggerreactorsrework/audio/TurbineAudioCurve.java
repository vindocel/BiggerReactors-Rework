package biggerreactorsrework.audio;

// Added 2026-10-28: deterministic anchor blending within the native pitch limits.
public final class TurbineAudioCurve {
    private TurbineAudioCurve() {}
    public record Voice(int anchor, float weight, float pitch) {}
    public static Voice[] voices(double rpm) {
        if (!Double.isFinite(rpm) || rpm <= 20) return new Voice[0];
        if (rpm < 150) return new Voice[]{voice(300, Math.min(1, (rpm - 20) / 130), rpm)};
        if (rpm < 450) return new Voice[]{voice(300, 1, rpm)};
        if (rpm < 600) return blend(300, 900, (rpm - 450) / 150, rpm);
        if (rpm < 1200) return new Voice[]{voice(900, 1, rpm)};
        if (rpm < 1500) return blend(900, 1800, (rpm - 1200) / 300, rpm);
        return new Voice[]{voice(1800, 1, rpm)};
    }
    private static Voice[] blend(int a, int b, double ratio, double rpm) {
        return new Voice[]{voice(a, 1 - ratio, rpm), voice(b, ratio, rpm)};
    }
    private static Voice voice(int anchor, double weight, double rpm) {
        return new Voice(anchor, (float)weight, (float)Math.clamp(rpm / anchor, .5, 2));
    }
}
