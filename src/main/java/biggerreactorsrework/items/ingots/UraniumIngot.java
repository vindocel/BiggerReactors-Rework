package biggerreactorsrework.items.ingots;

import net.minecraft.world.item.Item;
import biggerreactorsrework.core.registry.RegisterItem;

import javax.annotation.Nonnull;

public class UraniumIngot extends Item {

//? if =26.1.2 || =26.3 {
    @RegisterItem(name = "uranium_ingot")
    public static final UraniumIngot INSTANCE = biggerreactorsrework.core.registry.RegistryIds.named("uranium_ingot", () -> new UraniumIngot(new Properties()));

    @SuppressWarnings("unused")
    public UraniumIngot(@Nonnull Properties properties) {
        super(biggerreactorsrework.core.registry.RegistryIds.item(properties));
    }
//?} else if =1.21.1 {
/*    @RegisterItem(name = "uranium_ingot")
    public static final UraniumIngot INSTANCE = new UraniumIngot(new Properties());

    @SuppressWarnings("unused")
    public UraniumIngot(@Nonnull Properties properties) {
        super(properties);
    }
*/
//?}
}
