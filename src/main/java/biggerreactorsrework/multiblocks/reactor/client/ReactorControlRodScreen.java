package biggerreactorsrework.multiblocks.reactor.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.client.CommonButton;
import biggerreactorsrework.client.GaugeBar;
import biggerreactorsrework.client.TextBox;
import biggerreactorsrework.client.GuiArt;
import biggerreactorsrework.client.GuiIcon;
import biggerreactorsrework.fluids.LiquidUranium;
import biggerreactorsrework.multiblocks.reactor.containers.ReactorControlRodContainer;
import biggerreactorsrework.multiblocks.reactor.state.ReactorControlRodState;
import biggerreactorsrework.core.client.gui.screens.ModScreen;
import biggerreactorsrework.core.client.gui.RenderHelper;
import biggerreactorsrework.core.client.gui.elements.InteractiveElement;
import biggerreactorsrework.core.client.gui.elements.RenderedElement;

import javax.annotation.Nonnull;

@OnlyIn(Dist.CLIENT)
public class ReactorControlRodScreen extends ModScreen<ReactorControlRodContainer> {


    private ReactorControlRodState reactorControlRodState;

    public ReactorControlRodScreen(ReactorControlRodContainer container, Inventory playerInventory, Component title) {
        super(container, playerInventory, title, 136, 126);

        // Initialize control rod state.
        reactorControlRodState = (ReactorControlRodState) this.getMenu().getGuiPacket();
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
        this.initGauges();

        // Initialize symbols:
    }

    /**
     * Initialize controls.
     */
    public void initControls() {
        // (Top) Name text box:
        TextBox<ReactorControlRodContainer> textBox = new TextBox<>(this, this.font, 6, 26, 96, 16, reactorControlRodState.name);
        this.addScreenElement(textBox);

        // (Top) Name text box enter button:
        InteractiveElement<ReactorControlRodContainer> textBoxEnterButton = new InteractiveElement<>(this, 114, 27, 17, 14, 0, 0, Component.translatable("screen.biggerreactorsrework.reactor_control_rod.apply.tooltip"));
        textBoxEnterButton.onMouseReleased = (mX, mY, btn) -> {
            // Click logic. Extra check necessary since this is an "in-class" button.
            if (textBoxEnterButton.isMouseOver(mX, mY)) {
                // Mouse is hovering, do the thing.
                this.getMenu().executeRequest("setName", textBox.getContents());
                // Play the selection sound.
                textBoxEnterButton.playSound(SoundEvents.UI_BUTTON_CLICK);
                return true;
            } else {
                // It ain't hovered, don't do the thing.
                return false;
            }
        };
        textBoxEnterButton.onRender = ((mS, mX, mY) -> {
            // Custom rendering.
            GuiArt.button(mS, textBoxEnterButton.x, textBoxEnterButton.y, textBoxEnterButton.width, textBoxEnterButton.height, textBoxEnterButton.isMouseOver(mX, mY), GuiIcon.ARROW_ENTER);
        });
        //this.addElement(textBoxEnterButton);

        // (Top) Name text box enter button:
        CommonButton<ReactorControlRodContainer> textEnterButton = new CommonButton<>(this, 114, 27, 17, 14, GuiIcon.ARROW_ENTER, GuiIcon.ARROW_ENTER_DISABLED, Component.translatable("screen.biggerreactorsrework.reactor_redstone_port.apply.tooltip"));
        textEnterButton.onMouseReleased = (mX, mY, btn) -> {
            // Click logic.
            this.getMenu().executeRequest("setName", textBox.getContents());
            return true;
        };
        this.addScreenElement(textEnterButton);

        // (Center) Rod retract button:
        InteractiveElement<ReactorControlRodContainer> rodRetractButton = new InteractiveElement<>(this, 58, 82, 14, 15, 0, 0, Component.translatable("screen.biggerreactorsrework.reactor_control_rod.retract_rod.tooltip"));
        rodRetractButton.onMouseReleased = (mX, mY, btn) -> {
            // Click logic. Extra check necessary since this is an "in-class" button.
            if (rodRetractButton.isMouseOver(mX, mY)) {
                // Calculate amount of change:
                double delta;
                if (Screen.hasShiftDown() && Screen.hasControlDown()) delta = -100D;
                else if (Screen.hasControlDown()) delta = -50D;
                else if (Screen.hasShiftDown()) delta = -10D;
                else delta = -1D;
                // Mouse is hovering, do the thing.
                this.getMenu().executeRequest("changeInsertionLevel", new Pair<>(delta, Screen.hasAltDown()));
                // Play the selection sound.
                rodRetractButton.playSound(SoundEvents.UI_BUTTON_CLICK);
                return true;
            } else {
                // It ain't hovered, don't do the thing.
                return false;
            }
        };
        rodRetractButton.onRender = ((mS, mX, mY) -> {
            // Custom rendering.
            GuiArt.button(mS, rodRetractButton.x, rodRetractButton.y, rodRetractButton.width, rodRetractButton.height, rodRetractButton.isMouseOver(mX, mY), GuiIcon.ARROW_UP);
        });
        this.addScreenElement(rodRetractButton);

        // (Center) Rod insert button:
        InteractiveElement<ReactorControlRodContainer> rodInsertButton = new InteractiveElement<>(this, 58, 64, 14, 15, 0, 0, Component.translatable("screen.biggerreactorsrework.reactor_control_rod.insert_rod.tooltip"));
        rodInsertButton.onMouseReleased = (mX, mY, btn) -> {
            // Click logic. Extra check necessary since this is an "in-class" button.
            if (rodInsertButton.isMouseOver(mX, mY)) {
                // Calculate amount of change:
                double delta;
                if (Screen.hasShiftDown() && Screen.hasControlDown()) delta = 100D;
                else if (Screen.hasControlDown()) delta = 50D;
                else if (Screen.hasShiftDown()) delta = 10D;
                else delta = 1D;
                // Mouse is hovering, do the thing.
                this.getMenu().executeRequest("changeInsertionLevel", new Pair<>(delta, Screen.hasAltDown()));
                // Play the selection sound.
                rodInsertButton.playSound(SoundEvents.UI_BUTTON_CLICK);
                return true;
            } else {
                // It ain't hovered, don't do the thing.
                return false;
            }
        };
        rodInsertButton.onRender = (mS, mX, mY) -> {
            // Custom rendering.
            GuiArt.button(mS, rodInsertButton.x, rodInsertButton.y, rodInsertButton.width, rodInsertButton.height, rodInsertButton.isMouseOver(mX, mY), GuiIcon.ARROW_DOWN);
        };
        this.addScreenElement(rodInsertButton);
    }

