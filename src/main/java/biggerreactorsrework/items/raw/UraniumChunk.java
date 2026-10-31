package biggerreactorsrework.items.raw;

import net.minecraft.world.item.Item;
import biggerreactorsrework.core.registry.RegisterItem;

import javax.annotation.Nonnull;

public class UraniumChunk extends Item {

    @RegisterItem(name = "uranium_chunk")
    public static final UraniumChunk INSTANCE = biggerreactorsrework.core.registry.RegistryIds.named("uranium_chunk", () -> new UraniumChunk(new Properties()));

    @SuppressWarnings("unused")
    public UraniumChunk(@Nonnull Properties properties) {
        super(biggerreactorsrework.core.registry.RegistryIds.item(properties));
    }
}
