package biggerreactorsrework.client.deps;

//? if =26.1.2 || =26.3 {
import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.client.deps.jei.JeiBridge;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModList;
//?} else if =1.21.1 {
/*import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.client.deps.emi.EmiBridge;
import biggerreactorsrework.client.deps.jei.JeiBridge;
import biggerreactorsrework.client.deps.rei.ReiBridge;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
*/
//?}

/**
 * Opens the mod's recipe categories in the installed recipe viewer (the Industrial Processor's progress arrow is
 * clickable). The viewer-specific bridges are only touched when that viewer is loaded.
 */
//? if =26.1.2 || =26.3 {
public final class RecipeViewers {
    public static final Identifier INDUSTRIAL_PROCESSING = Identifier.fromNamespaceAndPath(BiggerReactors.modid, "industrial_processing");
    public static final Identifier MODERATOR_BLOCK = Identifier.fromNamespaceAndPath(BiggerReactors.modid, "classic/reactor_moderator_block");
    public static final Identifier MODERATOR_FLUID = Identifier.fromNamespaceAndPath(BiggerReactors.modid, "classic/reactor_coolant");
    public static final Identifier TURBINE_COIL = Identifier.fromNamespaceAndPath(BiggerReactors.modid, "classic/turbine_coil");
    public static final Identifier HEAT_EXCHANGE = Identifier.fromNamespaceAndPath(BiggerReactors.modid, "heat_exchange");

    private RecipeViewers() {
    }

    private static final String EMI_BRIDGE = "biggerreactorsrework.client.deps.emi.EmiBridge";
    private static final String REI_BRIDGE = "biggerreactorsrework.client.deps.rei.ReiBridge";

    /** The EMI and REI bridges exist only when turned on in gradle.properties. */
    private static boolean bridge(String className) {
        try {
            Class.forName(className, false, RecipeViewers.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException absent) {
            return false;
        }
    }

    /** EMI has its own plugin; the JEI plugin stays out of the way when EMI is active. */
    public static boolean emi() {
        return ModList.get().isLoaded("emi") && bridge(EMI_BRIDGE);
    }
//?} else if =1.21.1 {
/*public final class RecipeViewers {
    public static final ResourceLocation INDUSTRIAL_PROCESSING = ResourceLocation.fromNamespaceAndPath(BiggerReactors.modid, "industrial_processing");
    public static final ResourceLocation MODERATOR_BLOCK = ResourceLocation.fromNamespaceAndPath(BiggerReactors.modid, "classic/reactor_moderator_block");
    public static final ResourceLocation MODERATOR_FLUID = ResourceLocation.fromNamespaceAndPath(BiggerReactors.modid, "classic/reactor_coolant");
    public static final ResourceLocation TURBINE_COIL = ResourceLocation.fromNamespaceAndPath(BiggerReactors.modid, "classic/turbine_coil");
    public static final ResourceLocation HEAT_EXCHANGE = ResourceLocation.fromNamespaceAndPath(BiggerReactors.modid, "heat_exchange");

    private RecipeViewers() {
    }

    /^* EMI has its own plugin; the JEI plugin stays out of the way when EMI is installed. ^/
    public static boolean emi() {
        return ModList.get().isLoaded("emi");
    }
*/
//?}

    public static boolean available() {
        return emi() || rei() || (ModList.get().isLoaded("jei") && JeiBridge.ready());
    }

    /** Modified 2026-10-27: native REI support, with EMI retaining priority when both are installed. */
//? if =26.1.2 || =26.3 {
    public static boolean rei() {
        return ModList.get().isLoaded("roughlyenoughitems") && bridge(REI_BRIDGE);
    }

    private static void showIn(String className, Class<?> parameter, Object argument) {
        try {
            Class.forName(className).getMethod("show", parameter).invoke(null, argument);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not open " + className, e);
        }
    }

    /** Shows the categories (EMI shows the first one; JEI shows them all as tabs). */
    public static void show(Identifier... categories) {
        if (categories.length == 0) {
//?} else if =1.21.1 {
/*    public static boolean rei() {
        return ModList.get().isLoaded("roughlyenoughitems");
    }

    /^* Shows the categories (EMI shows the first one; JEI shows them all as tabs). ^/
    public static void show(ResourceLocation... categories) {
        if (categories.length == 0) {
*/
//?}
            return;
        }
//? if =26.1.2 || =26.3 {
        if (emi()) {
            showIn(EMI_BRIDGE, Identifier.class, categories[0]);
        } else if (rei()) {
            showIn(REI_BRIDGE, Identifier[].class, categories);
        } else if (ModList.get().isLoaded("jei")) {
//?} else if =1.21.1 {
/*        if (emi()) {
            EmiBridge.show(categories[0]);
        } else if (rei()) {
            ReiBridge.show(categories);
        } else if (ModList.get().isLoaded("jei")) {
*/
//?}
            JeiBridge.show(categories);
        }
    }
}
