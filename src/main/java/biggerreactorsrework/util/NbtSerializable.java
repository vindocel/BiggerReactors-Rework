package biggerreactorsrework.util;

import net.minecraft.nbt.CompoundTag;

/**
 * NBT round-trip for simulation state that only stores plain values (no registry-backed data).
 */
public interface NbtSerializable {
    CompoundTag serializeNBT();

    void deserializeNBT(CompoundTag nbt);
}
