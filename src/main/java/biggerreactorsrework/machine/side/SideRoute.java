package biggerreactorsrework.machine.side;

import java.util.Locale;

/**
 * Route types of every transmission. {@link #NONE} is shared by all transmissions.
 */
public enum SideRoute {
    NONE(null, 0xFFCFCFCF),
    ITEM_INPUT(Transmission.ITEM, 0xFFC9071F),
    ITEM_OUTPUT(Transmission.ITEM, 0xFF366BD0),
    ITEM_INPUT_OUTPUT(Transmission.ITEM, 0xFFA460D9),
    FLUID_INPUT(Transmission.FLUID, 0xFFC9071F),
    /**
     * Energy is always the light blue of the energy bar.
     */
    ENERGY_INPUT(Transmission.ENERGY, 0xFF36CFEE),
    // Modified 2026-10-27: append to preserve serialized ordinals.
    FLUID_OUTPUT(Transmission.FLUID, 0xFF366BD0),
    FLUID_INPUT_OUTPUT(Transmission.FLUID, 0xFFA460D9);

    public static final SideRoute[] VALUES = values();

    private final Transmission transmission;
    /**
     * Color of the route on the configuration panel (ARGB).
     */
    private final int color;

    SideRoute(Transmission transmission, int color) {
        this.transmission = transmission;
        this.color = color;
    }

    public int color() {
        return color;
    }

    public boolean belongsTo(Transmission t) {
        return this == NONE || transmission == t;
    }

    public boolean insertsItems() {
        return this == ITEM_INPUT || this == ITEM_INPUT_OUTPUT;
    }

    public boolean extractsItems() {
        return this == ITEM_OUTPUT || this == ITEM_INPUT_OUTPUT;
    }

    public String translationKey() {
        return "screen.biggerreactorsrework.side_config.route." + name().toLowerCase(Locale.ROOT);
    }
}
