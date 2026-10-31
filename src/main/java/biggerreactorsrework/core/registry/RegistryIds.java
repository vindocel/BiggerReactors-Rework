package biggerreactorsrework.core.registry;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Blocks and items need their registry id while being constructed; instances are built inside {@link #named}. */
public final class RegistryIds {
    private static final ThreadLocal<Deque<Identifier>> CURRENT = ThreadLocal.withInitial(ArrayDeque::new);

    private RegistryIds() {
    }

    public static <T> T named(String name, Supplier<T> constructor) {
        var stack = CURRENT.get();
        stack.push(Identifier.fromNamespaceAndPath(biggerreactorsrework.BiggerReactors.modid, name));
        try {
            return constructor.get();
        } finally {
            stack.pop();
        }
    }

    public static BlockBehaviour.Properties block(BlockBehaviour.Properties properties) {
        return properties.setId(ResourceKey.create(Registries.BLOCK, CURRENT.get().element()));
    }

    public static Item.Properties item(Item.Properties properties) {
        return properties.setId(ResourceKey.create(Registries.ITEM, CURRENT.get().element()));
    }
}
