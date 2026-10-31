package biggerreactorsrework.client;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.core.client.gui.RenderHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.joml.Vector3f;

/**
 * Vertical gauge bars shared by the screens. Each bar has its own textures under {@code textures/gui/<bar>/}, so it
 * can be edited without touching the screen backgrounds:
 * <ul>
 *     <li>{@code <bar>_empty.png}: the frame and the empty interior, drawn first;</li>
 *     <li>{@code <bar>_fill.png}: what fills the control rod, coolant flow and progress bars;</li>
 *     <li>{@code <bar>_shimmer_sheet.png}: the fill of the fuel, waste, heat and speed bars, animated like the energy
 *     bar, {@value #FRAMES} frames of {@value #HEIGHT} px stacked vertically.</li>
 * </ul>
 * The gauges are {@value #WIDTH}x{@value #HEIGHT} px, with a {@value #FRAME} px frame around the interior. Fluids are
 * drawn with their own animated texture and color at {@value #FLUID_OPACITY} of their opacity, under a transparent
 * reflection ({@code fluid/fluid_shimmer_sheet.png}).
 */
public final class GaugeBar {
    private GaugeBar() {
    }

    public static final int WIDTH = 18;
    public static final int HEIGHT = 64;
    private static final int FRAME = 3;
    private static final int INTERIOR_HEIGHT = HEIGHT - 2 * FRAME;
    private static final float FLUID_OPACITY = 0.8F;

    private static final int FRAMES = 42;
    private static final long FRAME_MS = 100;
    private static final int SHEET_HEIGHT = HEIGHT * FRAMES;

    private static final Identifier FUEL_EMPTY = texture("fuel/fuel_empty");
    private static final Identifier FUEL_SHIMMER = texture("fuel/fuel_shimmer_sheet");
    private static final Identifier WASTE_SHIMMER = texture("fuel/waste_shimmer_sheet");
    private static final Identifier HEAT_EMPTY = texture("heat/heat_empty");
    private static final Identifier HEAT_SHIMMER = texture("heat/heat_shimmer_sheet");
    private static final Identifier SPEED_EMPTY = texture("speed/speed_empty");
    private static final Identifier SPEED_SHIMMER = texture("speed/speed_shimmer_sheet");
    private static final Identifier FLUID_EMPTY = texture("fluid/fluid_empty");
    private static final Identifier FLUID_MARKS = texture("fluid/fluid_marks");
    private static final Identifier FLUID_SHIMMER = texture("fluid/fluid_shimmer_sheet");
    private static final Identifier CONTROL_ROD_FRAME = texture("control_rod/control_rod_frame");
    private static final Identifier CONTROL_ROD_FILL = texture("control_rod/control_rod_fill");

    private static final int COOLANT_FLOW_HEIGHT = 26;
    private static final int COOLANT_TANK_SIZE = 16;
    private static final int COOLANT_TANK_COVER = 0xFFC5C5C5;
    private static final Identifier COOLANT_FLOW_EMPTY = texture("coolant_flow/coolant_flow_empty");
    private static final Identifier COOLANT_FLOW_FILL = texture("coolant_flow/coolant_flow_fill");

