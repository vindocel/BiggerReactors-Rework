package biggerreactorsrework.items.tools;

import biggerreactorsrework.core.registry.RegisterItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Common entry point; the client installs the opener. */
public final class GuideBookItem extends Item {
    @RegisterItem(name = "guide_book")
    public static final GuideBookItem INSTANCE = biggerreactorsrework.core.registry.RegistryIds.named("guide_book", () -> new GuideBookItem());
    public static Runnable open = () -> { };
    private GuideBookItem() { super(biggerreactorsrework.core.registry.RegistryIds.item(new Item.Properties().stacksTo(1))); }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) open.run();
        return InteractionResult.SUCCESS;
    }
}
