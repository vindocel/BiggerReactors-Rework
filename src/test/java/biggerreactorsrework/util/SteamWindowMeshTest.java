package biggerreactorsrework.util;

import biggerreactorsrework.core.util.FluidSurfaceMesh;
import biggerreactorsrework.core.util.SteamWindowMesh;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

/** Added 2026-10-31: regression coverage for window coverage, holes, winding and animated steam. */
class SteamWindowMeshTest {
    @Test void allSixWindowWallsMergeWithoutCrossingTheInteriorOrCoveringSolidHoles() {
        var box = new FluidSurfaceMesh.Box(-4,-4,-4,4,4,4);
        var glass = new ArrayList<Integer>();
        for (int axis=0;axis<3;axis++) for(int side=0;side<2;side++) for(int u=-4;u<4;u++) for(int v=-4;v<4;v++) {
            if(u==0 && v==0) continue; // A port/casing in the window must stay clear.
            int[] p=new int[3];p[axis]=side==0?-5:4;p[(axis+1)%3]=u;p[(axis+2)%3]=v;
            for(int value:p)glass.add(value);
        }
        var faces=SteamWindowMesh.faces(box,glass.stream().mapToInt(Integer::intValue).toArray());
        assertTrue(faces.size()<6*63,"contiguous window cells were not merged");
        assertEquals(6*63,faces.stream().mapToDouble(FluidSurfaceMesh.Face::area).sum());
        for(var face:faces) {
            assertEquals(face.positive()?4-SteamWindowMesh.INSET:-4+SteamWindowMesh.INSET,face.plane());
            assertFalse(face.u0()<=.5 && face.u1()>.5 && face.v0()<=.5 && face.v1()>.5,"film covers a solid hole");
            assertEquals(Math.floorDiv((int)face.u0(),4),Math.floorDiv((int)face.u1()-1,4));
            assertEquals(Math.floorDiv((int)face.v0(),4),Math.floorDiv((int)face.v1()-1,4));
            float[] a=face.vertex(0),b=face.vertex(1),c=face.vertex(2);
            int u=(face.axis()+1)%3,v=(face.axis()+2)%3;
            float normal=(b[u]-a[u])*(c[v]-a[v])-(b[v]-a[v])*(c[u]-a[u]);
            assertTrue(face.positive()?normal>0:normal<0,"film must face out, towards the external viewer");
        }
    }
    @Test void emptyMalformedAndInteriorGlassNeverCreateFilms() {
        var box=new FluidSurfaceMesh.Box(0,0,0,4,4,4);
        assertTrue(SteamWindowMesh.faces(box,new int[0]).isEmpty());
        assertTrue(SteamWindowMesh.faces(box,new int[]{0,1}).isEmpty());
        assertTrue(SteamWindowMesh.faces(box,new int[]{1,1,1,-1,-1,-1}).isEmpty());
    }
    @Test void steamAnimationLoopsAndActuallyChangesAcrossFrames() throws Exception {
        assertEquals(0,SteamWindowMesh.frame(0));
        assertEquals(1,SteamWindowMesh.frame(4));
        assertEquals(0,SteamWindowMesh.frame(256));
        try(var input=java.nio.file.Files.newInputStream(java.nio.file.Path.of("src/main/resources/assets/biggerreactorsrework/textures/effect/turbine_steam.png"))) {
            var image=javax.imageio.ImageIO.read(input);
            assertEquals(32,image.getWidth());assertEquals(32*64,image.getHeight());
            int differences=0,maxStep=0;
            for(int frame=0;frame<64;frame++)for(int y=0;y<32;y++)for(int x=0;x<32;x++) {
                int color=image.getRGB(x,frame*32+y);
                assertEquals(0xFFFFFF,color&0xFFFFFF,"steam must be white rather than a dark mask");
                int alpha=color>>>24,next=image.getRGB(x,((frame+1)%64)*32+y)>>>24;
                assertTrue(alpha>=140 && alpha<=255);
                if(alpha!=next)differences++;
                maxStep=Math.max(maxStep,Math.abs(alpha-next));
            }
            assertTrue(differences>1000,"steam texture is static");
            assertTrue(maxStep<16,"animation has a jump, including at the loop boundary");
        }
    }
}
