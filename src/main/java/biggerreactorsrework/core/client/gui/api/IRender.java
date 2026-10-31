package biggerreactorsrework.core.client.gui.api;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Render interface.
 */
@Deprecated
public interface IRender {

    /**
     * Render
     *
     * @param poseStack The current pose stack.
     * @param mouseX    The x position of the mouse.
     * @param mouseY    The y position of the mouse.
     */
    void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY);
}
