package biggerreactorsrework.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;
import biggerreactorsrework.core.client.gui.RenderHelper;
import biggerreactorsrework.core.client.gui.elements.RenderedElement;

import javax.annotation.Nonnull;
import java.util.Locale;

@OnlyIn(Dist.CLIENT)
public class CommonRender {

    // Controls (selectors, buttons, text boxes) are drawn in code by GuiArt; gauge bars have their own textures,
    // see GaugeBar and EnergyBar.

    /**
     * Render an energy gauge.
     * Such a gauge is 18x64 pixels in size, drawn with the textures in {@code textures/gui/energy/}.
     *
     * @param graphics       The current graphics.
     * @param symbol         The symbol to draw as.
     * @param energyStored   The amount of energy to draw.
     * @param energyCapacity The max energy capacity that can be displayed.
     */
    public static <T extends AbstractContainerMenu> void renderEnergyGauge(@Nonnull GuiGraphics graphics, @Nonnull RenderedElement<T> symbol, long energyStored, long energyCapacity) {
        // DFM-style bar, filling the whole gauge slot.
        EnergyBar.draw(graphics, symbol.x, symbol.y, symbol.width, symbol.height, EnergyBar.fraction(energyStored, energyCapacity));
        // Update tooltip.
        symbol.tooltip = Component.literal(String.format("%s/%s",
                RenderHelper.formatValue(Math.max(0, energyStored), "FE"),
                RenderHelper.formatValue(Math.max(0, energyCapacity), "FE")));
        RenderHelper.clearRenderColor();
    }

    /**
     * Render a fluid gauge.
     * Such a gauge is 18x64 pixels in size, drawn with the textures in {@code textures/gui/fluid/}.
     *
     * @param graphics      The current graphics.
     * @param symbol        The symbol to draw as.
     * @param fluidStored   The amount of fluid to draw.
     * @param fluidCapacity The max fluid capacity that can be displayed.
     * @param fluid         The fluid to use.
     */
    public static <T extends AbstractContainerMenu> void renderFluidGauge(@Nonnull GuiGraphics graphics, @Nonnull RenderedElement<T> symbol, long fluidStored, long fluidCapacity, Fluid fluid) {
        GaugeBar.drawFluid(graphics, symbol.x, symbol.y, fluidStored, fluidCapacity, fluid);
        // Update tooltip.
        symbol.tooltip = Component.literal(String.format("%s/%s of %s",
                RenderHelper.formatValue((fluidStored / 1000.0), "B", true),
                RenderHelper.formatValue((fluidCapacity / 1000.0), "B", true),
                new FluidStack(fluid, 1).getHoverName().getString().toLowerCase(Locale.US)));
        RenderHelper.clearRenderColor();
    }
}
