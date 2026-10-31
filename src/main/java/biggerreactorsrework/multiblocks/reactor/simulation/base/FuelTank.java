package biggerreactorsrework.multiblocks.reactor.simulation.base;

import biggerreactorsrework.multiblocks.reactor.simulation.IReactorSimulation;
import biggerreactorsrework.core.serialization.ISerializable;
import biggerreactorsrework.core.serialization.DataCompound;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FuelTank implements IReactorSimulation.IFuelTank, ISerializable {
    
    private final long capacity;
    
    private long fuel = 0;
    private long waste = 0;
    
    private double partialUsed = 0;
    
    private double burnedLastTick = 0;
    
    public FuelTank(long capacity) {
        this.capacity = capacity;
    }
    
    public void burn(double amount) {
        if (Double.isInfinite(amount) || Double.isNaN(amount) || amount == 0) {
            burnedLastTick = 0;
            return;
        }
        
        double toProcess = partialUsed + amount;
        toProcess = Math.min(toProcess, (double) fuel);
        
        burnedLastTick = toProcess - partialUsed;
        
        partialUsed = toProcess;
        if (toProcess >= 1) {
            long toBurn = (long) toProcess;
            fuel -= toBurn;
            waste += toBurn;
            partialUsed -= toBurn;
        }
    }
    
    @Override
    public long capacity() {
        return capacity;
    }
    
    @Override
    public long totalStored() {
        // Modified 2026-10-30: retain overfilled stock without wrapping its sum.
        return fuel > Long.MAX_VALUE - waste ? Long.MAX_VALUE : fuel + waste;
    }
    
    @Override
    public long fuel() {
        return fuel;
    }
    
    @Override
    public long waste() {
        return waste;
    }
    
    @Override
    public long insertFuel(long amount, boolean simulated) {
        if (totalStored() >= capacity) {
            // if we are overfilled, then we need to *not* insert more
            return 0;
        }
        
        amount = Math.max(0, Math.min(amount, capacity - totalStored()));
        
        if (!simulated) {
            fuel += amount;
        }
        
        return amount;
    }
    
    @Override
    public long insertWaste(long amount, boolean simulated) {
        if (totalStored() >= capacity) {
            // if we are overfilled, then we need to *not* insert more
            return 0;
        }
        
        amount = Math.max(0, Math.min(amount, capacity - totalStored()));
        
        if (!simulated) {
            waste += amount;
        }
        
        return amount;
    }
    
    @Override
    public long extractFuel(long amount, boolean simulated) {
        amount = Math.max(0, Math.min(fuel, amount));
        if (!simulated) {
            fuel -= amount;
        }
        return amount;
    }
    
    @Override
    public long extractWaste(long amount, boolean simulated) {
        amount = Math.max(0, Math.min(waste, amount));
        if (!simulated) {
            waste -= amount;
        }
        return amount;
    }
    
    @Override
    public void restoreStored(long fuel, long waste) {
        this.fuel = fuel;
        this.waste = waste;
    }

    @Override
    public double burnedLastTick() {
        return burnedLastTick;
    }
    
    @Nullable
    @Override
    public DataCompound save() {
        DataCompound compound = new DataCompound();
        compound.put("fuel", fuel);
        compound.put("waste", waste);
        compound.put("partialUsed", partialUsed);
        return compound;
    }
    
    @Override
    public void load(@Nonnull DataCompound compound) {
        fuel = Math.max(0, compound.getLong("fuel"));
        waste = Math.max(0, compound.getLong("waste"));
        partialUsed = biggerreactorsrework.core.util.SavedValues.nonNegative(compound.getDouble("partialUsed"), 0);
        if (partialUsed >= 1) partialUsed = 0;
    }
}
