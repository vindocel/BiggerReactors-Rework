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
        for(boolean product:new boolean[]{false,true}) {
            var span=FluidPath.span(view.inputFill(),view.outputFill(),tile.coolantPathIndex,tile.coolantPathLength,product);
            var stack=CoolantVisuals.stack(product?view.output():view.input(),product?view.outputOrigin():view.inputOrigin());
            if(stack.isEmpty() || span.end()<=span.start())continue;
            for(var box:FluidPath.boxes(tile.coolantEntry,tile.coolantExit,span))
                FluidBoxRenderer.draw(pose,buffers,stack,box.x0(),box.y0(),box.z0(),box.x1(),box.y1(),box.z1(),light,overlay);
        }
    }
}
