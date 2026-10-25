package biggerreactorsrework.client.deps.jei;

import biggerreactorsrework.client.deps.RecipeViewerData.Cyanite;
import biggerreactorsrework.client.deps.RecipeViewers;
import biggerreactorsrework.items.ingots.BlutoniumIngot;
import biggerreactorsrework.items.ingots.CyaniteIngot;
import biggerreactorsrework.machine.blocks.CyaniteReprocessor;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;

/** Cyanite reprocessing, drawn like the machine's screen. */
public class CyaniteReprocessingCategory implements IRecipeCategory<CyaniteReprocessingCategory.Recipe> {
    public static final RecipeType<Recipe> RECIPE_TYPE = new RecipeType<>(RecipeViewers.CYANITE_REPROCESSING, Recipe.class);

    /** One native world recipe. */
    public record Recipe(biggerreactorsrework.machine.recipe.ReprocessingRecipe value) {
    }

    private final IDrawable icon;

    public CyaniteReprocessingCategory(IGuiHelper gui) {
        this.icon = gui.createDrawableItemStack(new ItemStack(CyaniteReprocessor.INSTANCE));
    }

    @Override
    public RecipeType<Recipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.biggerreactorsrework.cyanite_reprocessing");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return Cyanite.WIDTH;
    }

    @Override
    public int getHeight() {
        return Cyanite.HEIGHT;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Recipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, Cyanite.INPUT_X + 1, Cyanite.SLOT_Y + 1)
                .addItemStacks(java.util.Arrays.stream(recipe.value().ingredient().getItems()).map(stack -> stack.copyWithCount(recipe.value().count())).toList());
        builder.addSlot(RecipeIngredientRole.OUTPUT, Cyanite.OUTPUT_X + 1, Cyanite.SLOT_Y + 1)
                .addItemStack(recipe.value().result());
        // Water is drawn in the machine's own tank; this keeps it in the recipe lookups.
        builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addFluidStack(recipe.value().fluid(), (long)recipe.value().water()*recipe.value().duration());
    }

    @Override
    public void draw(Recipe recipe, IRecipeSlotsView view, GuiGraphics g, double mouseX, double mouseY) {
        Cyanite.draw(g, recipe.value());
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, Recipe recipe, IRecipeSlotsView view, double mouseX, double mouseY) {
        if (inside(mouseX, mouseY, Cyanite.ENERGY_X, 0, 18, Cyanite.HEIGHT)) {
            tooltip.addAll(Cyanite.energyTooltip(recipe.value()));
        } else if (inside(mouseX, mouseY, Cyanite.WATER_X, 0, 18, Cyanite.HEIGHT)) {
            tooltip.addAll(Cyanite.waterTooltip(recipe.value()));
        } else if (inside(mouseX, mouseY, Cyanite.ARROW_X, Cyanite.ARROW_Y, Cyanite.ARROW_WIDTH, Cyanite.ARROW_HEIGHT)) {
            tooltip.addAll(Cyanite.timeTooltip(recipe.value()));
        }
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }
}
