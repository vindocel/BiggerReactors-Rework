package biggerreactorsrework.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TurbineRotorDamageTest {
    @Test void stoppedAndInvalidSpeedsDoNotHurt() {
        for (double rpm : new double[]{0,-1,Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY}) assertEquals(0,TurbineRotorDamage.perHit(rpm,2200));
        assertEquals(0,TurbineRotorDamage.perHit(100,0));
    }
    @Test void damageUsesApprovedHeartRateAndStaysBounded() {
        assertEquals(1,TurbineRotorDamage.perHit(0.001,2200),0.0001);
        assertEquals(5.5,TurbineRotorDamage.perHit(1100,2200),0.0001);
        assertEquals(10,TurbineRotorDamage.perHit(2200,2200));
        assertEquals(10,TurbineRotorDamage.perHit(9999,2200));
    }
}
