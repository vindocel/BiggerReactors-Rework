//? if =26.1.2 || =26.3 {
package biggerreactorsrework.client.deps.jei;

import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
//?} else if =1.21.1 {
/*package biggerreactorsrework.client.deps.jei;

import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
*/
//?}
import java.util.List;

/** JEI side of {@link biggerreactorsrework.client.deps.RecipeViewers}; loaded only when JEI is installed. */
public final class JeiBridge {
    private static IJeiRuntime runtime;

    private JeiBridge() {
    }

    static void setRuntime(IJeiRuntime value) {
        runtime = value;
    }

    public static boolean ready() {
        return runtime != null;
//? if =26.1.2 || =26.3 {
    }

    public static void show(Identifier... categories) {
        if (runtime == null) {
//?} else if =1.21.1 {
/*    }

    public static void show(ResourceLocation... categories) {
        if (runtime == null) {
*/
//?}
            return;
//? if =26.1.2 || =26.3 {
        }
        List<IRecipeType<?>> types = new ArrayList<>();
        for (Identifier id : categories) {
            runtime.getRecipeManager().getRecipeType(id).ifPresent(types::add);
//?} else if =1.21.1 {
/*        }
        List<RecipeType<?>> types = new ArrayList<>();
        for (ResourceLocation id : categories) {
            runtime.getRecipeManager().getRecipeType(id).ifPresent(types::add);
*/
//?}
        }
        if (!types.isEmpty()) {
            runtime.getRecipesGui().showTypes(types);
        }
    }
}
