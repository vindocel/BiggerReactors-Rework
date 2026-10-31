package biggerreactorsrework.client;

import biggerreactorsrework.core.client.gui.screens.ModScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.resources.Identifier;

/** Scoped dark chrome: controls and gauge textures, never machine casing frames, icons, items or fluids. */
public final class GuiTheme {
    public static final int LIGHT_PANEL = 0xFFC6C6C6;
    public static final int DARK_PANEL = GuiThemePalette.chrome(LIGHT_PANEL, true);
    private static Boolean scopedDark;

    private GuiTheme() {}

    public static boolean dark() {
        if (scopedDark != null) return scopedDark;
        return Minecraft.getInstance().gui.screen() instanceof ModScreen<?> screen && GuiThemes.dark(screen.machinePos());
    }

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

    public static int chromeColor(int argb) { return GuiThemePalette.chrome(argb, dark()); }

    public static final RenderPipeline DARK_PIPELINE = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(biggerreactorsrework.BiggerReactors.modid, "pipeline/dark_gui"))
            .withFragmentShader(Identifier.fromNamespaceAndPath(biggerreactorsrework.BiggerReactors.modid, "core/dark_gui"))
            .build();

    public static void blit(GuiGraphicsExtractor g, Identifier texture, int x, int y, float u, float v,
                            int width, int height, int textureWidth, int textureHeight) {
        blit(g, texture, x, y, width, height, u, v, width, height, textureWidth, textureHeight);
    }

    /** Same UV/stretching contract as GuiGraphicsExtractor.blit, with neutral-pixel darkening applied only here. */
    public static void blit(GuiGraphicsExtractor g, Identifier texture, int x, int y, int width, int height,
                            float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        g.blit(dark() ? DARK_PIPELINE : RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, regionWidth, regionHeight, textureWidth, textureHeight);
    }
}
