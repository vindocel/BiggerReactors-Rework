package biggerreactorsrework.items.raw;

import net.minecraft.world.item.Item;
import biggerreactorsrework.core.registry.RegisterItem;

import javax.annotation.Nonnull;

public class UraniumChunk extends Item {

//? if =26.1.2 || =26.3 {
    @RegisterItem(name = "uranium_chunk")
    public static final UraniumChunk INSTANCE = biggerreactorsrework.core.registry.RegistryIds.named("uranium_chunk", () -> new UraniumChunk(new Properties()));

    @SuppressWarnings("unused")
    public UraniumChunk(@Nonnull Properties properties) {
        super(biggerreactorsrework.core.registry.RegistryIds.item(properties));
    }
//?} else if =1.21.1 {
/*    @RegisterItem(name = "uranium_chunk")
    public static final UraniumChunk INSTANCE = new UraniumChunk(new Properties());

    @SuppressWarnings("unused")
    public UraniumChunk(@Nonnull Properties properties) {
        super(properties);
    }
*/
//?}
}
