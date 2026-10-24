package biggerreactorsrework.fluids;

import biggerreactorsrework.core.registry.ModFluid;
import biggerreactorsrework.core.registry.RegisterFluid;

import javax.annotation.Nonnull;

@RegisterFluid(name = "liquid_uranium", color = 0xFFBCBA50)
public class LiquidUranium extends ModFluid {
    
    @RegisterFluid.Instance
    public static LiquidUranium INSTANCE;
    
    public LiquidUranium(@Nonnull Properties properties) {
        super(properties);
    }
}
