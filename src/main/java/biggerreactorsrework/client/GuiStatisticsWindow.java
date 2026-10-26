package biggerreactorsrework.client;

import biggerreactorsrework.core.client.gui.GuiSync;
import biggerreactorsrework.core.client.gui.screens.ModScreen;
import biggerreactorsrework.registries.CoolantComponents;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.fluids.FluidStack;

/** Local, read-only, scrollable machine statistics. Modified 2026-10-26: grouped by section. */
public final class GuiStatisticsWindow {
    private final ModScreen<?> screen;
    private boolean open,rendering,consumeRelease,dragging;
    private int scroll,maxScroll,offsetX,offsetY;
    private double dragX,dragY;
    public GuiStatisticsWindow(ModScreen<?> screen) {this.screen=screen;}
    public boolean open(){return screen.hasStatistics() && open;}
    public boolean rendering(){return rendering;}
    private int width(){return Math.min(260,Math.max(140,screen.width-16));}
    private int height(){return Math.min(236,Math.max(72,screen.height-20));}
    private int x(){return Mth.clamp((screen.width-width())/2+offsetX,4,Math.max(4,screen.width-width()-4));}
    private int y(){return Mth.clamp((screen.height-height())/2+offsetY,4,Math.max(4,screen.height-height()-4));}
    /** The area the open window covers, kept free of other overlays. */
    public net.minecraft.client.renderer.Rect2i area(){return new net.minecraft.client.renderer.Rect2i(x()-4,y()-4,width()+8,height()+8);}
    public static final int TAB_PITCH=GuiFrame.TAB_BUTTON+2;
    private int tabY(){return screen.settingsTabY()+TAB_PITCH;}
    private boolean onTab(double mx,double my){return screen.hasStatistics() && mx>=screen.settingsTabX() && mx<screen.settingsTabX()+GuiFrame.TAB_SIZE && my>=tabY()+GuiFrame.TAB_BUTTON_TOP-1 && my<tabY()+GuiFrame.TAB_SIZE;}
    public boolean contains(double mx,double my){return onTab(mx,my) || covers(mx,my);}
    /** Whether the open window is under the point. */
    public boolean covers(double mx,double my){return open() && area().contains((int)Math.floor(mx),(int)Math.floor(my));}
    public void renderTab(GuiGraphics g,int mx,int my){
        if(!screen.hasStatistics())return;
        int x=screen.settingsTabX(),y=tabY();
        GuiArt.tabButton(g,x,y,true,onTab(mx,my),open(),screen.getFrame());
        GuiIcon.STATISTICS.draw(g,GuiFrame.tabButtonX(x,true)+(GuiFrame.TAB_BUTTON-GuiIcon.STATISTICS.width())/2,
                y+GuiFrame.TAB_BUTTON_TOP+(GuiFrame.TAB_BUTTON-GuiIcon.STATISTICS.height())/2);
    }
    private static Component fluid(String id,String origin){
        var key=ResourceLocation.tryParse(id);
        if(key==null || !BuiltInRegistries.FLUID.containsKey(key))return Component.translatable("screen.biggerreactorsrework.statistics.status.awaiting_fluid");
        var stack=new FluidStack(BuiltInRegistries.FLUID.get(key),1);
        if(!origin.isEmpty()) stack.applyComponents(DataComponentPatch.builder().set(CoolantComponents.ORIGIN.get(),origin).build());
        return stack.getHoverName();
    }
    public static Component value(String value){
        if(value.startsWith("@status:"))return Component.translatable("screen.biggerreactorsrework.statistics.status."+value.substring(8));
        if(value.startsWith("@pair:")){
            String[] parts=value.substring(6).split("\\|",-1);
            if(parts.length>=3)return Component.empty().append(fluid(parts[0],parts.length>=4?parts[3]:"").copy().withColor(0xFF3A78C0))
                    .append(" → ").append(fluid(parts[1],parts[2]).copy().withColor(0xFFC8582A));
        }
        return Component.literal(value);
    }
    private static final int ROW=11,HEADING=15,BAR_ROW=15;
    private static final java.util.Map<String,String> SECTIONS=new java.util.HashMap<>();
    private static final java.util.List<String> SECTION_ORDER=java.util.List.of("structure","fuel","fluids","performance");
    static {
        for(var key:java.util.List.of("dimensions","fuel_rods","rotor_shafts","condenser_channels","evaporator_channels","contacts","heat_path"))SECTIONS.put(key,"structure");
        for(var key:java.util.List.of("fuel_capacity","fuel_stored","waste","fuel_temperature"))SECTIONS.put(key,"fuel");
        for(var key:java.util.List.of("pair","cold_capacity","primary_capacity","water_capacity","cold_stored","hot_stored","water_stored","steam_stored",
                "input_current","output_current","water_discarded","vent","primary_temperature","secondary_temperature","thermal_storage_change"))SECTIONS.put(key,"fluids");
        for(var key:java.util.List.of("burn_current","burn_potential","manual_limit","conversion_current","conversion_potential","steam_current","steam_potential",
                "water_potential","power_current","power_potential","rpm","efficiency","flow_selected","flow_structural","flow_efficient"))SECTIONS.put(key,"performance");
    }
    private static final java.util.regex.Pattern AMOUNT=java.util.regex.Pattern.compile("(-?[0-9][0-9.,]*) (mB/t|mB|FE/t)");

