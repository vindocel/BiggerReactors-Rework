package biggerreactorsrework.client.deps.jei.classic.turbine;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.client.deps.RecipeViewerData.Stats;
import biggerreactorsrework.multiblocks.turbine.blocks.TurbineTerminal;
import biggerreactorsrework.registries.TurbineCoilRegistry;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public class CoilCategory implements IRecipeCategory<CoilCategory.Recipe> {
    private final IDrawable icon;
    public static final Identifier UID = Identifier.fromNamespaceAndPath(BiggerReactors.modid, "classic/turbine_coil");
    public static final RecipeType<Recipe> RECIPE_TYPE = new RecipeType<>(UID, Recipe.class);

    public CoilCategory(IGuiHelper guiHelper) {
        icon = guiHelper.createDrawableItemStack(new ItemStack(TurbineTerminal.INSTANCE));
    }

    @Override
    public RecipeType<Recipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.biggerreactorsrework.classic.turbine_coil_block");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return Stats.WIDTH;
    }

    @Override
    public int getHeight() {
        return 3 * Stats.ROW;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Recipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 1, Stats.slotY(recipe.stats()) + 1).addItemStack(recipe.getInput());
    }

    @Override
    public void draw(Recipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY) {
        Stats.draw(guiGraphics, recipe.stats());
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, Recipe recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        int row = Stats.row(recipe.stats(), mouseX, mouseY);
        if (row >= 0) {
            tooltip.addAll(Stats.tooltip(recipe.stats().get(row)));
        }
    }

    public static class Recipe {
        private final ItemStack input;
        private final TurbineCoilRegistry.CoilData coilData;
        private final List<Stats.Stat> stats;

        public Recipe(ItemStack input, TurbineCoilRegistry.CoilData coilData) {
            this.input = input;
            this.coilData = coilData;
            this.stats = Stats.coil(coilData);
        }

        public ItemStack getInput() {
            return input;
        }

        public TurbineCoilRegistry.CoilData getCoilData() {
            return coilData;
        }

        public List<Stats.Stat> stats() {
            return stats;
        }
    }
}
