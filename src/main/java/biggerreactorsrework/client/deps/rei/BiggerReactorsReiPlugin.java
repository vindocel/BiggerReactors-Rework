package biggerreactorsrework.client.deps.rei;

import biggerreactorsrework.Config;
import biggerreactorsrework.client.GaugeBar;
import biggerreactorsrework.client.GuiTheme;
import biggerreactorsrework.client.deps.RecipeViewerData;
import biggerreactorsrework.client.deps.RecipeViewerData.Cyanite;
import biggerreactorsrework.client.deps.RecipeViewerData.Stats;
import biggerreactorsrework.client.deps.RecipeViewers;
import biggerreactorsrework.core.client.gui.screens.ModScreen;
import biggerreactorsrework.machine.blocks.CyaniteReprocessor;
import biggerreactorsrework.machine.recipe.ReprocessingRecipe;
import biggerreactorsrework.machine.recipe.ReprocessingRecipes;
import biggerreactorsrework.multiblocks.heatexchanger.blocks.*;
import biggerreactorsrework.multiblocks.reactor.blocks.ReactorTerminal;
import biggerreactorsrework.multiblocks.turbine.blocks.TurbineTerminal;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.entry.renderer.EntryRenderer;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.*;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZones;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.forge.REIPluginClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** Added 2026-10-27: optional native REI categories using the synchronized viewer data and machine artwork. */
@REIPluginClient
public final class BiggerReactorsReiPlugin implements REIClientPlugin {
    private static final CategoryIdentifier<Page> PROCESSING = CategoryIdentifier.of(RecipeViewers.CYANITE_REPROCESSING);
    private static final CategoryIdentifier<Page> MODERATORS = CategoryIdentifier.of(RecipeViewers.MODERATOR_BLOCK);
    private static final CategoryIdentifier<Page> COOLANTS = CategoryIdentifier.of(RecipeViewers.MODERATOR_FLUID);
    private static final CategoryIdentifier<Page> COILS = CategoryIdentifier.of(RecipeViewers.TURBINE_COIL);
    private static final CategoryIdentifier<Page> EXCHANGE = CategoryIdentifier.of(RecipeViewers.HEAT_EXCHANGE);

    @Override
    public void registerCategories(CategoryRegistry registry) {
        if (RecipeViewers.emi()) return;
        registry.add(new Category(PROCESSING, "cyanite_reprocessing", CyaniteReprocessor.INSTANCE, Cyanite.WIDTH, Cyanite.HEIGHT));
        registry.add(new Category(MODERATORS, "classic.reactor_moderator_block", ReactorTerminal.INSTANCE, Stats.WIDTH, 4 * Stats.ROW));
        registry.add(new Category(COOLANTS, "classic.reactor_moderator_fluid", ReactorTerminal.INSTANCE, Stats.WIDTH, 6 * Stats.ROW));
        registry.add(new Category(COILS, "classic.turbine_coil_block", TurbineTerminal.INSTANCE, Stats.WIDTH, 3 * Stats.ROW));
        registry.add(new Category(EXCHANGE, "heat_exchange", HeatExchangerTerminalBlock.INSTANCE, Stats.WIDTH, 6 * Stats.ROW));
        registry.addWorkstations(PROCESSING, EntryStacks.of(CyaniteReprocessor.INSTANCE));
        registry.addWorkstations(MODERATORS, EntryStacks.of(ReactorTerminal.INSTANCE));
        registry.addWorkstations(COOLANTS, EntryStacks.of(ReactorTerminal.INSTANCE));
        registry.addWorkstations(COILS, EntryStacks.of(TurbineTerminal.INSTANCE));
        for (var block : List.of(HeatExchangerTerminalBlock.INSTANCE, HeatExchangerCondenserChannelBlock.INSTANCE,
                HeatExchangerEvaporatorChannelBlock.INSTANCE, HeatExchangerFluidPortBlock.INSTANCE)) {
            registry.addWorkstations(EXCHANGE, EntryStacks.of(block));
        }
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        if (RecipeViewers.emi() || !Config.CONFIG.EnableJEIIntegration) return;
        for (var holder : registry.getRecipeManager().getAllRecipesFor(ReprocessingRecipes.TYPE.get())) {
            var recipe = holder.value();
            var inputs = new ArrayList<EntryIngredient>();
            inputs.add(EntryIngredients.ofItemStacks(Arrays.stream(recipe.ingredient().getItems())
                    .map(stack -> stack.copyWithCount(recipe.count())).toList()));
            if (recipe.totalFluid() > 0) inputs.add(EntryIngredients.of(recipe.fluid(), recipe.totalFluid()));
            var output = recipe.producesFluid() ? fluid(recipe.fluidResult()) : EntryIngredients.of(recipe.result());
            registry.add(new Page(PROCESSING, inputs, List.of(output), holder.id(), recipe, null, List.of()), holder);
        }
        for (var moderator : RecipeViewerData.moderatorBlocks()) {
            registry.add(info(MODERATORS, moderator.block(), Stats.moderator(moderator.properties())));
        }
        for (var coil : RecipeViewerData.coils()) registry.add(info(COILS, coil.block(), Stats.coil(coil.data())));
        for (var coolant : RecipeViewerData.coolants()) registry.add(conversion(COOLANTS, coolant.card()));
        for (var exchange : RecipeViewerData.heatExchanges()) registry.add(conversion(EXCHANGE, exchange.card()));
    }

