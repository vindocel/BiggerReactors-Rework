package biggerreactorsrework.client;

/**
 * Colors of a lit toggle: the block beside a switch's knob (top row, fill, bottom row) and the lit cell of a
 * selector (top row, left column, fill).
 */
public enum SelectorColors {
    RED(0xC4675C, 0xA33D36, 0x7A2E28, 0xC4675C, 0xBA5A51, 0xA33D36),
    CYAN(0x4D9BA3, 0x26858F, 0x1E6069, 0x4D9BA3, 0x41949D, 0x26858F),
    GREEN(0x5D9566, 0x397E45, 0x2B5B39, 0x5D9566, 0x528E5C, 0x397E45),
    YELLOW(0xCDAB60, 0xBD902E, 0x8E6C22, 0xCDAB60, 0xC8A352, 0xBD902E),
    BLUE(0x699CBC, 0x397DA7, 0x2B5E7D, 0x699CBC, 0x5B93B6, 0x397DA7),
    SILVER(0xB5C1C7, 0x9EADB5, 0x768288, 0xB5C1C7, 0xAEBBC2, 0x9EADB5);

    public final int switchTop;
    public final int switchFill;
    public final int switchBottom;
    public final int cellTop;
    public final int cellLeft;
    public final int cellFill;

    SelectorColors(int switchTop, int switchFill, int switchBottom, int cellTop, int cellLeft, int cellFill) {
        this.switchTop = 0xFF000000 | switchTop;
        this.switchFill = 0xFF000000 | switchFill;
        this.switchBottom = 0xFF000000 | switchBottom;
        this.cellTop = 0xFF000000 | cellTop;
        this.cellLeft = 0xFF000000 | cellLeft;
        this.cellFill = 0xFF000000 | cellFill;
    }
}
