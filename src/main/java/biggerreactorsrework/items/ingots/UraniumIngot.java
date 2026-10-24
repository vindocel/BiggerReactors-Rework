package biggerreactorsrework.items.ingots;

import net.minecraft.world.item.Item;
import biggerreactorsrework.core.registry.RegisterItem;

import javax.annotation.Nonnull;

public class UraniumIngot extends Item {
    
    @RegisterItem(name = "uranium_ingot")
    public static final UraniumIngot INSTANCE = new UraniumIngot(new Properties());
    
    @SuppressWarnings("unused")
    public UraniumIngot(@Nonnull Properties properties) {
        super(properties);
    }
}
