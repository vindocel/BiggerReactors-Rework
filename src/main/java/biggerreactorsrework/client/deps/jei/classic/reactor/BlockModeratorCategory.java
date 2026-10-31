package biggerreactorsrework.client.deps.jei.classic.reactor;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.client.deps.RecipeViewerData.Stats;
import biggerreactorsrework.multiblocks.reactor.blocks.ReactorTerminal;
import biggerreactorsrework.registries.ReactorModeratorRegistry;
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

public class BlockModeratorCategory implements IRecipeCategory<BlockModeratorCategory.Recipe> {
    private final IDrawable icon;
    public static final Identifier UID = Identifier.fromNamespaceAndPath(BiggerReactors.modid, "classic/reactor_moderator_block");
    public static final RecipeType<Recipe> RECIPE_TYPE = new RecipeType<>(UID, Recipe.class);

    public BlockModeratorCategory(IGuiHelper guiHelper) {
        icon = guiHelper.createDrawableItemStack(new ItemStack(ReactorTerminal.INSTANCE));
    }

    @Override
    public RecipeType<Recipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.biggerreactorsrework.classic.reactor_moderator_block");
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
        return 4 * Stats.ROW;
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
        private final ReactorModeratorRegistry.IModeratorProperties moderatorProperties;
        private final List<Stats.Stat> stats;

        public Recipe(ItemStack input, ReactorModeratorRegistry.IModeratorProperties moderatorProperties) {
            this.input = input;
            this.moderatorProperties = moderatorProperties;
            this.stats = Stats.moderator(moderatorProperties);
        }

        public ItemStack getInput() {
            return input;
        }

        public ReactorModeratorRegistry.IModeratorProperties getModeratorProperties() {
            return moderatorProperties;
        }

        public List<Stats.Stat> stats() {
            return stats;
        }
    }
}
