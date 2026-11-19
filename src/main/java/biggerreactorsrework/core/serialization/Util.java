package biggerreactorsrework.core.serialization;

import net.minecraft.nbt.CompoundTag;

public class Util {
    public static CompoundTag toCompoundTag(DataCompound compound) {
//? if =26.1.2 || =26.3 {
        final var nbt = new CompoundTag();
        nbt.putByteArray("asROBN", compound.toROBN().toByteArray());
        return nbt;
//?} else if =1.21.1 {
/*        final var nbt = new CompoundTag();
        nbt.putByteArray("asROBN", compound.toROBN());
        return nbt;
*/
//?}
    }

//? if =26.1.2 || =26.3 {
    public static DataCompound toDataCompound(CompoundTag nbt) {
        return new DataCompound(nbt.getByteArray("asROBN").orElseGet(() -> new byte[0]));
    }
//?} else if =1.21.1 {
/*    public static DataCompound toDataCompound(CompoundTag nbt) {
        return new DataCompound(nbt.getByteArray("asROBN"));
    }
*/
//?}
}
