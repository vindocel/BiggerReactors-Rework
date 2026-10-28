package biggerreactorsrework.core.robn;

import org.junit.jupiter.api.Test;
import java.util.*;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import static org.junit.jupiter.api.Assertions.*;

/** Modified 2026-10-26: hostile declarations are rejected before allocation or recursive decoding. */
class ROBNBoundsTest {
    @Test void validNestedDataRoundTrips() {
        var data=Map.of("rows",List.of(Map.of("key","output","value",42L)),"enabled",true,"fraction",.5);
        assertEquals(data,ROBN.fromROBN(ROBN.toROBN(data)));
    }
    @Test void hugeAndNegativeLengthsAreRejected() {
        for(int count:new int[]{Integer.MAX_VALUE,-1}) {
            var bytes=new ByteArrayList();bytes.add(ROBN.Type.Vector.val);bytes.addAll(ROBN.toROBN(count));bytes.add(ROBN.Type.Int64.val);
            assertThrows(IllegalArgumentException.class,()->ROBN.fromROBN(bytes));
        }
    }
    @Test void excessiveNestingIsRejected() {
        Object nested=true;
        for(int i=0;i<70;i++)nested=Map.of("child",nested);
        var bytes=ROBN.toROBN(nested);
        assertThrows(IllegalArgumentException.class,()->ROBN.fromROBN(bytes));
    }
    @Test void truncatedPrimitiveIsRejected() {
        var bytes=ROBN.toROBN(123456789L);bytes.removeByte(bytes.size()-1);
        assertThrows(IllegalArgumentException.class,()->ROBN.fromROBN(bytes));
    }
    @Test void incompleteMapAndTrailingBytesAreRejected() {
        var map=ROBN.toROBN(Map.of("a",1));map.removeByte(map.size()-1);
        assertThrows(IllegalArgumentException.class,()->ROBN.fromROBN(map));
        var trailing=ROBN.toROBN(true);trailing.add((byte)0);
        assertThrows(IllegalArgumentException.class,()->ROBN.fromROBN(trailing));
    }
}
