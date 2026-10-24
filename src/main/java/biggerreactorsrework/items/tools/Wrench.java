package biggerreactorsrework.items.tools;

import net.minecraft.world.item.Item;
import biggerreactorsrework.core.registry.RegisterItem;

import javax.annotation.Nonnull;

public class Wrench extends Item {
    
    @RegisterItem(name = "wrench")
    public static final Wrench INSTANCE = new Wrench(new Properties());
    
    @SuppressWarnings("unused")
    public Wrench(@Nonnull Properties properties) {
        super(properties.stacksTo(1));
    }
}
