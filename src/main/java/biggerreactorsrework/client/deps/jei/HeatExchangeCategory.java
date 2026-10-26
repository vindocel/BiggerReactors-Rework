package biggerreactorsrework.client.deps.jei;

import biggerreactorsrework.client.deps.RecipeViewerData;
import biggerreactorsrework.client.deps.RecipeViewerData.FluidCard;
import biggerreactorsrework.client.deps.RecipeViewers;
import biggerreactorsrework.multiblocks.heatexchanger.blocks.HeatExchangerTerminalBlock;
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

/** Added 2026-10-26: discoverable fluid conversions in the exchanger, not reactor recipes. */
public final class HeatExchangeCategory implements IRecipeCategory<RecipeViewerData.HeatExchange> {
    public static final RecipeType<RecipeViewerData.HeatExchange> RECIPE_TYPE =
            new RecipeType<>(RecipeViewers.HEAT_EXCHANGE, RecipeViewerData.HeatExchange.class);
    private final IDrawable icon;

    public HeatExchangeCategory(IGuiHelper helper) {
        icon = helper.createDrawableItemStack(new ItemStack(HeatExchangerTerminalBlock.INSTANCE));
    }

    @Override
    public RecipeType<RecipeViewerData.HeatExchange> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.biggerreactorsrework.heat_exchange");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeViewerData.HeatExchange recipe, IFocusGroup focus) {
        var card = recipe.card();
        builder.addSlot(RecipeIngredientRole.INPUT, 1, FluidCard.inputY() + 1)
                .addFluidStack(recipe.input().getFluid(), 1000, recipe.input().getComponentsPatch()).setFluidRenderer(1000, false, 16, 16);
        builder.addSlot(RecipeIngredientRole.OUTPUT, 1, card.outputY() + 1)
                .addFluidStack(recipe.output().getFluid(), 1000, recipe.output().getComponentsPatch()).setFluidRenderer(1000, false, 16, 16);
    }

    @Override
    public void draw(RecipeViewerData.HeatExchange recipe, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
        recipe.card().draw(g);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, RecipeViewerData.HeatExchange recipe, IRecipeSlotsView slots, double mouseX, double mouseY) {
        var card = recipe.card();
        int row = card.row(mouseX, mouseY);
        if (row >= 0) {
            tooltip.addAll(RecipeViewerData.Stats.tooltip(card.stats().get(row)));
        }
    }
}
