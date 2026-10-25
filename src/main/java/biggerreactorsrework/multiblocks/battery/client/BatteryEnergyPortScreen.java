package biggerreactorsrework.multiblocks.battery.client;

import biggerreactorsrework.client.Biselector;
import biggerreactorsrework.client.GuiArt;
import biggerreactorsrework.client.GuiFrame;
import biggerreactorsrework.client.GuiTheme;
import biggerreactorsrework.client.SelectorColors;
import biggerreactorsrework.core.client.gui.screens.ModScreen;
import biggerreactorsrework.multiblocks.battery.containers.BatteryEnergyPortContainer;
import biggerreactorsrework.multiblocks.battery.state.BatteryEnergyPortState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;

@OnlyIn(Dist.CLIENT)
public class BatteryEnergyPortScreen extends ModScreen<BatteryEnergyPortContainer> {
    
    private static final int WIDTH = 160;
    
    private final BatteryEnergyPortState portState;
    
    public BatteryEnergyPortScreen(BatteryEnergyPortContainer container, Inventory playerInventory, Component title) {
        super(container, playerInventory, title, WIDTH, 52);
        this.portState = (BatteryEnergyPortState) this.getMenu().getGuiPacket();
    }
    
    @Override
    public void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.getTitle())) / 2;
        
        Biselector<BatteryEnergyPortContainer> directionToggle = new Biselector<>(this, 8, 18, Component.translatable("screen.biggerreactorsrework.battery_energy_port.direction_toggle.tooltip"),
                () -> portState.direction ? 0 : 1, SelectorColors.BLUE);
        directionToggle.onMouseReleased = (mX, mY, btn) -> {
            this.getMenu().executeRequest("setDirection", directionToggle.getState() == 0 ? 1 : 0);
            return true;
        };
        this.addScreenElement(directionToggle);
    }
    
    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        String direction = portState.direction ? "input" : "output";
        GuiArt.scrollingText(graphics, this.getFont(), Component.translatable("screen.biggerreactorsrework.battery_energy_port.direction_toggle." + direction).getString(),
                this.getGuiLeft() + 42, this.getGuiTop() + 22, WIDTH - 42 - 8, GuiTheme.textColor());
        String connection = portState.connected ? "connected" : "disconnected";
        GuiArt.scrollingText(graphics, this.getFont(), Component.translatable("screen.biggerreactorsrework.battery_energy_port." + connection).getString(),
                this.getGuiLeft() + 8, this.getGuiTop() + 38, WIDTH - 16, GuiTheme.textColor());
    }
    
    @Override
    protected GuiFrame frame() {
        return GuiFrame.BATTERY;
    }
}
