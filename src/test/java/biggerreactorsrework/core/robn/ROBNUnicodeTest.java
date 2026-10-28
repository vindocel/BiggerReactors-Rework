package biggerreactorsrework.core.robn;

import org.junit.jupiter.api.Test;
import java.util.*;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import static org.junit.jupiter.api.Assertions.*;

/** Modified 2026-10-27: preserve Unicode on the wire while reading legacy saves. */
class ROBNUnicodeTest {
    @Test void symbolsAndNamesRoundTrip() {
        for(String text:List.of("9 \u00D7 12 \u00D7 9", "Vapor \u2192 \u00C1gua", "S\u00F3dio", "\u6C34", "\uD83D\uDD0B", "a\0b", ""))
            assertEquals(text,ROBN.fromROBN(ROBN.toROBN(text)));
    }
    @Test void unicodeMapKeysAndHomogeneousVectorsRoundTrip() {
        var value=Map.of("\u00C1gua",List.of("\u2192","\u6C34",""));
        assertEquals(value,ROBN.fromROBN(ROBN.toROBN(value)));
    }
    @Test void legacyStringsRemainReadable() {
        var bytes=new ByteArrayList(new byte[]{1,65,(byte)0xE1,0});
        assertEquals("A\u00E1",ROBN.fromROBN(bytes));
    }
    @Test void malformedUtf8IsRejected() {
        var bytes=new ByteArrayList();bytes.add(ROBN.Type.Utf8String.val);
        bytes.addAll(ROBN.toROBN(2));bytes.add((byte)0xC3);bytes.add((byte)0x28);
        assertThrows(IllegalArgumentException.class,()->ROBN.fromROBN(bytes));
    }
    @Test void oversizedUtf8DeclarationIsRejectedBeforeAllocation() {
        var bytes=new ByteArrayList();bytes.add(ROBN.Type.Utf8String.val);bytes.addAll(ROBN.toROBN(Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class,()->ROBN.fromROBN(bytes));
    }
}
