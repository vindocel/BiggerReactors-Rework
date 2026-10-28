package biggerreactorsrework.core.robn;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface ROBNObject {
    default ByteArrayList toROBN() {
        return ROBN.toROBN(toROBNMap());
    }
    
    Map<String, Object> toROBNMap();
    
    default void fromROBN(List<Byte> buf) {
        Object robnObject = ROBN.fromROBN(buf);
        if (!(robnObject instanceof Map)) {
            throw new IllegalArgumentException("Malformed binary");
        }
        // Modified 2026-10-26: reject nonstring compound keys at the decoding boundary.
        if (((Map<?, ?>) robnObject).keySet().stream().anyMatch(key -> !(key instanceof String)))
            throw new IllegalArgumentException("Compound keys must be strings");
        //noinspection unchecked
        fromROBNMap((Map<String, Object>) robnObject);
    }
    
    void fromROBNMap(Map<String, Object> map);
}