    /** One drawn line: a section heading, or a label with its value and, for current rates, how close they are to the potential. */
    private record Line(Component label,Component value,boolean heading,double progress){
        int height(){return heading?HEADING:progress>=0?BAR_ROW:ROW;}
    }

    private static double number(String text){
        try{return Double.parseDouble(text.replace(",",""));}catch(NumberFormatException e){return Double.NaN;}
    }
    private static double amount(String value){
        var m=AMOUNT.matcher(value);return m.matches()?number(m.group(1)):Double.NaN;
    }
    private static Component pretty(String value){
        var m=AMOUNT.matcher(value);
        if(!m.matches())return value(value);
        double v=number(m.group(1));
        return Component.literal(switch(m.group(2)){
            case "mB/t"->biggerreactorsrework.core.client.gui.RenderHelper.formatValue(v/1000.0,"B/t",true);
            case "mB"->biggerreactorsrework.core.client.gui.RenderHelper.formatValue(v/1000.0,"B",true);
            default->biggerreactorsrework.core.client.gui.RenderHelper.formatValue(v,"FE/t");
        });
    }
    private static int statusColor(String status){
        return switch(status){
            case "@status:normal","@status:full_inlet_tanks"->0xFF3FA34D;
            case "@status:inactive","@status:calculating","@status:awaiting_fluid"->0xFF8A8A8A;
            default->0xFFD08A22;
        };
    }
    private static int mutedColor(){return GuiTheme.dark()?0xFF9A9A9A:0xFF6A6A6A;}
    private static int accentColor(){return GuiTheme.dark()?0xFF7CC4CC:0xFF2A6E78;}

    private java.util.List<Line> lines(java.util.List<java.util.Map<String,Object>> rows){
        var values=new java.util.HashMap<String,String>();
        for(var row:rows)values.putIfAbsent(String.valueOf(row.get("key")),String.valueOf(row.get("value")));
        var grouped=new java.util.LinkedHashMap<String,java.util.List<java.util.Map<String,Object>>>();
        for(var section:SECTION_ORDER)grouped.put(section,new java.util.ArrayList<>());
        grouped.put("other",new java.util.ArrayList<>());
        for(var row:rows){
            String key=String.valueOf(row.get("key"));
            if(key.equals("status")||key.equals("reference"))continue;
            grouped.get(SECTIONS.getOrDefault(key,"other")).add(row);
        }
        var lines=new java.util.ArrayList<Line>();
        for(var section:grouped.entrySet()){
            if(section.getValue().isEmpty())continue;
            lines.add(new Line(Component.translatable("screen.biggerreactorsrework.statistics.section."+section.getKey()),Component.empty(),true,-1));
            for(var row:section.getValue()){
                String key=String.valueOf(row.get("key")),value=String.valueOf(row.get("value"));
                double progress=-1;
                if(key.endsWith("_current")){
                    double potential=amount(values.getOrDefault(key.substring(0,key.length()-8)+"_potential",""));
                    double current=amount(value);
                    if(potential>0 && !Double.isNaN(current))progress=Mth.clamp(current/potential,0,1);
                }
                lines.add(new Line(Component.translatable("screen.biggerreactorsrework.statistics."+key),pretty(value),false,progress));
            }
        }
        return lines;
    }

