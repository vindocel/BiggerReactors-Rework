package biggerreactorsrework.fluids;

import biggerreactorsrework.core.registry.ModFluid;
import biggerreactorsrework.core.registry.RegisterFluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

/** Component-bearing transport fluid. The origin is preserved in tanks, never placed in the reactor world. */
@RegisterFluid(name = "heated_coolant", creativeTab = false)
public class HeatedCoolant extends ModFluid {
    @RegisterFluid.Instance public static HeatedCoolant INSTANCE;
    public HeatedCoolant(BaseFlowingFluid.Properties properties) { super(properties); }
}
