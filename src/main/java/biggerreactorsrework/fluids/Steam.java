package biggerreactorsrework.fluids;

import biggerreactorsrework.core.registry.ModFluid;
import biggerreactorsrework.core.registry.RegisterFluid;

import javax.annotation.Nonnull;

@RegisterFluid(name = "steam", registerBucket = true)
public class Steam extends ModFluid {
    
    @RegisterFluid.Instance
    public static Steam INSTANCE;
    
    public Steam(@Nonnull Properties properties) {
        super(properties);
    }
}
