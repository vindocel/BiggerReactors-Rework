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
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;

/** Cyanite reprocessing, drawn like the machine's screen. */
public class CyaniteReprocessingCategory implements IRecipeCategory<CyaniteReprocessingCategory.Recipe> {
    public static final RecipeType<Recipe> RECIPE_TYPE = new RecipeType<>(RecipeViewers.CYANITE_REPROCESSING, Recipe.class);

    /** One native world recipe. */
    public record Recipe(biggerreactorsrework.machine.recipe.ReprocessingRecipe value) {
    }

    // Modified 2026-10-27: retain JEI ingredient semantics while using the machine's gauge art.
    private static final mezz.jei.api.ingredients.IIngredientRenderer<net.neoforged.neoforge.fluids.FluidStack> TANK_RENDERER =
            new mezz.jei.api.ingredients.IIngredientRenderer<>() {
                @Override public int getWidth() { return biggerreactorsrework.client.GaugeBar.WIDTH; }
                @Override public int getHeight() { return biggerreactorsrework.client.GaugeBar.HEIGHT; }
                @Override public void render(GuiGraphicsExtractor g, net.neoforged.neoforge.fluids.FluidStack fluid) {
                    biggerreactorsrework.client.GaugeBar.drawFluid(g,0,0,fluid.getAmount(),Math.max(1,fluid.getAmount()),fluid);
                }
                @Override public java.util.List<Component> getTooltip(net.neoforged.neoforge.fluids.FluidStack fluid,net.minecraft.world.item.TooltipFlag flag) {
                    return java.util.List.of(fluid.getHoverName(),Component.literal(
                        biggerreactorsrework.core.client.gui.RenderHelper.formatValue(fluid.getAmount()/1000.0,"B",true)));
                }
            };

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
                .addItemStacks(java.util.Arrays.stream(biggerreactorsrework.machine.recipe.RecipeAccess.stacks(recipe.value().ingredient())).map(stack -> stack.copyWithCount(recipe.value().count())).toList());
        if(recipe.value().producesFluid()) {
            var product=recipe.value().fluidResult();
            builder.addSlot(RecipeIngredientRole.OUTPUT,Cyanite.PRODUCT_X,0)
                .addFluidStack(product.getFluid(),product.getAmount(),product.getComponentsPatch())
                .setCustomRenderer(mezz.jei.api.neoforge.NeoForgeTypes.FLUID_STACK,TANK_RENDERER);
        } else builder.addSlot(RecipeIngredientRole.OUTPUT,Cyanite.OUTPUT_X+1,Cyanite.SLOT_Y+1).addItemStack(recipe.value().result());
        // Water is drawn in the machine's own tank; this keeps it in the recipe lookups.
        if(recipe.value().totalFluid()>0) builder.addSlot(RecipeIngredientRole.INPUT,Cyanite.WATER_X,0)
                .addFluidStack(recipe.value().fluid(),recipe.value().totalFluid())
                .setCustomRenderer(mezz.jei.api.neoforge.NeoForgeTypes.FLUID_STACK,TANK_RENDERER);
    }

    @Override
    public void draw(Recipe recipe, IRecipeSlotsView view, GuiGraphicsExtractor g, double mouseX, double mouseY) {
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
