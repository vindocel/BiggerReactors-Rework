package biggerreactorsrework.compat.iris.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/** Added 2026-11-03: supply the neutral overlay when an item shader needs it during development validation. */
@Mixin(RenderSetup.class)
abstract class ItemOverlaySamplerMixin {
    @Shadow @Final private RenderPipeline pipeline;

    @Inject(method = "getTextures", at = @At("RETURN"), cancellable = true)
    private void rework$bindItemOverlay(CallbackInfoReturnable<Map<String, RenderSetup.TextureAndSampler>> callback) {
        if (!SharedConstants.IS_RUNNING_IN_IDE || !rework$isItemPipeline(pipeline) || !rework$shadersActive()) return;
        var textures = callback.getReturnValue();
        if (textures.containsKey("Sampler1")) return;
        var withOverlay = new HashMap<>(textures);
        withOverlay.put("Sampler1", new RenderSetup.TextureAndSampler(
                Minecraft.getInstance().gameRenderer.overlayTexture().getTextureView(),
                RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)));
        callback.setReturnValue(withOverlay);
    }

    @Unique
    private static boolean rework$isItemPipeline(RenderPipeline pipeline) {
        var id = pipeline.getLocation();
        return switch (id.toString()) {
            case "minecraft:pipeline/item_cutout", "minecraft:pipeline/item_translucent",
                 "neoforge:pipeline/item_cutout_unlit", "neoforge:pipeline/item_translucent_unlit" -> true;
            default -> false;
        };
    }

    @Unique private static boolean rework$shaderApiChecked;
    @Unique private static Object rework$shaderApi;
    @Unique private static Method rework$shaderPackInUse;

    @Unique
    private static boolean rework$shadersActive() {
        try {
            if (!rework$shaderApiChecked) {
                rework$shaderApiChecked = true;
                Class<?> type = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                rework$shaderApi = type.getMethod("getInstance").invoke(null);
                rework$shaderPackInUse = type.getMethod("isShaderPackInUse");
            }
            return rework$shaderPackInUse != null
                    && Boolean.TRUE.equals(rework$shaderPackInUse.invoke(rework$shaderApi));
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }
}
