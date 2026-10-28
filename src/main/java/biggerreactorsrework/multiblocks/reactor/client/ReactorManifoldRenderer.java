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
        // Modified 2026-10-27: omit shared column/phase caps instead of drawing coincident surfaces.
        var volumes=new java.util.ArrayList<biggerreactorsrework.client.FluidBoxRenderer.Volume>();
        add(volumes,CoolantVisuals.stack(root.renderCold,""),c.bottom(),c.top(),index,tile.coolantColumnHeight);
        add(volumes,CoolantVisuals.stack(root.renderHot,root.renderOrigin),h.bottom(),h.top(),index,tile.coolantColumnHeight);
        biggerreactorsrework.client.FluidBoxRenderer.draw(pose,buffers,volumes,light,overlay);
    }
    private static void add(java.util.List<biggerreactorsrework.client.FluidBoxRenderer.Volume> volumes,FluidStack stack,float bottom,float top,int index,int height) {
        if(stack.isEmpty() || top<=bottom)return;
        int hidden=0;
        if(index>0 && bottom<=.00001f)hidden|=biggerreactorsrework.client.FluidBoxRenderer.mask(net.minecraft.core.Direction.DOWN);
        if(index+1<height && top>=.99999f)hidden|=biggerreactorsrework.client.FluidBoxRenderer.mask(net.minecraft.core.Direction.UP);
        volumes.add(new biggerreactorsrework.client.FluidBoxRenderer.Volume(stack,
                biggerreactorsrework.core.util.ManifoldFluidGeometry.box(bottom,top),hidden));
    }
}
