package biggerreactorsrework.items.dusts;

import net.minecraft.world.item.Item;
import biggerreactorsrework.core.registry.RegisterItem;

import javax.annotation.Nonnull;

public class BlutoniumDust extends Item {
    
    @RegisterItem(name = "blutonium_dust")
    public static final BlutoniumDust INSTANCE = biggerreactorsrework.core.registry.RegistryIds.named("blutonium_dust", () -> new BlutoniumDust(new Item.Properties()));
    
    @SuppressWarnings("unused")
    public BlutoniumDust(@Nonnull Properties properties) {
        super(biggerreactorsrework.core.registry.RegistryIds.item(properties));
    }
}