    private static final int PROGRESS_WIDTH = 24;
    private static final int PROGRESS_HEIGHT = 18;
    private static final Identifier PROGRESS_EMPTY = texture("progress/progress_empty");
    private static final Identifier PROGRESS_FILL = texture("progress/progress_fill");

    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath(BiggerReactors.modid, "textures/gui/" + name + ".png");
    }

    /**
     * Pixels of a bar {@code size} px long that {@code value} out of {@code capacity} covers, in [0, size].
     */
    static int filledPixels(int size, double value, double capacity) {
        if (!(value > 0) || !(capacity > 0)) {
            return 0;
        }
        return (int) Math.min(size, (size * value) / capacity);
    }

    /**
     * Draws the rows {@code top..bottom} (relative to the bar) of a full-size fill, inside the frame, from the frame
     * starting at row {@code frameV} of a texture {@code textureHeight} px tall.
     */
    private static void fillRows(GuiGraphicsExtractor g, Identifier texture, int x, int y, int top, int bottom, int frameV, int textureHeight) {
        top = Math.max(top, FRAME);
        bottom = Math.min(bottom, HEIGHT - FRAME);
        if (bottom > top) {
            // The overload without a pipeline takes destination corners and normalized UVs in 26.3.
            g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, texture, x + FRAME, y + top,
                    FRAME, frameV + top, WIDTH - 2 * FRAME, bottom - top, WIDTH, textureHeight);
        }
    }

    /**
     * First row of the current frame of the shimmer sheets, in step with the energy bar.
     */
    private static int shimmerFrameV() {
        return (int) ((Util.getMillis() / FRAME_MS) % FRAMES) * HEIGHT;
    }

    private static void shimmerRows(GuiGraphicsExtractor g, Identifier sheet, int x, int y, int top, int bottom) {
        fillRows(g, sheet, x, y, top, bottom, shimmerFrameV(), SHEET_HEIGHT);
    }

    /**
     * Row (relative to the bar) where a fill of {@code value} out of {@code capacity} starts, measured on the interior
     * and stacked on {@code bottom}.
     */
    private static int fillTop(int bottom, double value, double capacity) {
        return bottom - filledPixels(INTERIOR_HEIGHT, value, capacity);
    }

    private static void bar(GuiGraphicsExtractor g, Identifier empty, Identifier shimmer, int x, int y, double value, double capacity) {
        GuiTheme.blit(g, empty, x, y, 0, 0, WIDTH, HEIGHT, WIDTH, HEIGHT);
        shimmerRows(g, shimmer, x, y, fillTop(HEIGHT - FRAME, value, capacity), HEIGHT - FRAME);
    }

    /**
     * The interior rows {@code top..HEIGHT - FRAME} filled with the fluid's own animated texture and tint, its
     * opacity scaled by {@value #FLUID_OPACITY}.
     */
    private static void fluidRows(GuiGraphicsExtractor g, int x, int y, int top, net.neoforged.neoforge.fluids.FluidStack stack) {
        Fluid fluid=stack.getFluid();
        if (top >= HEIGHT - FRAME || fluid == Fluids.EMPTY) {
            return;
        }
        final TextureAtlasSprite sprite = FluidSprites.still(stack);
        final int tint = FluidSprites.tint(stack);
        final int color = net.minecraft.util.ARGB.color(Math.round((tint >>> 24) * FLUID_OPACITY), tint & 0xFFFFFF);
        g.enableScissor(x + FRAME, y + top, x + WIDTH - FRAME, y + HEIGHT - FRAME);
        for (int tileY = FRAME; tileY < HEIGHT - FRAME; tileY += 16) {
            g.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, sprite, x + FRAME, y + tileY, 16, 16, color);
        }
        g.disableScissor();
    }

    public static void drawHeat(GuiGraphicsExtractor g, int x, int y, double heat, double maxHeat) {
        bar(g, HEAT_EMPTY, HEAT_SHIMMER, x, y, heat, maxHeat);
    }

    public static void drawSpeed(GuiGraphicsExtractor g, int x, int y, double rpm, double maxRpm) {
        bar(g, SPEED_EMPTY, SPEED_SHIMMER, x, y, rpm, maxRpm);
    }

    /**
     * Fuel mix: waste at the bottom, fuel stacked on top of it.
     */
    public static void drawFuel(GuiGraphicsExtractor g, int x, int y, double waste, double fuel, double capacity) {
        GuiTheme.blit(g, FUEL_EMPTY, x, y, 0, 0, WIDTH, HEIGHT, WIDTH, HEIGHT);
        final int wasteTop = fillTop(HEIGHT - FRAME, waste, capacity);
        shimmerRows(g, WASTE_SHIMMER, x, y, wasteTop, HEIGHT - FRAME);
        shimmerRows(g, FUEL_SHIMMER, x, y, fillTop(wasteTop, fuel, capacity), wasteTop);
    }

    /**
     * Fluid tank: the fluid's own texture fills the bar, a transparent reflection moves over the filled part, and the
     * frame with the level marks goes on top.
     */
    public static void drawFluid(GuiGraphicsExtractor g,int x,int y,double stored,double capacity,Fluid fluid){drawFluid(g,x,y,stored,capacity,new net.neoforged.neoforge.fluids.FluidStack(fluid,1));}
    public static void drawFluid(GuiGraphicsExtractor g, int x, int y, double stored, double capacity, net.neoforged.neoforge.fluids.FluidStack stack) {
        Fluid fluid=stack.getFluid();
        GuiTheme.blit(g, FLUID_EMPTY, x, y, 0, 0, WIDTH, HEIGHT, WIDTH, HEIGHT);
        final int top = fillTop(HEIGHT - FRAME, stored, capacity);
        if (top < HEIGHT - FRAME && fluid != Fluids.EMPTY) {
            fluidRows(g, x, y, top, stack);
            // The reflection is mostly transparent: a plain blit draws without blending and would make it opaque.
            shimmerRows(g, FLUID_SHIMMER, x, y, top, HEIGHT - FRAME);
        }
        GuiTheme.blit(g, FLUID_MARKS, x, y, 0, 0, WIDTH, HEIGHT, WIDTH, HEIGHT);
    }

    /**
     * Control rod: the fuel fluid behind, the rod coming down from the top as it is inserted (0 to 100).
     */
    public static void drawControlRod(GuiGraphicsExtractor g, int x, int y, double insertion, Fluid fuel) {
        fluidRows(g, x, y, FRAME, new net.neoforged.neoforge.fluids.FluidStack(fuel,1));
        // The rod texture starts above the interior, under the frame.
        final int rows = FRAME + filledPixels(INTERIOR_HEIGHT, insertion, 100);
        if (rows > 0) {
            GuiTheme.blit(g, CONTROL_ROD_FILL, x, y, 0, 0, WIDTH, rows, WIDTH, HEIGHT);
        }
        GuiTheme.blit(g, CONTROL_ROD_FRAME, x, y, 0, 0, WIDTH, HEIGHT, WIDTH, HEIGHT);
    }

    /**
     * Coolant flow of an active reactor: an upward progress indicator ({@code progress} in [0, 1]) above a small tank
     * showing the coolant. Pass {@link Fluids#EMPTY} to leave the tank empty.
     */
    public static void drawCoolantFlow(GuiGraphicsExtractor g, int x, int y, double progress, Fluid coolant) {
        GuiTheme.blit(g, COOLANT_FLOW_EMPTY, x, y, 0, 0, WIDTH, COOLANT_FLOW_HEIGHT, WIDTH, COOLANT_FLOW_HEIGHT);
        final int rows = filledPixels(COOLANT_FLOW_HEIGHT, progress, 1);
        if (rows > 0) {
            final int top = COOLANT_FLOW_HEIGHT - rows;
            GuiTheme.blit(g, COOLANT_FLOW_FILL, x, y + top, 0, top, WIDTH, rows, WIDTH, COOLANT_FLOW_HEIGHT);
        }
        final int tankX = x + 1;
        final int tankY = y + COOLANT_FLOW_HEIGHT;
        if (coolant != Fluids.EMPTY) {
            RenderHelper.drawFluid(g, tankX, tankY, 0, COOLANT_TANK_SIZE, COOLANT_TANK_SIZE, coolant);
        }
        coolantTank(g, tankX, tankY);
    }

    /**
     * The tank over the coolant: a window on rows 1 to 4, dark on top and white at the bottom, and the panel covering
     * the rest of the 16x16 square.
     */
    private static void coolantTank(GuiGraphicsExtractor g, int x, int y) {
        final int size = COOLANT_TANK_SIZE;
        g.fill(x, y, x + 1, y + 1, GuiTheme.panelColor());
        GuiArt.fill(g, x + 1, y, x + size - 1, y + 1, GuiArt.SLOT_SHADOW);
        g.fill(x + size - 1, y, x + size, y + 1, GuiTheme.panelColor());
        GuiArt.fill(g, x, y + 1, x + 1, y + 2, GuiArt.SLOT_SHADOW);
        GuiArt.fill(g, x + size - 1, y + 1, x + size, y + 2, GuiArt.SLOT_SHADOW);
        GuiArt.fill(g, x, y + 4, x + 1, y + 5, GuiArt.WHITE);
        GuiArt.fill(g, x + size - 1, y + 4, x + size, y + 5, GuiArt.WHITE);
        g.fill(x, y + 5, x + 1, y + 6, GuiTheme.panelColor());
        GuiArt.fill(g, x + 1, y + 5, x + size - 1, y + 6, GuiArt.WHITE);
        g.fill(x + size - 1, y + 5, x + size, y + 6, GuiTheme.panelColor());
        g.fill(x, y + 6, x + size, y + size, GuiTheme.panelColor());
    }

    /**
     * Machine progress: an arrow that fills from left to right as {@code progress} reaches {@code total}.
     */
    public static void drawProgressArrow(GuiGraphicsExtractor g, int x, int y, double progress, double total) {
        GuiTheme.blit(g, PROGRESS_EMPTY, x, y, 0, 0, PROGRESS_WIDTH, PROGRESS_HEIGHT, PROGRESS_WIDTH, PROGRESS_HEIGHT);
        final int columns = filledPixels(PROGRESS_WIDTH, progress, total);
        if (columns > 0) {
            GuiTheme.blit(g, PROGRESS_FILL, x, y, 0, 0, columns, PROGRESS_HEIGHT, PROGRESS_WIDTH, PROGRESS_HEIGHT);
        }
    }
}
