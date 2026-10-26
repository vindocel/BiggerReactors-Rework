package biggerreactorsrework.multiblocks.reactor.client;

import biggerreactorsrework.client.CoolantVisuals;
import biggerreactorsrework.multiblocks.reactor.tiles.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.neoforged.neoforge.fluids.FluidStack;

/** Reads one aggregate terminal snapshot. Manifolds never own duplicate fluid inventories. */
public final class ReactorManifoldRenderer implements BlockEntityRenderer<ReactorManifoldTile> {
    public ReactorManifoldRenderer(BlockEntityRendererProvider.Context context){}
    public void render(ReactorManifoldTile tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(tile.coolantAnchor==null || tile.getLevel()==null || !(tile.getLevel().getBlockEntity(tile.coolantAnchor) instanceof ReactorTerminalTile root))return;
        var assembled=biggerreactorsrework.core.multiblock.IAssemblyStateBlock.ASSEMBLED;
        if(root.getBlockState().hasProperty(assembled) && !root.getBlockState().getValue(assembled))return;
        double cold=Math.clamp(root.renderColdFill,0,1),hot=Math.clamp(root.renderHotFill,0,1);
        int index=tile.getBlockPos().getY()-tile.coolantColumnBottom;
        var c=biggerreactorsrework.core.util.CoolantColumn.slice(cold,hot,index,tile.coolantColumnHeight,false);
        var h=biggerreactorsrework.core.util.CoolantColumn.slice(cold,hot,index,tile.coolantColumnHeight,true);
        cube(pose,buffers,CoolantVisuals.stack(root.renderCold,""),c.bottom(),c.top(),light,overlay);
        cube(pose,buffers,CoolantVisuals.stack(root.renderHot,root.renderOrigin),h.bottom(),h.top(),light,overlay);
    }
    private static void cube(PoseStack pose,MultiBufferSource buffers,FluidStack stack,float bottom,float top,int light,int overlay){
        biggerreactorsrework.client.FluidBoxRenderer.draw(pose,buffers,stack,.18f,bottom,.18f,.82f,top,.82f,light,overlay);
    }
}
