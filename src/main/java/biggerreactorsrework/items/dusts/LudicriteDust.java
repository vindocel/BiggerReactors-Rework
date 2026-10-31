package biggerreactorsrework.items.dusts;

import net.minecraft.world.item.Item;
import biggerreactorsrework.core.registry.RegisterItem;

import javax.annotation.Nonnull;


public class LudicriteDust extends Item {
    
    @RegisterItem(name = "ludicrite_dust")
    public static final LudicriteDust INSTANCE = biggerreactorsrework.core.registry.RegistryIds.named("ludicrite_dust", () -> new LudicriteDust(new Properties()));
    
    @SuppressWarnings("unused")
    public LudicriteDust(@Nonnull Properties properties) {
        super(biggerreactorsrework.core.registry.RegistryIds.item(properties));
    }
}