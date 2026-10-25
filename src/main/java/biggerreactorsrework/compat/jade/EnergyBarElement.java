package biggerreactorsrework.compat.jade;

import biggerreactorsrework.BiggerReactors;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import snownee.jade.api.ui.Element;

/**
 * A horizontal energy bar stretched over the tooltip width with its text inside: the screens' energy bar laid on its
 * side. Text longer than the bar slides from side to side.
 */
final class EnergyBarElement extends Element {
    private static final ResourceLocation FRAME = ResourceLocation.fromNamespaceAndPath(BiggerReactors.modid, "textures/gui/energy/energy_empty.png");
    private static final ResourceLocation SHEET = ResourceLocation.fromNamespaceAndPath(BiggerReactors.modid, "textures/gui/energy/energy_shimmer_sheet.png");
    private static final int FRAME_LENGTH = 72;
    private static final int BORDER = 3;
    private static final int FILL_LENGTH = 66;
    private static final int FILL_THICKNESS = 8;
    private static final int FRAMES = 42;
    private static final long FRAME_MS = 100;

    private static final int THICKNESS = 14;
    private static final int MIN_LENGTH = 100;
    private static final int MAX_LENGTH = 140;
    private static final int TEXT_PADDING = 4;
    private static final int TEXT = 0xFFFFFFFF;

    private final double fraction;
    private final Component text;

    private EnergyBarElement(double fraction, Component text) {
        this.fraction = Mth.clamp(fraction, 0, 1);
        this.text = text;
    }

    static EnergyBarElement of(double fraction, Component text) {
        return new EnergyBarElement(fraction, text);
    }

    @Override
    public Vec2 getSize() {
        int width = Minecraft.getInstance().font.width(text) + 2 * TEXT_PADDING;
        return new Vec2(Mth.clamp(width, MIN_LENGTH, MAX_LENGTH), THICKNESS + 1);
    }

    @Override
    public void render(GuiGraphics g, float x, float y, float maxX, float maxY) {
        int left = (int) x;
        int top = (int) y;
        int length = Math.max((int) getSize().x, (int) maxX - left);
        drawBar(g, left, top, length);
        drawText(g, left + TEXT_PADDING, top + 3, left + length - TEXT_PADDING, top + THICKNESS - 2);
    }

    private void drawBar(GuiGraphics g, int x, int y, int length) {
        int area = length - 2 * BORDER;
        int filled = (int) Math.round(area * fraction);
        g.pose().pushPose();
        // The vertical bar turned a quarter: its bottom, where the fill starts, ends up on the left.
        g.pose().translate(x + length, y, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(90));
        g.blit(FRAME, 0, 0, 0, 0, THICKNESS, BORDER, THICKNESS, FRAME_LENGTH);
        for (int pos = BORDER; pos < length - BORDER; pos += FILL_LENGTH) {
            int piece = Math.min(FILL_LENGTH, length - BORDER - pos);
            g.blit(FRAME, 0, pos, 0, BORDER, THICKNESS, piece, THICKNESS, FRAME_LENGTH);
        }
        g.blit(FRAME, 0, length - BORDER, 0, FRAME_LENGTH - BORDER, THICKNESS, BORDER, THICKNESS, FRAME_LENGTH);
        int frame = (int) ((Util.getMillis() / FRAME_MS) % FRAMES);
        for (int drawn = 0; drawn < filled; drawn += FILL_LENGTH) {
            int piece = Math.min(FILL_LENGTH, filled - drawn);
            g.blit(SHEET, BORDER, BORDER + area - drawn - piece, 0, frame * FILL_LENGTH + FILL_LENGTH - piece,
                    FILL_THICKNESS, piece, FILL_THICKNESS, FILL_LENGTH * FRAMES);
        }
        g.pose().popPose();
    }

    /** White text centered in the bar; when it does not fit it slides back and forth so all of it can be read. */
    private void drawText(GuiGraphics g, int left, int top, int right, int bottom) {
        Font font = Minecraft.getInstance().font;
        int width = font.width(text);
        int available = right - left;
        if (width <= available) {
            g.drawString(font, text, left + (available - width) / 2, top, TEXT, true);
            return;
        }
        int overflow = width - available;
        double seconds = Util.getMillis() / 1000.0;
        double period = Math.max(overflow * 0.5, 3.0);
        double phase = Math.sin(Math.PI / 2 * Math.cos(2 * Math.PI * seconds / period)) / 2 + 0.5;
        int offset = (int) Math.round(Mth.lerp(phase, 0, overflow));
        scissor(g, left, top - 1, right, bottom + 1);
        g.drawString(font, text, left - offset, top, TEXT, true);
        g.disableScissor();
    }

    /** The scissor works in screen pixels, so the tooltip's pose (position and scale) is applied to the area first. */
    private static void scissor(GuiGraphics g, int x0, int y0, int x1, int y1) {
        Matrix4f pose = g.pose().last().pose();
        Vector3f a = pose.transformPosition(x0, y0, 0, new Vector3f());
        Vector3f b = pose.transformPosition(x1, y1, 0, new Vector3f());
        g.enableScissor(Mth.floor(Math.min(a.x, b.x)), Mth.floor(Math.min(a.y, b.y)),
                Mth.ceil(Math.max(a.x, b.x)), Mth.ceil(Math.max(a.y, b.y)));
    }
}
