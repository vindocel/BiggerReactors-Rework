package biggerreactorsrework.client.deps.jei;

import biggerreactorsrework.client.FluidSprites;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

/** Draw component-bearing coolant using the same sprite as tanks and the guide. */
public final class CoolantIngredientRenderer implements IIngredientRenderer<FluidStack> {
    public static final CoolantIngredientRenderer INSTANCE = new CoolantIngredientRenderer();
    private CoolantIngredientRenderer() {}

    @Override public int getWidth() { return 16; }
    @Override public int getHeight() { return 16; }

    @Override public void render(GuiGraphicsExtractor graphics, FluidStack stack) {
        if (!stack.isEmpty()) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FluidSprites.still(stack), 0, 0, 16, 16, FluidSprites.tint(stack));
    }

    @Override public List<Component> getTooltip(FluidStack stack, TooltipFlag flags) {
        return List.of(stack.getHoverName());
    }
}
