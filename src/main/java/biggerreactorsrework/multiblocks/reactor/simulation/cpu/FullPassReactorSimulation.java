package biggerreactorsrework.multiblocks.reactor.simulation.cpu;
import biggerreactorsrework.multiblocks.reactor.simulation.*;
import biggerreactorsrework.multiblocks.reactor.simulation.base.BaseReactorSimulation;
// Modified 2026-11-02: retained construction API delegates to the single synchronous thermal model.
@Deprecated
public class FullPassReactorSimulation extends BaseReactorSimulation {
    public FullPassReactorSimulation(SimulationDescription description, SimulationConfiguration configuration) { super(description,configuration); }

    public static class MultiThreaded extends FullPassReactorSimulation {
        public MultiThreaded(SimulationDescription description, SimulationConfiguration configuration, boolean singleThread) { super(description,configuration); }
    }
}
