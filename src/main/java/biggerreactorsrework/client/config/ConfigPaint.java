//? if =26.1.2 {
package biggerreactorsrework.client.config;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import biggerreactorsrework.BiggerReactors;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.ColoredRectangleRenderState;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class ConfigPaint {

    public static final RenderPipeline FILL = RenderPipeline.builder(
            RenderPipelines.MATRICES_PROJECTION_SNIPPET
            )
            .withLocation(net.minecraft.resources.Identifier.fromNamespaceAndPath(BiggerReactors.modid, "pipeline/config_fill"))
            .withVertexShader(net.minecraft.resources.Identifier.fromNamespaceAndPath(BiggerReactors.modid, "core/config_fill"))
            .withFragmentShader(net.minecraft.resources.Identifier.fromNamespaceAndPath(BiggerReactors.modid, "core/config_fill"))
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
            .build();

    public static final RenderPipeline TEXT = RenderPipeline.builder(
            RenderPipelines.MATRICES_PROJECTION_SNIPPET
            )
            .withLocation(net.minecraft.resources.Identifier.fromNamespaceAndPath(BiggerReactors.modid, "pipeline/config_text"))
            .withVertexShader(net.minecraft.resources.Identifier.fromNamespaceAndPath(BiggerReactors.modid, "core/config_text"))
            .withFragmentShader(net.minecraft.resources.Identifier.fromNamespaceAndPath(BiggerReactors.modid, "core/config_text"))
            .withSampler("Sampler0")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS)
            .build();

    private static boolean sLookedUp;
    @Nullable
    private static Field sRenderState;
    @Nullable
    private static Field sScissorStack;
    @Nullable
    private static Method sScissorPeek;

    private ConfigPaint() {
    }

    public static void registerPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(FILL);
        event.registerPipeline(TEXT);
    }

    private static void lookUp() {
        if (sLookedUp) {
            return;
        }
        sLookedUp = true;
        try {
            Field state = GuiGraphicsExtractor.class.getDeclaredField("guiRenderState");
            state.setAccessible(true);
            Field scissors = GuiGraphicsExtractor.class.getDeclaredField("scissorStack");
            scissors.setAccessible(true);
            Method peek = scissors.getType().getDeclaredMethod("peek");
            peek.setAccessible(true);
            sRenderState = state;
            sScissorStack = scissors;
            sScissorPeek = peek;
        } catch (ReflectiveOperationException | RuntimeException unavailable) {
            BiggerReactors.LOGGER.warn("Configuration screens draw through the game's own methods: {}", unavailable.toString());
        }
    }

    @Nullable
    private static Target target(GuiGraphicsExtractor g) {
        lookUp();
        if (sRenderState == null || sScissorStack == null || sScissorPeek == null) {
            return null;
        }
        try {
            return new Target((GuiRenderState) sRenderState.get(g), (ScreenRectangle) sScissorPeek.invoke(sScissorStack.get(g)));
        } catch (ReflectiveOperationException | RuntimeException unavailable) {
            sRenderState = null;
            return null;
        }
    }

    private record Target(GuiRenderState state, @Nullable ScreenRectangle scissor) {
    }

    public static void fill(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int color) {
        Target target = target(g);
        if (target == null) {
            g.fill(x0, y0, x1, y1, color);
            return;
        }
        // Render-state bounds require positive extents for overlap ordering and clipping.
        int left = Math.min(x0, x1), right = Math.max(x0, x1);
        int top = Math.min(y0, y1), bottom = Math.max(y0, y1);
        target.state().addGuiElement(new ColoredRectangleRenderState(FILL, TextureSetup.noTexture(), new Matrix3x2f(g.pose()),
                left, top, right, bottom, color, color, target.scissor()));
    }

    public static void text(GuiGraphicsExtractor g, Font font, @Nullable String text, int x, int y, int color, boolean shadow) {
        if (text != null) {
            text(g, font, Component.literal(text).getVisualOrderText(), x, y, color, shadow);
        }
    }

    public static void text(GuiGraphicsExtractor g, Font font, Component text, int x, int y, int color, boolean shadow) {
        text(g, font, text.getVisualOrderText(), x, y, color, shadow);
    }

    public static void text(GuiGraphicsExtractor g, Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
        if ((color >>> 24) == 0) {
            return;
        }
        Target target = target(g);
        if (target == null) {
            g.text(font, text, x, y, color, shadow);
            return;
        }
        Matrix3x2f pose = new Matrix3x2f(g.pose());
        ScreenRectangle scissor = target.scissor();
        Font.PreparedText prepared = font.prepareText(text, x, y, color, shadow, false, 0);
        // the render state drops elements without bounds: every glyph carries the bounds of its whole line
        ScreenRectangle bounds = prepared.bounds();
        if (bounds == null) {
            return;
        }
        bounds = bounds.transformMaxBounds(pose);
        if (scissor != null) {
            bounds = scissor.intersection(bounds);
            if (bounds == null) {
                return;
            }
        }
        ScreenRectangle area = bounds;
        prepared.visit(new Font.GlyphVisitor() {
            @Override
            public void acceptGlyph(TextRenderable.Styled glyph) {
                target.state().addGuiElement(new Glyph(pose, glyph, scissor, area));
            }

            @Override
            public void acceptEffect(TextRenderable effect) {
                target.state().addGuiElement(new Glyph(pose, effect, scissor, area));
            }
        });
    }

    public static void centeredText(GuiGraphicsExtractor g, Font font, String text, int centerX, int y, int color) {
        text(g, font, text, centerX - font.width(text) / 2, y, color, true);
    }

    private record Glyph(Matrix3x2fc pose, TextRenderable renderable, @Nullable ScreenRectangle scissorArea,
                         ScreenRectangle bounds) implements GuiElementRenderState {
        @Override
        public void buildVertices(VertexConsumer consumer) {
            renderable.render(new Matrix4f().mul(pose), consumer, 15728880, true);
        }

        @Override
        public RenderPipeline pipeline() {
            RenderPipeline own = renderable.guiPipeline();
            if (own == RenderPipelines.GUI_TEXT) {
                return TEXT;
            }
            return own == RenderPipelines.GUI ? FILL : own;
        }

        @Override
        public TextureSetup textureSetup() {
            RenderPipeline own = renderable.guiPipeline();
            if (own == RenderPipelines.GUI) {
                return TextureSetup.noTexture();
            }
            var sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
            return own == RenderPipelines.GUI_TEXT ? TextureSetup.singleTexture(renderable.textureView(), sampler)
                    : TextureSetup.singleTextureWithLightmap(renderable.textureView(), sampler);
        }
    }

    public static void scrollingText(GuiGraphicsExtractor g, Font font, String text, int x, int y, int width, int color) {
        if (width <= 0) return;
        int overflow = Math.max(0, font.width(text) - width);
        double period = Math.max(overflow * 0.5, 3.0);
        double phase = Math.sin(Math.PI / 2 * Math.cos(2 * Math.PI * net.minecraft.util.Util.getMillis() / 1000.0 / period)) / 2 + 0.5;
        int offset = (int) Math.round(phase * overflow);
        g.enableScissor(x, y - 1, x + width, y + font.lineHeight + 1);
        text(g, font, text, x - offset, y, color, false);
        g.disableScissor();
//?} else if =1.21.1 {
/*package biggerreactorsrework.client.config;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
// Modified 2026-11-18: native immediate-mode configuration painting.
public final class ConfigPaint {
    private ConfigPaint() {}
    public static void fill(GuiGraphics g,int x,int y,int right,int bottom,int color) { g.fill(x,y,right,bottom,color); }
    public static void text(GuiGraphics g,Font font,String text,int x,int y,int color,boolean shadow) { g.drawString(font,text,x,y,color,shadow); }
    public static void text(GuiGraphics g,Font font,Component text,int x,int y,int color,boolean shadow) { g.drawString(font,text,x,y,color,shadow); }
    public static void text(GuiGraphics g,Font font,FormattedCharSequence text,int x,int y,int color,boolean shadow) { g.drawString(font,text,x,y,color,shadow); }
    public static void scrollingText(GuiGraphics g,Font font,String text,int x,int y,int width,int color) {
        if (width<=0) return;
        int overflow=Math.max(0,font.width(text)-width);
        double period=Math.max(overflow*0.5,3.0);
        double phase=Math.sin(Math.PI/2*Math.cos(2*Math.PI*net.minecraft.Util.getMillis()/1000.0/period))/2+0.5;
        g.enableScissor(x,y-1,x+width,y+font.lineHeight+1);
        text(g,font,text,x-(int)Math.round(phase*overflow),y,color,false);
        g.disableScissor();
*/
//?} else if =26.3 {
/*package biggerreactorsrework.client.config;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import biggerreactorsrework.BiggerReactors;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.ColoredRectangleRenderState;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class ConfigPaint {

    public static final RenderPipeline FILL = RenderPipeline.builder(
            RenderPipelines.GUI_SNIPPET
            )
            .withLocation(net.minecraft.resources.Identifier.fromNamespaceAndPath(BiggerReactors.modid, "pipeline/config_fill"))
            .withVertexShader(net.minecraft.resources.Identifier.fromNamespaceAndPath(BiggerReactors.modid, "core/config_fill"))
            .withFragmentShader(net.minecraft.resources.Identifier.fromNamespaceAndPath(BiggerReactors.modid, "core/config_fill"))
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR).withPrimitiveTopology(PrimitiveTopology.QUADS)
            .build();

    public static final RenderPipeline TEXT = RenderPipeline.builder(
            RenderPipelines.GUI_TEXTURED_SNIPPET
            )
            .withLocation(net.minecraft.resources.Identifier.fromNamespaceAndPath(BiggerReactors.modid, "pipeline/config_text"))
            .withVertexShader(net.minecraft.resources.Identifier.fromNamespaceAndPath(BiggerReactors.modid, "core/config_text"))
            .withFragmentShader(net.minecraft.resources.Identifier.fromNamespaceAndPath(BiggerReactors.modid, "core/config_text"))

            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP).withPrimitiveTopology(PrimitiveTopology.QUADS)
            .build();

    private static boolean sLookedUp;
    @Nullable
    private static Field sRenderState;
    @Nullable
    private static Field sScissorStack;
    @Nullable
    private static Method sScissorPeek;

    private ConfigPaint() {
    }

    public static void registerPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(FILL);
        event.registerPipeline(TEXT);
    }

    private static void lookUp() {
        if (sLookedUp) {
            return;
        }
        sLookedUp = true;
        try {
            Field state = GuiGraphicsExtractor.class.getDeclaredField("guiRenderState");
            state.setAccessible(true);
            Field scissors = GuiGraphicsExtractor.class.getDeclaredField("scissorStack");
            scissors.setAccessible(true);
            Method peek = scissors.getType().getDeclaredMethod("peek");
            peek.setAccessible(true);
            sRenderState = state;
            sScissorStack = scissors;
            sScissorPeek = peek;
        } catch (ReflectiveOperationException | RuntimeException unavailable) {
            BiggerReactors.LOGGER.warn("Configuration screens draw through the game's own methods: {}", unavailable.toString());
        }
    }

    @Nullable
    private static Target target(GuiGraphicsExtractor g) {
        lookUp();
        if (sRenderState == null || sScissorStack == null || sScissorPeek == null) {
            return null;
        }
        try {
            return new Target((GuiRenderState) sRenderState.get(g), (ScreenRectangle) sScissorPeek.invoke(sScissorStack.get(g)));
        } catch (ReflectiveOperationException | RuntimeException unavailable) {
            sRenderState = null;
            return null;
        }
    }

    private record Target(GuiRenderState state, @Nullable ScreenRectangle scissor) {
    }

    public static void fill(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int color) {
        Target target = target(g);
        if (target == null) {
            g.fill(x0, y0, x1, y1, color);
            return;
        }
        // Render-state bounds require positive extents for overlap ordering and clipping.
        int left = Math.min(x0, x1), right = Math.max(x0, x1);
        int top = Math.min(y0, y1), bottom = Math.max(y0, y1);
        target.state().addGuiElement(new ColoredRectangleRenderState(FILL, TextureSetup.noTexture(), new Matrix3x2f(g.pose()),
                left, top, right, bottom, color, color, target.scissor()));
    }

    public static void text(GuiGraphicsExtractor g, Font font, @Nullable String text, int x, int y, int color, boolean shadow) {
        if (text != null) {
            text(g, font, Component.literal(text).getVisualOrderText(), x, y, color, shadow);
        }
    }

    public static void text(GuiGraphicsExtractor g, Font font, Component text, int x, int y, int color, boolean shadow) {
        text(g, font, text.getVisualOrderText(), x, y, color, shadow);
    }

    public static void text(GuiGraphicsExtractor g, Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
        if ((color >>> 24) == 0) {
            return;
        }
        Target target = target(g);
        if (target == null) {
            g.text(font, text, x, y, color, shadow);
            return;
        }
        Matrix3x2f pose = new Matrix3x2f(g.pose());
        ScreenRectangle scissor = target.scissor();
        Font.PreparedText prepared = font.prepareText(text, x, y, color, shadow, false, 0);
        // the render state drops elements without bounds: every glyph carries the bounds of its whole line
        ScreenRectangle bounds = prepared.bounds();
        if (bounds == null) {
            return;
        }
        bounds = bounds.transformMaxBounds(pose);
        if (scissor != null) {
            bounds = scissor.intersection(bounds);
            if (bounds == null) {
                return;
            }
        }
        ScreenRectangle area = bounds;
        prepared.visit(new Font.GlyphVisitor() {
            @Override
            public void acceptGlyph(TextRenderable.Styled glyph) {
                target.state().addGuiElement(new Glyph(pose, glyph, scissor, area));
            }

            @Override
            public void acceptEffect(TextRenderable effect) {
                target.state().addGuiElement(new Glyph(pose, effect, scissor, area));
            }
        });
    }

    public static void centeredText(GuiGraphicsExtractor g, Font font, String text, int centerX, int y, int color) {
        text(g, font, text, centerX - font.width(text) / 2, y, color, true);
    }

    private record Glyph(Matrix3x2fc pose, TextRenderable renderable, @Nullable ScreenRectangle scissorArea,
                         ScreenRectangle bounds) implements GuiElementRenderState {
        @Override
        public void buildVertices(VertexConsumer consumer) {
            renderable.render(new Matrix4f().mul(pose), consumer, 15728880, true);
        }

        @Override
        public RenderPipeline pipeline() {
            RenderPipeline own = renderable.guiPipeline();
            if (own == RenderPipelines.GUI_TEXT) {
                return TEXT;
            }
            return own == RenderPipelines.GUI ? FILL : own;
        }

        @Override
        public TextureSetup textureSetup() {
            RenderPipeline own = renderable.guiPipeline();
            if (own == RenderPipelines.GUI) {
                return TextureSetup.noTexture();
            }
            var sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
            return own == RenderPipelines.GUI_TEXT ? TextureSetup.singleTexture(renderable.textureView(), sampler)
                    : TextureSetup.singleTextureWithLightmap(renderable.textureView(), sampler);
        }
    }

    public static void scrollingText(GuiGraphicsExtractor g, Font font, String text, int x, int y, int width, int color) {
        if (width <= 0) return;
        int overflow = Math.max(0, font.width(text) - width);
        double period = Math.max(overflow * 0.5, 3.0);
        double phase = Math.sin(Math.PI / 2 * Math.cos(2 * Math.PI * net.minecraft.util.Util.getMillis() / 1000.0 / period)) / 2 + 0.5;
        int offset = (int) Math.round(phase * overflow);
        g.enableScissor(x, y - 1, x + width, y + font.lineHeight + 1);
        text(g, font, text, x - offset, y, color, false);
        g.disableScissor();
*/
//?}
    }
}
