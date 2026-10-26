package biggerreactorsrework.multiblocks.heatexchanger;

import biggerreactorsrework.Config;
import biggerreactorsrework.core.util.HeatBody;
import biggerreactorsrework.core.util.Statistics;
import biggerreactorsrework.registries.FluidTransitionRegistry;
import biggerreactorsrework.util.FluidTransitionTank;

/** A snapshot of the real channel geometry; same update order and losses as the controller. */
public final class HeatExchangerCapacity {
    private static final class Tank extends FluidTransitionTank {
        Tank(boolean cond,long capacity,FluidTransitionRegistry.FluidTransition profile) { super(cond);perSideCapacity=capacity;activeTransition=profile; }
        void supply() {inAmount=perSideCapacity;outAmount=0;}
    }
    public record Geometry(int condenser,int evaporator,double contact,double condenserAir,double evaporatorAir,double ambientArea,double airCapacity,double ambientTemperature) { }
    public static Statistics.Potential measure(Geometry geo,FluidTransitionRegistry.FluidTransition primary,FluidTransitionRegistry.FluidTransition secondary) {
        if(primary==null || secondary==null || primary.boilingPoint<=secondary.boilingPoint) return new Statistics.Potential(0,0,0);
        var cfg=Config.CONFIG.HeatExchanger;
        var hot=new Tank(true,geo.condenser*cfg.ChannelTankVolumePerBlock,primary);
        var water=new Tank(false,geo.evaporator*cfg.ChannelTankVolumePerBlock,secondary);
        HeatBody c=new HeatBody(),e=new HeatBody(),a=new HeatBody(),ambient=new HeatBody();
        c.setRfPerKelvin(geo.condenser*cfg.ChannelFEPerKelvinUnitVolume);
        e.setRfPerKelvin(geo.evaporator*cfg.ChannelFEPerKelvinUnitVolume);
        a.setRfPerKelvin(geo.airCapacity);
        ambient.setInfinite(true);
        for(var b:new HeatBody[]{c,e,a,ambient})b.setTemperature(geo.ambientTemperature);
        double converted=0,steam=0;
        for(int tick=0;tick<70000;tick++) {
            if((tick&511)==0 && Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException();
            hot.supply();water.supply();
            hot.transferWith(c,geo.condenser*cfg.ChannelInternalSurfaceArea);
            c.transferWith(a,geo.condenserAir);
            c.transferWith(e,geo.contact);
            e.transferWith(a,geo.evaporatorAir);
            water.transferWith(e,geo.evaporator*cfg.ChannelInternalSurfaceArea);
            a.transferWith(ambient,geo.ambientArea);
            if(tick>=60000) {converted+=hot.transitionedLastTick();steam+=water.transitionedLastTick();}
        }
        return new Statistics.Potential(steam/10000,converted/10000,0);
    }
    private HeatExchangerCapacity() { }
}
