package biggerreactorsrework.client.deps.emi;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.Config;
import biggerreactorsrework.client.deps.RecipeViewerData;
import biggerreactorsrework.client.deps.RecipeViewerData.Cyanite;
import biggerreactorsrework.client.deps.RecipeViewers;
import biggerreactorsrework.items.ingots.BlutoniumIngot;
import biggerreactorsrework.items.ingots.CyaniteIngot;
import biggerreactorsrework.machine.blocks.CyaniteReprocessor;
import biggerreactorsrework.multiblocks.reactor.blocks.ReactorTerminal;
import biggerreactorsrework.multiblocks.turbine.blocks.TurbineTerminal;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.TextWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;

import java.util.List;

/**
 * EMI integration: Cyanite Reprocessing (drawn like the machine's screen), reactor moderators and turbine coils, the
 * same categories as the JEI plugin.
 */
@EmiEntrypoint
public class BiggerReactorsEmiPlugin implements EmiPlugin {
    static final EmiStack REPROCESSOR = EmiStack.of(CyaniteReprocessor.INSTANCE);
    static final EmiStack REACTOR_TERMINAL = EmiStack.of(ReactorTerminal.INSTANCE);
    static final EmiStack TURBINE_TERMINAL = EmiStack.of(TurbineTerminal.INSTANCE);
    static final EmiRecipeCategory CYANITE_REPROCESSING = new EmiRecipeCategory(RecipeViewers.CYANITE_REPROCESSING, REPROCESSOR);
    static final EmiRecipeCategory MODERATOR_BLOCK = new EmiRecipeCategory(RecipeViewers.MODERATOR_BLOCK, REACTOR_TERMINAL);
    static final EmiRecipeCategory MODERATOR_FLUID = new EmiRecipeCategory(RecipeViewers.MODERATOR_FLUID, REACTOR_TERMINAL);
    static final EmiRecipeCategory TURBINE_COIL = new EmiRecipeCategory(RecipeViewers.TURBINE_COIL, TURBINE_TERMINAL);
    private static final ResourceLocation INFO_BACKGROUND = ResourceLocation.fromNamespaceAndPath(BiggerReactors.modid, "textures/jei/common.png");
    private static final int INFO_WIDTH = 144;
    private static final int INFO_TEXT_X = 80;
    private static final int INFO_LINE = 12;
    private static final int INFO_TEXT = 0xFF000000;

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(CYANITE_REPROCESSING);
        registry.addCategory(MODERATOR_BLOCK);
        registry.addCategory(MODERATOR_FLUID);
        registry.addCategory(TURBINE_COIL);
        registry.addWorkstation(CYANITE_REPROCESSING, REPROCESSOR);
        registry.addWorkstation(MODERATOR_BLOCK, REACTOR_TERMINAL);
        registry.addWorkstation(MODERATOR_FLUID, REACTOR_TERMINAL);
        registry.addWorkstation(TURBINE_COIL, TURBINE_TERMINAL);
        if (!Config.CONFIG.EnableJEIIntegration) {
            return;
        }
        for (var holder : registry.getRecipeManager().getAllRecipesFor(biggerreactorsrework.machine.recipe.ReprocessingRecipes.TYPE.get())) registry.addRecipe(new Reprocessing(holder));
        for (RecipeViewerData.ModeratorBlock moderator : RecipeViewerData.moderatorBlocks()) {
            registry.addRecipe(new Info(MODERATOR_BLOCK, "moderator_block", EmiStack.of(moderator.block()),
                    RecipeViewerData.moderatorLines(moderator.properties()), 0, 46));
        }
        for (RecipeViewerData.ModeratorFluid moderator : RecipeViewerData.moderatorFluids()) {
            registry.addRecipe(new Info(MODERATOR_FLUID, "moderator_fluid", EmiStack.of(moderator.fluid(), 1000),
                    RecipeViewerData.moderatorLines(moderator.properties()), 0, 46));
        }
        for (RecipeViewerData.Coil coil : RecipeViewerData.coils()) {
            registry.addRecipe(new Info(TURBINE_COIL, "turbine_coil", EmiStack.of(coil.block()),
                    RecipeViewerData.coilLines(coil.data()), 6, 34));
        }
    }

    /** Synthetic recipe ids start with a slash, as EMI asks. */
    private static ResourceLocation syntheticId(String kind, ResourceLocation entry) {
        return ResourceLocation.fromNamespaceAndPath(BiggerReactors.modid, "/" + kind + "/" + entry.getNamespace() + "/" + entry.getPath());
    }

    private static final class Reprocessing extends BasicEmiRecipe {
        private final biggerreactorsrework.machine.recipe.ReprocessingRecipe recipe;
        Reprocessing(net.minecraft.world.item.crafting.RecipeHolder<biggerreactorsrework.machine.recipe.ReprocessingRecipe> holder) {
            super(CYANITE_REPROCESSING, holder.id(),
                    Cyanite.WIDTH, Cyanite.HEIGHT);
            recipe = holder.value();
            inputs.add(EmiIngredient.of(recipe.ingredient(), recipe.count()));
            // Water is drawn in the machine's own tank; listing it keeps it in the recipe lookups.
            if (recipe.water()>0) inputs.add(EmiStack.of(recipe.fluid(), (long)recipe.water()*recipe.duration()));
            outputs.add(EmiStack.of(recipe.result()));
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            widgets.addDrawable(0, 0, Cyanite.WIDTH, Cyanite.HEIGHT, (g, mouseX, mouseY, delta) -> Cyanite.draw(g, recipe));
            widgets.addSlot(inputs.getFirst(), Cyanite.INPUT_X, Cyanite.SLOT_Y).drawBack(false);
            widgets.addSlot(outputs.getFirst(), Cyanite.OUTPUT_X, Cyanite.SLOT_Y).drawBack(false).recipeContext(this);
            widgets.addTooltipText(Cyanite.energyTooltip(recipe), Cyanite.ENERGY_X, 0, 18, Cyanite.HEIGHT);
            widgets.addTooltipText(Cyanite.waterTooltip(recipe), Cyanite.WATER_X, 0, 18, Cyanite.HEIGHT);
            widgets.addTooltipText(Cyanite.timeTooltip(recipe), Cyanite.ARROW_X, Cyanite.ARROW_Y, Cyanite.ARROW_WIDTH, Cyanite.ARROW_HEIGHT);
        }
    }

    /** A block or fluid with its properties written beside it, like the JEI categories. */
    private static final class Info extends BasicEmiRecipe {
        private final EmiIngredient stack;
        private final List<Component> lines;
        private final int v;
        private final int height;

        Info(EmiRecipeCategory category, String kind, EmiStack stack, List<Component> lines, int v, int height) {
            super(category, syntheticId(kind, stack.getId()), INFO_WIDTH, height);
            this.stack = stack;
            this.lines = lines;
            this.v = v;
            this.height = height;
            inputs.add(stack);
        }

        @Override
        public boolean supportsRecipeTree() {
            return false;
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            widgets.addTexture(INFO_BACKGROUND, 0, 0, INFO_WIDTH, height, 0, v);
            widgets.addSlot(stack, 0, (height - 18) / 2).drawBack(false);
            for (int i = 0; i < lines.size(); i++) {
                widgets.addText(lines.get(i), INFO_TEXT_X, i * INFO_LINE, INFO_TEXT, false).horizontalAlign(TextWidget.Alignment.CENTER);
            }
        }
    }
}
