package biggerreactorsrework.client.deps.emi;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.Config;
import biggerreactorsrework.client.deps.RecipeViewerData;
import biggerreactorsrework.client.deps.RecipeViewerData.Cyanite;
import biggerreactorsrework.client.deps.RecipeViewerData.Stats;
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
import dev.emi.emi.api.widget.WidgetHolder;
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
    static final EmiStack EXCHANGER=EmiStack.of(biggerreactorsrework.multiblocks.heatexchanger.blocks.HeatExchangerTerminalBlock.INSTANCE);
    static final EmiRecipeCategory HEAT_EXCHANGE=new EmiRecipeCategory(RecipeViewers.HEAT_EXCHANGE,EXCHANGER);

    @Override
    public void register(EmiRegistry registry) {
        registry.addGenericExclusionArea((screen, consumer) -> {
            if (screen instanceof biggerreactorsrework.core.client.gui.screens.ModScreen<?> modScreen) {
                for (var area : modScreen.extraAreas()) {
                    consumer.accept(new dev.emi.emi.api.widget.Bounds(area.getX(), area.getY(), area.getWidth(), area.getHeight()));
                }
            }
        });
        registry.addCategory(CYANITE_REPROCESSING);
        registry.addCategory(MODERATOR_BLOCK);
        registry.addCategory(MODERATOR_FLUID);
        registry.addCategory(TURBINE_COIL);
        registry.addCategory(HEAT_EXCHANGE);
        registry.addWorkstation(CYANITE_REPROCESSING, REPROCESSOR);
        registry.addWorkstation(MODERATOR_BLOCK, REACTOR_TERMINAL);
        registry.addWorkstation(MODERATOR_FLUID, REACTOR_TERMINAL);
        registry.addWorkstation(TURBINE_COIL, TURBINE_TERMINAL);
        registry.addWorkstation(HEAT_EXCHANGE,EXCHANGER);
        registry.addWorkstation(HEAT_EXCHANGE,EmiStack.of(biggerreactorsrework.multiblocks.heatexchanger.blocks.HeatExchangerCondenserChannelBlock.INSTANCE));
        registry.addWorkstation(HEAT_EXCHANGE,EmiStack.of(biggerreactorsrework.multiblocks.heatexchanger.blocks.HeatExchangerEvaporatorChannelBlock.INSTANCE));
        registry.addWorkstation(HEAT_EXCHANGE,EmiStack.of(biggerreactorsrework.multiblocks.heatexchanger.blocks.HeatExchangerFluidPortBlock.INSTANCE));
        if (!Config.CONFIG.EnableJEIIntegration) {
            return;
        }
        for (var holder : registry.getRecipeManager().getAllRecipesFor(biggerreactorsrework.machine.recipe.ReprocessingRecipes.TYPE.get())) registry.addRecipe(new Reprocessing(holder));
        for (RecipeViewerData.ModeratorBlock moderator : RecipeViewerData.moderatorBlocks()) {
            registry.addRecipe(new Info(MODERATOR_BLOCK, "moderator_block", EmiStack.of(moderator.block()),
                    Stats.moderator(moderator.properties())));
        }
        for (var coolant : RecipeViewerData.coolants()) registry.addRecipe(new FluidConversion(MODERATOR_FLUID, "coolant",
                net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(coolant.input().getFluid()), coolant.card()));
        for (var recipe : RecipeViewerData.heatExchanges()) registry.addRecipe(new FluidConversion(HEAT_EXCHANGE, "heat_exchange",
                RecipeViewerData.heatExchangeId(recipe), recipe.card()));
        for (RecipeViewerData.Coil coil : RecipeViewerData.coils()) {
            registry.addRecipe(new Info(TURBINE_COIL, "turbine_coil", EmiStack.of(coil.block()),
                    Stats.coil(coil.data())));
        }
    }

    /** Added 2026-10-26: a cold and a hot fluid with the properties of the conversion drawn as bars, like the moderator pages. */
    private static final class FluidConversion extends BasicEmiRecipe {
        private final RecipeViewerData.FluidCard card;

        FluidConversion(EmiRecipeCategory category, String kind, ResourceLocation cold, RecipeViewerData.FluidCard card) {
            super(category, syntheticId(kind, cold),
                    RecipeViewerData.FluidCard.WIDTH, card.height());
            this.card = card;
            inputs.add(EmiStack.of(card.input().getFluid(), card.input().getComponentsPatch(), 1000));
            outputs.add(EmiStack.of(card.output().getFluid(), card.output().getComponentsPatch(), 1000));
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            widgets.addDrawable(0, 0, RecipeViewerData.FluidCard.WIDTH, card.height(), (g, mouseX, mouseY, delta) -> card.draw(g));
            widgets.addSlot(inputs.getFirst(), 0, RecipeViewerData.FluidCard.inputY()).drawBack(false);
            widgets.addSlot(outputs.getFirst(), 0, card.outputY()).drawBack(false).recipeContext(this);
            for (int i = 0; i < card.stats().size(); i++) {
                widgets.addTooltipText(Stats.tooltip(card.stats().get(i)), Stats.rowX(), RecipeViewerData.FluidCard.rowY(i),
                        RecipeViewerData.FluidCard.WIDTH - Stats.rowX(), Stats.ROW);
            }
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

    /** A block or fluid with its properties drawn as bars beside it, like the JEI categories. */
    private static final class Info extends BasicEmiRecipe {
        private final EmiIngredient stack;
        private final List<Stats.Stat> stats;

        Info(EmiRecipeCategory category, String kind, EmiStack stack, List<Stats.Stat> stats) {
            super(category, syntheticId(kind, stack.getId()), Stats.WIDTH, Stats.height(stats));
            this.stack = stack;
            this.stats = stats;
            inputs.add(stack);
        }

        @Override
        public boolean supportsRecipeTree() {
            return false;
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            widgets.addDrawable(0, 0, Stats.WIDTH, Stats.height(stats), (g, mouseX, mouseY, delta) -> Stats.draw(g, stats));
            widgets.addSlot(stack, 0, Stats.slotY(stats)).drawBack(false);
            for (int i = 0; i < stats.size(); i++) {
                widgets.addTooltipText(Stats.tooltip(stats.get(i)), Stats.rowX(), i * Stats.ROW, Stats.rowWidth(), Stats.ROW);
            }
        }
    }
}
