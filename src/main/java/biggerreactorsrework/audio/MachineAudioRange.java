package biggerreactorsrework.audio;

// Added 2026-10-28: operating volume stays calibrated; only propagation is extended.
public final class MachineAudioRange {
    private MachineAudioRange() {}
    // Modified 2026-10-28: all three multiblocks use the same propagation distance.
    public static int blocks(int kind) { return 25; }
    public static int trackingBlocks(int kind) { return blocks(kind) + 2; }
}
