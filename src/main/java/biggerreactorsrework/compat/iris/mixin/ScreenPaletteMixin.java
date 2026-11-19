package biggerreactorsrework.compat.iris.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//? if =26.1.2 || =26.3 {
import biggerreactorsrework.client.ScreenPaletteGuard;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
//?} else if =1.21.1 {
/*import biggerreactorsrework.client.ScreenPaletteGuard;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
*/
//?}
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Screen.class)
//? if =26.1.2 || =26.3 {
public abstract class ScreenPaletteMixin {
    @WrapMethod(method = "extractRenderStateWithTooltipAndSubtitles")
    private void biggerreactorsrework$preservePalette(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                                     float partialTick, Operation<Void> original) {
//?} else if =1.21.1 {
/*public abstract class ScreenPaletteMixin {
    @WrapMethod(method = "renderWithTooltip")
    private void biggerreactorsrework$preservePalette(GuiGraphics graphics, int mouseX, int mouseY,
                                                     float partialTick, Operation<Void> original) {
*/
//?}
        ScreenPaletteGuard.draw((Screen) (Object) this, () -> original.call(graphics, mouseX, mouseY, partialTick));
    }
}
