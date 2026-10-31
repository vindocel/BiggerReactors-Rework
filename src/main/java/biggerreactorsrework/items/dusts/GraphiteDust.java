package biggerreactorsrework.items.dusts;

import net.minecraft.world.item.Item;
import biggerreactorsrework.core.registry.RegisterItem;

import javax.annotation.Nonnull;

public class GraphiteDust extends Item {
    
    @RegisterItem(name = "graphite_dust")
    public static final GraphiteDust INSTANCE = biggerreactorsrework.core.registry.RegistryIds.named("graphite_dust", () -> new GraphiteDust(new Properties()));
    
    @SuppressWarnings("unused")
    public GraphiteDust(@Nonnull Properties properties) {
        super(biggerreactorsrework.core.registry.RegistryIds.item(properties));
    }
}