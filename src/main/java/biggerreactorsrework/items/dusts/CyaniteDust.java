package biggerreactorsrework.items.dusts;

import net.minecraft.world.item.Item;
import biggerreactorsrework.core.registry.RegisterItem;

import javax.annotation.Nonnull;

public class CyaniteDust extends Item {
    
    @RegisterItem(name = "cyanite_dust")
    public static final CyaniteDust INSTANCE = biggerreactorsrework.core.registry.RegistryIds.named("cyanite_dust", () -> new CyaniteDust(new Item.Properties()));
    
    @SuppressWarnings("unused")
    public CyaniteDust(@Nonnull Properties properties) {
        super(biggerreactorsrework.core.registry.RegistryIds.item(properties));
    }
}