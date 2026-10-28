package biggerreactorsrework.client;

import biggerreactorsrework.core.util.CoolantColumn;
import biggerreactorsrework.core.util.ManifoldFluidGeometry;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ManifoldFluidGeometryTest {
    @Test void coolantReachesTheOuterGrilleWithoutCoincidentFaces() {
        var box=ManifoldFluidGeometry.box(0,1);
        assertTrue(box.valid());
        assertTrue(box.x0()>0 && box.x0()<.002);
        assertTrue(box.z0()>0 && box.z0()<.002);
        assertTrue(box.x1()<1 && box.x1()>.998);
        assertTrue(box.z1()<1 && box.z1()>.998);
        assertEquals(box.x0(),1-box.x1(),.000001);
        assertEquals(0,box.y0()); assertEquals(1,box.y1());
    }
    @Test void geometryKeepsColdHotBoundaryAndColumnContinuity() {
        for(int i=0;i<5;i++) {
            var cold=CoolantColumn.slice(.7,.3,i,5,false);
            var hot=CoolantColumn.slice(.7,.3,i,5,true);
            for(var slice:new CoolantColumn.Slice[]{cold,hot}) {
                var box=ManifoldFluidGeometry.box(slice.bottom(),slice.top());
                assertEquals(slice.bottom(),box.y0()); assertEquals(slice.top(),box.y1());
                assertEquals(slice.top()>slice.bottom(),box.valid());
            }
            assertEquals(1,(cold.top()-cold.bottom())+(hot.top()-hot.bottom()),.000001);
        }
    }
}
