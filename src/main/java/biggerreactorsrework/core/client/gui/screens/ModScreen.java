package biggerreactorsrework.core.client.gui.screens;

import biggerreactorsrework.client.GuiTheme;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.renderer.texture.Tickable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import biggerreactorsrework.client.GuiArt;
import biggerreactorsrework.client.GuiFrame;
import biggerreactorsrework.client.GuiSettingsWindow;
import biggerreactorsrework.core.client.gui.GuiSync;
import net.minecraft.core.BlockPos;
import biggerreactorsrework.core.client.gui.RenderHelper;
import biggerreactorsrework.core.client.gui.api.IRender;
import biggerreactorsrework.core.client.gui.api.ITooltip;
import biggerreactorsrework.core.client.gui.elements.AbstractElement;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Basic screen. This may be used to build your own GUIs for mods, or can be safely ignored if you'd rather roll your own.
 * This abstraction also assumes you are working with GUIs that have an associated container. If not, then you are advised
 * to use the vanilla system for containerless GUIs (which, for the most part, is pretty alright).
 *
 * @param <T> Screens must have a screen container implementing {@link net.minecraft.world.inventory.AbstractContainerMenu AbstractContainerMenu}.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("rawtypes")
public abstract class ModScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> implements MenuAccess<T> {

    /**
     * The list of elements that are a part of this screen.
     */
    private final List<AbstractElement> screenElements;

    private final GuiSettingsWindow settingsWindow = new GuiSettingsWindow(this);

    public GuiSettingsWindow settingsWindow() { return settingsWindow; }

    /** Appearance is configured from machine terminals, not their ports and auxiliary controls. */
    public boolean hasAppearanceSettings() { return false; }

    /** Whether an auxiliary window covers the Settings tab, including its click target and tooltip. */
    public boolean isSettingsTabCovered(double mouseX, double mouseY) { return false; }

    public BlockPos machinePos() {
        return ((GuiSync.IGUIPacketProvider) this.menu).machinePos();
    }

    /** Left side, away from the reprocessor's existing right-side routing tab. */
    public int settingsTabX() { return this.leftPos - GuiFrame.TAB_SIZE; }
    public int settingsTabY() { return this.topPos + 4; }

    /**
     * Space kept between the title and the edges of the panel.
     */
    protected static final int TITLE_MARGIN = 8;

    /**
     * The texture map this screen accesses.
     */
    protected ResourceLocation textureAtlas;

    /**
     * Player inventory
     */
    protected Inventory inventory;

    /**
     * This constructor makes no assumptions.
     */
    public ModScreen(T screenContainer, Inventory playerInventory, Component title, ResourceLocation textureAtlas, int width, int height) {
        super(screenContainer, playerInventory, title);
        this.inventory = playerInventory;
        this.textureAtlas = textureAtlas;
        this.imageWidth = width;
        this.imageHeight = height;
        this.screenElements = Lists.newArrayList();
    }

    /**
     * Constructor for screens drawn in code (see {@link #drawBackground(GuiGraphics, int, int)}).
     */
    public ModScreen(T screenContainer, Inventory playerInventory, Component title, int width, int height) {
        this(screenContainer, playerInventory, title, RenderHelper.getBlankTextureResource(), width, height);
    }

    /**
     * Register an element with this screen.
     *
     * @param element The element to register.
     */
    public void addScreenElement(AbstractElement element) {
        if (element != null) {
            this.screenElements.add(element);
        }
        //this.addListener(element);
    }

    /**
     * Get the width of the screen.
     *
     * @return The width.
     */
    public int getWidth() {
        return this.width;
    }

    /**
     * Get the height of the screen.
     *
     * @return The height.
     */
    public int getHeight() {
        return this.height;
    }

    /**
     * Get the font renderer for this screen.
     *
     * @return The font renderer.
     */
    public Font getFont() {
        return this.font;
    }

    /**
     * Update the current texture atlas.
     *
     * @param textureAtlas The atlas to switch to.
     */
    public void setTextureAtlas(ResourceLocation textureAtlas) {
        this.textureAtlas = textureAtlas;
    }

