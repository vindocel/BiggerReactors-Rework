package biggerreactorsrework.items.dusts;

import net.minecraft.world.item.Item;
import biggerreactorsrework.core.registry.RegisterItem;

import javax.annotation.Nonnull;

public class UraniumDust extends Item {
    
    @RegisterItem(name = "uranium_dust")
    public static final UraniumDust INSTANCE = biggerreactorsrework.core.registry.RegistryIds.named("uranium_dust", () -> new UraniumDust(new Properties()));
    
    @SuppressWarnings("unused")
    public UraniumDust(@Nonnull Properties properties) {
        super(biggerreactorsrework.core.registry.RegistryIds.item(properties));
    }
}