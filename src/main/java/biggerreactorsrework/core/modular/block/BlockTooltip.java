//? if =26.1.2 || =26.3 {
package biggerreactorsrework.core.modular.block;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Extra tooltip lines of a block's item. */
public interface BlockTooltip {
    void appendTooltip(ItemStack stack, Consumer<Component> tooltip);
}
//?} else if =1.21.1 {
//?}
