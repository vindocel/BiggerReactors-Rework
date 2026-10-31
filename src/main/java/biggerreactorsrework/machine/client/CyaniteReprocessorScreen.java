package biggerreactorsrework.machine.client;

import biggerreactorsrework.client.GuiTheme;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluids;
import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.client.CommonRender;
import biggerreactorsrework.client.GaugeBar;
import biggerreactorsrework.client.GuiArt;
import biggerreactorsrework.client.GuiFrame;
import biggerreactorsrework.client.GuiIcon;
import biggerreactorsrework.items.ingots.CyaniteIngot;
import biggerreactorsrework.machine.blocks.CyaniteReprocessor;
import biggerreactorsrework.machine.containers.CyaniteReprocessorContainer;
import biggerreactorsrework.machine.side.RelativeFace;
import biggerreactorsrework.machine.side.SideConfiguration;
import biggerreactorsrework.machine.side.SideRoute;
import biggerreactorsrework.machine.side.SideRules;
import biggerreactorsrework.machine.side.Transmission;
import biggerreactorsrework.machine.state.CyaniteReprocessorState;
import biggerreactorsrework.core.client.gui.screens.ModScreen;
import biggerreactorsrework.core.client.gui.RenderHelper;
import biggerreactorsrework.core.client.gui.elements.RenderedElement;
import biggerreactorsrework.core.client.gui.elements.TooltipElement;
import biggerreactorsrework.client.deps.RecipeViewers;


import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

// Modified 2026-10-27: input water beside FE, separate product tank and a dedicated title row.
public class CyaniteReprocessorScreen extends ModScreen<CyaniteReprocessorContainer> {


    private CyaniteReprocessorState cyaniteReprocessorState;

    // Side configuration: a tab on the right edge of the screen opens a window over it, with one tab per material.
    private static final Identifier FRONT_SPRITE = Identifier.fromNamespaceAndPath(BiggerReactors.modid, "block/cyanite_reprocessor");
    private static final Identifier CASING_SPRITE = Identifier.fromNamespaceAndPath(BiggerReactors.modid, "block/reactor/casing/disassembled");

    // Tabs are ears of the metal frame; stacked tabs share one ear with their buttons 2 px apart.
    private static final int TAB_SIZE = GuiFrame.TAB_SIZE;
    private static final int TAB_PITCH = GuiFrame.TAB_BUTTON + 2;
    private static final int SIDE_TAB_Y = 4;
    private static final int WINDOW_WIDTH = 120;
    private static final int WINDOW_HEIGHT = 96;
    private static final int WINDOW_Y = 16;
    private static final int SLOT_SIZE = 22;
    private static final int[] COLUMNS = {25, 49, 73};
    private static final int[] ROWS = {20, 44, 68};
    private static final int BUTTON_SIZE = 15;
    // Automation switches and the close button share one size, which centers their 4x3 and 4x5 glyphs exactly.
    private static final int CLOSE_WIDTH = 10;
    private static final int CLOSE_HEIGHT = 11;
    private static final int SWITCH_WIDTH = CLOSE_WIDTH;
    private static final int SWITCH_HEIGHT = CLOSE_HEIGHT;
    private static final int TOP_BAR_Y = 6;
    private static final int TOP_BAR_HEIGHT = 19;

    /**
     * Where the window was dragged to, relative to its default place; kept while the game runs.
     */
    private static int windowOffsetX = 0;
    private static int windowOffsetY = 0;

    /**
     * Faces in the cross, as {face, column, row}: UP on top, LEFT/FRONT/RIGHT in the middle, BACK and DOWN below.
     */
    private static final int[][] CROSS = {
            {RelativeFace.UP.ordinal(), 1, 0},
            {RelativeFace.LEFT.ordinal(), 0, 1},
            {RelativeFace.FRONT.ordinal(), 1, 1},
            {RelativeFace.RIGHT.ordinal(), 2, 1},
            {RelativeFace.BACK.ordinal(), 0, 2},
            {RelativeFace.DOWN.ordinal(), 1, 2},
    };

