package biggerreactorsrework.multiblocks.reactor.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.client.*;
import biggerreactorsrework.multiblocks.reactor.containers.ReactorRedstonePortContainer;
import biggerreactorsrework.multiblocks.reactor.state.ReactorRedstonePortSelection;
import biggerreactorsrework.multiblocks.reactor.state.ReactorRedstonePortState;
import biggerreactorsrework.multiblocks.reactor.state.ReactorRedstonePortTriggers;
import biggerreactorsrework.core.client.gui.elements.InteractiveElement;
import biggerreactorsrework.core.client.gui.elements.RenderedElement;
import biggerreactorsrework.core.client.gui.screens.ModScreen;

import javax.annotation.Nonnull;

@OnlyIn(Dist.CLIENT)
public class ReactorRedstonePortScreen extends ModScreen<ReactorRedstonePortContainer> {


    private static final String[] INPUT_TRANSLATIONS = new String[]{
            "screen.biggerreactorsrework.reactor_redstone_port.input_reactor_activity",
            "screen.biggerreactorsrework.reactor_redstone_port.input_control_rod_insertion",
            "screen.biggerreactorsrework.reactor_redstone_port.input_eject_waste"
    };

    private static final String[] OUTPUT_TRANSLATIONS = new String[]{
            "screen.biggerreactorsrework.reactor_redstone_port.output_fuel_temp",
            "screen.biggerreactorsrework.reactor_redstone_port.output_casing_temp",
            "screen.biggerreactorsrework.reactor_redstone_port.output_fuel_enrichment",
            "screen.biggerreactorsrework.reactor_redstone_port.output_fuel_amount",
            "screen.biggerreactorsrework.reactor_redstone_port.output_waste_amount",
            "screen.biggerreactorsrework.reactor_redstone_port.output_output_stored"
    };

    // Tab icons, in the same order as the translations, and where they sit inside their tab.
    private static final GuiIcon[] INPUT_ICONS = new GuiIcon[]{GuiIcon.REACTOR_ACTIVITY, GuiIcon.CONTROL_ROD_INSERTION, GuiIcon.EJECT_WASTE};
    private static final int[] INPUT_ICON_X = new int[]{4, 5, 4};
    private static final int[] INPUT_ICON_Y = new int[]{7, 4, 4};
    private static final GuiIcon[] OUTPUT_ICONS = new GuiIcon[]{GuiIcon.FUEL_HEAT, GuiIcon.CASE_HEAT, GuiIcon.FUEL_ENRICHMENT,
            GuiIcon.FUEL_AMOUNT, GuiIcon.WASTE_AMOUNT, GuiIcon.BATTERY};
    private static final int[] OUTPUT_ICON_X = new int[]{5, 5, 5, 5, 5, 8};

    private ReactorRedstonePortState reactorRedstonePortState;

    // Controls:
    InteractiveElement<ReactorRedstonePortContainer> applyChangesButton;
    InteractiveElement<ReactorRedstonePortContainer> revertChangesButton;
    Biselector<ReactorRedstonePortContainer> triggerTypeToggle;
    Triselector<ReactorRedstonePortContainer> triggerModeToggle;
    TextBox<ReactorRedstonePortContainer> textBufferA;
    CommonButton<ReactorRedstonePortContainer> textEnterButtonA;
    TextBox<ReactorRedstonePortContainer> textBufferB;
    CommonButton<ReactorRedstonePortContainer> textEnterButtonB;

    // Symbols:
    RenderedElement<ReactorRedstonePortContainer> selectedTabSymbol;

    public ReactorRedstonePortScreen(ReactorRedstonePortContainer container, Inventory playerInventory, Component title) {
        super(container, playerInventory, title, 200, 178);
        // Initialize access port state.
        reactorRedstonePortState = (ReactorRedstonePortState) this.getMenu().getGuiPacket();
    }

    /**
     * Initialize the screen.
     */
    @Override
    public void init() {
        super.init();

        // Set title to be drawn in the center.
        this.titleLabelX = (this.imageWidth - this.font.width(this.getTitle())) / 2;

        // Initialize tooltips:

        // Initialize controls:
        this.initControls();

        // Initialize gauges:

        // Initialize symbols:
        this.initSymbols();
    }

