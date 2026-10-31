package biggerreactorsrework.multiblocks.reactor.simulation.base;

import biggerreactorsrework.multiblocks.reactor.simulation.SimulationConfiguration;
import biggerreactorsrework.multiblocks.reactor.simulation.IReactorSimulation;
import biggerreactorsrework.registries.FluidTransitionRegistry;
import biggerreactorsrework.registries.ReactorModeratorRegistry;
import biggerreactorsrework.core.serialization.ISerializable;
import biggerreactorsrework.core.serialization.DataCompound;
import biggerreactorsrework.core.util.HeatBody;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CoolantTank extends HeatBody implements IReactorSimulation.ICoolantTank, ReactorModeratorRegistry.IModeratorProperties, ISerializable {
    private record TransferState(long liquid, long vapor, double thermalRemainder, double[] pressureHistory) {}

    @Override
    public Object transferSnapshot() {
        return new TransferState(liquidAmount, vaporAmount, thermalRemainder, outputPressure.history());
    }

    @Override
    public void restoreTransferSnapshot(Object snapshot) {
        if (!(snapshot instanceof TransferState state)) return;
        liquidAmount = state.liquid;
        vaporAmount = state.vapor;
        thermalRemainder = state.thermalRemainder;
        outputPressure.restoreHistory(state.pressureHistory);
    }

    // Modified 2026-10-26: applies only to this active-reactor tank.
    public final biggerreactorsrework.core.util.OutputBackpressure outputPressure=new biggerreactorsrework.core.util.OutputBackpressure();
    public void updateOutputPressure(){outputPressure.tick(vaporAmount,perSideCapacity);}
    private final SimulationConfiguration configuration;
    
    private final long perSideCapacity;
    private long liquidAmount = 0;
    private long vaporAmount = 0;
    
    private final ReactorModeratorRegistry.IModeratorProperties defaultModeratorProperties;
    @Nullable
    private ReactorModeratorRegistry.IModeratorProperties moderatorProperties = null;
    @Nullable
    private FluidTransitionRegistry.ITransitionProperties transitionProperties = null;
    private long maxTransitionedLastTick;
    private long transitionedLastTick;
    private long rfTransferredLastTick;
    private double thermalRemainder;
    public double environmentalLossLastTick;
    public FluidTransitionRegistry.ITransitionProperties transition() { return transitionProperties; }
    public double recovery() { return transitionProperties instanceof FluidTransitionRegistry.FluidTransition t ? t.recovery : 1; }
    
    CoolantTank(long perSideCapacity, ReactorModeratorRegistry.IModeratorProperties defaultModeratorProperties, SimulationConfiguration configuration) {
        this.configuration = configuration;
        this.perSideCapacity = perSideCapacity;
        this.defaultModeratorProperties = defaultModeratorProperties;
        this.setInfinite(true);
    }
    
    @Override
    public double transferWith(HeatBody body, double rfkt) {
        if (transitionProperties == null) {
            environmentalLossLastTick=0;
            maxTransitionedLastTick = 0;
            transitionedLastTick = 0;
            rfTransferredLastTick = 0;
            return 0;
        }
        
        rfkt *= transitionProperties.liquidRFMKT();
        
        double multiplier = (double) liquidAmount / (double) perSideCapacity;
        rfkt *= Math.max(multiplier, 0.01);
        
        double newTemp = body.temperature() - transitionProperties.boilingPoint();
        newTemp *= Math.exp(-rfkt / body.rfPerKelvin());
        newTemp += transitionProperties.boilingPoint();
        
        double toTransfer = newTemp - body.temperature();
        toTransfer *= body.rfPerKelvin();
        
        toTransfer = absorbRF(toTransfer);
        
        body.absorbRF(toTransfer);
        return -toTransfer;
    }
    
    @Override
    public double absorbRF(double rf) {
        environmentalLossLastTick=0;
        if (!Double.isFinite(rf) || rf > 0 || transitionProperties == null) {
            maxTransitionedLastTick = 0;
            transitionedLastTick = 0;
            rfTransferredLastTick = 0;
            return 0;
        }
        
        rf = Math.abs(rf)*outputPressure.limit();
    
        final double effectiveLatentHeat = transitionProperties.latentHeat();
        
        double captured=rf*recovery();
        long toTransition = (long) (captured / effectiveLatentHeat);
        final long maxTransitionable = Math.min(liquidAmount, perSideCapacity - vaporAmount);
        
        maxTransitionedLastTick = toTransition;
        var result=PhaseChange.accept(captured,effectiveLatentHeat,maxTransitionable,thermalRemainder);
        toTransition = result.units();
        thermalRemainder = result.remainder();
        transitionedLastTick = toTransition;
        
        liquidAmount -= transitionedLastTick;
        vaporAmount += transitionedLastTick;
        
        rf = result.accepted();
        rfTransferredLastTick = (long) rf;
        environmentalLossLastTick=rf/recovery()-rf;
        rf+=environmentalLossLastTick;
        rf *= -1;
        
        return rf;
    }
    
    @Override
    public void dumpLiquid() {
        liquidAmount = 0;
        if(vaporAmount==0)thermalRemainder=0;
    }
    
    @Override
    public void dumpVapor() {
        vaporAmount = 0;
        outputPressure.released(vaporAmount,perSideCapacity);
        if(liquidAmount==0)thermalRemainder=0;
    }
    
    @Override
    public long insertLiquid(long amount) {
        liquidAmount += amount;
        return amount;
    }
    
    @Override
    public long extractLiquid(long amount) {
        liquidAmount -= amount;
        return amount;
    }
    
    @Override
    public long insertVapor(long amount) {
        vaporAmount += amount;
        if(amount<0)outputPressure.released(vaporAmount,perSideCapacity);
        return amount;
    }
    
    @Override
    public long extractVapor(long amount) {
        vaporAmount -= amount;
        if(amount>0)outputPressure.released(vaporAmount,perSideCapacity);
        return amount;
    }
    
    @Override
    public long liquidAmount() {
        return liquidAmount;
    }
    
    @Override
    public long vaporAmount() {
        return vaporAmount;
    }
    
    @Override
    public long perSideCapacity() {
        return perSideCapacity;
    }
    
    @Override
    public void setModeratorProperties(ReactorModeratorRegistry.IModeratorProperties moderatorProperties) {
        this.moderatorProperties = moderatorProperties;
    }
    
    @Override
    public void setTransitionProperties(FluidTransitionRegistry.ITransitionProperties transitionProperties) {
        this.transitionProperties = transitionProperties;
        if(transitionProperties==null)thermalRemainder=0;
    }
    
    @Override
    public long transitionedLastTick() {
        return transitionedLastTick;
    }
    
    @Override
    public long maxTransitionedLastTick() {
        return maxTransitionedLastTick;
    }
    
    @Override
    public long rfTransferredLastTick() {
        return rfTransferredLastTick;
    }
    
    @Override
    public double absorption() {
        if (perSideCapacity == 0 || moderatorProperties == null) {
            return defaultModeratorProperties.absorption();
        }
        double absorption = 0;
        absorption += defaultModeratorProperties.absorption() * ((perSideCapacity) - (liquidAmount));
        absorption += moderatorProperties.absorption() * liquidAmount;
        absorption /= perSideCapacity;
        return absorption;
    }
    
    @Override
    public double heatEfficiency() {
        if (perSideCapacity == 0 || moderatorProperties == null) {
            return defaultModeratorProperties.heatEfficiency();
        }
        double heatEfficiency = 0;
        heatEfficiency += defaultModeratorProperties.heatEfficiency() * ((perSideCapacity) - (liquidAmount));
        heatEfficiency += moderatorProperties.heatEfficiency() * liquidAmount;
        heatEfficiency /= perSideCapacity;
        return heatEfficiency;
    }
    
    @Override
    public double moderation() {
        if (perSideCapacity == 0 || moderatorProperties == null) {
            return defaultModeratorProperties.moderation();
        }
        double moderation = 0;
        moderation += defaultModeratorProperties.moderation() * ((perSideCapacity) - (liquidAmount));
        moderation += moderatorProperties.moderation() * liquidAmount;
        moderation /= perSideCapacity;
        return moderation;
    }
    
    @Override
    public double heatConductivity() {
        if (perSideCapacity == 0 || moderatorProperties == null) {
            return defaultModeratorProperties.heatConductivity();
        }
        double heatConductivity = 0;
        heatConductivity += defaultModeratorProperties.heatConductivity() * ((perSideCapacity) - (liquidAmount));
        heatConductivity += moderatorProperties.heatConductivity() * liquidAmount;
        heatConductivity /= perSideCapacity;
        return heatConductivity;
    }
    
    @Nullable
    @Override
    public DataCompound save() {
        var compound = new DataCompound();
        compound.put("liquidAmount", liquidAmount);
        compound.put("gasAmount", vaporAmount);
        compound.put("thermalRemainder",thermalRemainder);
        compound.put("outputPressure",outputPressure.save());
        return compound;
    }
    
    @Override
    public void load(@Nonnull DataCompound compound) {
        liquidAmount = compound.getLong("liquidAmount");
        vaporAmount = compound.getLong("gasAmount");
        liquidAmount=Math.max(0,liquidAmount);
        vaporAmount=Math.max(0,vaporAmount);
        thermalRemainder=Math.max(0,compound.getDouble("thermalRemainder"));
        if(!Double.isFinite(thermalRemainder))thermalRemainder=0;
        outputPressure.load(compound.getCompound("outputPressure").toROBNMap());
    }
}
