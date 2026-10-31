package biggerreactorsrework.core.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Added 2026-10-31: bounded immutable geometry reuse; no texture, tint, lighting or world state is cached. */
public final class FluidMeshCache {
    private record Key(FluidSurfaceMesh.Box box,List<FluidSurfaceMesh.Box> joined,int hidden) {}
    private final int capacity;
    private final Map<Key,List<FluidSurfaceMesh.Face>> entries = new LinkedHashMap<>(16,0.75F,true);
    public FluidMeshCache(int capacity) {
        if(capacity<1)throw new IllegalArgumentException("capacity must be positive");
        this.capacity=capacity;
    }
    public List<FluidSurfaceMesh.Face> faces(FluidSurfaceMesh.Box box,List<FluidSurfaceMesh.Box> joined,int hidden) {
        var key=new Key(box,List.copyOf(joined),hidden);
        var faces=entries.get(key);
        if(faces!=null)return faces;
        faces=List.copyOf(FluidSurfaceMesh.faces(box,joined,hidden));
        if(entries.size()>=capacity)entries.remove(entries.keySet().iterator().next());
        entries.put(key,faces);
        return faces;
    }
    public int size(){return entries.size();}
}
