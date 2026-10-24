package biggerreactorsrework.multiblocks.reactor.simulation.base;

import biggerreactorsrework.multiblocks.reactor.simulation.SimulationConfiguration;
import biggerreactorsrework.multiblocks.reactor.simulation.IReactorSimulation;
import biggerreactorsrework.core.serialization.ISerializable;
import biggerreactorsrework.core.serialization.DataCompound;
import biggerreactorsrework.core.util.HeatBody;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Battery extends HeatBody implements IReactorSimulation.IBattery, ISerializable {
    
    private final SimulationConfiguration configuration;
    private final long capacity;
    private long stored;
    private long generatedLastTick;
    
    {
        setInfinite(true);
    }
    
    public Battery(long capacity, SimulationConfiguration configuration) {
        this.configuration = configuration;
        this.capacity = capacity;
    }
    
    @Override
    public double transferWith(HeatBody other, double rfkt) {
        double newTemp = other.temperature() - temperature();
        newTemp *= Math.exp(-rfkt / other.rfPerKelvin());
        newTemp += temperature();
        
        double rfTransferred = (newTemp - other.temperature()) * other.rfPerKelvin();
        
        generatedLastTick = (long) (-rfTransferred * configuration.outputMultiplier() * configuration.passiveOutputMultiplier());
        stored += generatedLastTick;
        if (stored > capacity) {
            stored = capacity;
        }
        
        other.setTemperature(newTemp);
        
        return rfTransferred;
    }
    
    @Override
    public long extract(long toExtract) {
        stored -= toExtract;
        return toExtract;
    }
    
    @Override
    public long stored() {
        return stored;
    }
    
    @Override
    public long capacity() {
        return capacity;
    }
    
    @Override
    public long generatedLastTick() {
        return generatedLastTick;
    }
    
    @Nullable
    @Override
    public DataCompound save() {
        var compound = new DataCompound();
        compound.put("storedPower", stored);
        return compound;
    }
    
    @Override
    public void load(@Nonnull DataCompound compound) {
        stored = compound.getLong("storedPower");
    }
}