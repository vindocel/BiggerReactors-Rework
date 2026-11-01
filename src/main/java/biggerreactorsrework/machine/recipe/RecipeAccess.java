package biggerreactorsrework.machine.recipe;

// Modified 2026-11-01: native Minecraft 26.1.2 API.
import java.util.List;
import java.util.Optional;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.level.Level;

/** Recipes of a level on either side; the client's copy holds the types sent with each datapack sync. */
public final class RecipeAccess {
    private static volatile RecipeMap clientRecipes = RecipeMap.EMPTY;
    private static volatile RecipeMap overrideRecipes;

    private RecipeAccess() {
    }

    public static void setClientRecipes(RecipeMap recipes) {
        clientRecipes = recipes;
    }

    /** Replaces the recipes seen through this class until reset with null; used by the GameTests. */
    public static void overrideRecipes(RecipeMap recipes) {
        overrideRecipes = recipes;
    }

    public static RecipeMap map(Level level) {
        if (overrideRecipes != null) {
            return overrideRecipes;
        }
        MinecraftServer server = level.getServer();
        return !level.isClientSide() && server != null ? server.getRecipeManager().recipeMap() : clientRecipes;
    }

    public static Optional<RecipeHolder<?>> byKey(Level level, ResourceKey<Recipe<?>> key) {
        return Optional.ofNullable(map(level).byKey(key));
    }

    public static ItemStack[] stacks(Ingredient ingredient) {
        return ingredient.items().map(ItemStack::new).toArray(ItemStack[]::new);
    }

    public static ItemStack result(RegistryAccess registries, Recipe<?> recipe) {
        ContextMap context = new ContextMap.Builder().withParameter(SlotDisplayContext.REGISTRIES, registries).create(SlotDisplayContext.CONTEXT);
        for (RecipeDisplay display : recipe.display()) {
            ItemStack result = display.result().resolveForFirstStack(context);
            if (!result.isEmpty()) {
                return result;
            }
        }
        return ItemStack.EMPTY;
    }

    public static List<Optional<Ingredient>> grid(Recipe<?> recipe) {
        if (recipe instanceof ShapedRecipe shaped) {
            return shaped.getIngredients();
        }
        return recipe.placementInfo().ingredients().stream().map(Optional::of).toList();
    }
}
