package biggerreactorsrework.items.dusts;

import net.minecraft.world.item.Item;
import biggerreactorsrework.core.registry.RegisterItem;

import javax.annotation.Nonnull;

public class GraphiteDust extends Item {

//? if =26.1.2 || =26.3 {
    @RegisterItem(name = "graphite_dust")
    public static final GraphiteDust INSTANCE = biggerreactorsrework.core.registry.RegistryIds.named("graphite_dust", () -> new GraphiteDust(new Properties()));

    @SuppressWarnings("unused")
    public GraphiteDust(@Nonnull Properties properties) {
        super(biggerreactorsrework.core.registry.RegistryIds.item(properties));
    }
//?} else if =1.21.1 {
/*    @RegisterItem(name = "graphite_dust")
    public static final GraphiteDust INSTANCE = new GraphiteDust(new Properties());

    @SuppressWarnings("unused")
    public GraphiteDust(@Nonnull Properties properties) {
        super(properties);
    }
*/
//?}
}
