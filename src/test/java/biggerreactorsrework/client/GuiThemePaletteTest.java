package biggerreactorsrework.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GuiThemePaletteTest {
    @Test void neutralPartsUseTheDfmDarkTones() {
        assertEquals(0xFF525252, GuiThemePalette.chrome(0xFFFFFFFF, true));
        assertEquals(0xFF2C2C2C, GuiThemePalette.chrome(0xFF8B8B8B, true));
        assertEquals(0xFF121212, GuiThemePalette.chrome(0xFF373737, true));
        assertEquals(0xFF3F3F3F, GuiThemePalette.chrome(0xFFC6C6C6, true));
    }

    @Test void saturatedEnergyFuelAndStateColorsStayUnchanged() {
        for (int color : new int[]{0xFF36CFEE, 0xFFACDC42, 0xFF26858F, 0xFFFF0000, 0xFFFFFF00}) {
            assertEquals(color, GuiThemePalette.chrome(color, true));
        }
    }

    @Test void lightAndAlphaArePreserved() {
        assertEquals(0x80FFFFFF, GuiThemePalette.chrome(0x80FFFFFF, false));
        assertEquals(0x80525252, GuiThemePalette.chrome(0x80FFFFFF, true));
    }

    @Test void intermediateSaturationTransitionsSmoothly() {
        int shaded = GuiThemePalette.chrome(0xFF8080B3, true);
        assertTrue((shaded & 255) > Math.round(179 * 0.32F));
        assertTrue((shaded & 255) < 179);
        assertEquals(shaded >> 16 & 255, shaded >> 8 & 255);
    }
}
