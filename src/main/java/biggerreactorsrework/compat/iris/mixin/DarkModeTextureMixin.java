package biggerreactorsrework.compat.iris.mixin;

import biggerreactorsrework.BiggerReactors;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Pseudo
@Mixin(targets = "my.celium.org.darkmode.client.ScreenPaths", remap = false)
public abstract class DarkModeTextureMixin {
    @Inject(method = "isScreenTexture", at = @At("HEAD"), cancellable = true, require = 0)
    private static void biggerreactorsrework$keepTexture(String namespace, String path, boolean modScreens,
                                                        List<String> skipped, CallbackInfoReturnable<Boolean> callback) {
        if (BiggerReactors.modid.equals(namespace)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "isScreenSprite", at = @At("HEAD"), cancellable = true, require = 0)
    private static void biggerreactorsrework$keepSprite(String namespace, String path, boolean modScreens, boolean hud,
                                                       List<String> skipped, CallbackInfoReturnable<Boolean> callback) {
        if (BiggerReactors.modid.equals(namespace)) {
            callback.setReturnValue(false);
        }
    }
}
