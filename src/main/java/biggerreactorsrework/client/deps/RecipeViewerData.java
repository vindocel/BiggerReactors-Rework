package biggerreactorsrework.client.deps;

import biggerreactorsrework.Config;
import biggerreactorsrework.client.EnergyBar;
import biggerreactorsrework.client.GaugeBar;
import biggerreactorsrework.client.GuiArt;
import biggerreactorsrework.client.GuiTheme;
import biggerreactorsrework.core.client.gui.RenderHelper;
import biggerreactorsrework.registries.FluidTransitionRegistry;
import biggerreactorsrework.registries.ReactorModeratorRegistry;
import biggerreactorsrework.registries.TurbineCoilRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;

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

    public record Coolant(net.neoforged.neoforge.fluids.FluidStack input,net.neoforged.neoforge.fluids.FluidStack output,double latent,double temperature,
                          FluidTransitionRegistry.FluidTransition profile){
        public FluidCard card(){
            return new FluidCard(input,output,Stats.coolant(profile),List.of(coolantDestination(output)),List.of(0));
        }
    }
    public static Component coolantDestination(net.neoforged.neoforge.fluids.FluidStack output) {
        return Component.translatable(output.getFluid()==biggerreactorsrework.fluids.Steam.INSTANCE ?
            "recipe_viewer.biggerreactorsrework.coolant.turbine" : "recipe_viewer.biggerreactorsrework.coolant.exchanger");
    }
    public static List<Coolant> coolants(){
        var result=new ArrayList<Coolant>();
        for(var t:biggerreactorsrework.registries.CoolantSync.profiles()){
            var hot=new net.neoforged.neoforge.fluids.FluidStack(t.gases.getFirst(),1000);
            if(t.genericHot())hot.applyComponents(biggerreactorsrework.registries.CoolantComponents.heated(t));
            result.add(new Coolant(new net.neoforged.neoforge.fluids.FluidStack(t.liquids.getFirst(),1000),hot,t.latentHeat,t.boilingPoint,t));
        }
        return List.copyOf(result);
    }

    /** Added 2026-10-26: actual exchanger directions, including the generic hot coolant's origin. */
    public record HeatExchange(net.neoforged.neoforge.fluids.FluidStack input,
                               net.neoforged.neoforge.fluids.FluidStack output,double temperature,boolean evaporation,
                               FluidTransitionRegistry.FluidTransition profile) {
        public Component circuit(){
            return Component.translatable("recipe_viewer.biggerreactorsrework.exchanger."+(evaporation?"evaporation":"cooling"));
        }
        public Component requirement(){
            return Component.translatable("recipe_viewer.biggerreactorsrework.exchanger."+(evaporation?"requires_hot":"requires_water"));
        }
        public FluidCard card(){
            return new FluidCard(input,output,Stats.exchange(profile,evaporation),List.of(circuit(),requirement()),
                    List.of(evaporation?FluidCard.COLD:FluidCard.HOT,0));
        }
    }
    public static List<HeatExchange> heatExchanges() {
        var coolants=coolants();
        var water=coolants.stream().filter(c->c.input().getFluid()==Fluids.WATER && c.output().getFluid()==biggerreactorsrework.fluids.Steam.INSTANCE).findFirst();
        if(water.isEmpty())return List.of();
        var result=new ArrayList<HeatExchange>();
        result.add(new HeatExchange(water.get().input().copy(),water.get().output().copy(),water.get().temperature(),true,water.get().profile()));
        for(var coolant:coolants)if(coolant.temperature()>water.get().temperature())
            result.add(new HeatExchange(coolant.output().copy(),coolant.input().copy(),coolant.temperature(),false,coolant.profile()));
        return List.copyOf(result);
    }
    public static ResourceLocation heatExchangeId(HeatExchange recipe) {
        return net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey((recipe.evaporation()?recipe.input():recipe.output()).getFluid());
    }

    /**
     * Added 2026-10-26. A fluid conversion laid out like the moderator pages: the cold and hot fluids stacked on the left with an arrow
     * between them, their names on the first row, the properties as bars and a few notes under them.
     */
    public record FluidCard(net.neoforged.neoforge.fluids.FluidStack input, net.neoforged.neoforge.fluids.FluidStack output,
                            List<Stats.Stat> stats, List<Component> notes, List<Integer> noteColors) {
        public static final int WIDTH = Stats.WIDTH;
        public static final int COLD = 0xFF3A78C0;
        public static final int HOT = 0xFFC8582A;

        public int height() {
            return (stats.size() + notes.size() + 1) * Stats.ROW;
        }

        /** Slot frames (18x18); the fluids go 1 px inside. */
        public static int inputY() {
            return 0;
        }

        public int outputY() {
            return height() - 18;
        }

        public void draw(GuiGraphics g) {
            Font font = Minecraft.getInstance().font;
            GuiArt.slot(g, 0, inputY(), 18);
            GuiArt.slot(g, 0, outputY(), 18);
            int top = inputY() + 20, bottom = outputY() - 2;
            g.fill(8, top, 10, bottom - 3, GuiTheme.textColor());
            for (int i = 0; i < 4; i++) {
                g.fill(5 + i, bottom - 4 + i, 13 - i, bottom - 3 + i, GuiTheme.textColor());
            }
            Component title = Component.empty()
                    .append(input.getHoverName().copy().withColor(COLD))
                    .append(Component.literal(" → ").withColor(GuiTheme.textColor()))
                    .append(output.getHoverName().copy().withColor(HOT));
            GuiArt.scrollingText(g, font, title, Stats.rowX(), 2, WIDTH - Stats.rowX(), GuiTheme.textColor());
            Stats.drawRows(g, stats, Stats.ROW, WIDTH);
            for (int i = 0; i < notes.size(); i++) {
                int y = (stats.size() + 1 + i) * Stats.ROW + 2;
                GuiArt.scrollingText(g, font, notes.get(i), Stats.rowX(), y, WIDTH - Stats.rowX(), noteColors.get(i) == 0 ? GuiTheme.textColor() : noteColors.get(i));
            }
        }

        /** Index of the property row under the mouse, or -1. */
        public int row(double mouseX, double mouseY) {
            return Stats.row(stats, mouseX, mouseY - Stats.ROW, WIDTH);
        }

        /** Top of the property row {@code index}. */
        public static int rowY(int index) {
            return (index + 1) * Stats.ROW;
        }
    }

    public static List<Coil> coils() {
        List<Coil> result = new ArrayList<>();
        TurbineCoilRegistry.Client.getImmutableRegistry().forEach((block, data) -> result.add(new Coil(new ItemStack(block.asItem()), data)));
        return result;
    }

    /**
     * Moderator and coil properties drawn as bars. Each bar is scaled to the largest value of that property and
     * colored by how the value ranks among every registered entry: green near the top, red near the bottom.
     */
    public static final class Stats {
        private Stats() {
        }

        public static final int WIDTH = 168;
        public static final int ROW = 11;
        private static final int LABEL_X = 22;
        private static final int MARK_X = 77;
        private static final int BAR_X = 86;
        private static final int BAR_WIDTH = 48;
        private static final int BAR_HEIGHT = 5;
        private static final int BAR_FRAME = 0xFF373737;
        private static final int BAR_TRACK = 0xFF5A5A5A;
        private static final int LOW = 0xFFD04838;
        private static final int MIDDLE = 0xFFE0B030;
        private static final int HIGH = 0xFF48B048;
        private static final int TRADE_OFF = 0xFF5A8CD8;
        private static final String KEY = "recipe_viewer.biggerreactorsrework.stat.";

        /**
         * One property: its value, the largest value registered, its place counted from the highest (1 is the
         * highest) among {@code count} entries, and its rank from 0 (lowest) to 1 (highest). A trade-off property
         * has no better direction and is drawn in a neutral color.
         */
        public record Stat(String name, double value, double max, int place, int count, double rank, boolean tradeOff) {
        }

        public static List<Stat> moderator(ReactorModeratorRegistry.IModeratorProperties properties) {
            List<ReactorModeratorRegistry.IModeratorProperties> all = new ArrayList<>();
            ReactorModeratorRegistry.Client.forEach((block, entry) -> {
                if (!(block instanceof AirBlock)) {
                    all.add(entry);
                }
            });
            return List.of(
                    stat("moderation", properties.moderation(), all, ReactorModeratorRegistry.IModeratorProperties::moderation, false),
                    stat("absorption", properties.absorption(), all, ReactorModeratorRegistry.IModeratorProperties::absorption, true),
                    stat("efficiency", properties.heatEfficiency(), all, ReactorModeratorRegistry.IModeratorProperties::heatEfficiency, false),
                    stat("conductivity", properties.heatConductivity(), all, ReactorModeratorRegistry.IModeratorProperties::heatConductivity, false));
        }

        public static List<Stat> coil(TurbineCoilRegistry.CoilData data) {
            List<TurbineCoilRegistry.CoilData> all = new ArrayList<>(TurbineCoilRegistry.Client.getImmutableRegistry().values());
            return List.of(
                    stat("coil_efficiency", data.efficiency, all, coil -> coil.efficiency, false),
                    stat("bonus", data.bonus, all, coil -> coil.bonus, false),
                    stat("extraction", data.extractionRate, all, coil -> coil.extractionRate, false));
        }

        public static List<Stat> coolant(FluidTransitionRegistry.FluidTransition profile) {
            List<FluidTransitionRegistry.FluidTransition> all = biggerreactorsrework.registries.CoolantSync.profiles();
            return List.of(
                    stat("coolant_heat", profile.latentHeat, all, t -> t.latentHeat, false),
                    stat("coolant_temperature", profile.boilingPoint, all, t -> t.boilingPoint, true),
                    stat("coolant_conductivity", profile.liquidRFMKT, all, t -> t.liquidRFMKT, false),
                    stat("coolant_recovery", profile.recovery, all, t -> t.recovery, false));
        }

        /** The evaporator heats the cold fluid; the condenser takes the heat out of the hot one. */
        public static List<Stat> exchange(FluidTransitionRegistry.FluidTransition profile, boolean evaporation) {
            List<FluidTransitionRegistry.FluidTransition> all = biggerreactorsrework.registries.CoolantSync.profiles();
            return List.of(
                    stat("coolant_temperature", profile.boilingPoint, all, t -> t.boilingPoint, true),
                    stat("coolant_heat", profile.latentHeat, all, t -> t.latentHeat, false),
                    evaporation ? stat("coolant_conductivity", profile.liquidRFMKT, all, t -> t.liquidRFMKT, false)
                            : stat("coolant_hot_conductivity", profile.gasRFMKT, all, t -> t.gasRFMKT, false));
        }

        private static <T> Stat stat(String name, double value, List<T> all, ToDoubleFunction<T> property, boolean tradeOff) {
            double max = 0;
            int lower = 0, higher = 0, equal = 0;
            for (T entry : all) {
                double other = property.applyAsDouble(entry);
                max = Math.max(max, other);
                if (other < value) {
                    lower++;
                } else if (other > value) {
                    higher++;
                } else {
                    equal++;
                }
            }
            int count = Math.max(1, lower + higher + equal);
            double rank = count == 1 ? 1 : (lower + (equal - 1) / 2.0) / (count - 1);
            return new Stat(name, value, Math.max(max, value), higher + 1, count, Mth.clamp(rank, 0, 1), tradeOff);
        }

        public static int height(List<Stat> stats) {
            return stats.size() * ROW;
        }

        /** The slot frame for the block or fluid, centered on the left. */
        public static int slotY(List<Stat> stats) {
            return (height(stats) - 18) / 2;
        }

        public static void draw(GuiGraphics g, List<Stat> stats) {
            GuiArt.slot(g, 0, slotY(stats), 18);
            drawRows(g, stats, 0, WIDTH);
        }

        /** The rows only, from {@code top}, with the values ending at {@code width}. */
        public static void drawRows(GuiGraphics g, List<Stat> stats, int top, int width) {
            Font font = Minecraft.getInstance().font;
            int text = GuiTheme.textColor();
            for (int i = 0; i < stats.size(); i++) {
                Stat stat = stats.get(i);
                int y = top + i * ROW + 2;
                GuiArt.scrollingText(g, font, Component.translatable(KEY + stat.name()).getString(), LABEL_X, y, MARK_X - LABEL_X - 2, text);
                g.drawString(font, mark(stat), MARK_X, y, color(stat), false);
                int barY = y + 1;
                g.fill(BAR_X - 1, barY - 1, BAR_X + BAR_WIDTH + 1, barY + BAR_HEIGHT + 1, BAR_FRAME);
                g.fill(BAR_X, barY, BAR_X + BAR_WIDTH, barY + BAR_HEIGHT, BAR_TRACK);
                int fill = stat.max() <= 0 ? 0 : (int) Math.round(BAR_WIDTH * stat.value() / stat.max());
                if (stat.value() > 0) {
                    fill = Math.max(1, fill);
                }
                g.fill(BAR_X, barY, BAR_X + fill, barY + BAR_HEIGHT, color(stat));
                g.fill(BAR_X, barY, BAR_X + fill, barY + 1, FastColor.ARGB32.lerp(0.35f, color(stat), GuiArt.WHITE));
                String value = display(stat);
                g.drawString(font, value, width - font.width(value), y, text, false);
            }
        }

        /** Index of the row under the mouse, or -1. */
        public static int row(List<Stat> stats, double mouseX, double mouseY) {
            return row(stats, mouseX, mouseY, WIDTH);
        }

        public static int row(List<Stat> stats, double mouseX, double mouseY, int width) {
            if (mouseX < LABEL_X || mouseX >= width || mouseY < 0) {
                return -1;
            }
            int row = (int) (mouseY / ROW);
            return row < stats.size() ? row : -1;
        }

        public static int rowX() {
            return LABEL_X;
        }

        public static int rowWidth() {
            return WIDTH - LABEL_X;
        }

        public static List<Component> tooltip(Stat stat) {
            String key = KEY + stat.name();
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("guide.biggerreactorsrework.table." + stat.name() + ".title"));
            lines.add(Component.translatable("guide.biggerreactorsrework.table." + stat.name() + ".tip").withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable(KEY + "place", display(stat), stat.place(), stat.count()).withStyle(ChatFormatting.DARK_AQUA));
            if (!stat.tradeOff()) {
                if (stat.rank() >= 2 / 3.0) {
                    lines.add(Component.translatable(key + ".high").withStyle(ChatFormatting.GREEN));
                } else if (stat.rank() <= 1 / 3.0) {
                    lines.add(Component.translatable(key + ".low").withStyle(ChatFormatting.RED));
                } else {
                    lines.add(Component.translatable(KEY + "average").withStyle(ChatFormatting.YELLOW));
                }
            }
            if (I18n.exists(key + ".pro")) {
                lines.add(Component.translatable(key + ".pro").withStyle(ChatFormatting.GREEN));
            }
            if (I18n.exists(key + ".con")) {
                lines.add(Component.translatable(key + ".con").withStyle(ChatFormatting.RED));
            }
            if (I18n.exists(key + ".note")) {
                lines.add(Component.translatable(key + ".note").withStyle(ChatFormatting.GOLD));
            }
            return lines;
        }

        private static String mark(Stat stat) {
            if (stat.tradeOff()) {
                return "±";
            }
            return stat.rank() >= 2 / 3.0 ? "▲" : stat.rank() <= 1 / 3.0 ? "▼" : "◆";
        }

        private static int color(Stat stat) {
            if (stat.tradeOff()) {
                return TRADE_OFF;
            }
            double rank = stat.rank();
            return rank < 0.5 ? FastColor.ARGB32.lerp((float) (rank * 2), LOW, MIDDLE) : FastColor.ARGB32.lerp((float) (rank * 2 - 1), MIDDLE, HIGH);
        }

        private static String display(Stat stat) {
            return switch (stat.name()) {
                case "coolant_temperature" -> String.format(java.util.Locale.ROOT, "%.0fK", stat.value());
                case "coolant_recovery" -> String.format(java.util.Locale.ROOT, "%.0f%%", stat.value() * 100);
                default -> format(stat.value());
            };
        }

        private static String format(double value) {
            String text = String.format(java.util.Locale.ROOT, "%.3f", value);
            text = text.replaceAll("0+$", "");
            return text.endsWith(".") ? text + "0" : text;
        }
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

        public static void draw(GuiGraphics g) { draw(g, null); }

        public static void draw(GuiGraphics g, biggerreactorsrework.machine.recipe.ReprocessingRecipe recipe) {
            long energy = recipe == null ? energy() : (long)recipe.energy()*recipe.duration();
            long water = recipe == null ? water() : (long)recipe.water()*recipe.duration();
            int ticks = recipe == null ? ticks() : recipe.duration();
            EnergyBar.draw(g, ENERGY_X, 0, GaugeBar.WIDTH, HEIGHT,
                    EnergyBar.fraction(energy, Config.CONFIG.CyaniteReprocessor.EnergyTankCapacity));
            GaugeBar.drawFluid(g, WATER_X, 0, water, Config.CONFIG.CyaniteReprocessor.WaterTankCapacity, recipe == null ? Fluids.WATER : recipe.fluid());
            GuiArt.slot(g, INPUT_X, SLOT_Y, 18);
            GuiArt.slot(g, OUTPUT_X, SLOT_Y, 18);
            long cycle = Math.max(1, ticks) * 50L;
            GaugeBar.drawProgressArrow(g, ARROW_X, ARROW_Y, Util.getMillis() % cycle, cycle);
        }

        public static List<Component> energyTooltip() { return energyTooltip(null); }

        public static List<Component> energyTooltip(biggerreactorsrework.machine.recipe.ReprocessingRecipe recipe) {
            long energy = recipe == null ? energy() : (long)recipe.energy()*recipe.duration();
            return List.of(Component.translatable("recipe_viewer.biggerreactorsrework.cyanite_reprocessing.energy",
                    RenderHelper.formatValue(energy, "FE")));
        }

        public static List<Component> waterTooltip() { return waterTooltip(null); }

        public static List<Component> waterTooltip(biggerreactorsrework.machine.recipe.ReprocessingRecipe recipe) {
            long water = recipe == null ? water() : (long)recipe.water()*recipe.duration();
            return List.of(Component.translatable("recipe_viewer.biggerreactorsrework.cyanite_reprocessing.water",
                    RenderHelper.formatValue(water / 1000.0, "B", true)));
        }

        public static List<Component> timeTooltip() { return timeTooltip(null); }

        public static List<Component> timeTooltip(biggerreactorsrework.machine.recipe.ReprocessingRecipe recipe) {
            int ticks = recipe == null ? ticks() : recipe.duration();
            return List.of(Component.translatable("recipe_viewer.biggerreactorsrework.cyanite_reprocessing.time",
                    String.format(java.util.Locale.ROOT, "%.1f", ticks / 20.0)));
        }
    }
}