    private final SideConfiguration sides = new SideConfiguration(SideRules.CYANITE_REPROCESSOR);
    private final ItemStack machineIcon = new ItemStack(CyaniteReprocessor.INSTANCE);
    private final ItemStack cyaniteIcon = new ItemStack(CyaniteIngot.INSTANCE);
    private Transmission tab = Transmission.ITEM;
    private boolean sideWindowOpen = false;
    private boolean draggingWindow = false;
    private double dragX;
    private double dragY;

    @Nullable
    private RenderedElement<CyaniteReprocessorContainer> progressBar;

    public CyaniteReprocessorScreen(CyaniteReprocessorContainer container, Inventory playerInventory, Component title) {
        super(container, playerInventory, title, 176, 190);

        // Initialize reprocessor state.
        cyaniteReprocessorState = (CyaniteReprocessorState) this.getMenu().getGuiPacket();
    }

    /**
     * Initialize the screen.
     */
    @Override
    public void init() {
        super.init();

        // Set title to be drawn in the center.
        this.titleLabelX = (this.getImageWidth() / 2) - (this.getFont().width(this.getTitle()) / 2);

        // Initialize tooltips:
        this.initTooltips();

        // Initialize gauges:
        this.initGauges();

        // Initialize symbols:
        this.initSymbols();
    }

    /**
     * Initialize tooltips.
     */
    public void initTooltips() {
        // (Left) Internal battery:
        this.addScreenElement(new TooltipElement<>(this, 8, 21, 16, 16, Component.translatable("screen.biggerreactorsrework.cyanite_reprocessor.internal_battery.tooltip")));
    }

    /**
     * Initialize gauges.
     */
    public void initGauges() {
        // (Top) Internal battery:
        RenderedElement<CyaniteReprocessorContainer> internalBattery = new RenderedElement<>(this, 7, 40, 18, 64, 0, 0, Component.empty());
        internalBattery.onRender = (@Nonnull GuiGraphicsExtractor graphics, int mX, int mY) -> CommonRender.renderEnergyGauge(graphics,
                internalBattery, cyaniteReprocessorState.energyStored, cyaniteReprocessorState.energyCapacity);
        this.addScreenElement(internalBattery);

        // (Top) Water tank:
        RenderedElement<CyaniteReprocessorContainer> waterTank = new RenderedElement<>(this, 28, 40, 18, 64, 0, 0, Component.empty());
        waterTank.onRender = (@Nonnull GuiGraphicsExtractor graphics, int mX, int mY) -> CommonRender.renderFluidGauge(graphics,
                waterTank, cyaniteReprocessorState.waterStored, cyaniteReprocessorState.waterCapacity,
                net.minecraft.core.registries.BuiltInRegistries.FLUID.getValue(Identifier.parse(cyaniteReprocessorState.inputFluid)));
        this.addScreenElement(waterTank);
        var outputTank = new RenderedElement<CyaniteReprocessorContainer>(this,151,40,18,64,0,0,Component.empty());
        outputTank.onRender=(graphics,mX,mY)->CommonRender.renderFluidGauge(graphics,outputTank,
            cyaniteReprocessorState.outputStored,cyaniteReprocessorState.outputCapacity,
            net.minecraft.core.registries.BuiltInRegistries.FLUID.getValue(Identifier.parse(cyaniteReprocessorState.outputFluid)));
        this.addScreenElement(outputTank);
        var status=new RenderedElement<CyaniteReprocessorContainer>(this,54,84,90,10,0,0,null);
        status.onRender=(g,mx,my)->g.text(this.getFont(),this.getFont().plainSubstrByWidth(
            Component.translatable("screen.biggerreactorsrework.industrial_processor."+cyaniteReprocessorState.condition).getString(),90),status.x,status.y,GuiTheme.textColor(),false);
        this.addScreenElement(status);

        // (Center) Progress bar:
        // With a recipe viewer installed, clicking the arrow shows the reprocessing recipe.
        RenderedElement<CyaniteReprocessorContainer> progressBar = new RenderedElement<>(this, 83, 55, 24, 18, 0, 0,
                RecipeViewers.available() ? Component.translatable("screen.biggerreactorsrework.show_recipes") : null);
        this.progressBar = progressBar;
        progressBar.onRender = (@Nonnull GuiGraphicsExtractor graphics, int mX, int mY) -> GaugeBar.drawProgressArrow(graphics,
                progressBar.x, progressBar.y, cyaniteReprocessorState.workTime, cyaniteReprocessorState.workTimeTotal);
        this.addScreenElement(progressBar);
    }

