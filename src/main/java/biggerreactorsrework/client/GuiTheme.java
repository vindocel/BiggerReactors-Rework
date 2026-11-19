//? if =26.1.2 {
package biggerreactorsrework.client;

// Modified 2026-11-01: native Minecraft 26.1.2 API.
import biggerreactorsrework.core.client.gui.screens.ModScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.resources.Identifier;

/** Scoped dark chrome: controls and gauge textures, never machine casing frames, icons, items or fluids. */
public final class GuiTheme {
//?} else if =1.21.1 {
/*package biggerreactorsrework.client;

import biggerreactorsrework.core.client.gui.screens.ModScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

/^* Scoped dark chrome: controls and gauge textures, never machine casing frames, icons, items or fluids. ^/
public final class GuiTheme {
*/
//?} else if =26.3 {
/*package biggerreactorsrework.client;

import biggerreactorsrework.core.client.gui.screens.ModScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.resources.Identifier;

/^* Scoped dark chrome: controls and gauge textures, never machine casing frames, icons, items or fluids. ^/
public final class GuiTheme {
*/
//?}
    public static final int LIGHT_PANEL = 0xFFC6C6C6;
//? if =26.1.2 || =26.3 {
    public static final int DARK_PANEL = GuiThemePalette.chrome(LIGHT_PANEL, true);
    private static Boolean scopedDark;
//?} else if =1.21.1 {
/*    public static final int DARK_PANEL = GuiThemePalette.chrome(LIGHT_PANEL, true);
    private static ShaderInstance shader;
    private static Boolean scopedDark;
*/
//?}

    private GuiTheme() {}

    public static boolean dark() {
//? if =26.1.2 || =1.21.1 {
        if (scopedDark != null) return scopedDark;
        return Minecraft.getInstance().screen instanceof ModScreen<?> screen && GuiThemes.dark(screen.machinePos());
    }
//?} else if =26.3 {
/*        if (scopedDark != null) return scopedDark;
        return Minecraft.getInstance().gui.screen() instanceof ModScreen<?> screen && GuiThemes.dark(screen.machinePos());
    }
*/
//?}

    /** Modified 2026-10-27: temporary theme for external viewer drawing, without changing player preferences. */
    public static void withTheme(boolean dark, Runnable draw) {
        Boolean previous = scopedDark;
        scopedDark = dark;
        try {
            draw.run();
        } finally {
            scopedDark = previous;
        }
    }

    public static int panelColor() { return dark() ? DARK_PANEL : LIGHT_PANEL; }
    public static int textColor() { return dark() ? 0xFFD0D0D0 : 0xFF404040; }

//? if =26.1.2 || =26.3 {
    public static int chromeColor(int argb) { return GuiThemePalette.chrome(argb, dark()); }

    public static final RenderPipeline DARK_PIPELINE = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(biggerreactorsrework.BiggerReactors.modid, "pipeline/dark_gui"))
            .withFragmentShader(Identifier.fromNamespaceAndPath(biggerreactorsrework.BiggerReactors.modid, "core/dark_gui"))
            .build();

    public static void blit(GuiGraphicsExtractor g, Identifier texture, int x, int y, float u, float v,
                            int width, int height, int textureWidth, int textureHeight) {
//?} else if =1.21.1 {
/*    public static int chromeColor(int argb) { return GuiThemePalette.chrome(argb, dark()); }

    public static void setShader(ShaderInstance instance) { shader = instance; }

    public static void blit(GuiGraphics g, ResourceLocation texture, int x, int y, float u, float v,
                            int width, int height, int textureWidth, int textureHeight) {
*/
//?}
        blit(g, texture, x, y, width, height, u, v, width, height, textureWidth, textureHeight);
//? if =26.1.2 || =26.3 {
    }

    /** Same UV/stretching contract as GuiGraphicsExtractor.blit, with neutral-pixel darkening applied only here. */
    public static void blit(GuiGraphicsExtractor g, Identifier texture, int x, int y, int width, int height,
                            float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        g.blit(dark() ? DARK_PIPELINE : RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, regionWidth, regionHeight, textureWidth, textureHeight);
    }
//?} else if =1.21.1 {
/*    }

    /^* Same UV/stretching contract as GuiGraphics.blit, with neutral-pixel darkening applied only here. ^/
    public static void blit(GuiGraphics g, ResourceLocation texture, int x, int y, int width, int height,
                            float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        if (!dark() || shader == null) {
            g.blit(texture, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight);
            return;
        }
        g.flush();
        ShaderInstance previous = RenderSystem.getShader();
        try {
            RenderSystem.setShader(() -> shader);
            RenderSystem.setShaderTexture(0, texture);
            var pose = g.pose().last().pose();
            var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            float u0 = u / textureWidth, u1 = (u + regionWidth) / textureWidth;
            float v0 = v / textureHeight, v1 = (v + regionHeight) / textureHeight;
            buffer.addVertex(pose, x, y, 0).setUv(u0, v0);
            buffer.addVertex(pose, x, y + height, 0).setUv(u0, v1);
            buffer.addVertex(pose, x + width, y + height, 0).setUv(u1, v1);
            buffer.addVertex(pose, x + width, y, 0).setUv(u1, v0);
            BufferUploader.drawWithShader(buffer.buildOrThrow());
        } finally {
            RenderSystem.setShader(() -> previous);
        }
    }
*/
//?}
}
