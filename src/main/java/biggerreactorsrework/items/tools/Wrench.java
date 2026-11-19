package biggerreactorsrework.items.tools;

import net.minecraft.world.item.Item;
import biggerreactorsrework.core.registry.RegisterItem;

import javax.annotation.Nonnull;

public class Wrench extends Item {

//? if =26.1.2 || =26.3 {
    @RegisterItem(name = "wrench")
    public static final Wrench INSTANCE = biggerreactorsrework.core.registry.RegistryIds.named("wrench", () -> new Wrench(new Properties()));

    @SuppressWarnings("unused")
    public Wrench(@Nonnull Properties properties) {
        super(biggerreactorsrework.core.registry.RegistryIds.item(properties.stacksTo(1)));
    }
//?} else if =1.21.1 {
/*    @RegisterItem(name = "wrench")
    public static final Wrench INSTANCE = new Wrench(new Properties());

    @SuppressWarnings("unused")
    public Wrench(@Nonnull Properties properties) {
        super(properties.stacksTo(1));
    }
*/
//?}
}
