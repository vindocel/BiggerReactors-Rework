package biggerreactorsrework.compat.iris.mixin;

import biggerreactorsrework.client.ScreenPaletteGuard;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
//? if =26.1.2 || =26.3 {
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
//?} else if =1.21.1 {
/*import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
*/
//?}
import net.neoforged.neoforge.client.ClientHooks;
import org.spongepowered.asm.mixin.Mixin;

/** Includes popup drawing in the screen's pre/post events, scoped separately for every GUI layer. */
@Mixin(ClientHooks.class)
public abstract class ScreenEventPaletteMixin {
//? if =26.1.2 || =26.3 {
    //? if >=26.3 {
    /*@WrapMethod(method = "extractScreenInternal", remap = false)
    *///?} else {
    @WrapMethod(method = "drawScreenInternal", remap = false)
    //?}
    private static void biggerreactorsrework$preserveEventPalette(Screen screen, GuiGraphicsExtractor graphics,
                                                                 int mouseX, int mouseY, float partialTick,
//?} else if =1.21.1 {
/*    @WrapMethod(method = "drawScreenInternal", remap = false)
    private static void biggerreactorsrework$preserveEventPalette(Screen screen, GuiGraphics graphics,
                                                                 int mouseX, int mouseY, float partialTick,
*/
//?}
                                                                 Operation<Void> original) {
        ScreenPaletteGuard.draw(screen, () -> original.call(screen, graphics, mouseX, mouseY, partialTick));
    }
}
