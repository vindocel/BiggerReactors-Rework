package biggerreactorsrework.items.ingots;

import net.minecraft.world.item.Item;
import biggerreactorsrework.core.registry.RegisterItem;

import javax.annotation.Nonnull;

public class CyaniteIngot extends Item {
    
    @RegisterItem(name = "cyanite_ingot")
    public static final CyaniteIngot INSTANCE = new CyaniteIngot(new Properties());
    
    @SuppressWarnings("unused")
    public CyaniteIngot(@Nonnull Properties properties) {
        super(properties);
    }
}
