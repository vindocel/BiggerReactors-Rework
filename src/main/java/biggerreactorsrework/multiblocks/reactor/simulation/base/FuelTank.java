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
    
    // Modified 2026-11-02: only unconsumed fuel can pay for heat, including the final fractional unit.
    public double usableFuel() { return Math.max(0, fuel - partialUsed); }
    public void burn(double amount) {
        burnedLastTick = 0;
        if (!Double.isFinite(amount) || amount <= 0 || fuel <= 0) return;
        double available = usableFuel();
        double consumed = Math.min(amount, available);
        if (consumed <= 0) return;
        double toProcess = partialUsed + consumed;
        long whole = Math.min(fuel, (long) toProcess);
        fuel -= whole;
        waste = waste > Long.MAX_VALUE - whole ? Long.MAX_VALUE : waste + whole;
        partialUsed = fuel == 0 ? 0 : Math.max(0, toProcess - whole);
        burnedLastTick = consumed;
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
        amount = Math.max(0, Math.min(fuel - (partialUsed > 0 ? 1 : 0), amount));
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
        if (partialUsed >= 1 || fuel == 0) partialUsed = 0;
    }
}
