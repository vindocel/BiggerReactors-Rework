package biggerreactorsrework.items.dusts;

import net.minecraft.world.item.Item;
import biggerreactorsrework.core.registry.RegisterItem;

import javax.annotation.Nonnull;


public class LudicriteDust extends Item {

//? if =26.1.2 || =26.3 {
    @RegisterItem(name = "ludicrite_dust")
    public static final LudicriteDust INSTANCE = biggerreactorsrework.core.registry.RegistryIds.named("ludicrite_dust", () -> new LudicriteDust(new Properties()));

    @SuppressWarnings("unused")
    public LudicriteDust(@Nonnull Properties properties) {
        super(biggerreactorsrework.core.registry.RegistryIds.item(properties));
    }
//?} else if =1.21.1 {
/*    @RegisterItem(name = "ludicrite_dust")
    public static final LudicriteDust INSTANCE = new LudicriteDust(new Properties());

    @SuppressWarnings("unused")
    public LudicriteDust(@Nonnull Properties properties) {
        super(properties);
    }
*/
//?}
}
