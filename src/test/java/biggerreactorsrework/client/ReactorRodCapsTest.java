package biggerreactorsrework.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static biggerreactorsrework.client.ReactorRodCaps.*;

class ReactorRodCapsTest {
    @Test void fullColumnHidesBothInternalCapsAndFramePlates() {
        assertEquals(FRAME_UP | FRAME_DOWN | FUEL_UP | FUEL_DOWN | WASTE_UP | WASTE_DOWN,
                mask(16,16,true,16,16,true,16,16));
    }
    @Test void exposedAndPartialSurfacesRemainVisible() {
        assertEquals(0,mask(16,16,false,0,0,false,0,0));
        assertEquals(FRAME_UP | FRAME_DOWN,mask(8,8,true,16,16,true,8,8));
        assertEquals(FRAME_UP | FRAME_DOWN,mask(16,0,true,0,0,true,0,0));
        assertEquals(FRAME_DOWN | FUEL_DOWN,mask(5,0,false,0,0,true,16,0));
    }
    @Test void fuelDoesNotHideTheWasteSurfaceOrViceVersa() {
        assertEquals(FRAME_UP,mask(16,0,true,0,16,false,0,0));
        assertEquals(FRAME_UP,mask(0,16,true,16,0,false,0,0));
    }
}
