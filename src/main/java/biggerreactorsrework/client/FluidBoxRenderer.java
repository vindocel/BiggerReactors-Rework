package biggerreactorsrework.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/** Shared interior fluid geometry for manifolds and exchanger channels. */
public final class FluidBoxRenderer {
    public static void draw(PoseStack pose,MultiBufferSource buffers,FluidStack stack,float x0,float y0,float z0,float x1,float y1,float z1,int light,int overlay){
        if(stack.isEmpty() || y1<=y0)return;
        var extension=IClientFluidTypeExtensions.of(stack.getFluid());var texture=extension.getStillTexture(stack);if(texture==null)return;
        var sprite=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
        var vertices=buffers.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));int color=extension.getTintColor(stack);
        float[][] corners={{x0,y0,z0},{x1,y0,z0},{x1,y1,z0},{x0,y1,z0},{x0,y0,z1},{x1,y0,z1},{x1,y1,z1},{x0,y1,z1}};
        int[][] faces={{0,3,2,1},{4,5,6,7},{0,4,7,3},{1,2,6,5},{3,7,6,2},{0,1,5,4}};
        float[][] normals={{0,0,-1},{0,0,1},{-1,0,0},{1,0,0},{0,1,0},{0,-1,0}};
        for(int f=0;f<6;f++)for(int v=0;v<4;v++){
            float[] point=corners[faces[f][v]];
            vertices.addVertex(pose.last(),point[0],point[1],point[2]).setColor((color>>16)&255,(color>>8)&255,color&255,220)
                .setUv(v==0||v==3?sprite.getU0():sprite.getU1(),v<2?sprite.getV1():sprite.getV0()).setOverlay(overlay).setLight(light).setNormal(pose.last(),normals[f][0],normals[f][1],normals[f][2]);
        }
    }
    private FluidBoxRenderer() { }
}
