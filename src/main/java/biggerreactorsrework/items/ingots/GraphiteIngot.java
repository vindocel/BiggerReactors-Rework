package biggerreactorsrework.items.ingots;

import net.minecraft.world.item.Item;
import biggerreactorsrework.core.registry.RegisterItem;

import javax.annotation.Nonnull;

public class GraphiteIngot extends Item {
    
    @RegisterItem(name = "graphite_ingot")
    public static final GraphiteIngot INSTANCE = biggerreactorsrework.core.registry.RegistryIds.named("graphite_ingot", () -> new GraphiteIngot(new Properties()));
    
    @SuppressWarnings("unused")
    public GraphiteIngot(@Nonnull Properties properties) {
        super(biggerreactorsrework.core.registry.RegistryIds.item(properties));
    }
}
