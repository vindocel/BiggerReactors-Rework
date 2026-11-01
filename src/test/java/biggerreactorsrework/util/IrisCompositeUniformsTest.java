package biggerreactorsrework.util;

import biggerreactorsrework.compat.iris.IrisCompositeUniforms;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IrisCompositeUniformsTest {
    @Test void onlyAffectedCustomCompositePassUsesExternalSampler() {
        assertTrue(IrisCompositeUniforms.usesCustomSampler("iris:composite", true, "1.11.7+mc26.3"));
        assertFalse(IrisCompositeUniforms.usesCustomSampler("iris:composite", false, "1.11.7+mc26.3"));
        assertFalse(IrisCompositeUniforms.usesCustomSampler("minecraft:blit_screen", true, "1.11.7+mc26.3"));
        assertFalse(IrisCompositeUniforms.usesCustomSampler("iris:composite", true, "1.11.8"));
        assertFalse(IrisCompositeUniforms.usesCustomSampler("iris:composite", true, ""));
    }
}
