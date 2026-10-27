package biggerreactorsrework.client;

import biggerreactorsrework.core.util.FluidSurfaceMesh;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.List;

/** Modified 2026-10-27: shared shader-compatible exterior surfaces, native tint alpha and vanilla render layers. */
public final class FluidBoxRenderer {
    public record Volume(FluidStack stack,FluidSurfaceMesh.Box box,int hidden) { }
    public static int mask(Direction side) {
        int axis=switch(side.getAxis()){case X->0;case Y->1;case Z->2;};
        return 1<<(axis*2+(side.getAxisDirection()==Direction.AxisDirection.POSITIVE?1:0));
    }
    public static void draw(PoseStack pose,MultiBufferSource buffers,FluidStack stack,float x0,float y0,float z0,float x1,float y1,float z1,int light,int overlay){
        draw(pose,buffers,List.of(new Volume(stack,new FluidSurfaceMesh.Box(x0,y0,z0,x1,y1,z1),0)),light,overlay);
    }
    public static void draw(PoseStack pose,MultiBufferSource buffers,List<Volume> volumes,int light,int overlay){
        var joined=volumes.stream().filter(v->!v.stack.isEmpty()).map(Volume::box).toList();
        for(var volume:volumes){
            var stack=volume.stack;if(stack.isEmpty() || !volume.box.valid())continue;
            var extension=IClientFluidTypeExtensions.of(stack.getFluid());var texture=extension.getStillTexture(stack);if(texture==null)continue;
            var sprite=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
            int color=extension.getTintColor(stack),alpha=color>>>24;
            // Contained fluids have no artificial transparency; genuinely translucent tints keep their alpha.
            var layer=alpha==255?RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS):RenderType.entityTranslucentCull(InventoryMenu.BLOCK_ATLAS);
            var vertices=buffers.getBuffer(layer);
            int emission=stack.getFluidType().getLightLevel(stack);
            int packedLight=LightTexture.pack(Math.max(LightTexture.block(light),emission),LightTexture.sky(light));
            for(var face:FluidSurfaceMesh.faces(volume.box,joined,volume.hidden))for(int i=0;i<4;i++){
                var point=face.vertex(i);float[] normal=new float[3];normal[face.axis()]=face.positive()?1:-1;
                float u=point[(face.axis()+1)%3],v=point[(face.axis()+2)%3];
                vertices.addVertex(pose.last(),point[0],point[1],point[2])
                        .setColor((color>>16)&255,(color>>8)&255,color&255,alpha)
                        .setUv(sprite.getU(u),sprite.getV(1-v)).setOverlay(overlay).setLight(packedLight)
                        .setNormal(pose.last(),normal[0],normal[1],normal[2]);
            }
        }
    }
    private FluidBoxRenderer() { }
}
