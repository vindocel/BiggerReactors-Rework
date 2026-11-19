package biggerreactorsrework.items.ingots;

import net.minecraft.world.item.Item;
import biggerreactorsrework.core.registry.RegisterItem;

import javax.annotation.Nonnull;

public class CyaniteIngot extends Item {

//? if =26.1.2 || =26.3 {
    @RegisterItem(name = "cyanite_ingot")
    public static final CyaniteIngot INSTANCE = biggerreactorsrework.core.registry.RegistryIds.named("cyanite_ingot", () -> new CyaniteIngot(new Properties()));

    @SuppressWarnings("unused")
    public CyaniteIngot(@Nonnull Properties properties) {
        super(biggerreactorsrework.core.registry.RegistryIds.item(properties));
    }
//?} else if =1.21.1 {
/*    @RegisterItem(name = "cyanite_ingot")
    public static final CyaniteIngot INSTANCE = new CyaniteIngot(new Properties());

    @SuppressWarnings("unused")
    public CyaniteIngot(@Nonnull Properties properties) {
        super(properties);
    }
*/
//?}
}
