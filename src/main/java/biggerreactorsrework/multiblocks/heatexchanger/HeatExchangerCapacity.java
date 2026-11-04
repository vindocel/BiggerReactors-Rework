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
    public record Geometry(int condenser,int evaporator,double contact,double condenserAir,double evaporatorAir,double ambientArea,double airCapacity,double ambientTemperature,double thermalLinks) {
        public Geometry(int condenser,int evaporator,double contact,double condenserAir,double evaporatorAir,double ambientArea,double airCapacity,double ambientTemperature) {
            this(condenser,evaporator,contact,condenserAir,evaporatorAir,ambientArea,airCapacity,ambientTemperature,contact/Config.CONFIG.HeatExchanger.ChannelFEPerKelvinMetreSquared);
        }
    }
    public static Statistics.Potential measure(Geometry geo,FluidTransitionRegistry.FluidTransition primary,FluidTransitionRegistry.FluidTransition secondary) {
        if(primary==null || secondary==null)return new Statistics.Potential(0,0,0);
        var cfg=Config.CONFIG.HeatExchanger;var reactor=Config.CONFIG.Reactor;
        var preset=reactor.Thermal.snapshot(reactor.PerFuelRodCapacity,reactor.CoolantTankAmountPerFuelRod);
        var hot=new Tank(true,geo.condenser*cfg.ChannelTankVolumePerBlock,primary);
        var water=new Tank(false,geo.evaporator*cfg.evaporatorTankVolumePerBlock(),secondary);
        var exchange=new biggerreactorsrework.thermal.ThermalExchanger();double converted=0,steam=0;
        for(int tick=0;tick<100;tick++) {
            if(Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException();
            hot.supply();water.supply();exchange.tick(hot,water,geo.thermalLinks*preset.exchangerRate(),preset);
            converted+=exchange.hotLastTick;steam+=exchange.steamLastTick;
        }
        return new Statistics.Potential(steam/100,converted/100,0);
    }
    private HeatExchangerCapacity() { }
}
