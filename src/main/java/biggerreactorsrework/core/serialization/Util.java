package biggerreactorsrework.core.serialization;

import net.minecraft.nbt.CompoundTag;

public class Util {
    public static CompoundTag toCompoundTag(DataCompound compound) {
        final var nbt = new CompoundTag();
        nbt.putByteArray("asROBN", compound.toROBN().toByteArray());
        return nbt;
    }
    
    public static DataCompound toDataCompound(CompoundTag nbt) {
        return new DataCompound(nbt.getByteArray("asROBN").orElseGet(() -> new byte[0]));
    }
}
