package biggerreactorsrework.core.client.gui.api;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import javax.annotation.Nonnull;

/**
 * Tooltip interface.
 */
@Deprecated
public interface ITooltip {

    /**
     * Render tooltip.
     *
     * @param poseStack The current pose stack.
     * @param mouseX    The x position of the mouse.
     * @param mouseY    The y position of the mouse.
     */
    void renderTooltip(@Nonnull GuiGraphicsExtractor graphics, int mouseX, int mouseY);
}
