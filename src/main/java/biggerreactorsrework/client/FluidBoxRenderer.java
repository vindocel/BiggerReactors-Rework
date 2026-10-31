package biggerreactorsrework.client;

import biggerreactorsrework.core.util.FluidSurfaceMesh;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.List;

/** Modified 2026-10-31: shared exterior surfaces, native tint alpha and vanilla render layers on the submit pipeline. */
public final class FluidBoxRenderer {
    private static final ThreadLocal<biggerreactorsrework.core.util.FluidMeshCache> MESHES =
            ThreadLocal.withInitial(() -> new biggerreactorsrework.core.util.FluidMeshCache(256));
    public record Volume(FluidStack stack,FluidSurfaceMesh.Box box,int hidden) { }
    public static int mask(Direction side) {
        int axis=switch(side.getAxis()){case X->0;case Y->1;case Z->2;};
        return 1<<(axis*2+(side.getAxisDirection()==Direction.AxisDirection.POSITIVE?1:0));
    }
    public static void draw(PoseStack pose,SubmitNodeCollector collector,FluidStack stack,float x0,float y0,float z0,float x1,float y1,float z1,int light,int overlay){
        draw(pose,collector,List.of(new Volume(stack,new FluidSurfaceMesh.Box(x0,y0,z0,x1,y1,z1),0)),light,overlay);
    }
    public static void draw(PoseStack pose,SubmitNodeCollector collector,List<Volume> volumes,int light,int overlay){
        var joined=volumes.stream().filter(v->!v.stack.isEmpty()).map(Volume::box).toList();
        for(var volume:volumes){
            var stack=volume.stack;if(stack.isEmpty() || !volume.box.valid())continue;
            var sprite=FluidSprites.still(stack);
            int color=FluidSprites.tint(stack),alpha=color>>>24;
            // Contained fluids have no artificial transparency; genuinely translucent tints keep their alpha.
            var layer=alpha==255?RenderTypes.entityCutoutCull(TextureAtlas.LOCATION_BLOCKS):RenderTypes.entityTranslucentCull(TextureAtlas.LOCATION_BLOCKS);
            int emission=stack.getFluidType().getLightLevel(stack);
            int packedLight=LightCoordsUtil.pack(Math.max(LightCoordsUtil.block(light),emission),LightCoordsUtil.sky(light));
            var faces=MESHES.get().faces(volume.box,joined,volume.hidden);
            collector.submitCustomGeometry(pose,layer,(last,vertices)->{
                for(var face:faces)for(int i=0;i<4;i++){
                    float normal=face.positive()?1:-1;
                    float u=face.coordinate(i,(face.axis()+1)%3),v=face.coordinate(i,(face.axis()+2)%3);
                    vertices.addVertex(last,face.coordinate(i,0),face.coordinate(i,1),face.coordinate(i,2))
                            .setColor((color>>16)&255,(color>>8)&255,color&255,alpha)
                            .setUv(sprite.getU(u),sprite.getV(1-v)).setOverlay(overlay).setLight(packedLight)
                            .setNormal(last,face.axis()==0?normal:0,face.axis()==1?normal:0,face.axis()==2?normal:0);
                }
            });
        }
    }
    private FluidBoxRenderer() { }
}
