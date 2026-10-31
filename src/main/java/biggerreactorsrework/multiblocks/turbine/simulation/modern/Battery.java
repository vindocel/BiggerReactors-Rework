package biggerreactorsrework.multiblocks.turbine.simulation.modern;


import net.minecraft.nbt.CompoundTag;
import biggerreactorsrework.multiblocks.turbine.simulation.ITurbineBattery;

public class Battery implements ITurbineBattery {
    private long capacity;
    private long stored;
    private long generatedLastTick;
    private boolean capacityConfigured;
    
    @Override
    public long extract(long toExtract) {
        long amount = Math.max(0, Math.min(stored, toExtract));
        stored -= amount;
        return amount;
    }
    
    public long generate(long maxToGenerate){
        generatedLastTick = Math.max(0, Math.min(maxToGenerate, Math.max(0, capacity - stored)));
        stored += generatedLastTick;
        return generatedLastTick;
    }
    
    public long generatedLastTick(){
        return generatedLastTick;
    }
    
    @Override
    public void restoreStored(long stored) {
        this.stored = stored;
    }

    @Override
    public long stored() {
        return stored;
    }
    
    @Override
    public long capacity() {
        return capacity;
    }
    
    public void setCapacity(long capacity) {
        this.capacity = Math.max(0, capacity);
        this.capacityConfigured = true;
        stored = Math.min(stored, this.capacity);
    }
    
    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putLong("storedPower", stored);
        return nbt;
    }
    
    @Override
    public void deserializeNBT(CompoundTag nbt) {
        stored = Math.max(0, nbt.getLongOr("storedPower", 0L));
        if (capacityConfigured) stored = Math.min(stored, capacity);
    }
}
