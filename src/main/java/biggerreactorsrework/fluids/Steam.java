package biggerreactorsrework.fluids;

import biggerreactorsrework.core.registry.ModFluid;
import biggerreactorsrework.core.registry.RegisterFluid;

import javax.annotation.Nonnull;

// Modified 2026-11-03: steam is a gas; it moves only through closed tanks and pipes, never buckets.
@RegisterFluid(name = "steam", color = 0xF0FFFFFF)
public class Steam extends ModFluid {
    
    @RegisterFluid.Instance
    public static Steam INSTANCE;
    
    public Steam(@Nonnull Properties properties) {
        super(properties);
    }
}