    public void render(GuiGraphics g,int mx,int my){
        if(!screen.hasStatistics())return;
        rendering=true;
        g.pose().pushPose();g.flush();g.pose().translate(0,0,850);
        try{
            if(!open()){if(onTab(mx,my))g.renderTooltip(screen.getFont(),Component.translatable("screen.biggerreactorsrework.statistics.title"),mx,my);return;}
            var font=screen.getFont();
            int x=x(),y=y(),w=width(),h=height(),text=GuiTheme.textColor();
            GuiArt.panel(g,x,y,w,h,screen.getFrame());
            g.drawString(font,Component.translatable("screen.biggerreactorsrework.statistics.title"),x+8,y+8,text,false);
            GuiArt.button(g,x+w-20,y+5,12,12,mx>=x+w-20 && mx<x+w-8 && my>=y+5 && my<y+17,GuiIcon.CLOSE,screen.getFrame());
            var rows=GuiSync.statistics((GuiSync.IGUIPacketProvider)screen.getMenu());
            String status=null,reference=null;
            for(var row:rows){
                if("status".equals(row.get("key")))status=String.valueOf(row.get("value"));
                if("reference".equals(row.get("key")))reference=String.valueOf(row.get("value"));
            }
            int left=x+8,right=x+w-10,top=y+22;
            // Condition of the machine, always visible above the scrolling list.
            var statusText=status==null?Component.translatable("screen.biggerreactorsrework.statistics.status.calculating"):value(status);
            var referenceLines=reference==null?java.util.List.<net.minecraft.util.FormattedCharSequence>of()
                    :font.split(Component.translatable("screen.biggerreactorsrework.statistics.reference").append(": ").append(value(reference)),right-left-10);
            int cardHeight=14+referenceLines.size()*10;
            int color=status==null?0xFF8A8A8A:statusColor(status);
            g.fill(left,top,right,top+cardHeight,GuiTheme.dark()?0x22FFFFFF:0x22000000);
            g.fill(left,top,left+2,top+cardHeight,color);
            g.fill(left+6,top+4,left+11,top+9,color);
            GuiArt.scrollingText(g,font,statusText,left+14,top+3,right-left-18,color);
            for(int i=0;i<referenceLines.size();i++)g.drawString(font,referenceLines.get(i),left+6,top+14+i*10,mutedColor(),false);
            int listTop=top+cardHeight+4,listBottom=y+h-7;
            var lines=lines(rows);
            int total=0;for(var line:lines)total+=line.height();
            maxScroll=Math.max(0,total-(listBottom-listTop));scroll=Mth.clamp(scroll,0,maxScroll);
            int rowRight=maxScroll>0?right-4:right;
            g.enableScissor(left,listTop,x+w-6,listBottom);
            int ly=listTop-scroll,stripe=0;
            for(var line:lines){
                int lh=line.height();
                if(ly+lh>=listTop && ly<=listBottom){
                    if(line.heading()){
                        g.drawString(font,line.label(),left+1,ly+4,accentColor(),false);
                        g.fill(left,ly+13,rowRight,ly+14,(accentColor()&0x00FFFFFF)|0x80000000);
                    }else{
                        if(stripe%2==1)g.fill(left,ly,rowRight,ly+lh,GuiTheme.dark()?0x14FFFFFF:0x14000000);
                        int valueWidth=Math.min(font.width(line.value()),(rowRight-left)/2);
                        int labelWidth=rowRight-left-valueWidth-10;
                        GuiArt.scrollingText(g,font,line.label(),left+3,ly+2,labelWidth,text);
                        GuiArt.scrollingText(g,font,line.value(),rowRight-2-valueWidth,ly+2,valueWidth,text);
                        if(line.progress()>=0){
                            int barLeft=left+3,barRight=rowRight-2,barY=ly+11;
                            double p=line.progress();
                            int fill=p>=.9?0xFF3FA34D:p>=.5?0xFFD0A030:0xFFC0503A;
                            g.fill(barLeft,barY,barRight,barY+2,GuiTheme.dark()?0x40FFFFFF:0x40000000);
                            g.fill(barLeft,barY,barLeft+(int)Math.round((barRight-barLeft)*p),barY+2,fill);
                        }
                    }
                }
                stripe=line.heading()?0:stripe+1;
                ly+=lh;
            }
            g.disableScissor();
            if(maxScroll>0){
                int span=listBottom-listTop;
                int thumb=Math.max(10,span*span/Math.max(1,total));
                int thumbTop=listTop+(span-thumb)*scroll/maxScroll;
                g.fill(x+w-8,listTop,x+w-6,listBottom,GuiTheme.dark()?0x30FFFFFF:0x30000000);
                g.fill(x+w-8,thumbTop,x+w-6,thumbTop+thumb,text);
            }
        }finally{g.flush();g.pose().popPose();rendering=false;}
    }
    public boolean mousePressed(double mx,double my,int button){
        if(!contains(mx,my))return false;
        consumeRelease=true;
        if(button==0){
            if(onTab(mx,my)){open=!open;dragging=false;}
            else if(mx>=x()+width()-20 && mx<x()+width()-8 && my>=y()+5 && my<y()+17)open=false;
            else if(mx>=x() && mx<x()+width()-22 && my>=y() && my<y()+20){dragging=true;dragX=mx-x();dragY=my-y();}
        }
        return true;
    }
    public boolean release(){boolean consumed=consumeRelease;consumeRelease=false;dragging=false;return consumed;}
    public boolean drag(double mx,double my){
        if(!open() || !dragging)return false;
        if(dragging){
            offsetX=(int)Math.round(mx-dragX)-(screen.width-width())/2;
            offsetY=(int)Math.round(my-dragY)-(screen.height-height())/2;
            offsetX=x()-(screen.width-width())/2;offsetY=y()-(screen.height-height())/2;
        }
        return true;
    }
    public boolean scroll(double mx,double my,double delta){if(!covers(mx,my))return false;scroll=Mth.clamp(scroll-(int)(delta*22),0,maxScroll);return true;}
    public boolean key(int code){if(!open())return false;if(code==256)open=false;else if(code==264)scroll=Mth.clamp(scroll+22,0,maxScroll);else if(code==265)scroll=Mth.clamp(scroll-22,0,maxScroll);return true;}
}
