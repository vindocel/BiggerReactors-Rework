package biggerreactorsrework.client.deps.rei;

import me.shedaniel.rei.api.client.view.ViewSearchBuilder;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import net.minecraft.resources.Identifier;

/** Modified 2026-11-01: native 26.1.2 identifiers; loaded only when REI is installed. */
public final class ReiBridge {
    private ReiBridge() {
    }

    public static void show(Identifier... categories) {
        var search = ViewSearchBuilder.builder();
        for (var category : categories) {
            search.addCategory(CategoryIdentifier.of(category));
        }
        search.open();
    }
}
