package biggerreactorsrework.fluids;

import biggerreactorsrework.core.registry.ModFluid;
import biggerreactorsrework.core.registry.RegisterFluid;

import javax.annotation.Nonnull;

@RegisterFluid(name = "liquid_obsidian", registerBucket = true)
public class LiquidObsidian extends ModFluid {
    
    @RegisterFluid.Instance
    public static LiquidObsidian INSTANCE;
    
    public LiquidObsidian(@Nonnull Properties properties) {
        super(properties);
    }
}