    /**
     * Initialize symbols.
     */
    public void initSymbols() {
        // (Right) Water tank symbol:
        RenderedElement<CyaniteReprocessorContainer> waterTankSymbol = new RenderedElement<>(this, 29, 21, 16, 16, 0, 0, Component.translatable("screen.biggerreactorsrework.cyanite_reprocessor.water_tank.tooltip"));
        waterTankSymbol.onRender = (@Nonnull GuiGraphicsExtractor graphics, int mX, int mY) -> GuiArt.fluidSymbol(graphics,
                waterTankSymbol.x, waterTankSymbol.y, Fluids.WATER.getSource(), GuiIcon.WATER_DROP_MASK, GuiIcon.WATER_DROP, GuiTheme.panelColor(), true);
        this.addScreenElement(waterTankSymbol);
        var outputSymbol=new RenderedElement<CyaniteReprocessorContainer>(this,152,21,16,16,0,0,
            Component.translatable("screen.biggerreactorsrework.industrial_processor.output"));
        outputSymbol.onRender=(g,mx,my)->GuiArt.fluidSymbol(g,outputSymbol.x,outputSymbol.y,
            cyaniteReprocessorState.outputStored>0?net.minecraft.core.registries.BuiltInRegistries.FLUID.getValue(Identifier.parse(cyaniteReprocessorState.outputFluid)):biggerreactorsrework.fluids.LiquidObsidian.INSTANCE,
            GuiIcon.WATER_DROP_MASK,GuiIcon.WATER_DROP,GuiTheme.panelColor(),true);
        this.addScreenElement(outputSymbol);
    }

    @Override
    public List<net.minecraft.client.renderer.Rect2i> extraAreas() {
        var areas = super.extraAreas();
        areas.add(new net.minecraft.client.renderer.Rect2i(this.sideTabX(), this.sideTabY(), TAB_SIZE, TAB_SIZE));
        if (this.sideWindowOpen) {
            areas.add(new net.minecraft.client.renderer.Rect2i(this.windowX() - TAB_SIZE, Math.min(this.windowTabY(0), this.windowY() - GuiFrame.BAND),
                    WINDOW_WIDTH + TAB_SIZE + GuiFrame.BAND, Math.max(WINDOW_HEIGHT + 2 * GuiFrame.BAND, windowEarHeight())));
        }
        return areas;
    }

    private int sideTabX() {
        return this.getLeftPos() + this.getImageWidth();
    }

    private int sideTabY() {
        return this.getTopPos() + SIDE_TAB_Y;
    }

    /**
     * The window, with its tabs, always stays inside the game window.
     */
    private int defaultWindowX() {
        return this.getLeftPos() + (this.getImageWidth() - WINDOW_WIDTH) / 2;
    }

    private int defaultWindowY() {
        return this.getTopPos() + WINDOW_Y;
    }

    private int windowX() {
        int x = this.defaultWindowX() + windowOffsetX;
        return Mth.clamp(x, TAB_SIZE, Math.max(TAB_SIZE, this.width - WINDOW_WIDTH - GuiFrame.BAND));
    }

    private int windowY() {
        int y = this.defaultWindowY() + windowOffsetY;
        return Mth.clamp(y, GuiFrame.BAND, Math.max(GuiFrame.BAND, this.height - WINDOW_HEIGHT - GuiFrame.BAND));
    }

    private int closeX() {
        return this.windowX() + WINDOW_WIDTH - 6 - CLOSE_WIDTH;
    }

    private int resetX() {
        return this.windowX() + COLUMNS[2] + (SLOT_SIZE - BUTTON_SIZE) / 2;
    }

    private int resetY() {
        return this.windowY() + ROWS[2] + (SLOT_SIZE - BUTTON_SIZE) / 2;
    }

