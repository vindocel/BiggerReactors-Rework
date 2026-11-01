package biggerreactorsrework.multiblocks.turbine.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.RenderPipelines;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import net.minecraft.client.renderer.texture.OverlayTexture;
import biggerreactorsrework.core.util.FluidSurfaceMesh;
import biggerreactorsrework.core.util.SteamWindowMesh;
import java.util.List;

/** Modified 2026-10-31: seamless animated films behind windows; no intersecting interior planes. */
public final class TurbineSteamRenderer {
    private static final net.minecraft.resources.Identifier STEAM_TEXTURE = net.minecraft.resources.Identifier.fromNamespaceAndPath(
            "biggerreactorsrework", "textures/effect/turbine_steam.png");
    private TurbineSteamRenderer() {}

    // Modified 2026-10-31: eyes preserves faint texels; ordinary entity shaders discard them.
    private static final RenderPipeline FILM_PIPELINE = pipeline(false);
    private static final RenderPipeline SHADER_FILM_PIPELINE = pipeline(true);
    private static final RenderType FILM = film("turbine_steam", FILM_PIPELINE);
    private static final RenderType SHADER_FILM = film("turbine_steam_shader", SHADER_FILM_PIPELINE);

    private static RenderPipeline pipeline(boolean depthWrite) {
        return RenderPipelines.EYES.toBuilder()
                .withLocation(net.minecraft.resources.Identifier.fromNamespaceAndPath("biggerreactorsrework",
                        depthWrite ? "pipeline/turbine_steam_shader" : "pipeline/turbine_steam"))
                .withCull(true)
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, depthWrite))
                .build();
    }

    private static RenderType film(String name, RenderPipeline pipeline) {
        return RenderType.create(name, RenderSetup.builder(pipeline).withTexture("Sampler0", STEAM_TEXTURE)
                .sortOnUpload().createRenderSetup());
    }

    public static void registerPipelines(net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent event) {
        event.registerPipeline(FILM_PIPELINE);
        event.registerPipeline(SHADER_FILM_PIPELINE);
    }

    private static final class Shaders {
        static final Object API;
        static final java.lang.reflect.Method ACTIVE, SHADOWS;
        static {
            Object api = null;
            java.lang.reflect.Method active = null, shadows = null;
            if (net.neoforged.fml.ModList.get().isLoaded("iris")) try {
                var type = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                api = type.getMethod("getInstance").invoke(null);
                active = type.getMethod("isShaderPackInUse");
                shadows = type.getMethod("isRenderingShadowPass");
            } catch (ReflectiveOperationException e) { api = null; }
            API = api; ACTIVE = active; SHADOWS = shadows;
        }
        static boolean ask(java.lang.reflect.Method method) {
            if (API == null || method == null) return false;
            try { return (boolean) method.invoke(API); }
            catch (ReflectiveOperationException e) { return false; }
        }
    }

    static void submit(PoseStack pose, SubmitNodeCollector collector, List<FluidSurfaceMesh.Face> windows, float density, int frame) {
        if (Shaders.ask(Shaders.SHADOWS)) return;
        boolean shader = Shaders.ask(Shaders.ACTIVE);
        final int opacity = shader ? biggerreactorsrework.util.TurbineAtmosphere.shaderOpacity(density)
                : biggerreactorsrework.util.TurbineAtmosphere.opacity(density);
        if (windows.isEmpty() || opacity <= 0) return;
        collector.submitCustomGeometry(pose, shader ? SHADER_FILM : FILM, (last, vertices) -> {
            for (var face : windows) for (int i = 0; i < 4; i++) {
                float u = (Math.floorMod((int) face.u0(), SteamWindowMesh.TILE_SIZE) +
                        face.coordinate(i, (face.axis() + 1) % 3) - face.u0()) / SteamWindowMesh.TILE_SIZE;
                float v = (Math.floorMod((int) face.v0(), SteamWindowMesh.TILE_SIZE) +
                        face.coordinate(i, (face.axis() + 2) % 3) - face.v0()) / SteamWindowMesh.TILE_SIZE;
                float normal = face.positive() ? 1 : -1;
                // The vertical texture coordinate follows world Y on either wall orientation.
                if (face.axis() == 0) { float oldU = u; u = v; v = oldU; }
                vertices.addVertex(last, face.coordinate(i, 0), face.coordinate(i, 1), face.coordinate(i, 2))
                        .setColor(245,248,250,opacity).setUv(u,(frame + v) / SteamWindowMesh.FRAMES)
                        .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0)
                        .setNormal(last,face.axis()==0?normal:0,face.axis()==1?normal:0,face.axis()==2?normal:0);
            }
        });
    }
}
