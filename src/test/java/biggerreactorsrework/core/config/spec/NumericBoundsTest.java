package biggerreactorsrework.core.config.spec;

import biggerreactorsrework.core.config.*;
import biggerreactorsrework.core.parsers.Element;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Modified 2026-10-26: exercise actual configuration nodes at both endpoints. */
class NumericBoundsTest {
    public static class Values {
        @ConfigValue(range="[0,2]") public int closed=1;
        @ConfigValue(range="(0,2)") public double open=1;
        @ConfigValue public double signed=-2;
    }
    private SpecObjectNode root() { return new SpecObjectNode(new Values(),"bounds",ConfigType.COMMON,new ConfigOptionsDefaults(ConfigType.COMMON,false,false,false)); }
    @Test void closedIntegerEndpointsAreValid() {
        var node=(SpecIntegralNode)root().subNodes.get("closed");
        assertTrue(node.isValueValid(0));assertTrue(node.isValueValid(1));assertTrue(node.isValueValid(2));
        assertFalse(node.isValueValid(-1));assertFalse(node.isValueValid(3));
    }
    @Test void upperOpenBoundIsCorrectedBelowUpperNotBelowLower() {
        var node=(SpecFloatNode)root().subNodes.get("open");
        assertTrue(node.isValueValid(1));assertFalse(node.isValueValid(0));assertFalse(node.isValueValid(2));
        double corrected=node.correctToValidState(new Element(Element.Type.Number,null,"open",2d)).asDouble();
        assertTrue(corrected>1 && corrected<2);assertTrue(node.isValueValid(corrected));
    }
    @Test void signedRangeAndNonfiniteNumbersAreHandled() {
        var node=(SpecFloatNode)root().subNodes.get("signed");
        assertTrue(node.isValueValid(-10));assertTrue(node.isValueValid(0));
        assertFalse(node.isValueValid(Double.NaN));assertFalse(node.isValueValid(Double.POSITIVE_INFINITY));
        assertEquals(-2d,node.correctToValidState(new Element(Element.Type.Number,null,"signed",Double.NaN)).asDouble());
    }
}
