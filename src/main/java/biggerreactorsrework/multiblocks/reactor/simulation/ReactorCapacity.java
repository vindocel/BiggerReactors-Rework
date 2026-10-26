package biggerreactorsrework.multiblocks.reactor.simulation;

import biggerreactorsrework.core.util.Statistics;
import biggerreactorsrework.multiblocks.reactor.simulation.cpu.TimeSlicedReactorSimulation;
import biggerreactorsrework.registries.FluidTransitionRegistry;
import biggerreactorsrework.registries.ReactorModeratorRegistry;

/** Same thermal/consumption model as gameplay, with full fuel and ideal circulation. */
public final class ReactorCapacity {
    public static Statistics.Potential measure(SimulationDescription description,SimulationConfiguration config,
                                               FluidTransitionRegistry.FluidTransition profile) {
        var sim=new TimeSlicedReactorSimulation(description,config);
        if(sim.coolantTank()!=null && profile!=null) {
            sim.coolantTank().setTransitionProperties(profile);
            sim.coolantTank().setModeratorProperties(new ReactorModeratorRegistry.ModeratorProperties(.3,.7,1.35,profile.liquidRFMKT));
        }
        double burn=0,conversion=0,power=0;
        final int warm=160000,sample=20000;
        for(int tick=0;tick<warm+sample;tick++) {
            if((tick&511)==0 && Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException();
            sim.fuelTank().extractWaste(Long.MAX_VALUE,false);
            sim.fuelTank().insertFuel(Long.MAX_VALUE,false);
            if(sim.coolantTank()!=null) {
                sim.coolantTank().extractVapor(sim.coolantTank().vaporAmount());
                sim.coolantTank().insertLiquid(sim.coolantTank().perSideCapacity()-sim.coolantTank().liquidAmount());
            }
            sim.tick(true);
            if(tick>=warm) {
                burn+=sim.fuelTank().burnedLastTick();
                if(sim.coolantTank()!=null) conversion+=sim.coolantTank().transitionedLastTick();
                else { power+=sim.battery().generatedLastTick();sim.battery().extract(Long.MAX_VALUE); }
            }
        }
        return new Statistics.Potential(burn/sample,conversion/sample,power/sample);
    }
    private ReactorCapacity() { }
}