    /**
     * Automation switches sit in the top bar, right-aligned before the close button.
     */
    private int switchX(int index, int count) {
        return this.closeX() - (count - index) * (SWITCH_WIDTH + 2);
    }

    private void syncSides() {
        this.sides.unpack(Transmission.ITEM, this.cyaniteReprocessorState.itemSides);
        this.sides.unpack(Transmission.FLUID, this.cyaniteReprocessorState.fluidSides);
        this.sides.unpack(Transmission.ENERGY, this.cyaniteReprocessorState.energySides);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private boolean insideWindow(double mouseX, double mouseY) {
        return this.sideWindowOpen && (inside(mouseX, mouseY, this.windowX() - GuiFrame.BAND, this.windowY() - GuiFrame.BAND,
                WINDOW_WIDTH + 2 * GuiFrame.BAND, WINDOW_HEIGHT + 2 * GuiFrame.BAND)
                || inside(mouseX, mouseY, this.windowX() - TAB_SIZE, this.windowTabY(0), TAB_SIZE, windowEarHeight()));
    }

    private static int windowEarHeight() {
        return TAB_PITCH * (Transmission.VALUES.length - 1) + TAB_SIZE;
    }

    private int windowTabY(int index) {
        return this.windowY() + 4 + TAB_PITCH * index;
    }

    private boolean onSideTab(double mouseX, double mouseY) {
        return inside(mouseX, mouseY, this.sideTabX(), this.sideTabY(), TAB_SIZE, TAB_SIZE);
    }

    private boolean onWindowTab(double mouseX, double mouseY, Transmission transmission) {
        return inside(mouseX, mouseY, GuiFrame.tabButtonX(this.windowX() - TAB_SIZE, true),
                this.windowTabY(transmission.ordinal()) + GuiFrame.TAB_BUTTON_TOP, GuiFrame.TAB_BUTTON, GuiFrame.TAB_BUTTON);
    }

    /**
     * An automation switch: the request that toggles it, whether it is on, whether it pulls in (or pushes out) and its
     * translation key suffix.
     */
    private record AutoSwitch(String request, boolean active, boolean input, String key) {
    }

    /**
     * Automation switches of the current tab; the energy tab has none.
     */
    private List<AutoSwitch> autoSwitches() {
        List<AutoSwitch> switches = new ArrayList<>();
        switch (this.tab) {
            case ITEM -> {
                switches.add(new AutoSwitch("autoInput", this.cyaniteReprocessorState.autoInput, true, "auto_input"));
                switches.add(new AutoSwitch("autoEject", this.cyaniteReprocessorState.autoEject, false, "auto_eject"));
            }
            case FLUID -> {
                switches.add(new AutoSwitch("autoInputFluid",this.cyaniteReprocessorState.autoInputFluid,true,"auto_input_fluid"));
                switches.add(new AutoSwitch("autoEjectFluid",this.cyaniteReprocessorState.autoEjectFluid,false,"auto_eject_fluid"));
            }
            case ENERGY -> {
            }
        }
        return switches;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        super.extractBackground(graphics, mouseX, mouseY, partialTicks);
        // Tab that opens the side configuration, with the machine itself as its icon.
        this.frame().ear(graphics, this.sideTabX(), this.sideTabY(), TAB_SIZE, false);
        GuiArt.tabButton(graphics, this.sideTabX(), this.sideTabY(), false, this.onSideTab(mouseX, mouseY), this.sideWindowOpen, this.frame());
        GuiArt.tabIcon(graphics, this.sideTabX(), this.sideTabY(), false, 16, 16, () -> graphics.item(this.machineIcon, 0, 0));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        if (this.sideWindowOpen) {
            this.syncSides();
            graphics.nextStratum();
            this.renderSideWindow(graphics, mouseX, mouseY);
        }
        List<Component> tooltip = this.sideTooltip(mouseX, mouseY);
        if (!tooltip.isEmpty()) {
            graphics.setComponentTooltipForNextFrame(this.getFont(), tooltip, mouseX, mouseY);
        }
    }

    private void renderSideWindow(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        final int left = this.windowX();
        final int top = this.windowY();

        GuiArt.panel(graphics, left, top, WINDOW_WIDTH, WINDOW_HEIGHT, this.frame());

        // One tab per material on the left edge, all on one ear.
        int earX = left - TAB_SIZE;
        this.frame().ear(graphics, earX, this.windowTabY(0), windowEarHeight(), true);
        for (Transmission transmission : Transmission.VALUES) {
            boolean selected = transmission == this.tab;
            boolean hovered = this.onWindowTab(mouseX, mouseY, transmission);
            int y = this.windowTabY(transmission.ordinal());
            GuiArt.tabButton(graphics, earX, y, true, hovered, selected, this.frame());
            switch (transmission) {
                case ITEM -> GuiArt.tabIcon(graphics, earX, y, true, 16, 16, () -> graphics.item(this.cyaniteIcon, 0, 0));
                case FLUID -> GuiArt.tabIcon(graphics, earX, y, true, 16, 16, () -> GuiArt.fluidSymbol(graphics, 0, 0,
                        Fluids.WATER.getSource(), GuiIcon.WATER_DROP_MASK, GuiIcon.WATER_DROP, GuiArt.buttonFill(selected, false, this.frame()), false));
                case ENERGY -> GuiArt.tabIcon(graphics, earX, y, true, GuiIcon.BATTERY.width(), GuiIcon.BATTERY.height(),
                        () -> GuiIcon.BATTERY.draw(graphics, 0, 0));
            }
        }

        graphics.text(this.getFont(), Component.translatable(this.tab.translationKey()), left + 7, top + 8, GuiTheme.textColor(), false);

        // Top bar: automation switches and the close button.
        List<AutoSwitch> switches = this.autoSwitches();
        for (int i = 0; i < switches.size(); i++) {
            AutoSwitch toggle = switches.get(i);
            int x = this.switchX(i, switches.size());
            int y = top + TOP_BAR_Y;
            // A switch that is on keeps the teal fill.
            GuiArt.button(graphics, x, y, SWITCH_WIDTH, SWITCH_HEIGHT, inside(mouseX, mouseY, x, y, SWITCH_WIDTH, SWITCH_HEIGHT),
                    toggle.active(), false, toggle.input() ? GuiIcon.AUTO_INPUT : GuiIcon.AUTO_EJECT, this.frame());
        }
        GuiArt.button(graphics, this.closeX(), top + TOP_BAR_Y, CLOSE_WIDTH, CLOSE_HEIGHT,
                inside(mouseX, mouseY, this.closeX(), top + TOP_BAR_Y, CLOSE_WIDTH, CLOSE_HEIGHT), GuiIcon.CLOSE, this.frame());

        // Faces: the block's own textures, with the route color around them.
        final TextureAtlasSprite front = biggerreactorsrework.client.FluidSprites.blockSprite(FRONT_SPRITE);
        final TextureAtlasSprite casing = biggerreactorsrework.client.FluidSprites.blockSprite(CASING_SPRITE);
        for (int[] cell : CROSS) {
            RelativeFace face = RelativeFace.VALUES[cell[0]];
            int x = left + COLUMNS[cell[1]];
            int y = top + ROWS[cell[2]];
            GuiArt.slot(graphics, x, y, SLOT_SIZE);
            SideRoute route = this.sides.get(face, this.tab);
            if (route != SideRoute.NONE) {
                graphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, route.color());
            }
            graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, face == RelativeFace.FRONT ? front : casing, x + 3, y + 3, 16, 16);
            if (face != RelativeFace.FRONT && inside(mouseX, mouseY, x, y, SLOT_SIZE, SLOT_SIZE)) {
                graphics.fill(x + 3, y + 3, x + 19, y + 19, 0x50FFFFFF);
            }
        }

