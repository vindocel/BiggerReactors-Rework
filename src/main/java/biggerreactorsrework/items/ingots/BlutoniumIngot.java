package biggerreactorsrework.items.ingots;

import net.minecraft.world.item.Item;
import biggerreactorsrework.core.registry.RegisterItem;

import javax.annotation.Nonnull;

public class BlutoniumIngot extends Item {
    
    @RegisterItem(name = "blutonium_ingot")
    public static final BlutoniumIngot INSTANCE = biggerreactorsrework.core.registry.RegistryIds.named("blutonium_ingot", () -> new BlutoniumIngot(new Properties()));
    
    @SuppressWarnings("unused")
    public BlutoniumIngot(@Nonnull Properties properties) {
        super(biggerreactorsrework.core.registry.RegistryIds.item(properties));
    }
}
