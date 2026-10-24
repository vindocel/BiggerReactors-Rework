package biggerreactorsrework.client.deps;

import biggerreactorsrework.Config;
import biggerreactorsrework.client.EnergyBar;
import biggerreactorsrework.client.GaugeBar;
import biggerreactorsrework.client.GuiArt;
import biggerreactorsrework.core.client.gui.RenderHelper;
import biggerreactorsrework.registries.ReactorModeratorRegistry;
import biggerreactorsrework.registries.TurbineCoilRegistry;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.List;

/** Recipe data and drawing shared by the optional recipe viewer plugins (client side). */
public final class RecipeViewerData {
    private RecipeViewerData() {
    }

    public record ModeratorBlock(ItemStack block, ReactorModeratorRegistry.IModeratorProperties properties) {
    }

    public record ModeratorFluid(Fluid fluid, ReactorModeratorRegistry.IModeratorProperties properties) {
    }

    public record Coil(ItemStack block, TurbineCoilRegistry.CoilData data) {
    }

    public static List<ModeratorBlock> moderatorBlocks() {
        List<ModeratorBlock> result = new ArrayList<>();
        ReactorModeratorRegistry.Client.forEach((block, properties) -> {
            if (!(block instanceof LiquidBlock) && !(block instanceof AirBlock)) {
                result.add(new ModeratorBlock(new ItemStack(block.asItem()), properties));
            }
        });
        return result;
    }

    public static List<ModeratorFluid> moderatorFluids() {
        List<ModeratorFluid> result = new ArrayList<>();
        ReactorModeratorRegistry.Client.forEach((block, properties) -> {
            if (block instanceof LiquidBlock liquid) {
                result.add(new ModeratorFluid(liquid.fluid, properties));
            }
        });
        return result;
    }

    public static List<Coil> coils() {
        List<Coil> result = new ArrayList<>();
        TurbineCoilRegistry.Client.getImmutableRegistry().forEach((block, data) -> result.add(new Coil(new ItemStack(block.asItem()), data)));
        return result;
    }

    public static List<Component> moderatorLines(ReactorModeratorRegistry.IModeratorProperties properties) {
        return List.of(
                Component.translatable("jei.biggerreactorsrework.classic.reactor_moderator_moderation", properties.moderation()),
                Component.translatable("jei.biggerreactorsrework.classic.reactor_moderator_absorption", properties.absorption()),
                Component.translatable("jei.biggerreactorsrework.classic.reactor_moderator_conductivity", properties.heatConductivity()),
                Component.translatable("jei.biggerreactorsrework.classic.reactor_moderator_efficiency", properties.heatEfficiency()));
    }

    public static List<Component> coilLines(TurbineCoilRegistry.CoilData data) {
        return List.of(
                Component.translatable("jei.biggerreactorsrework.classic.turbine_coil_bonus", data.bonus),
                Component.translatable("jei.biggerreactorsrework.classic.turbine_coil_efficiency", data.efficiency),
                Component.translatable("jei.biggerreactorsrework.classic.turbine_coil_extraction", data.extractionRate));
    }

    /**
     * The Cyanite Reprocessor's processing area, laid out like its screen: battery, input, progress arrow, output and
     * water tank. One recipe: 2 cyanite ingots, water and energy make 1 blutonium ingot.
     */
    public static final class Cyanite {
        private Cyanite() {
        }

        public static final int CYANITE_PER_INGOT = 2;
        public static final int WIDTH = 162;
        public static final int HEIGHT = GaugeBar.HEIGHT;
        /** Slot frames (18x18); the items go 1 px inside. */
        public static final int INPUT_X = 36;
        public static final int OUTPUT_X = 108;
        public static final int SLOT_Y = 15;
        public static final int ARROW_X = 68;
        public static final int ARROW_Y = 15;
        public static final int ARROW_WIDTH = 24;
        public static final int ARROW_HEIGHT = 18;
        public static final int ENERGY_X = 0;
        public static final int WATER_X = WIDTH - GaugeBar.WIDTH;

        public static int ticks() {
            return Config.CONFIG.CyaniteReprocessor.TotalWorkTime;
        }

        public static long energy() {
            return (long) Config.CONFIG.CyaniteReprocessor.EnergyConsumptionPerTick * ticks();
        }

        public static long water() {
            return (long) Config.CONFIG.CyaniteReprocessor.WaterConsumptionPerTick * ticks();
        }

        public static void draw(GuiGraphics g) {
            EnergyBar.draw(g, ENERGY_X, 0, GaugeBar.WIDTH, HEIGHT,
                    EnergyBar.fraction(energy(), Config.CONFIG.CyaniteReprocessor.EnergyTankCapacity));
            GaugeBar.drawFluid(g, WATER_X, 0, water(), Config.CONFIG.CyaniteReprocessor.WaterTankCapacity, Fluids.WATER);
            GuiArt.slot(g, INPUT_X, SLOT_Y, 18);
            GuiArt.slot(g, OUTPUT_X, SLOT_Y, 18);
            long cycle = Math.max(1, ticks()) * 50L;
            GaugeBar.drawProgressArrow(g, ARROW_X, ARROW_Y, Util.getMillis() % cycle, cycle);
        }

        public static List<Component> energyTooltip() {
            return List.of(Component.translatable("recipe_viewer.biggerreactorsrework.cyanite_reprocessing.energy",
                    RenderHelper.formatValue(energy(), "FE")));
        }

        public static List<Component> waterTooltip() {
            return List.of(Component.translatable("recipe_viewer.biggerreactorsrework.cyanite_reprocessing.water",
                    RenderHelper.formatValue(water() / 1000.0, "B", true)));
        }

        public static List<Component> timeTooltip() {
            return List.of(Component.translatable("recipe_viewer.biggerreactorsrework.cyanite_reprocessing.time",
                    String.format(java.util.Locale.ROOT, "%.1f", ticks() / 20.0)));
        }
    }
}
