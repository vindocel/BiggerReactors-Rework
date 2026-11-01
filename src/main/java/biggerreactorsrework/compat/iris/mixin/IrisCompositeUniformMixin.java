package biggerreactorsrework.compat.iris.mixin;

import biggerreactorsrework.compat.iris.IrisCompositeUniforms;
import com.mojang.renderpearl.frontend.FrontendRenderPass;
import com.mojang.renderpearl.frontend.FrontendRenderPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import java.util.Collection;
import java.util.ArrayList;

/** Added 2026-10-31: retain strict validation except the placeholder sampler owned by Iris's custom pass. */
@Mixin(FrontendRenderPass.class)
abstract class IrisCompositeUniformMixin {
    @Shadow private FrontendRenderPipeline boundPipeline;

    @Unique private static final String REWORK_IRIS_VERSION = net.neoforged.fml.loading.LoadingModList.get()
            .getMods().stream().filter(m -> m.getModId().equals("iris"))
            .map(m -> m.getVersion().toString()).findFirst().orElse("");

    @ModifyVariable(method = "validateDraw", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Collection<String> rework$customCompositeSampler(Collection<String> dynamicUniforms) {
        if (!rework$isCustomComposite()) return dynamicUniforms;
        if (dynamicUniforms.contains("InSampler")) return dynamicUniforms;
        var customUniforms = new ArrayList<>(dynamicUniforms);
        customUniforms.add("InSampler");
        return customUniforms;
    }

    // The placeholder declares ALWAYS_PASS/no depth writes, but RenderPearl treats any depth state as requiring
    // an attachment. Iris binds its own framebuffer; the color-only center-depth sampling pass needs none.
    @Redirect(method = "validateDraw", at = @At(value = "INVOKE",
            target = "Lcom/mojang/renderpearl/frontend/FrontendRenderPipeline;wantsDepthTexture()Z"))
    private boolean rework$customCompositeDepth(FrontendRenderPipeline pipeline) {
        return pipeline.wantsDepthTexture() && !rework$isCustomComposite();
    }

    @Redirect(method = "validateDraw", at = @At(value = "INVOKE", target = "Ljava/util/List;get(I)Ljava/lang/Object;"))
    private Object rework$omittedVertexBindings(java.util.List<?> bindings, int index) {
        // Iris's compiled custom pipelines store only their active vertex slot. Omitted slots are unbound,
        // whereas the frontend normally pads this list to 16 entries before validating it.
        if (boundPipeline != null && bindings == boundPipeline.vertexFormats() && index >= bindings.size()
                && (REWORK_IRIS_VERSION.equals("1.11.7") || REWORK_IRIS_VERSION.startsWith("1.11.7+"))) return null;
        return bindings.get(index);
    }

    @Unique private boolean rework$isCustomComposite() {
        if (boundPipeline == null || !IrisCompositeUniforms.usesCustomSampler(boundPipeline.name(), true, REWORK_IRIS_VERSION))
            return false;
        return IrisCompositeUniforms.hasCustomPass(this);
    }
}
