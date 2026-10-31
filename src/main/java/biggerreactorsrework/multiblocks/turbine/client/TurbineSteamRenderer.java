package biggerreactorsrework.multiblocks.turbine.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import biggerreactorsrework.core.util.FluidSurfaceMesh;
import biggerreactorsrework.core.util.SteamWindowMesh;
import java.util.List;

/** Modified 2026-10-31: seamless animated films behind windows; no intersecting interior planes. */
final class TurbineSteamRenderer {
    private static final net.minecraft.resources.Identifier STEAM_TEXTURE = net.minecraft.resources.Identifier.fromNamespaceAndPath(
            "biggerreactorsrework", "textures/effect/turbine_steam.png");
    private TurbineSteamRenderer() {}

    static void submit(PoseStack pose, SubmitNodeCollector collector, List<FluidSurfaceMesh.Face> windows, float density, int frame) {
        final int opacity = biggerreactorsrework.util.TurbineAtmosphere.opacity(density);
        if (windows.isEmpty() || opacity <= 0) return;
        collector.submitCustomGeometry(pose, RenderTypes.entityTranslucentCull(STEAM_TEXTURE), (last, vertices) -> {
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
