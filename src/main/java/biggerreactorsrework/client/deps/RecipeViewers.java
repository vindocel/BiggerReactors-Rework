package biggerreactorsrework.client.deps;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.client.deps.emi.EmiBridge;
import biggerreactorsrework.client.deps.jei.JeiBridge;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

/**
 * Opens the mod's recipe categories in the installed recipe viewer (the Cyanite Reprocessor's progress arrow is
 * clickable). The viewer-specific bridges are only touched when that viewer is loaded.
 */
public final class RecipeViewers {
    public static final ResourceLocation CYANITE_REPROCESSING = ResourceLocation.fromNamespaceAndPath(BiggerReactors.modid, "cyanite_reprocessing");
    public static final ResourceLocation MODERATOR_BLOCK = ResourceLocation.fromNamespaceAndPath(BiggerReactors.modid, "classic/reactor_moderator_block");
    public static final ResourceLocation MODERATOR_FLUID = ResourceLocation.fromNamespaceAndPath(BiggerReactors.modid, "classic/reactor_moderator_fluid");
    public static final ResourceLocation TURBINE_COIL = ResourceLocation.fromNamespaceAndPath(BiggerReactors.modid, "classic/turbine_coil");

    private RecipeViewers() {
    }

    /** EMI has its own plugin; the JEI plugin stays out of the way when EMI is installed. */
    public static boolean emi() {
        return ModList.get().isLoaded("emi");
    }

    public static boolean available() {
        return emi() || (ModList.get().isLoaded("jei") && JeiBridge.ready());
    }

    /** Shows the categories (EMI shows the first one; JEI shows them all as tabs). */
    public static void show(ResourceLocation... categories) {
        if (categories.length == 0) {
            return;
        }
        if (emi()) {
            EmiBridge.show(categories[0]);
        } else if (ModList.get().isLoaded("jei")) {
            JeiBridge.show(categories);
        }
    }
}