    /**
     * Initialize controls.
     */
    public void initControls() {
        // Requst names for redstone port:
        // setSelectedTab (int)
        // setTriggerPS (bool)
        // setTriggerAB (bool)
        // setTriggerMode (int)
        // setTextBufferA (String)
        // setTextBufferB (String)
        // revertChanges (void)
        // applyChanges (void)

        // (Left) Add input tab buttons:
        for (int i = 0; i < 3; i++) {
            final int cI = i;
            final InteractiveElement<ReactorRedstonePortContainer> inputTab = new InteractiveElement<>(this, 0, (cI * 25), 25, 24, 0, 0, Component.translatable(INPUT_TRANSLATIONS[cI] + ".tooltip"));
            inputTab.onMouseReleased = (mX, mY, btn) -> {
                // Click logic. Extra check necessary since this is an "in-class" button.
                if (inputTab.isMouseOver(mX, mY)) {
                    // Mouse is hovering, do the thing.
                    this.getMenu().executeRequest("setSelectedTab", cI);
                    this.reactorRedstonePortState.selectedTab = ReactorRedstonePortSelection.fromInt(cI);
                    // Play the selection sound.
                    inputTab.playSound(SoundEvents.UI_BUTTON_CLICK);
                    return true;
                } else {
                    // It ain't hovered, don't do the thing.
                    return false;
                }
            };
            inputTab.onRender = ((mS, mX, mY) -> {
                // Custom rendering.
                GuiArt.panelTab(mS, inputTab.x, inputTab.y, false, inputTab.stateEnable, cI == 0);
                INPUT_ICONS[cI].draw(mS, inputTab.x + INPUT_ICON_X[cI], inputTab.y + INPUT_ICON_Y[cI]);
            });
            inputTab.onTick = () -> {
                // Check if this tab is selected.
                inputTab.stateEnable = (this.reactorRedstonePortState.selectedTab == ReactorRedstonePortSelection.fromInt(cI));
            };
            this.addScreenElement(inputTab);
        }

        // (Right) Add output tab buttons:
        for (int i = 0; i < 6; i++) {
            final int cI = i;
            InteractiveElement<ReactorRedstonePortContainer> outputTab = new InteractiveElement<>(this, 175, (cI * 25), 25, 24, 0, 0, Component.translatable(OUTPUT_TRANSLATIONS[cI] + ".tooltip"));
            outputTab.onMouseReleased = (mX, mY, btn) -> {
                // Click logic. Extra check necessary since this is an "in-class" button.
                if (outputTab.isMouseOver(mX, mY)) {
                    // Mouse is hovering, do the thing.
                    this.getMenu().executeRequest("setSelectedTab", cI + 3);
                    this.reactorRedstonePortState.selectedTab = ReactorRedstonePortSelection.fromInt(cI + 3);
                    // Play the selection sound.
                    outputTab.playSound(SoundEvents.UI_BUTTON_CLICK);
                    return true;
                } else {
                    // It ain't hovered, don't do the thing.
                    return false;
                }
            };
            outputTab.onRender = (mS, mX, mY) -> {
                // Custom rendering.
                GuiArt.panelTab(mS, outputTab.x, outputTab.y, true, outputTab.stateEnable, cI == 0);
                OUTPUT_ICONS[cI].draw(mS, outputTab.x + OUTPUT_ICON_X[cI], outputTab.y + 4);
            };
            outputTab.onTick = () -> {
                // Check if this tab is selected.
                outputTab.stateEnable = (this.reactorRedstonePortState.selectedTab == ReactorRedstonePortSelection.fromInt(cI + 3));
            };
            this.addScreenElement(outputTab);
        }

        // (Bottom) Apply changes button:
        this.applyChangesButton = new InteractiveElement<>(this, 156, 156, 15, 15, 0, 0, Component.translatable("screen.biggerreactorsrework.reactor_redstone_port.apply_changes.tooltip"));
        this.applyChangesButton.onMouseReleased = (mX, mY, btn) -> {
            // Click logic. Extra check necessary since this is an "in-class" button.
            if (this.applyChangesButton.isMouseOver(mX, mY)) {
                // Mouse is hovering, do the thing.

                if (this.reactorRedstonePortState.isInput()) {
                    // This is an input, so update the PS value.
                    this.getMenu().executeRequest("setTriggerPS", this.triggerTypeToggle.getState() != 0);
                } else {
                    // This is an output, so update the AB value.
                    this.getMenu().executeRequest("setTriggerAB", this.triggerTypeToggle.getState() != 0);
                }
                this.getMenu().executeRequest("setTriggerMode", this.triggerModeToggle.getState());


                // Implicitly set the text buffers: if they've changed, the user probably wants to apply them.
                this.getMenu().executeRequest("setTextBufferA", this.textBufferA.getContents().replaceAll("[^\\d.]", ""));
                this.getMenu().executeRequest("setTextBufferB", this.textBufferB.getContents().replaceAll("[^\\d.]", ""));
                // Trigger state change.
                this.getMenu().executeRequest("applyChanges", 0);
                // Play the selection sound.
                this.applyChangesButton.playSound(SoundEvents.UI_BUTTON_CLICK);
                return true;
            } else {
                // It ain't hovered, don't do the thing.
                return false;
            }
        };
        this.applyChangesButton.onRender = (mS, mX, mY) -> {
            // Custom rendering.
            GuiArt.pushButton(mS, this.applyChangesButton.x, this.applyChangesButton.y, this.applyChangesButton.width, this.applyChangesButton.height, this.applyChangesButton.isMouseOver(mX, mY), GuiIcon.CONFIRM);
        };
        this.addScreenElement(this.applyChangesButton);

        // (Bottom) Revert changes button:
        this.revertChangesButton = new InteractiveElement<>(this, 138, 156, 15, 15, 0, 0, Component.translatable("screen.biggerreactorsrework.reactor_redstone_port.revert_changes.tooltip"));
        this.revertChangesButton.onMouseReleased = (mX, mY, btn) -> {
            // Click logic. Extra check necessary since this is an "in-class" button.
            if (this.revertChangesButton.isMouseOver(mX, mY)) {
                // Mouse is hovering, do the thing.
                this.getMenu().executeRequest("revertChanges", 0);
                // Play the selection sound.
                this.revertChangesButton.playSound(SoundEvents.UI_BUTTON_CLICK);
                return true;
            } else {
                // It ain't hovered, don't do the thing.
                return false;
            }
        };
        this.revertChangesButton.onRender = (mS, mX, mY) -> {
            // Custom rendering.
            GuiArt.pushButton(mS, this.revertChangesButton.x, this.revertChangesButton.y, this.revertChangesButton.width, this.revertChangesButton.height, this.revertChangesButton.isMouseOver(mX, mY), GuiIcon.REVERT);
        };
        //this.addElement(this.revertChangesButton);

        // (Left) Trigger type toggle:
        this.triggerTypeToggle = new Biselector<>(this, 29, 42, Component.empty(),
                () -> (this.reactorRedstonePortState.triggerPS.toBool() || this.reactorRedstonePortState.triggerAB.toBool())
                        ? 1 : 0, SelectorColors.YELLOW, SelectorColors.RED);
        this.triggerTypeToggle.onMouseReleased = (mX, mY, btn) -> {
            // Click logic. Check if PS or AB.
            if (this.reactorRedstonePortState.isInput()) {
                // This is an input, so update the PS value.
                this.getMenu().executeRequest("setTriggerPS", this.triggerTypeToggle.getState() == 0);
            } else {
                // This is an output, so update the AB value.
                this.getMenu().executeRequest("setTriggerAB", this.triggerTypeToggle.getState() == 0);
            }
            return true;
        };
        this.triggerTypeToggle.onTick = () -> {
            // Check if the element should be PS or AB, and update.
            if (this.reactorRedstonePortState.isInput()) {
                // This is an input, so use the PS tooltip.
                this.triggerTypeToggle.tooltip = Component.translatable("screen.biggerreactorsrework.reactor_redstone_port.trigger_type_toggle.ps.tooltip");
            } else {
                // This is an output, so use the AB tooltip.
                this.triggerTypeToggle.tooltip = Component.translatable("screen.biggerreactorsrework.reactor_redstone_port.trigger_type_toggle.ab.tooltip");
            }
        };
        this.addScreenElement(triggerTypeToggle);

        // (Left) Trigger mode toggle:
        this.triggerModeToggle = new Triselector<>(this, 29, 58, Component.translatable("screen.biggerreactorsrework.reactor_terminal.auto_eject_toggle.tooltip"),
                () -> this.reactorRedstonePortState.triggerMode, SelectorColors.GREEN, SelectorColors.RED, SelectorColors.YELLOW);
        this.triggerModeToggle.onMouseReleased = (mX, mY, btn) -> {
            // Click logic.
            this.getMenu().executeRequest("setTriggerMode", this.triggerModeToggle.getState());
            return true;
        };
        this.addScreenElement(this.triggerModeToggle);

        // (Top) Text buffer A:
        this.textBufferA = new TextBox<>(this, this.font, 27, 91, 96, 16, this.reactorRedstonePortState.textBufferA);
        this.addScreenElement(this.textBufferA);

        // (Top) Text buffer A enter button:
        this.textEnterButtonA = new CommonButton<>(this, 135, 92, 17, 14, GuiIcon.ARROW_ENTER, GuiIcon.ARROW_ENTER_DISABLED, Component.translatable("screen.biggerreactorsrework.reactor_redstone_port.apply.tooltip"));
        this.textEnterButtonA.onMouseReleased = (mX, mY, btn) -> {
            // Click logic.
            this.getMenu().executeRequest("setTextBufferA", this.textBufferA.getContents().replaceAll("[^\\d.]", ""));
            return true;
        };
        this.addScreenElement(this.textEnterButtonA);

        // (Top) Text buffer B:
        this.textBufferB = new TextBox<>(this, this.font, 27, 122, 96, 16, this.reactorRedstonePortState.textBufferB);
        this.addScreenElement(textBufferB);

        // (Top) Text buffer B enter button:
        this.textEnterButtonB = new CommonButton<>(this, 135, 123, 17, 14, GuiIcon.ARROW_ENTER, GuiIcon.ARROW_ENTER_DISABLED, Component.translatable("screen.biggerreactorsrework.reactor_redstone_port.apply.tooltip"));
        this.textEnterButtonB.onMouseReleased = (mX, mY, btn) -> {
            // Click logic.
            this.getMenu().executeRequest("setTextBufferB", this.textBufferB.getContents().replaceAll("[^\\d.]", ""));
            return true;
        };
        this.addScreenElement(this.textEnterButtonB);
    }

