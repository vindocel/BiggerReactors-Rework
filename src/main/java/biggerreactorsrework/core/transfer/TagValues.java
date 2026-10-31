package biggerreactorsrework.core.transfer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.storage.ValueInput;

/** Block entity data read as one NBT compound, key by key at the top level. */
public final class TagValues {
    private TagValues() {
    }

    public static CompoundTag read(ValueInput input) {
        CompoundTag tag = new CompoundTag();
        for (String key : input.keySet()) {
            input.read(key, ExtraCodecs.NBT).ifPresent(value -> tag.put(key, value));
        }
        return tag;
    }
}
