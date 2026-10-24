package biggerreactorsrework.client;

/**
 * Colors of a selector cell, idle and hovered.
 */
public enum SelectorColors {
    DISABLED(GuiArt.Palette.of(0x242424, 0x181818, 0x0E0E0E), GuiArt.Palette.of(0x2A2A2A, 0x1D1D1D, 0x101010)),
    RED(GuiArt.Palette.of(0xDA7F6D, 0xC46A62, 0xAE5757), GuiArt.Palette.of(0xFF9580, 0xE57C73, 0xCC6666)),
    CYAN(GuiArt.Palette.of(0x67C6CF, 0x5CAAB9, 0x528FA3), GuiArt.Palette.of(0x79E8F2, 0x6CC7D9, 0x60A7BF)),
    GREEN(GuiArt.Palette.of(0x449944, 0x3B8341, 0x316D3B), GuiArt.Palette.of(0x50B350, 0x45994C, 0x398045)),
    YELLOW(GuiArt.Palette.of(0xDACF57, 0xC4C44F, 0xA6AE46), GuiArt.Palette.of(0xFFF266, 0xE5E55C, 0xC2CC52)),
    BLUE(GuiArt.Palette.of(0x5762DA, 0x4F4FC4, 0x4F46AE), GuiArt.Palette.of(0x6673FF, 0x5C5CE5, 0x5C52CC)),
    BLACK(GuiArt.Palette.of(0x8E8E8E, 0x626262, 0x373737), GuiArt.Palette.of(0xA6A6A6, 0x737373, 0x404040));

    /**
     * The colors of the idle/non-hovered cell.
     */
    public final GuiArt.Palette idle;

    /**
     * The colors of the hovered cell.
     */
    public final GuiArt.Palette hovered;

    SelectorColors(GuiArt.Palette idle, GuiArt.Palette hovered) {
        this.idle = idle;
        this.hovered = hovered;
    }
}
