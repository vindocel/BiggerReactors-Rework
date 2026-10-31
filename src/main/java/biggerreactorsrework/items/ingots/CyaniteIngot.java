package biggerreactorsrework.items.ingots;

import net.minecraft.world.item.Item;
import biggerreactorsrework.core.registry.RegisterItem;

import javax.annotation.Nonnull;

public class CyaniteIngot extends Item {
    
    @RegisterItem(name = "cyanite_ingot")
    public static final CyaniteIngot INSTANCE = biggerreactorsrework.core.registry.RegistryIds.named("cyanite_ingot", () -> new CyaniteIngot(new Properties()));
    
    @SuppressWarnings("unused")
    public CyaniteIngot(@Nonnull Properties properties) {
        super(biggerreactorsrework.core.registry.RegistryIds.item(properties));
    }
}
