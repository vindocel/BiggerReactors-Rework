package biggerreactorsrework.multiblocks.turbine.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import biggerreactorsrework.core.util.FluidSurfaceMesh;
import biggerreactorsrework.core.util.SteamWindowMesh;
import java.util.List;

/** Modified 2026-10-31: continuous animated film over the whole window; no intersecting interior planes. */
final class TurbineSteamRenderer {
    private static final net.minecraft.resources.ResourceLocation STEAM_TEXTURE = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
            "biggerreactorsrework", "textures/effect/turbine_steam.png");
    private TurbineSteamRenderer() {}

    // Most entity shaders, and shader packs through them, cut alpha below 0.1 into hard patches. Block programs light
    // the film as a shaded wall and beacon programs overexpose it. The unlit eyes shader keeps a faint white film in both.
    private static final class Film extends RenderType {
        private Film() { super("", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true, () -> {}, () -> {}); }
        static final RenderType TYPE = type("biggerreactorsrework_turbine_steam", COLOR_WRITE);
        // Shader packs compose sky and clouds later over pixels without depth, which would erase the film there.
        static final RenderType SHADER_TYPE = type("biggerreactorsrework_turbine_steam_shader", COLOR_DEPTH_WRITE);
        private static RenderType type(String name, WriteMaskStateShard mask) {
            return create(name, DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, true, CompositeState.builder()
                    .setShaderState(RENDERTYPE_EYES_SHADER)
                    .setTextureState(new TextureStateShard(STEAM_TEXTURE, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(CULL)
                    .setWriteMaskState(mask)
                    .createCompositeState(false));
        }
    }

    /** Optional shader mod state, read by reflection so the API is never required. */
    private static final class Shaders {
        private static final Object API;
        private static final java.lang.reflect.Method IN_USE, SHADOWS;
        static {
            Object api = null;
            java.lang.reflect.Method inUse = null, shadows = null;
            if (net.neoforged.fml.ModList.get().isLoaded("iris")) try {
                var type = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                api = type.getMethod("getInstance").invoke(null);
                inUse = type.getMethod("isShaderPackInUse");
                shadows = type.getMethod("isRenderingShadowPass");
            } catch (ReflectiveOperationException e) {
                api = null;
            }
            API = api;
            IN_USE = inUse;
            SHADOWS = shadows;
        }
        private static boolean ask(java.lang.reflect.Method method) {
            if (API == null) return false;
            try {
                return (boolean) method.invoke(API);
            } catch (ReflectiveOperationException e) {
                return false;
            }
        }
        static boolean active() { return ask(IN_USE); }
        static boolean renderingShadows() { return ask(SHADOWS); }
    }

    static void draw(PoseStack pose, MultiBufferSource buffers, List<FluidSurfaceMesh.Face> windows, float density, int frame) {
        if (Shaders.renderingShadows()) return;
        final int opacity = Shaders.active() ? biggerreactorsrework.util.TurbineAtmosphere.shaderOpacity(density) : biggerreactorsrework.util.TurbineAtmosphere.opacity(density);
        if (windows.isEmpty() || opacity <= 0) return;
        var vertices = buffers.getBuffer(Shaders.active() ? Film.SHADER_TYPE : Film.TYPE);
        var last = pose.last();
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
                    .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(last,face.axis()==0?normal:0,face.axis()==1?normal:0,face.axis()==2?normal:0);
        }
    }
}