    /**
     * Initialize symbols.
     */
    public void initSymbols() {
        // Only a tooltip: this element has never drawn anything.
        this.selectedTabSymbol = new RenderedElement<>(this, 92, 20, 16, 16, 0, 0, Component.empty());
        this.selectedTabSymbol.onTick = () -> {
            // Set tooltip based on type.
            this.selectedTabSymbol.tooltip = Component.translatable((this.reactorRedstonePortState.isInput())
                    ? INPUT_TRANSLATIONS[this.reactorRedstonePortState.selectedTab.toInt()]
                    : OUTPUT_TRANSLATIONS[this.reactorRedstonePortState.selectedTab.toInt() - 3]);
        };
        this.addScreenElement(this.selectedTabSymbol);
    }

    /**
     * Tick/update this screen.
     */
    @Override
    public void containerTick() {
        super.containerTick();

        // Check what tab is selected.
        switch (this.reactorRedstonePortState.selectedTab) {
            // Default case INPUT_ACTIVITY:
            case INPUT_CONTROL_ROD_INSERTION: {
                this.textBufferA.actionEnable = true;
                this.textEnterButtonA.actionEnable = true;
                if (this.reactorRedstonePortState.triggerPS == ReactorRedstonePortTriggers.PULSE_OR_ABOVE) {
                    this.triggerModeToggle.actionEnable = true;
                    this.textBufferB.actionEnable = false;
                    this.textBufferB.clear();
                    this.textEnterButtonB.actionEnable = false;
                } else {
                    this.triggerModeToggle.actionEnable = false;
                    this.textBufferB.actionEnable = true;
                    this.textEnterButtonB.actionEnable = true;
                }
                break;
            }
            case OUTPUT_FUEL_TEMP:
            case OUTPUT_CASING_TEMP:
            case OUTPUT_FUEL_ENRICHMENT:
            case OUTPUT_FUEL_AMOUNT:
            case OUTPUT_WASTE_AMOUNT:
            case OUTPUT_ENERGY_AMOUNT: {
                this.triggerModeToggle.actionEnable = false;
                this.textBufferA.actionEnable = true;
                this.textEnterButtonA.actionEnable = true;
                this.textBufferB.actionEnable = false;
                this.textBufferB.clear();
                this.textEnterButtonB.actionEnable = false;
                break;
            }
            default: {
                this.triggerModeToggle.actionEnable = false;
                // Disable text buffer A.
                if (this.textBufferA.actionEnable || this.textEnterButtonA.actionEnable) {
                    this.textBufferA.clear();
                    this.textBufferA.actionEnable = false;
                    this.textEnterButtonA.actionEnable = false;
                }
                // Disable text buffer B.
                if (this.textBufferB.actionEnable || this.textEnterButtonB.actionEnable) {
                    this.textBufferB.clear();
                    this.textBufferB.actionEnable = false;
                    this.textEnterButtonB.actionEnable = false;
                }
                break;
            }
        }
    }

