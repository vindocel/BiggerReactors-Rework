package biggerreactorsrework.fluids;

import biggerreactorsrework.core.registry.ModFluid;
import biggerreactorsrework.core.registry.RegisterFluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

/** Component-bearing transport fluid. The origin is preserved in tanks, never placed in the reactor world. */
// Modified 2026-11-03: every coolant heated by the reactor burns entities like lava when placed.
@RegisterFluid(name = "heated_coolant", creativeTab = false, burns = true, registerBucket = true)
public class HeatedCoolant extends ModFluid {
    @RegisterFluid.Instance public static HeatedCoolant INSTANCE;
    public HeatedCoolant(BaseFlowingFluid.Properties properties) { super(properties); }
}
