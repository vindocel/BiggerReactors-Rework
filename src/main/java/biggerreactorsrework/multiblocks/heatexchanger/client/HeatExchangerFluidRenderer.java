package biggerreactorsrework.multiblocks.heatexchanger.client;

import biggerreactorsrework.client.CoolantVisuals;
import biggerreactorsrework.client.FluidBoxRenderer;
import biggerreactorsrework.core.util.FluidPath;
import biggerreactorsrework.multiblocks.heatexchanger.tiles.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.*;

/** Shared measuring bar along the full circuit, including bends. Modified 2026-10-25. */
public final class HeatExchangerFluidRenderer implements BlockEntityRenderer<HeatExchangerChannelTile> {
    public HeatExchangerFluidRenderer(BlockEntityRendererProvider.Context context) { }
    public void render(HeatExchangerChannelTile tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var root=tile.renderTerminal();if(root==null || tile.coolantPathLength<=0)return;
        var view=tile.CONDENSER?root.primaryView:root.secondaryView;
        // Modified 2026-10-27: one exterior mesh for both phases and all bend pieces.
        var volumes=new java.util.ArrayList<FluidBoxRenderer.Volume>();
        for(boolean product:new boolean[]{false,true}) {
            var span=FluidPath.span(view.inputFill(),view.outputFill(),tile.coolantPathIndex,tile.coolantPathLength,product);
            var stack=CoolantVisuals.stack(product?view.output():view.input(),product?view.outputOrigin():view.inputOrigin());
            if(stack.isEmpty() || span.end()<=span.start())continue;
            for(var box:FluidPath.boxes(tile.coolantEntry,tile.coolantExit,span)) {
                var geometry=new biggerreactorsrework.core.util.FluidSurfaceMesh.Box(box.x0(),box.y0(),box.z0(),box.x1(),box.y1(),box.z1());
                int hidden=0;
                if(tile.coolantPathIndex>0 && touches(geometry,tile.coolantEntry))hidden|=FluidBoxRenderer.mask(tile.coolantEntry);
                if(tile.coolantPathIndex+1<tile.coolantPathLength && touches(geometry,tile.coolantExit))hidden|=FluidBoxRenderer.mask(tile.coolantExit);
                volumes.add(new FluidBoxRenderer.Volume(stack,geometry,hidden));
            }
        }
        FluidBoxRenderer.draw(pose,buffers,volumes,light,overlay);
    }
    private static boolean touches(biggerreactorsrework.core.util.FluidSurfaceMesh.Box box,net.minecraft.core.Direction face) {
        int axis=switch(face.getAxis()){case X->0;case Y->1;case Z->2;};
        return face.getAxisDirection()==net.minecraft.core.Direction.AxisDirection.POSITIVE?box.max(axis)>=.99999f:box.min(axis)<=.00001f;
    }
}
