package biggerreactorsrework.audio;

// Added 2026-10-28: compact server-owned operating state, independent of menus.
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

public record MachineAudioState(int kind, BlockPos min, BlockPos max, boolean running, double rpm) {
    public static final int REACTOR = 0, BOILER = 1, TURBINE = 2;
    public static final MachineAudioState EMPTY = new MachineAudioState(-1, BlockPos.ZERO, BlockPos.ZERO, false, 0);

    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putInt("kind", kind);
        tag.putLong("min", min.asLong());
        tag.putLong("max", max.asLong());
        tag.putBoolean("running", running);
        tag.putDouble("rpm", rpm);
        return tag;
    }

    public static MachineAudioState load(CompoundTag tag) {
        if (!tag.contains("kind") || !tag.contains("min") || !tag.contains("max")) return EMPTY;
        int kind = tag.getIntOr("kind", 0);
        double rpm = tag.getDoubleOr("rpm", 0D);
        var min = BlockPos.of(tag.getLongOr("min", 0L));
        var max = BlockPos.of(tag.getLongOr("max", 0L));
        if (kind < REACTOR || kind > TURBINE || !Double.isFinite(rpm) || rpm < 0
                || min.getX() > max.getX() || min.getY() > max.getY() || min.getZ() > max.getZ()) return EMPTY;
        return new MachineAudioState(kind, min, max, tag.getBooleanOr("running", false), rpm);
    }
}
