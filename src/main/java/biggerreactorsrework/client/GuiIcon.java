package biggerreactorsrework.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import biggerreactorsrework.BiggerReactors;

import javax.annotation.Nonnull;

/**
 * One icon of the screens, each in its own file under {@code textures/gui/icons/}, drawn at its real size.
 */
@OnlyIn(Dist.CLIENT)
public record GuiIcon(ResourceLocation texture, int width, int height) {

    // Status icons.
    public static final GuiIcon BATTERY = of("battery", 10, 16);
    public static final GuiIcon CASE_HEAT = of("case_heat", 16, 16);
    public static final GuiIcon CONDENSER_EXHAUST = of("condenser_exhaust", 16, 16);
    public static final GuiIcon CONDENSER_FLOW_RATE = of("condenser_flow_rate", 16, 16);
    public static final GuiIcon CONDENSER_INTAKE = of("condenser_intake", 16, 16);
    public static final GuiIcon CONDENSER_TEMPERATURE = of("condenser_temperature", 16, 16);
    public static final GuiIcon CONTROL_ROD_INSERTION = of("control_rod_insertion", 13, 16);
    public static final GuiIcon EJECT_WASTE = of("eject_waste", 16, 16);
    public static final GuiIcon ENERGY_GENERATION_RATE = of("energy_generation_rate", 12, 14);
    public static final GuiIcon EVAPORATOR_EXHAUST = of("evaporator_exhaust", 16, 16);
    public static final GuiIcon EVAPORATOR_FLOW_RATE = of("evaporator_flow_rate", 16, 16);
    public static final GuiIcon EVAPORATOR_INTAKE = of("evaporator_intake", 16, 16);
    public static final GuiIcon EVAPORATOR_TEMPERATURE = of("evaporator_temperature", 16, 16);
    public static final GuiIcon FLOW_RATE = of("flow_rate", 16, 16);
    public static final GuiIcon FUEL_AMOUNT = of("fuel_amount", 15, 16);
    public static final GuiIcon FUEL_ENRICHMENT = of("fuel_enrichment", 15, 16);
    public static final GuiIcon FUEL_HEAT = of("fuel_heat", 16, 16);
    public static final GuiIcon FUEL_MIX = of("fuel_mix", 15, 16);
    public static final GuiIcon FUEL_USAGE_RATE = of("fuel_usage_rate", 16, 13);
    public static final GuiIcon HELP = of("help", 16, 16);
    public static final GuiIcon REACTIVITY = of("reactivity", 16, 16);
    public static final GuiIcon REACTOR_ACTIVITY = of("reactor_activity", 16, 12);
    public static final GuiIcon ROTOR_EFFICIENCY = of("rotor_efficiency", 16, 16);
    public static final GuiIcon ROTOR_SPEED = of("rotor_speed", 16, 16);
    public static final GuiIcon TACHOMETER = of("tachometer", 16, 16);
    public static final GuiIcon TEMPERATURE = of("temperature", 8, 16);
    public static final GuiIcon WASTE_AMOUNT = of("waste_amount", 15, 16);

    // Fluid symbols: outline drawn over the fluid, and the white mask of what the panel covers around it.
    public static final GuiIcon DROP_INPUT = of("drop_input", 13, 16);
    public static final GuiIcon DROP_INPUT_MASK = of("drop_input_mask", 16, 16);
    public static final GuiIcon DROP_OUTPUT = of("drop_output", 13, 16);
    public static final GuiIcon DROP_OUTPUT_MASK = of("drop_output_mask", 16, 16);
    public static final GuiIcon DROP_RATE = of("drop_rate", 13, 16);
    public static final GuiIcon DROP_RATE_MASK = of("drop_rate_mask", 16, 16);
    public static final GuiIcon WATER_DROP = of("water_drop", 11, 16);
    public static final GuiIcon WATER_DROP_MASK = of("water_drop_mask", 16, 16);

    // Button glyphs.
    public static final GuiIcon ARROW_LEFT = of("arrow_left", 7, 6);
    public static final GuiIcon ARROW_RIGHT = of("arrow_right", 7, 6);
    public static final GuiIcon ARROW_UP = of("arrow_up", 6, 8);
    public static final GuiIcon ARROW_DOWN = of("arrow_down", 6, 8);
    public static final GuiIcon ARROW_ENTER_DISABLED = of("arrow_enter_disabled", 9, 7);
    public static final GuiIcon ARROW_ENTER = of("arrow_enter", 9, 7);
    public static final GuiIcon CLOSE = of("close", 4, 5);
    public static final GuiIcon CONFIRM = of("confirm", 7, 8);
    public static final GuiIcon EJECT = of("eject", 7, 8);
    public static final GuiIcon AUTO_INPUT = of("auto_input", 4, 3);
    public static final GuiIcon AUTO_EJECT = of("auto_eject", 4, 3);
    public static final GuiIcon REVERT = of("revert", 7, 8);

    private static GuiIcon of(String name, int width, int height) {
        return new GuiIcon(ResourceLocation.fromNamespaceAndPath(BiggerReactors.modid, "textures/gui/icons/" + name + ".png"), width, height);
    }

    public void draw(@Nonnull GuiGraphics graphics, int x, int y) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blit(this.texture, x, y, 0, 0, this.width, this.height, this.width, this.height);
        RenderSystem.disableBlend();
    }

    /**
     * Draw the icon multiplied by a color; used to paint white masks in the color behind them.
     */
    public void drawTinted(@Nonnull GuiGraphics graphics, int x, int y, int color) {
        RenderSystem.setShaderColor(((color >> 16) & 0xFF) / 255F, ((color >> 8) & 0xFF) / 255F, (color & 0xFF) / 255F, 1.0F);
        this.draw(graphics, x, y);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