    @Override
    public void registerExclusionZones(ExclusionZones zones) {
        zones.register(net.minecraft.client.gui.screens.Screen.class, screen -> screen instanceof ModScreen<?> modScreen
                ? modScreen.extraAreas().stream().map(area -> new Rectangle(area.getX(), area.getY(), area.getWidth(), area.getHeight())).toList()
                : List.of());
    }

    private static EntryIngredient fluid(FluidStack stack) {
        return EntryIngredients.of(dev.architectury.fluid.FluidStack.create(stack.getFluid(), stack.getAmount(), stack.getComponentsPatch()));
    }

    private static Page info(CategoryIdentifier<Page> category, ItemStack stack, List<Stats.Stat> stats) {
        return new Page(category, List.of(EntryIngredients.of(stack)), List.of(), null, null, null, stats);
    }

    private static Page conversion(CategoryIdentifier<Page> category, RecipeViewerData.FluidCard card) {
        return new Page(category, List.of(fluid(card.input())), List.of(fluid(card.output())), null, null, card, card.stats());
    }

    private record Page(CategoryIdentifier<Page> category, List<EntryIngredient> inputs, List<EntryIngredient> outputs,
                        ResourceLocation location, ReprocessingRecipe recipe, RecipeViewerData.FluidCard card,
                        List<Stats.Stat> stats) implements Display {
        @Override public List<EntryIngredient> getInputEntries() { return inputs; }
        @Override public List<EntryIngredient> getOutputEntries() { return outputs; }
        @Override public CategoryIdentifier<?> getCategoryIdentifier() { return category; }
        @Override public Optional<ResourceLocation> getDisplayLocation() { return Optional.ofNullable(location); }
    }

    private record Category(CategoryIdentifier<Page> id, String title, ItemLike icon, int width, int height)
            implements DisplayCategory<Page> {
        @Override public CategoryIdentifier<Page> getCategoryIdentifier() { return id; }
        @Override public Component getTitle() { return Component.translatable("jei.biggerreactorsrework." + title); }
        @Override public Renderer getIcon() { return EntryStacks.of(icon); }
        @Override public int getDisplayWidth(Page display) { return width + 8; }
        @Override public int getDisplayHeight() { return height + 8; }

        @Override
        public List<Widget> setupDisplay(Page page, Rectangle bounds) {
            var widgets = new ArrayList<Widget>();
            int x = bounds.x + 4, y = bounds.y + 4;
            widgets.add(Widgets.createRecipeBase(bounds));
            widgets.add(Widgets.createDrawableWidget((g, mouseX, mouseY, delta) -> {
                g.pose().pushPose();
                g.pose().translate(x, y, 0);
                try {
                    withViewerTheme(() -> {
                        if (page.recipe() != null) Cyanite.draw(g, page.recipe());
                        else if (page.card() != null) page.card().draw(g);
                        else Stats.draw(g, page.stats());
                    });
                } finally {
                    g.pose().popPose();
                }
            }));
            if (page.recipe() != null) {
                var recipe = page.recipe();
                slot(widgets, page.inputs().getFirst(), x + Cyanite.INPUT_X, y + Cyanite.SLOT_Y, false);
                if (recipe.producesFluid()) tank(widgets, recipe.fluidResult(), x + Cyanite.PRODUCT_X, y, true, List.of());
                else slot(widgets, page.outputs().getFirst(), x + Cyanite.OUTPUT_X, y + Cyanite.SLOT_Y, true);
                if (recipe.totalFluid() > 0) tank(widgets,
                        new FluidStack(recipe.fluid(), (int) Math.min(Integer.MAX_VALUE, recipe.totalFluid())),
                        x + Cyanite.WATER_X, y, false, Cyanite.waterTooltip(recipe));
                widgets.add(Widgets.createTooltip(new Rectangle(x + Cyanite.ENERGY_X, y, GaugeBar.WIDTH, Cyanite.HEIGHT), Cyanite.energyTooltip(recipe)));
                widgets.add(Widgets.createTooltip(new Rectangle(x + Cyanite.ARROW_X, y + Cyanite.ARROW_Y,
                        Cyanite.ARROW_WIDTH, Cyanite.ARROW_HEIGHT), Cyanite.timeTooltip(recipe)));
            } else {
                int rowTop = page.card() == null ? 0 : Stats.ROW;
                if (page.card() == null) slot(widgets, page.inputs().getFirst(), x, y + Stats.slotY(page.stats()), false);
                else {
                    fluidSlot(widgets, page.card().input(), x, y + RecipeViewerData.FluidCard.inputY(), false);
                    fluidSlot(widgets, page.card().output(), x, y + page.card().outputY(), true);
                }
                for (int i = 0; i < page.stats().size(); i++) {
                    widgets.add(Widgets.createTooltip(new Rectangle(x + Stats.rowX(), y + rowTop + i * Stats.ROW,
                            Stats.rowWidth(), Stats.ROW), Stats.tooltip(page.stats().get(i))));
                }
            }
            return widgets;
        }
    }

