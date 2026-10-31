package biggerreactorsrework.core.util;

import net.minecraft.core.Direction;
import java.util.*;

/** A measuring bar following a pipe from inlet to outlet, independent of world orientation.
 * Modified 2026-10-26: the two phases cover every segment whenever the circuit contains fluid.
 */
public final class FluidPath {
    // Modified 2026-10-29: channel frames have a two-pixel wall; fluid meets its inner edge with 0.02 px clearance.
    private static final float MIN = 2.02f / 16, MAX = 1 - MIN;
    public record Span(float start,float end) { }
    public record Box(float x0,float y0,float z0,float x1,float y1,float z1) { }
    public static Span span(double input,double output,int index,int length,boolean product) {
        input=Double.isFinite(input)?Math.clamp(input,0,1):0;
        output=Double.isFinite(output)?Math.clamp(output,0,1):0;
        double scale=input+output>0?input+output:1;
        double start=product?length*(1-output/scale):0;
        double end=product?length:length*input/scale;
        return new Span((float)Math.clamp(start-index,0,1),(float)Math.clamp(end-index,0,1));
    }
    public static List<Box> boxes(Direction entry,Direction exit,Span span) {
        if(entry==exit || span.end<=span.start)return List.of();
        var result=new ArrayList<Box>(3);
        if(entry.getAxis()==exit.getAxis()) {
            add(result,new float[]{MIN,MIN,MIN,MAX,MAX,MAX},entry.getOpposite(),0,1,span,true);
        } else {
            arm(result,entry,entry.getOpposite(),0,MIN,span);
            add(result,new float[]{MIN,MIN,MIN,MAX,MAX,MAX},entry.getOpposite(),MIN,MAX,span,false);
            arm(result,exit,exit,MAX,1,span);
        }
        return result;
    }
    private static int axis(Direction direction){return switch(direction.getAxis()){case X->0;case Y->1;case Z->2;};}
    private static void arm(List<Box> result,Direction face,Direction travel,float from,float to,Span span){
        float[] bounds={MIN,MIN,MIN,MAX,MAX,MAX};int axis=axis(face);
        if(face.getAxisDirection()==Direction.AxisDirection.POSITIVE){bounds[axis]=MAX;bounds[axis+3]=1;}else{bounds[axis]=0;bounds[axis+3]=MIN;}
        add(result,bounds,travel,from,to,span,false);
    }
    private static void add(List<Box> result,float[] b,Direction travel,float from,float to,Span span,boolean straight){
        float low=Math.max(from,span.start),high=Math.min(to,span.end);if(high<=low)return;
        int axis=axis(travel);if(straight){b[axis]=0;b[axis+3]=1;}
        float min=b[axis],size=b[axis+3]-min;
        float a=(low-from)/(to-from),z=(high-from)/(to-from);
        if(travel.getAxisDirection()==Direction.AxisDirection.NEGATIVE){float old=a;a=1-z;z=1-old;}
        b[axis]=min+size*a;b[axis+3]=min+size*z;
        result.add(new Box(b[0],b[1],b[2],b[3],b[4],b[5]));
    }
    private FluidPath() { }
}
