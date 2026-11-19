package biggerreactorsrework.core.client.gui.api;

//? if =26.1.2 || =26.3 {
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import javax.annotation.Nonnull;
//?} else if =1.21.1 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
*/
//?}

/**
 * Tooltip interface.
 */
//? if =26.1.2 || =26.3 {
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
//?} else if =1.21.1 {
/*@Deprecated
@OnlyIn(Dist.CLIENT)
public interface ITooltip {

    /^*
     * Render tooltip.
     *
     * @param poseStack The current pose stack.
     * @param mouseX    The x position of the mouse.
     * @param mouseY    The y position of the mouse.
     ^/
    void renderTooltip(@Nonnull GuiGraphics graphics, int mouseX, int mouseY);
}
*/
//?}
