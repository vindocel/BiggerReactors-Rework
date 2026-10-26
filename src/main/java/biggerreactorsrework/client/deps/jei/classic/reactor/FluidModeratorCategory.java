package biggerreactorsrework.client.deps.jei.classic.reactor;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.client.deps.RecipeViewerData;
import biggerreactorsrework.client.deps.RecipeViewerData.FluidCard;
import biggerreactorsrework.multiblocks.reactor.blocks.ReactorTerminal;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Reworked 2026-10-25: selected coolant conversions replace world-fluid moderation. Modified 2026-10-26: drawn like the moderator pages. */
public class FluidModeratorCategory implements IRecipeCategory<FluidModeratorCategory.Recipe> {
    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(BiggerReactors.modid, "classic/reactor_coolant");
    public static final RecipeType<Recipe> RECIPE_TYPE = new RecipeType<>(UID, Recipe.class);
    private final IDrawable icon;

    public FluidModeratorCategory(IGuiHelper helper) {
        icon = helper.createDrawableItemStack(new ItemStack(ReactorTerminal.INSTANCE));
    }

    @Override
    public RecipeType<Recipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.biggerreactorsrework.classic.reactor_moderator_fluid");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return FluidCard.WIDTH;
    }

    @Override
    public int getHeight() {
        return 6 * RecipeViewerData.Stats.ROW;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Recipe recipe, IFocusGroup focuses) {
        var card = recipe.card();
        builder.addSlot(RecipeIngredientRole.INPUT, 1, FluidCard.inputY() + 1)
                .addFluidStack(card.input().getFluid(), 1000, card.input().getComponentsPatch()).setFluidRenderer(1000, false, 16, 16);
        builder.addSlot(RecipeIngredientRole.OUTPUT, 1, card.outputY() + 1)
                .addFluidStack(card.output().getFluid(), 1000, card.output().getComponentsPatch()).setFluidRenderer(1000, false, 16, 16);
    }

    @Override
    public void draw(Recipe recipe, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
        recipe.card().draw(g);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, Recipe recipe, IRecipeSlotsView slots, double mouseX, double mouseY) {
        var card = recipe.card();
        int row = card.row(mouseX, mouseY);
        if (row >= 0) {
            tooltip.addAll(RecipeViewerData.Stats.tooltip(card.stats().get(row)));
        }
    }

    public record Recipe(RecipeViewerData.Coolant coolant, FluidCard card) {
        public Recipe(RecipeViewerData.Coolant coolant) {
            this(coolant, coolant.card());
        }
    }
}
