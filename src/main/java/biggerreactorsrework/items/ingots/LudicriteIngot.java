package biggerreactorsrework.items.ingots;

import net.minecraft.world.item.Item;
import biggerreactorsrework.core.registry.RegisterItem;

import javax.annotation.Nonnull;

public class LudicriteIngot extends Item {

//? if =26.1.2 || =26.3 {
    @RegisterItem(name = "ludicrite_ingot")
    public static final LudicriteIngot INSTANCE = biggerreactorsrework.core.registry.RegistryIds.named("ludicrite_ingot", () -> new LudicriteIngot(new Properties()));

    @SuppressWarnings("unused")
    public LudicriteIngot(@Nonnull Properties properties) {
        super(biggerreactorsrework.core.registry.RegistryIds.item(properties));
    }
//?} else if =1.21.1 {
/*    @RegisterItem(name = "ludicrite_ingot")
    public static final LudicriteIngot INSTANCE = new LudicriteIngot(new Properties());

    @SuppressWarnings("unused")
    public LudicriteIngot(@Nonnull Properties properties) {
        super(properties);
    }
*/
//?}
}