        // Every face at once.
        boolean resetHovered = inside(mouseX, mouseY, this.resetX(), this.resetY(), BUTTON_SIZE, BUTTON_SIZE);
        GuiArt.button(graphics, this.resetX(), this.resetY(), BUTTON_SIZE, BUTTON_SIZE, resetHovered, GuiIcon.REVERT, this.frame());
    }

    @Override
    public boolean isSettingsTabCovered(double mouseX, double mouseY) {
        return this.insideWindow(mouseX, mouseY);
    }

    private List<Component> sideTooltip(int mouseX, int mouseY) {
        List<Component> lines = new ArrayList<>();
        if (this.onSideTab(mouseX, mouseY)) {
            lines.add(Component.translatable("screen.biggerreactorsrework.side_config"));
            return lines;
        }
        if (!this.sideWindowOpen) {
            return lines;
        }
        final int left = this.windowX();
        final int top = this.windowY();
        for (Transmission transmission : Transmission.VALUES) {
            if (this.onWindowTab(mouseX, mouseY, transmission)) {
                lines.add(Component.translatable(transmission.translationKey()));
                return lines;
            }
        }
        for (int[] cell : CROSS) {
            if (inside(mouseX, mouseY, left + COLUMNS[cell[1]], top + ROWS[cell[2]], SLOT_SIZE, SLOT_SIZE)) {
                RelativeFace face = RelativeFace.VALUES[cell[0]];
                lines.add(Component.translatable(face.translationKey()));
                if (face == RelativeFace.FRONT) {
                    lines.add(Component.translatable("screen.biggerreactorsrework.side_config.locked").withStyle(ChatFormatting.GRAY));
                } else {
                    SideRoute route = this.sides.get(face, this.tab);
                    lines.add(Component.translatable(route.translationKey()).withColor(route.color() & 0xFFFFFF));
                    lines.add(Component.translatable("screen.biggerreactorsrework.side_config.cycle.hint").withStyle(ChatFormatting.DARK_GRAY));
                }
                return lines;
            }
        }
        if (inside(mouseX, mouseY, this.resetX(), this.resetY(), BUTTON_SIZE, BUTTON_SIZE)) {
            SideRoute target = this.sides.batchTarget(this.tab, true);
            lines.add(Component.translatable("screen.biggerreactorsrework.side_config.batch"));
            lines.add(Component.translatable(target.translationKey()).withColor(target.color() & 0xFFFFFF));
            lines.add(Component.translatable("screen.biggerreactorsrework.side_config.batch.hint").withStyle(ChatFormatting.DARK_GRAY));
            return lines;
        }
        if (inside(mouseX, mouseY, this.closeX(), top + TOP_BAR_Y, CLOSE_WIDTH, CLOSE_HEIGHT)) {
            lines.add(Component.translatable("screen.biggerreactorsrework.side_config.close"));
            return lines;
        }
        List<AutoSwitch> switches = this.autoSwitches();
        for (int i = 0; i < switches.size(); i++) {
            if (inside(mouseX, mouseY, this.switchX(i, switches.size()), top + TOP_BAR_Y, SWITCH_WIDTH, SWITCH_HEIGHT)) {
                lines.add(Component.translatable("screen.biggerreactorsrework.side_config." + switches.get(i).key()));
                lines.add(Component.translatable(switches.get(i).active()
                                ? "screen.biggerreactorsrework.side_config.on" : "screen.biggerreactorsrework.side_config.off")
                        .withStyle(ChatFormatting.GRAY));
                return lines;
            }
        }
        return lines;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT || button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT) {
            if (this.sideClicked(mouseX, mouseY, button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT)) {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
        }
        // The window covers the slots under it.
        if (this.insideWindow(mouseX, mouseY)) {
            if (button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && this.sideWindowOpen) {
                this.startDrag(mouseX, mouseY);
            }
            return true;
        }
        if (button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && this.progressBar != null && this.progressBar.isMouseOver(mouseX, mouseY) && RecipeViewers.available()) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            RecipeViewers.show(RecipeViewers.CYANITE_REPROCESSING);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Handles a click on the side configuration tab or window; left goes forward, right goes back.
     */
    private boolean sideClicked(double mouseX, double mouseY, boolean forward) {
        if (this.onSideTab(mouseX, mouseY)) {
            this.sideWindowOpen = !this.sideWindowOpen;
            return true;
        }
        if (!this.sideWindowOpen) {
            return false;
        }
        final int left = this.windowX();
        final int top = this.windowY();
        for (Transmission transmission : Transmission.VALUES) {
            if (this.onWindowTab(mouseX, mouseY, transmission)) {
                this.tab = transmission;
                return true;
            }
        }
        if (inside(mouseX, mouseY, this.closeX(), top + TOP_BAR_Y, CLOSE_WIDTH, CLOSE_HEIGHT)) {
            this.sideWindowOpen = false;
            return true;
        }
        for (int[] cell : CROSS) {
            if (cell[0] != RelativeFace.FRONT.ordinal()
                    && inside(mouseX, mouseY, left + COLUMNS[cell[1]], top + ROWS[cell[2]], SLOT_SIZE, SLOT_SIZE)) {
                this.getMenu().executeRequest("sideCycle", cell[0] * 8 + this.tab.ordinal() * 2 + (forward ? 0 : 1));
                return true;
            }
        }
        if (inside(mouseX, mouseY, this.resetX(), this.resetY(), BUTTON_SIZE, BUTTON_SIZE)) {
            if (net.minecraft.client.Minecraft.getInstance().hasShiftDown()) {
                this.getMenu().executeRequest("sideClearAll", 0);
            } else {
                this.getMenu().executeRequest("sideBatch", this.tab.ordinal() * 2 + (forward ? 0 : 1));
            }
            return true;
        }
        List<AutoSwitch> switches = this.autoSwitches();
        for (int i = 0; i < switches.size(); i++) {
            if (inside(mouseX, mouseY, this.switchX(i, switches.size()), top + TOP_BAR_Y, SWITCH_WIDTH, SWITCH_HEIGHT)) {
                this.getMenu().executeRequest(switches.get(i).request(), 0);
                return true;
            }
        }
        return false;
    }

    /**
     * The window is dragged by its top bar (outside its buttons).
     */
    private boolean startDrag(double mouseX, double mouseY) {
        if (!inside(mouseX, mouseY, this.windowX(), this.windowY(), WINDOW_WIDTH, TOP_BAR_HEIGHT)) {
            return false;
        }
        this.draggingWindow = true;
        this.dragX = mouseX - this.windowX();
        this.dragY = mouseY - this.windowY();
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.draggingWindow) {
            windowOffsetX = (int) Math.round(mouseX - this.dragX) - this.defaultWindowX();
            windowOffsetY = (int) Math.round(mouseY - this.dragY) - this.defaultWindowY();
            // Store where the window really is, so it does not jump back after being pushed against a border.
            windowOffsetX = this.windowX() - this.defaultWindowX();
            windowOffsetY = this.windowY() - this.defaultWindowY();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.draggingWindow && button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) {
            this.draggingWindow = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Escape closes the side window before the screen.
        if (this.sideWindowOpen && keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) {
            this.sideWindowOpen = false;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
        // Slots under the window are neither highlighted nor show their tooltip.
        return !this.insideWindow(mouseX, mouseY) && super.isHovering(x, y, width, height, mouseX, mouseY);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop) {
        return super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop)
                && !this.onSideTab(mouseX, mouseY)
                && !this.insideWindow(mouseX, mouseY);
    }

    /**
     * Draw the panel and its fixed details.
     */
    @Override
    protected void drawBackground(@Nonnull GuiGraphicsExtractor graphics, int x, int y) {
        GuiArt.panel(graphics, x, y, 176, 190, this.frame());
        GuiIcon.BATTERY.draw(graphics, x + 11, y + 21);
        GuiArt.ring(graphics, x + 53, y + 53, 22, 0xFF3DAAFF);
        GuiArt.slot(graphics, x + 55, y + 55, 18);
        GuiArt.ring(graphics, x + 113, y + 53, 22, 0xFF2727FF);
        GuiArt.slot(graphics, x + 115, y + 55, 18);
        GuiArt.playerInventory(graphics, x + 7, y + 107);
    }

    @Override
    protected GuiFrame frame() {
        return GuiFrame.CYANITE_REPROCESSOR;
    }
    @Override public boolean hasAppearanceSettings() { return true; }
}
