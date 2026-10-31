package biggerreactorsrework.util;

import biggerreactorsrework.core.util.FluidMeshCache;
import biggerreactorsrework.core.util.FluidSurfaceMesh;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RenderGeometryReuseTest {
    @Test void cachedJoinedFluidFacesAreExactlyTheOriginalMesh() {
        var cache=new FluidMeshCache(8);
        for(int level=1;level<=80;level++) {
            var a=new FluidSurfaceMesh.Box(.1F,0,.1F,.9F,level/80F,.9F);
            var b=new FluidSurfaceMesh.Box(.9F,0,.3F,1,level/80F,.7F);
            var joined=List.of(a,b);
            for(int mask=0;mask<64;mask++) {
                assertEquals(FluidSurfaceMesh.faces(a,joined,mask),cache.faces(a,joined,mask));
                assertSame(cache.faces(a,joined,mask),cache.faces(a,List.copyOf(joined),mask));
                assertTrue(cache.size()<=8);
            }
        }
    }
    @Test void scalarCoordinatesPreserveAllFacePositionsAndWinding() {
        var box=new FluidSurfaceMesh.Box(.1F,.2F,.3F,.7F,.8F,.9F);
        for(var face:FluidSurfaceMesh.faces(box,List.of(box),0))for(int vertex=0;vertex<4;vertex++) {
            float[] old=face.vertex(vertex);
            for(int axis=0;axis<3;axis++)assertEquals(old[axis],face.coordinate(vertex,axis));
        }
    }
}
