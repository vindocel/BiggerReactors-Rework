package biggerreactorsrework.multiblocks.heatexchanger.client;

import biggerreactorsrework.client.GuiTheme;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.client.Biselector;
import biggerreactorsrework.client.SelectorColors;
import biggerreactorsrework.client.GuiArt;
import biggerreactorsrework.client.GuiFrame;
import biggerreactorsrework.client.GuiIcon;
import biggerreactorsrework.multiblocks.heatexchanger.containers.HeatExchangerFluidPortContainer;
import biggerreactorsrework.multiblocks.heatexchanger.state.HeatExchangerFluidPortState;
import biggerreactorsrework.multiblocks.reactor.containers.ReactorCoolantPortContainer;
import biggerreactorsrework.core.client.gui.elements.InteractiveElement;
import biggerreactorsrework.core.client.gui.screens.ModScreen;

import javax.annotation.Nonnull;

@OnlyIn(Dist.CLIENT)
public class HeatExchangerFluidPortScreen extends ModScreen<HeatExchangerFluidPortContainer> {


    private HeatExchangerFluidPortState heatExchangerFluidPortState;

    public HeatExchangerFluidPortScreen(HeatExchangerFluidPortContainer container, Inventory playerInventory, Component title) {
        super(container, playerInventory, title, 176, 92);

        // Initialize access port state.
        heatExchangerFluidPortState = (HeatExchangerFluidPortState) this.getMenu().getGuiPacket();
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
    }

    /**
     * Initialize controls.
     */
    private Biselector<HeatExchangerFluidPortContainer> maintenance;

    private boolean showRecovery() {
        return heatExchangerFluidPortState.direction;
    }

    public void initControls() {
        maintenance = new Biselector<>(this,8,66,Component.translatable("thermal.biggerreactorsrework.recover_hot_coolant_tooltip"),()->heatExchangerFluidPortState.maintenanceCold?1:0,SelectorColors.CYAN);
        maintenance.onMouseReleased = (mx, my, button) -> {
            if (!showRecovery()) return false;
            getMenu().executeRequest("recoverInput", !heatExchangerFluidPortState.maintenanceCold);
            return true;
        };
        maintenance.disable();
        addScreenElement(maintenance);

        // (Left) Direction toggle:
        Biselector<HeatExchangerFluidPortContainer> directionToggle = new Biselector<>(this, 8, 33, Component.translatable("screen.biggerreactorsrework.heat_exchanger_fluid_port.direction_toggle.tooltip"),
                () -> heatExchangerFluidPortState.direction ? 0 : 1,
                () -> heatExchangerFluidPortState.condenser ? SelectorColors.RED : SelectorColors.CYAN,
                () -> heatExchangerFluidPortState.condenser ? SelectorColors.CYAN : SelectorColors.RED);
        directionToggle.onMouseReleased = (mX, mY, btn) -> {
            // Click logic.
            this.getMenu().executeRequest("setDirection", directionToggle.getState() == 0 ? 1 : 0);
            return true;
        };
        this.addScreenElement(directionToggle);

        // (Left) Manual dump button:
        InteractiveElement<HeatExchangerFluidPortContainer> manualEjectButton = new InteractiveElement<>(this, 8, 49, 15, 15, 0, 0, Component.translatable("screen.biggerreactorsrework.heat_exchanger_fluid_port.manual_dump.tooltip"));
        manualEjectButton.onMouseReleased = (mX, mY, btn) -> {
            // Click logic. Extra check necessary since this is an "in-class" button.
            if (manualEjectButton.isMouseOver(mX, mY)) {
                // Mouse is hovering, do the thing.
                this.getMenu().executeRequest("dumpTanks", this.heatExchangerFluidPortState.condenser);
                // Play the selection sound.
                manualEjectButton.playSound(SoundEvents.UI_BUTTON_CLICK);
                return true;
            } else {
                // It ain't hovered, don't do the thing.
                return false;
            }
        };
        manualEjectButton.onRender = ((mS, mX, mY) -> {
            // Custom rendering.
            GuiArt.button(mS, manualEjectButton.x, manualEjectButton.y, manualEjectButton.width, manualEjectButton.height, manualEjectButton.isMouseOver(mX, mY), GuiIcon.EJECT, this.frame());
        });
        this.addScreenElement(manualEjectButton);
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
        String recoveryKey = heatExchangerFluidPortState.condenser
                ? "thermal.biggerreactorsrework.recover_hot_coolant"
                : "thermal.biggerreactorsrework.recover_water";
        if (showRecovery()) {
            maintenance.tooltip = Component.translatable(recoveryKey + "_tooltip");
            maintenance.enable();
        } else maintenance.disable();
        super.render(graphics, mouseX, mouseY, partialTicks);

        // Render text to show what channel this is connected to.
        if (heatExchangerFluidPortState.condenser) {
            // Text for when connected to a condenser:
            graphics.drawString(this.getFont(), Component.translatable("screen.biggerreactorsrework.heat_exchanger_fluid_port.channel_type.condenser").getString(), this.getGuiLeft() + 8, this.getGuiTop() + 22, GuiTheme.textColor(), false);
        } else {
            // Text for when connected to an evaporator.
            graphics.drawString(this.getFont(), Component.translatable("screen.biggerreactorsrework.heat_exchanger_fluid_port.channel_type.evaporator").getString(), this.getGuiLeft() + 8, this.getGuiTop() + 22, GuiTheme.textColor(), false);
        }

        String circuit = heatExchangerFluidPortState.condenser ? "coolant_" : "";
        String direction = heatExchangerFluidPortState.direction ? "input" : "output";
        graphics.drawString(getFont(), Component.translatable("screen.biggerreactorsrework.heat_exchanger_fluid_port.direction_toggle."
                + circuit + direction), getGuiLeft() + 42, getGuiTop() + 37, GuiTheme.textColor(), false);

        // Render text for manual tank eject:
        graphics.drawString(this.getFont(), Component.translatable("screen.biggerreactorsrework.heat_exchanger_fluid_port.manual_dump").getString(), this.getGuiLeft() + 26, this.getGuiTop() + 53, GuiTheme.textColor(), false);
        if (showRecovery()) GuiArt.scrollingText(graphics,getFont(),Component.translatable(recoveryKey).getString(),getGuiLeft()+42,getGuiTop()+70,126,GuiTheme.textColor());
    }

    @Override
    protected GuiFrame frame() {
        return GuiFrame.HEAT_EXCHANGER;
    }
}
