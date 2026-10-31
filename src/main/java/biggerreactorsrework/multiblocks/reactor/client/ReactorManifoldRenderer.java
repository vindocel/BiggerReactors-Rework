package biggerreactorsrework.multiblocks.reactor.client;

import biggerreactorsrework.client.CoolantVisuals;
import biggerreactorsrework.multiblocks.reactor.tiles.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

/** Reads one aggregate terminal snapshot. Manifolds never own duplicate fluid inventories. */
public final class ReactorManifoldRenderer implements BlockEntityRenderer<ReactorManifoldTile,ReactorManifoldRenderer.State> {
    public static final class State extends BlockEntityRenderState {
        final java.util.List<biggerreactorsrework.client.FluidBoxRenderer.Volume> volumes=new java.util.ArrayList<>();
    }
    @Override public State createRenderState(){return new State();}
    @Override public void submit(State state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){
        biggerreactorsrework.client.FluidBoxRenderer.draw(pose,collector,state.volumes,state.lightCoords,OverlayTexture.NO_OVERLAY);
    }
    public ReactorManifoldRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void extractRenderState(ReactorManifoldTile tile,State state,float partial,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay progress){
        BlockEntityRenderer.super.extractRenderState(tile,state,partial,camera,progress);
        state.volumes.clear();
        if(tile.coolantAnchor==null || tile.getLevel()==null || !(tile.getLevel().getBlockEntity(tile.coolantAnchor) instanceof ReactorTerminalTile root))return;
        var assembled=biggerreactorsrework.core.multiblock.IAssemblyStateBlock.ASSEMBLED;
        if(root.getBlockState().hasProperty(assembled) && !root.getBlockState().getValue(assembled))return;
        double cold=Math.clamp(root.renderColdFill,0,1),hot=Math.clamp(root.renderHotFill,0,1);
        int index=tile.getBlockPos().getY()-tile.coolantColumnBottom;
        var c=biggerreactorsrework.core.util.CoolantColumn.slice(cold,hot,index,tile.coolantColumnHeight,false);
        var h=biggerreactorsrework.core.util.CoolantColumn.slice(cold,hot,index,tile.coolantColumnHeight,true);
        // Modified 2026-10-27: omit shared column/phase caps instead of drawing coincident surfaces.
        var volumes=state.volumes;
        add(volumes,CoolantVisuals.stack(root.renderCold,""),c.bottom(),c.top(),index,tile.coolantColumnHeight);
        add(volumes,CoolantVisuals.stack(root.renderHot,root.renderOrigin),h.bottom(),h.top(),index,tile.coolantColumnHeight);
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