    /**
     * Draw the screen.
     *
     * @param poseStack    The current pose stack.
     * @param mouseX       The x position of the mouse.
     * @param mouseY       The y position of the mouse.
     * @param partialTicks Partial ticks.
     */
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // super.render draws the background (blur included) itself; drawing it here too would blur twice per frame
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        // Draw tooltips for all the elements that belong to this screen.
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        for (AbstractElement element : this.screenElements) {
            // Check conditions, and render.
            if (element instanceof ITooltip) {
                ((ITooltip) element).renderTooltip(guiGraphics, mouseX, mouseY);
            }
        }
    }
    
    /**
     * Draw the foreground.
     *
     * @param poseStack The current pose stack.
     * @param mouseX    The x position of the mouse.
     * @param mouseY    The y position of the mouse.
     */
    @Override
    public void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Bind to the correct texture & reset render color.
        RenderHelper.bindTexture(this.textureAtlas);
        RenderHelper.setRenderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // Draw all the elements that belong to this screen.
        for (AbstractElement element : this.screenElements) {
            // Check conditions, and render.
            if (element instanceof IRender) {
                ((IRender) element).render(guiGraphics, mouseX, mouseY);
            }
        }

        // Draw title; one too long for its room slides inside it.
        String title = this.title.getString();
        if (this.font.width(title) <= this.titleWidth()) {
            guiGraphics.drawString(this.font, title, this.titleLabelX, this.titleLabelY, GuiTheme.textColor(), false);
        } else {
            GuiArt.scrollingText(guiGraphics, this.font, title, TITLE_MARGIN, this.titleLabelY, this.titleWidth(), GuiTheme.textColor());
        }
    }

    /**
     * Room for the title, starting {@link #TITLE_MARGIN} from the left edge. By default the whole width of the panel;
     * screens with something next to the title end it earlier.
     */
    protected int titleWidth() {
        return this.imageWidth - 2 * TITLE_MARGIN;
    }

    /**
     * Draw the background.
     *
     * @param poseStack    The current pose stack.
     * @param partialTicks Partial ticks.
     * @param mouseX       The x position of the mouse.
     * @param mouseY       The y position of the mouse.
     */
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        RenderHelper.setRenderColor(1.0F, 1.0F, 1.0F, 1.0F);
        this.drawBackground(guiGraphics, this.getGuiLeft(), this.getGuiTop());
        this.settingsWindow.renderTab(guiGraphics, mouseX, mouseY);
    }

    /**
     * Draw the static part of the screen. By default this is the plain panel; screens add their own details.
     *
     * @param graphics The current graphics.
     * @param x        The left of the screen.
     * @param y        The top of the screen.
     */
    protected void drawBackground(GuiGraphics graphics, int x, int y) {
        GuiArt.panel(graphics, x, y, this.getXSize(), this.getYSize(), this.frame());
    }

    /**
     * Metal frame of this screen, matching the casing of the block it belongs to.
     */
    protected abstract GuiFrame frame();

    public GuiFrame getFrame() {
        return this.frame();
    }

    /**
     * The frame around the panel belongs to the screen: clicking it must not drop the carried item.
     */
    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int button) {
        if (settingsWindow.contains(mouseX, mouseY)) return false;
        return mouseX < guiLeft - GuiFrame.BAND || mouseY < guiTop - GuiFrame.BAND
                || mouseX >= guiLeft + this.imageWidth + GuiFrame.BAND || mouseY >= guiTop + this.imageHeight + GuiFrame.BAND;
    }

    /**
     * Returns whether the mouse is over the desired area.
     *
     * @param mouseX     The x position of the mouse.
     * @param mouseY     The y position of the mouse.
     * @param areaX      The x position of the area you want to check for.
     * @param areaY      The y position of the area you want to check for.
     * @param areaWidth  The width of the area to check.
     * @param areaHeight The height of the area to check.
     * @return True if the mouse is over the desired area, false otherwise.
     */
    public boolean isMouseOver(double mouseX, double mouseY, double areaX, double areaY, int areaWidth, int areaHeight) {
        // Get actual x and y positions.
        int relativeX = (int) (this.getGuiLeft() + areaX);
        int relativeY = (int) (this.getGuiTop() + areaY);
        // Check the mouse.
        return ((mouseX > relativeX) && (mouseX < relativeX + areaWidth)
                && (mouseY > relativeY) && (mouseY < relativeY + areaHeight));
    }

    /**
     * Tick/update this screen.
     */
    @Override
    public void containerTick() {
        // Iterate through this screen's elements.
        for (AbstractElement element : this.screenElements) {
            // Trigger.
            ((Tickable) element).tick();
        }
    }

    /**
     * Triggered when the mouse is moved.
     *
     * @param mouseX The x position of the mouse.
     * @param mouseY The y position of the mouse.
     */
    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        // Iterate through this screen's elements.
        for (AbstractElement element : this.screenElements) {
            // Check conditions, and trigger.
            if (element != null) {
                element.mouseMoved(mouseX, mouseY);
            }
        }
    }
    /**
     * Triggered when the mouse is clicked.
     * For most purposes, it is recommended to tie logic to #mouseReleased.
     *
     * @param mouseX The x position of the mouse.
     * @param mouseY The y position of the mouse.
     * @param button The button clicked.
     * @return Whether the event was consumed.
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Iterate through this screen's elements.
        boolean handled = false;
        for (AbstractElement element : this.screenElements) {
            // Check conditions, and trigger.
            if (element != null) {
                handled = (handled || element.mouseClicked(mouseX, mouseY, button));
            }
        }
        return (handled || super.mouseClicked(mouseX, mouseY, button));
    }

    /**
     * Triggered when the mouse is released.
     *
     * @param mouseX The x position of the mouse.
     * @param mouseY The y position of the mouse.
     * @param button The button clicked.
     * @return Whether the event was consumed.
     */
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        // Iterate through this screen's elements.
        boolean handled = false;
        for (AbstractElement element : this.screenElements) {
            // Check conditions, and trigger.
            if (element != null) {
                handled = (handled || element.mouseReleased(mouseX, mouseY, button));
            }
        }
        return (handled || super.mouseReleased(mouseX, mouseY, button));
    }

    /**
     * Triggered when the mouse is dragged.
     *
     * @param mouseX The x position of the mouse.
     * @param mouseY The y position of the mouse.
     * @param button The button clicked.
     * @param dragX  Drag x.
     * @param dragY  Drag y.
     * @return Whether the event was consumed.
     */
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // Iterate through this screen's elements.
        boolean handled = false;
        for (AbstractElement element : this.screenElements) {
            // Check conditions, and trigger.
            if (element != null) {
                handled = (handled || element.mouseDragged(mouseX, mouseY, button, dragX, dragY));
            }
        }
        return (handled || super.mouseDragged(mouseX, mouseY, button, dragX, dragY));
    }

    /**
     * Triggered when the mouse is scrolled.
     *
     * @param mouseX The x position of the mouse.
     * @param mouseY The y position of the mouse.
     * @param delta  How far the mouse scrolled.
     * @return Whether the event was consumed.
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta, double idfk) {
        // Iterate through this screen's elements.
        boolean handled = false;
        for (AbstractElement element : this.screenElements) {
            // Check conditions, and trigger.
            if (element != null) {
                handled = (handled || element.mouseScrolled(mouseX, mouseY, delta, idfk));
            }
        }
        return (handled || super.mouseScrolled(mouseX, mouseY, delta, idfk));
    }

    /**
     * Triggered when a key is pressed.
     *
     * @param keyCode   The key code pressed.
     * @param scanCode  The scan code pressed.
     * @param modifiers Any modifiers pressed.
     * @return Whether the event was consumed.
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Iterate through this screen's elements.
        boolean handled = false;
        for (AbstractElement element : this.screenElements) {
            // Check conditions, and trigger.
            if (element != null) {
                handled = (handled || element.keyPressed(keyCode, scanCode, modifiers));
            }
        }
        // Return either the handle status, or the parent's return.
        return (handled || super.keyPressed(keyCode, scanCode, modifiers));
    }

    /**
     * Triggered when a key is released.
     *
     * @param keyCode   The key code released.
     * @param scanCode  The scan code released.
     * @param modifiers Any modifiers released.
     * @return Whether the event was consumed.
     */
    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        // Iterate through this screen's elements.
        boolean handled = false;
        for (AbstractElement element : this.screenElements) {
            // Check conditions, and trigger.
            if (element != null) {
                handled = (handled || element.keyReleased(keyCode, scanCode, modifiers));
            }
        }
        return (handled || super.keyReleased(keyCode, scanCode, modifiers));
    }

    /**
     * Triggered when a character is typed.
     *
     * @param codePoint The character typed.
     * @param modifiers Any modifiers released.
     * @return Whether the event was consumed.
     */
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        // Iterate through this screen's elements.
        boolean handled = false;
        for (AbstractElement element : this.screenElements) {
            // Check conditions, and trigger.
            if (element != null) {
                handled = (handled || element.charTyped(codePoint, modifiers));
            }
        }
        return (handled || super.charTyped(codePoint, modifiers));
    }
}
