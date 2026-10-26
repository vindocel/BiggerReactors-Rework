package biggerreactorsrework.client.guide;

import biggerreactorsrework.util.GuideStructures.Example;
import biggerreactorsrework.multiblocks.reactor.blocks.*;
import biggerreactorsrework.multiblocks.turbine.blocks.*;
import biggerreactorsrework.multiblocks.battery.blocks.*;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import java.util.*;

/** Models and materials on facing pages. */
final class GuideMultiblock {
    private static final String FRAME="frame",WALLS="walls";
    private static final Map<String,ViewState> STATES=new HashMap<>();
    private static final class ViewState {float yaw=35,pitch=28,zoom=1;int layer=-1;boolean rotating=true;long lastFrame;String highlight;}
    record Box(int x,int y,int z,int w,int h,int d,BlockState block,String group){long count(){return (long)w*h*d;}}
    record Preview(String id,int width,int height,int depth,List<Box> boxes,Block glass){}
    record Part(String group,Item item,long count){}
    static final class Spread implements GuideElement {
        final Component title;final Viewer viewer;final PartsPanel parts;
        Spread(Preview p,Component title){this.title=title;var state=STATES.computeIfAbsent(p.id(),k->new ViewState());if(state.layer>=p.height())state.layer=-1;viewer=new Viewer(p,state);parts=new PartsPanel(p,state);}
        public int height(GuideScreen s,int w){return 0;}
        public void render(GuideScreen s,GuiGraphics g,int x,int y,int w,int mx,int my){}
    }
    record DimensionsTitle(Component title) implements GuideElement {
        public int height(GuideScreen s,int w){return 15;}
        public void render(GuideScreen s,GuiGraphics g,int x,int y,int w,int mx,int my){
            s.fixedText(g,title,x,y+2,w,GuideElements.HEADING);g.fill(x,y+12,x+w,y+13,GuideElements.RULE);
        }
    }
    static List<GuideElement> elements(Example e){
        var boxes=new ArrayList<Box>();Block glass=e.id().equals("turbine")?TurbineGlass.INSTANCE:e.id().equals("battery")?BatteryGlass.INSTANCE:e.id().equals("exchanger")?null:ReactorGlass.INSTANCE;
        boolean[] inlet={false};
        e.parts().forEach((pos,placed)->{
            if(placed.isAir())return;
            var block=display(placed,inlet);
            String group=key(block.getBlock());
            if(block.getBlock()==ReactorCasing.INSTANCE||block.getBlock()==TurbineCasing.INSTANCE||block.getBlock()==BatteryCasing.INSTANCE){
                int bounds=(pos.getX()==0||pos.getX()==e.width()-1?1:0)+(pos.getY()==0||pos.getY()==e.height()-1?1:0)+(pos.getZ()==0||pos.getZ()==e.depth()-1?1:0);group=bounds>=2?FRAME:WALLS;
            }
            boxes.add(new Box(pos.getX(),pos.getY(),pos.getZ(),1,1,1,block,group));
        });
        return List.of(new Spread(new Preview(e.id(),e.width(),e.height(),e.depth(),List.copyOf(boxes),glass),Component.translatable("guide.biggerreactorsrework.viewer."+(e.id().equals("minimal")?"minimum":"dimensions"),e.width(),e.height(),e.depth())));
    }
    /**
     * Battery parts as they look in a working battery: charged cells, and one port each way. Manifolds are drawn
     * closed, with their full frame on every face. Modified 2026-10-26.
     */
    private static BlockState display(BlockState block,boolean[] inlet){
        if(block.getBlock()==ReactorManifold.INSTANCE){
            for(var side:List.of(biggerreactorsrework.core.modular.block.IConnectedTexture.Module.NORTH_CONNECTED_PROPERTY,
                    biggerreactorsrework.core.modular.block.IConnectedTexture.Module.SOUTH_CONNECTED_PROPERTY,
                    biggerreactorsrework.core.modular.block.IConnectedTexture.Module.WEST_CONNECTED_PROPERTY,
                    biggerreactorsrework.core.modular.block.IConnectedTexture.Module.EAST_CONNECTED_PROPERTY,
                    biggerreactorsrework.core.modular.block.IConnectedTexture.Module.TOP_CONNECTED_PROPERTY,
                    biggerreactorsrework.core.modular.block.IConnectedTexture.Module.BOTTOM_CONNECTED_PROPERTY))block=block.setValue(side,false);
            return block;
        }
        if(block.getBlock()==BatteryCell.INSTANCE)return block.setValue(biggerreactorsrework.core.multiblock.IAssemblyStateBlock.ASSEMBLED,true).setValue(BatteryCell.CHARGED,true);
        if(block.getBlock()==BatteryEnergyPort.INSTANCE){
            if(inlet[0])return block.setValue(BatteryEnergyPort.PortDirection.PORT_DIRECTION_ENUM_PROPERTY,BatteryEnergyPort.PortDirection.OUTLET);
            inlet[0]=true;
        }
        return block;
    }
    private static String key(Block block){return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).toString();}
    static void reset(){STATES.values().forEach(s->{s.highlight=null;s.lastFrame=0;});}
    static void endFrame(){STATES.values().forEach(s->s.highlight=null);}
    static final class Viewer implements GuideElement {
        private final Preview example;private final ViewState state;private boolean dragging;
        Viewer(Preview p,ViewState s){example=p;state=s;}
        public int height(GuideScreen s,int w){return Math.min(230,s.pageHeight()-50);}
        private int viewHeight(GuideScreen s,int w){return height(s,w)-20;}
        public void render(GuideScreen s,GuiGraphics g,int x,int y,int width,int mx,int my){
            int vh=viewHeight(s,width);g.fill(x,y,x+width,y+vh,0xFF222832);long now=net.minecraft.Util.getMillis();
            if(state.rotating&&!dragging&&state.lastFrame!=0)state.yaw+=(now-state.lastFrame)*0.02f;state.lastFrame=now;
            if(!s.turning()){
                g.enableScissor(x,y,x+width,y+vh);var pose=g.pose();pose.pushPose();pose.translate(x+width/2f,y+vh/2f,200);
                float scale=state.zoom*Math.min(width,vh)/(Math.max(example.width(),Math.max(example.depth(),example.height()))*1.8f);
                pose.scale(scale,-scale,scale);pose.mulPose(Axis.XP.rotationDegrees(state.pitch));pose.mulPose(Axis.YP.rotationDegrees(state.yaw));pose.translate(-example.width()/2f,-example.height()/2f,-example.depth()/2f);
                Lighting.setupFor3DItems();boolean glass=(now/2500)%2==1;
                for(var box:example.boxes()){
                    if(state.layer>=0&&(state.layer<box.y||state.layer>=box.y+box.h))continue;
                    int by=state.layer<0?box.y:state.layer,bh=state.layer<0?box.h:1;
                    var block=WALLS.equals(box.group)&&example.glass()!=null&&glass?example.glass().defaultBlockState():box.block;
                    pose.pushPose();pose.translate(box.x,by,box.z);pose.scale(box.w,bh,box.d);
                    Minecraft.getInstance().getBlockRenderer().renderSingleBlock(block,pose,g.bufferSource(),LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
                    // Coolant inside, as in a working reactor; the frame alone shows only its outer faces.
                    if(block.getBlock()==ReactorManifold.INSTANCE)biggerreactorsrework.client.FluidBoxRenderer.draw(pose,g.bufferSource(),
                            new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000),.18f,0f,.18f,.82f,1f,.82f,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
                    pose.popPose();
                }g.flush();
                if(state.highlight!=null)for(var box:example.boxes()){
                    if(!box.group.equals(state.highlight)||state.layer>=0&&(state.layer<box.y||state.layer>=box.y+box.h))continue;
                    int by=state.layer<0?box.y:state.layer,bh=state.layer<0?box.h:1;
                    LevelRenderer.renderLineBox(pose,g.bufferSource().getBuffer(RenderType.lines()),new AABB(box.x-0.015,by-0.015,box.z-0.015,box.x+box.w+0.015,by+bh+0.015,box.z+box.d+0.015),1f,0.85f,0.1f,1f);
                }g.flush();pose.popPose();Lighting.setupForFlatItems();g.disableScissor();
            }
            int bw=(width-4)/2;
            var layer=state.layer<0?Component.translatable("guide.biggerreactorsrework.viewer.layer_all"):Component.translatable("guide.biggerreactorsrework.viewer.layer",state.layer+1);
            s.textButton(g,x,y+vh+4,bw,14,layer,mx,my,true);s.textButton(g,x+bw+4,y+vh+4,width-bw-4,14,Component.translatable("guide.biggerreactorsrework.viewer."+(state.rotating?"rotate_on":"rotate_off")),mx,my,true);
        }
        public boolean click(GuideScreen s,double mx,double my,int x,int y,int width,int button){
            int vh=viewHeight(s,width);if(mx<x||mx>=x+width||my<y)return false;
            if(my>=y+vh+4&&my<y+vh+18){if(mx<x+(width-4)/2)state.layer=(state.layer+2)%(example.height()+1)-1;else state.rotating=!state.rotating;s.playClick();return true;}
            if(my<y+vh){dragging=true;return true;}return false;
        }
        public boolean drag(GuideScreen s,double dx,double dy){if(!dragging)return false;state.yaw+=(float)dx*1.5f;state.pitch=net.minecraft.util.Mth.clamp(state.pitch+(float)dy*1.2f,-10,85);return true;}
        void zoom(double direction){state.zoom=net.minecraft.util.Mth.clamp(state.zoom*(float)Math.pow(1.2,direction),0.75f,12f);}
        void release(){dragging=false;}
    }
    static final class PartsPanel implements GuideElement {
        private final Preview example;private final ViewState state;private final List<Part> parts;
        PartsPanel(Preview p,ViewState s){example=p;state=s;var counts=new LinkedHashMap<String,Part>();for(var box:p.boxes()){var old=counts.get(box.group);counts.put(box.group,new Part(box.group,box.block.getBlock().asItem(),box.count()+(old==null?0:old.count)));}parts=List.copyOf(counts.values());}
        public int height(GuideScreen s,int width){return Math.min(parts.size()*24,s.pageHeight()-28);}
        private int row(GuideScreen s,int width){return height(s,width)/Math.max(1,parts.size());}
        void prepareHover(GuideScreen s,int x,int y,int width,int mx,int my){int row=row(s,width),index=(my-y)/Math.max(1,row);if(mx>=x&&mx<x+width&&my>=y&&index<parts.size())state.highlight=parts.get(index).group;}
        public void render(GuideScreen s,GuiGraphics g,int x,int y,int width,int mx,int my){
            int row=row(s,width);for(int i=0;i<parts.size();i++){
                var part=parts.get(i);int py=y+i*row;boolean walls=WALLS.equals(part.group)&&example.glass()!=null,hover=part.group.equals(state.highlight);
                var stack=new ItemStack(walls&&(net.minecraft.Util.getMillis()/2500)%2==1?example.glass().asItem():part.item);
                if(hover)g.fill(x,py,x+width,py+row,0x40FFE033);s.itemSlot(g,stack,x,py,mx,my);
                Component name=walls?Component.translatable("guide.biggerreactorsrework.viewer.walls",part.count):FRAME.equals(part.group)?Component.translatable("guide.biggerreactorsrework.viewer.frame",part.count):Component.literal(part.count+"\u00d7 ").append(stack.getHoverName());
                s.fixedText(g,name,x+23,py+5,width-23,hover?0xFF8A6500:GuideElements.BODY);
            }
        }
    }
}
