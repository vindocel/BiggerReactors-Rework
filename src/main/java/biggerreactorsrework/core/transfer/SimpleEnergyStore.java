package biggerreactorsrework.core.transfer;

import net.minecraft.util.Mth;

/** Energy buffer with fixed capacity and per-call transfer limits. */
public class SimpleEnergyStore implements EnergyStore {
    protected int energy;
    protected final int capacity;
    private final int maxReceive;
    private final int maxExtract;

    public SimpleEnergyStore(int capacity, int maxReceive, int maxExtract) {
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        int accepted = Mth.clamp(capacity - energy, 0, Math.min(maxReceive, Math.max(0, toReceive)));
        if (!simulate) {
            energy += accepted;
        }
        return accepted;
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        int given = Math.min(energy, Math.min(maxExtract, Math.max(0, toExtract)));
        if (!simulate) {
            energy -= given;
        }
        return given;
    }

    @Override
    public int getEnergyStored() {
        return energy;
    }

    @Override
    public int getMaxEnergyStored() {
        return capacity;
    }

    @Override
    public boolean canExtract() {
        return maxExtract > 0;
    }

    @Override
    public boolean canReceive() {
        return maxReceive > 0;
    }
}
