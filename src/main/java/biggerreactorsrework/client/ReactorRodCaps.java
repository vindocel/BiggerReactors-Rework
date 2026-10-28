package biggerreactorsrework.client;

// Added 2026-10-28: hide only caps internal to a continuous rod/material column.
public final class ReactorRodCaps {
    public static final int FRAME_UP = 1, FRAME_DOWN = 2, FUEL_UP = 4, FUEL_DOWN = 8, WASTE_UP = 16, WASTE_DOWN = 32;
    private ReactorRodCaps() {}
    public static int mask(int fuel, int waste, boolean aboveRod, int aboveFuel, int aboveWaste,
                           boolean belowRod, int belowFuel, int belowWaste) {
        int mask = (aboveRod ? FRAME_UP : 0) | (belowRod ? FRAME_DOWN : 0);
        if (aboveRod && fuel == 16 && aboveFuel > 0) mask |= FUEL_UP;
        if (belowRod && fuel > 0 && belowFuel == 16) mask |= FUEL_DOWN;
        if (aboveRod && waste == 16 && aboveWaste > 0) mask |= WASTE_UP;
        if (belowRod && waste > 0 && belowWaste == 16) mask |= WASTE_DOWN;
        return mask;
    }
}
