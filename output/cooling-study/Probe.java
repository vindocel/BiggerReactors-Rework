import biggerreactorsrework.multiblocks.reactor.simulation.*;
import biggerreactorsrework.multiblocks.reactor.simulation.cpu.TimeSlicedReactorSimulation;
import biggerreactorsrework.registries.ReactorModeratorRegistry.ModeratorProperties;
import biggerreactorsrework.registries.FluidTransitionRegistry.TransitionProperties;

class Probe {
  public static void main(String[] args) {
    var description = new SimulationDescription();
    description.setSize(3,3,3);
    description.setDefaultIModeratorProperties(new ModeratorProperties(.1,.25,1.1,.05));
    description.setControlRod(1,1,true);
    for(int y=0;y<3;y++) {
      description.setManifold(0,y,1,true);
      description.setManifold(2,y,1,true);
      description.setManifold(1,y,0,true);
      description.setManifold(1,y,2,true);
    }
    var sim = new TimeSlicedReactorSimulation(description,new SimulationConfiguration(293.15,false));
    sim.coolantTank().setModeratorProperties(new ModeratorProperties(.3,.7,1.35,.75));
    sim.coolantTank().setTransitionProperties(new TransitionProperties(15,1273.15,1.9,1,0));
    sim.coolantTank().insertLiquid(sim.coolantTank().perSideCapacity());
    for(int i=0;i<20000;i++) {
      sim.fuelTank().extractWaste(Long.MAX_VALUE,false);
      sim.fuelTank().insertFuel(Long.MAX_VALUE,false);
      sim.coolantTank().extractVapor(sim.coolantTank().vaporAmount());
      sim.coolantTank().insertLiquid(sim.coolantTank().perSideCapacity()-sim.coolantTank().liquidAmount());
      sim.tick(true);
    }
    System.out.printf("burn=%f fuelK=%f casingK=%f coolantPerTick=%d cap=%d%n",sim.fuelTank().burnedLastTick(),sim.fuelHeat(),sim.stackHeat(),sim.coolantTank().transitionedLastTick(),sim.coolantTank().perSideCapacity());
  }
}
