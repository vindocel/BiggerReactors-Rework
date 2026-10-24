package biggerreactorsrework.machine.side;

import java.util.Locale;

/**
 * Independent transmissions configured per face, one tab each on the side configuration panel.
 */
public enum Transmission {
    ITEM,
    FLUID,
    ENERGY;

    public static final Transmission[] VALUES = values();

    public String translationKey() {
        return "screen.biggerreactorsrework.side_config.transmission." + name().toLowerCase(Locale.ROOT);
    }
}
