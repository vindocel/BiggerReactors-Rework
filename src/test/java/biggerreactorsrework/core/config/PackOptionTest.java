package biggerreactorsrework.core.config;

import biggerreactorsrework.core.config.spec.SpecValueNode;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Added 2026-11-01: reject unsafe edits before they reach files or live configuration fields. */
class PackOptionTest {
    public static class Values {
        @ConfigValue(range="[1,)") public int size = 3;
        @ConfigValue(range="[0,1]") public double efficiency = .5;
        @ConfigValue public boolean enabled = true;
        @ConfigValue(range="(0,)") public long capacity = 3_200_000_000L;
        @ConfigValue public float ratio = 1f;
    }
    private PackConfiguration.Option option(String key) {
        var registration = new ConfigRegistration(new Values(), "test", "test", "", "", ConfigFormat.TOML,
                ConfigType.SERVER, ConfigType.SERVER, false, List.of(), List.of());
        return new PackConfiguration.Option(key, registration, (SpecValueNode) registration.rootConfigSpecNode.subNodes.get(key));
    }
    @Test void rejectsIntegerOverflowAndFractionalCounts() {
        var option = option("size");
        assertThrows(IllegalArgumentException.class, () -> option.checked(Long.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> option.checked(3.5));
        assertThrows(IllegalArgumentException.class, () -> option.checked(0));
        assertEquals("3", option.current());
    }
    @Test void rejectsNonfiniteAndOutOfRangeFractions() {
        var option = option("efficiency");
        for (var bad : new Object[]{Double.NaN, Double.POSITIVE_INFINITY, -0.1, 1.1, "not a number"})
            assertThrows(IllegalArgumentException.class, () -> option.checked(bad));
        assertEquals("0", option.checked(0)); assertEquals("1", option.checked(1));
    }
    @Test void booleanCannotSilentlyTurnArbitraryInputIntoFalse() {
        var option = option("enabled");
        assertThrows(IllegalArgumentException.class, () -> option.checked("disabled"));
        assertThrows(IllegalArgumentException.class, () -> option.checked(1));
        assertEquals("false", option.checked(false));
    }
    @Test void longCapacityRetainsValuesBeyondIntegerRange() {
        var option = option("capacity");
        assertEquals("9000000000", option.checked(9_000_000_000L));
        assertEquals("3200000000", option.current());
    }
    @Test void nativeFileAndRemoteTreeValidationCannotNarrowOversizedOrFractionalIntegers() {
        var option=option("size");
        for(var bad : new Number[]{Long.MAX_VALUE, 3.5, Double.NaN}) {
            var incoming=new biggerreactorsrework.core.parsers.Element(biggerreactorsrework.core.parsers.Element.Type.Number,null,"size",bad);
            option.node().writeElement(option.node().correctToValidState(incoming));
            assertEquals("3",option.current());
        }
    }
    @Test void nativeTreeValidationRejectsFiniteDoubleThatWouldOverflowFloat() {
        var option=option("ratio");
        var incoming=new biggerreactorsrework.core.parsers.Element(biggerreactorsrework.core.parsers.Element.Type.Number,null,"ratio",1e100);
        option.node().writeElement(option.node().correctToValidState(incoming));
        assertEquals("1.0",option.current());
    }
}
