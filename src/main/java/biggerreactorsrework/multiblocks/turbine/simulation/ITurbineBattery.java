package biggerreactorsrework.multiblocks.turbine.simulation;

import net.minecraft.nbt.CompoundTag;
import biggerreactorsrework.util.NbtSerializable;

public interface ITurbineBattery extends NbtSerializable {
    long extract(long toExtract);

//? if =26.1.2 || =26.3 {
    long stored();

    void restoreStored(long stored);

    long capacity();
//?} else if =1.21.1 {
/*    long stored();

    long capacity();
*/
//?}
}
