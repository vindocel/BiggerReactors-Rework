package biggerreactorsrework.client;

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

/** Scoped dark chrome: controls and gauge textures, never machine casing frames, icons, items or fluids. */
public final class GuiTheme {
    public static final int LIGHT_PANEL = 0xFFC6C6C6;
    public static final int DARK_PANEL = GuiThemePalette.chrome(LIGHT_PANEL, true);
    private static ShaderInstance shader;

    private GuiTheme() {}

    public static boolean dark() {
        return Minecraft.getInstance().screen instanceof ModScreen<?> screen && GuiThemes.dark(screen.machinePos());
    }

    public static int panelColor() { return dark() ? DARK_PANEL : LIGHT_PANEL; }
    public static int textColor() { return dark() ? 0xFFD0D0D0 : 0xFF404040; }

    public static int chromeColor(int argb) { return GuiThemePalette.chrome(argb, dark()); }

    public static void setShader(ShaderInstance instance) { shader = instance; }

    public static void blit(GuiGraphics g, ResourceLocation texture, int x, int y, float u, float v,
                            int width, int height, int textureWidth, int textureHeight) {
        blit(g, texture, x, y, width, height, u, v, width, height, textureWidth, textureHeight);
    }

    /** Same UV/stretching contract as GuiGraphics.blit, with neutral-pixel darkening applied only here. */
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
}
