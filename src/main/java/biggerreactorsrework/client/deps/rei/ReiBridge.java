package biggerreactorsrework.client.deps.rei;

import me.shedaniel.rei.api.client.view.ViewSearchBuilder;
//? if =26.1.2 {
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import net.minecraft.resources.Identifier;

/** Modified 2026-11-01: native 26.1.2 identifiers; loaded only when REI is installed. */
public final class ReiBridge {
//?} else if =1.21.1 || =26.3 {
/*import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import net.minecraft.resources.ResourceLocation;

/^* Added 2026-10-27: client bridge loaded only when REI is installed. ^/
public final class ReiBridge {
*/
//?}
    private ReiBridge() {
//? if =26.1.2 {
    }

    public static void show(Identifier... categories) {
        var search = ViewSearchBuilder.builder();
//?} else if =1.21.1 || =26.3 {
/*    }

    public static void show(ResourceLocation... categories) {
        var search = ViewSearchBuilder.builder();
*/
//?}
        for (var category : categories) {
            search.addCategory(CategoryIdentifier.of(category));
        }
        search.open();
    }
}
