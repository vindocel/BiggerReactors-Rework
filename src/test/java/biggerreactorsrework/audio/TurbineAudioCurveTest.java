package biggerreactorsrework.audio;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TurbineAudioCurveTest {
    @Test void anchorsUseOriginalCadenceAndUnitGain() {
        for (int rpm : new int[]{300, 900, 1800}) {
            var voices = TurbineAudioCurve.voices(rpm);
            assertEquals(1, voices.length);
            assertEquals(rpm, voices[0].anchor());
            assertEquals(1, voices[0].pitch());
            assertEquals(1, voices[0].weight());
        }
    }
    @Test void transitionsRespectNativePitchAndNeverAmplifyTheMix() {
        for (double rpm = 21; rpm < 10000; rpm += .5) {
            var voices = TurbineAudioCurve.voices(rpm);
            assertTrue(voices.length <= 2);
            float sum = 0;
            for (var v : voices) {
                assertTrue(v.pitch() >= .5 && v.pitch() <= 2);
                assertTrue(v.weight() >= 0 && v.weight() <= 1);
                sum += v.weight();
            }
            assertTrue(sum <= 1.000001);
        }
    }
    @Test void invalidAndStoppedRotorsAreSilent() {
        for (double rpm : new double[]{Double.NaN, Double.POSITIVE_INFINITY, -1, 0, 20})
            assertEquals(0, TurbineAudioCurve.voices(rpm).length);
        assertTrue(TurbineAudioCurve.voices(21)[0].weight() < .01);
    }
}