    /**
     * Draw the status text for this screen.
     *
     * @param poseStack    The current pose stack.
     * @param mouseX       The x position of the mouse.
     * @param mouseY       The y position of the mouse.
     * @param partialTicks Partial ticks.
     */
    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);

        // Render common text.
        if (this.reactorRedstonePortState.isInput()) {
            // Check what type of trigger is used (pulse or signal):
            if (this.reactorRedstonePortState.triggerPS == ReactorRedstonePortTriggers.PULSE_OR_ABOVE) {
                // Text for on pulse:
                graphics.drawString(this.getFont(), Component.translatable("screen.biggerreactorsrework.reactor_redstone_port.trigger_type_toggle.ps.on_pulse").getString(), this.getGuiLeft() + 63, this.getGuiTop() + 45, 4210752, false);
            } else {
                // Text for on signal:
                graphics.drawString(this.getFont(), Component.translatable("screen.biggerreactorsrework.reactor_redstone_port.trigger_type_toggle.ps.on_signal").getString(), this.getGuiLeft() + 63, this.getGuiTop() + 45, 4210752, false);
            }
        } else {
            // Check what type of trigger is used (above or below):
            if (this.reactorRedstonePortState.triggerAB == ReactorRedstonePortTriggers.PULSE_OR_ABOVE) {
                // Text for on above:
                graphics.drawString(this.getFont(), Component.translatable("screen.biggerreactorsrework.reactor_redstone_port.trigger_type_toggle.ab.while_above").getString(), this.getGuiLeft() + 63, this.getGuiTop() + 45, 4210752, false);
            } else {
                // Text for on below:
                graphics.drawString(this.getFont(), Component.translatable("screen.biggerreactorsrework.reactor_redstone_port.trigger_type_toggle.ab.while_below").getString(), this.getGuiLeft() + 63, this.getGuiTop() + 45, 4210752, false);
            }
        }

        // Render tab-specific text.
        switch (this.reactorRedstonePortState.selectedTab) {
            // Default case INPUT_ACTIVITY:
            case INPUT_CONTROL_ROD_INSERTION: {
                // Check trigger type:
                if (this.reactorRedstonePortState.triggerPS == ReactorRedstonePortTriggers.PULSE_OR_ABOVE) {
                    // When set to pulse:
                    if (this.reactorRedstonePortState.triggerMode == 0) {
                        // Insert by (mode A/0):
                        graphics.drawString(this.getFont(), Component.translatable("screen.biggerreactorsrework.reactor_redstone_port.trigger_mode_toggle.mode_a").getString(), this.getGuiLeft() + 80, this.getGuiTop() + 62, 4210752, false);
                        graphics.drawString(this.getFont(), Component.translatable("screen.biggerreactorsrework.reactor_redstone_port.text_buffer_a.mode_a").getString(), this.getGuiLeft() + 29, this.getGuiTop() + 82, 4210752, false);
                    } else if (this.reactorRedstonePortState.triggerMode == 1) {
                        // Retract by (mode B/1):
                        graphics.drawString(this.getFont(), Component.translatable("screen.biggerreactorsrework.reactor_redstone_port.trigger_mode_toggle.mode_b").getString(), this.getGuiLeft() + 80, this.getGuiTop() + 62, 4210752, false);
                        graphics.drawString(this.getFont(), Component.translatable("screen.biggerreactorsrework.reactor_redstone_port.text_buffer_a.mode_b").getString(), this.getGuiLeft() + 29, this.getGuiTop() + 82, 4210752, false);
                    } else {
                        // Set to (mode C/2):
                        graphics.drawString(this.getFont(), Component.translatable("screen.biggerreactorsrework.reactor_redstone_port.trigger_mode_toggle.mode_c").getString(), this.getGuiLeft() + 80, this.getGuiTop() + 62, 4210752, false);
                        graphics.drawString(this.getFont(), Component.translatable("screen.biggerreactorsrework.reactor_redstone_port.text_buffer_a.mode_c").getString(), this.getGuiLeft() + 29, this.getGuiTop() + 82, 4210752, false);
                    }
                } else {
                    // When set to signal:
                    graphics.drawString(this.getFont(), Component.translatable("screen.biggerreactorsrework.reactor_redstone_port.input_control_rod_insertion.while_on").getString(), this.getGuiLeft() + 29, this.getGuiTop() + 82, 4210752, false);
                    graphics.drawString(this.getFont(), Component.translatable("screen.biggerreactorsrework.reactor_redstone_port.input_control_rod_insertion.while_off").getString(), this.getGuiLeft() + 29, this.getGuiTop() + 113, 4210752, false);
                }
                break;
            }
            case OUTPUT_FUEL_TEMP:
            case OUTPUT_CASING_TEMP: {
                graphics.drawString(this.getFont(), Component.translatable("screen.biggerreactorsrework.reactor_redstone_port.text_buffer_a.trigger_at").getString(), this.getGuiLeft() + 29, this.getGuiTop() + 82, 4210752, false);
                graphics.drawString(this.getFont(), "\u00B0C", this.getGuiLeft() + 155, this.getGuiTop() + 96, 4210752, false);
                break;
            }
            case OUTPUT_FUEL_ENRICHMENT:
            case OUTPUT_ENERGY_AMOUNT: {
                graphics.drawString(this.getFont(), Component.translatable("screen.biggerreactorsrework.reactor_redstone_port.text_buffer_a.trigger_at").getString(), this.getGuiLeft() + 29, this.getGuiTop() + 82, 4210752, false);
                graphics.drawString(this.getFont(), "%", this.getGuiLeft() + 155, this.getGuiTop() + 96, 4210752, false);
                break;
            }
            case OUTPUT_FUEL_AMOUNT:
            case OUTPUT_WASTE_AMOUNT: {
                graphics.drawString(this.getFont(), Component.translatable("screen.biggerreactorsrework.reactor_redstone_port.text_buffer_a.trigger_at").getString(), this.getGuiLeft() + 29, this.getGuiTop() + 82, 4210752, false);
                graphics.drawString(this.getFont(), "mB", this.getGuiLeft() + 155, this.getGuiTop() + 96, 4210752, false);
                break;
            }
        }
    }

    /**
     * Draw the panel and its fixed details.
     */
    @Override
    protected void drawBackground(@Nonnull GuiGraphics graphics, int x, int y) {
        GuiArt.panel(graphics, x + 21, y, 158, 178);
    }
}
