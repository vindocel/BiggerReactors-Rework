package biggerreactorsrework.client;

import biggerreactorsrework.core.util.FluidSurfaceMesh;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Modified 2026-10-27: exterior coverage, winding and joined-phase seams without a graphics context. */
class FluidSurfaceMeshTest {
    private FluidSurfaceMesh.Box box(float x0,float x1){return new FluidSurfaceMesh.Box(x0,0,0,x1,1,1);}
    private double area(List<FluidSurfaceMesh.Box> boxes){return boxes.stream().flatMap(b->FluidSurfaceMesh.faces(b,boxes,0).stream()).mapToDouble(FluidSurfaceMesh.Face::area).sum();}
    @Test void isolatedCubeHasSixFaces(){var b=box(0,1);assertEquals(6,FluidSurfaceMesh.faces(b,List.of(b),0).size());}
    @Test void twoPhasesHaveNoInternalCaps(){assertEquals(6,area(List.of(box(0,.4f),box(.4f,1))),.00001);}
    @Test void adjacentSegmentsHaveOnlyExteriorArea(){assertEquals(10,area(List.of(box(0,1),box(1,2))),.00001);}
    @Test void partialContactPreservesUncoveredExterior(){var a=box(0,1);var b=new FluidSurfaceMesh.Box(1,0,0,2,.5f,1);assertEquals(9,area(List.of(a,b)),.00001);}
    @Test void multiplePhaseNeighborsCoverOneCap(){assertEquals(10,area(List.of(box(0,1),new FluidSurfaceMesh.Box(1,0,0,2,.5f,1),new FluidSurfaceMesh.Box(1,.5f,0,2,1,1))),.00001);}
    @Test void normalsMatchOutwardWinding(){
        var b=box(0,1);
        for(var face:FluidSurfaceMesh.faces(b,List.of(b),0)){
            var p=face.vertex(0);var a=face.vertex(1);var c=face.vertex(2);
            double[] x={a[0]-p[0],a[1]-p[1],a[2]-p[2]},y={c[0]-p[0],c[1]-p[1],c[2]-p[2]};
            double[] cross={x[1]*y[2]-x[2]*y[1],x[2]*y[0]-x[0]*y[2],x[0]*y[1]-x[1]*y[0]};
            assertTrue(cross[face.axis()]*(face.positive()?1:-1)>0);
        }
    }
    @Test void invalidBoxesAndMaskedCapsAreOmitted(){var b=box(0,1);assertEquals(4,FluidSurfaceMesh.faces(b,List.of(b),3).size());assertTrue(FluidSurfaceMesh.faces(box(1,1),List.of(),0).isEmpty());}
}
