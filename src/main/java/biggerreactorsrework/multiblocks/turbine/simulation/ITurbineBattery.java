package biggerreactorsrework.multiblocks.turbine.simulation;

import net.minecraft.nbt.CompoundTag;
import biggerreactorsrework.util.NbtSerializable;

public interface ITurbineBattery extends NbtSerializable {
    long extract(long toExtract);
    
    long stored();

    void restoreStored(long stored);
    
    long capacity();
}