    /**
     * Initialize gauges.
     */
    public void initGauges() {
        // (Center) Control rod insertion gauge:
        RenderedElement<ReactorControlRodContainer> rodInsertionGauge = new RenderedElement<>(this, 36, 50, 18, 64, 0, 126, Component.empty());
        rodInsertionGauge.onRender = (@Nonnull GuiGraphics graphics, int mX, int mY) -> ReactorControlRodScreen.renderInsertionLevel(graphics, rodInsertionGauge, this.reactorControlRodState.insertionLevel);
        this.addScreenElement(rodInsertionGauge);
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

        // Render text for text box:
        graphics.drawString(this.getFont(), Component.translatable("screen.biggerreactorsrework.reactor_control_rod.name").getString(), this.getGuiLeft() + 8, this.getGuiTop() + 17, 4210752, false);

        // Render text for insertion level:
        graphics.drawString(this.getFont(), String.format("%.1f%%", reactorControlRodState.insertionLevel), this.getGuiLeft() + 76, this.getGuiTop() + 77, 4210752, false);
    }

    /**
     * Render a reactor fuel mix gauge.
     *
     * @param poseStack      The current pose stack.
     * @param symbol         The symbol to draw as.
     * @param insertionLevel How far the control rod is inserted. 0 is no insertion, 100 is full insertion.
     */
    public static void renderInsertionLevel(@Nonnull GuiGraphics graphics, @Nonnull RenderedElement<ReactorControlRodContainer> symbol, double insertionLevel) {
        // Fuel behind, rod coming down from the top as it is inserted.
        GaugeBar.drawControlRod(graphics, symbol.x, symbol.y, insertionLevel, LiquidUranium.INSTANCE.getSource());
        // Update tooltip.
        symbol.tooltip = Component.literal(String.format("%.1f%%", insertionLevel));
    }
}
