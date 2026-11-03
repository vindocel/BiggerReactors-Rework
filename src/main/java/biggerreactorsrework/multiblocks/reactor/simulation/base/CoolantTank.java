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
    
    // Modified 2026-11-02: only this tick's paid budget is processed; no productive thermal remainder.
    public biggerreactorsrework.thermal.ThermalModel.Conversion convertHeat(double heat, biggerreactorsrework.thermal.ThermalModel.Preset preset) {
        boolean water=transitionProperties instanceof FluidTransitionRegistry.FluidTransition t && t.liquids.contains(net.minecraft.world.level.material.Fluids.WATER);
        var result=biggerreactorsrework.thermal.ThermalModel.convert(heat,transitionProperties==null?0:liquidAmount,
                Math.max(0,perSideCapacity-vaporAmount),preset.heatPerCoolant(),water?preset.steamBudget():preset.heatPerCoolant());
        maxTransitionedLastTick=biggerreactorsrework.thermal.ThermalModel.wholeUnits(heat/preset.heatPerCoolant());
        transitionedLastTick=result.volume();rfTransferredLastTick=(long)result.processed();
        liquidAmount-=result.volume();vaporAmount+=result.volume();
        environmentalLossLastTick=result.processLoss();thermalRemainder=0;return result;
    }
    @Override public double transferWith(HeatBody body,double rfkt) { return 0; }
    @Override public double absorbRF(double rf) {
        if(!Double.isFinite(rf) || rf>=0)return 0;
        return -convertHeat(-rf,configuration.thermal()).processed();
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
        amount = Math.max(-liquidAmount,Math.min(amount,Math.max(0,perSideCapacity-liquidAmount)));
        liquidAmount += amount;
        return amount;
    }
    
    @Override
    public long extractLiquid(long amount) {
        amount = Math.max(0,Math.min(amount,liquidAmount));
        liquidAmount -= amount;
        return amount;
    }
    
    @Override
    public long insertVapor(long amount) {
        amount = Math.max(-vaporAmount,Math.min(amount,Math.max(0,perSideCapacity-vaporAmount)));
        vaporAmount += amount;
        if(amount<0)outputPressure.released(vaporAmount,perSideCapacity);
        return amount;
    }
    
    @Override
    public long extractVapor(long amount) {
        amount = Math.max(0,Math.min(amount,vaporAmount));
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
        compound.put("thermalModelVersion",biggerreactorsrework.thermal.ThermalModel.VERSION);
        compound.put("outputPressure",outputPressure.save());
        return compound;
    }
    
    @Override
    public void load(@Nonnull DataCompound compound) {
        liquidAmount = compound.getLong("liquidAmount");
        vaporAmount = compound.getLong("gasAmount");
        liquidAmount=Math.max(0,liquidAmount);
        vaporAmount=Math.max(0,vaporAmount);
        thermalRemainder=0;
        outputPressure.load(compound.getCompound("outputPressure").toROBNMap());
    }
}