    private static void slot(List<Widget> widgets, EntryIngredient ingredient, int x, int y, boolean output) {
        // Modified 2026-10-27: explicit frame bounds leave the same 1 px inset as the shared artwork.
        var slot = Widgets.createSlot(new Rectangle(x, y, 18, 18)).entries(ingredient).disableBackground();
        widgets.add(output ? slot.markOutput() : slot.markInput());
    }

    /** Modified 2026-10-27: read the live REI theme on every draw, including separate tank widgets. */
    private static void withViewerTheme(Runnable draw) {
        GuiTheme.withTheme(me.shedaniel.rei.api.client.config.ConfigObject.getInstance().isUsingDarkTheme(), draw);
    }

    private static void fluidSlot(List<Widget> widgets, FluidStack fluid, int x, int y, boolean output) {
        var entry = EntryStacks.of(dev.architectury.fluid.FluidStack.create(fluid.getFluid(), fluid.getAmount(), fluid.getComponentsPatch()));
        var delegate = entry.getRenderer();
        entry.withRenderer(new EntryRenderer<dev.architectury.fluid.FluidStack>() {
            @Override
            public void render(EntryStack<dev.architectury.fluid.FluidStack> stack, GuiGraphics g, Rectangle bounds,
                               int mouseX, int mouseY, float delta) {
                var extension = net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid.getFluid());
                var texture = extension.getStillTexture(fluid);
                if (texture == null) return;
                var sprite = net.minecraft.client.Minecraft.getInstance()
                        .getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(texture);
                int tint = extension.getTintColor(fluid);
                g.blit(bounds.x, bounds.y, 0, bounds.width, bounds.height, sprite,
                        ((tint >> 16) & 255) / 255F, ((tint >> 8) & 255) / 255F, (tint & 255) / 255F, ((tint >>> 24) & 255) / 255F);
            }

            @Override
            public Tooltip getTooltip(EntryStack<dev.architectury.fluid.FluidStack> stack, TooltipContext context) {
                return delegate.getTooltip(stack, context);
            }
        });
        slot(widgets, EntryIngredient.of(entry), x, y, output);
    }

    /** Full-height native ingredient hitbox; the custom renderer preserves the machine's decorated gauge. */
    private static void tank(List<Widget> widgets, FluidStack fluid, int x, int y, boolean output, List<Component> extraTooltip) {
        var entry = EntryStacks.of(dev.architectury.fluid.FluidStack.create(fluid.getFluid(), fluid.getAmount(), fluid.getComponentsPatch()));
        var delegate = entry.getRenderer();
        entry.withRenderer(new EntryRenderer<dev.architectury.fluid.FluidStack>() {
            @Override
            public void render(EntryStack<dev.architectury.fluid.FluidStack> stack, GuiGraphics g, Rectangle bounds,
                               int mouseX, int mouseY, float delta) {
                withViewerTheme(() -> GaugeBar.drawFluid(g, bounds.x, bounds.y, fluid.getAmount(), Math.max(1, fluid.getAmount()), fluid));
            }

            @Override
            public Tooltip getTooltip(EntryStack<dev.architectury.fluid.FluidStack> stack, TooltipContext context) {
                return delegate.getTooltip(stack, context).addAllTexts(extraTooltip);
            }
        });
        var slot = Widgets.createSlot(new Rectangle(x - 1, y - 1, GaugeBar.WIDTH + 2, GaugeBar.HEIGHT + 2))
                .entry(entry).disableBackground();
        widgets.add(output ? slot.markOutput() : slot.markInput());
    }
}
