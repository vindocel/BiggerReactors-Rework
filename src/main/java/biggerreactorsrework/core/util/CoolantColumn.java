package biggerreactorsrework.core.util;

/** Shared column space: hot rises from the bottom, cold descends from the top.
 * Modified 2026-10-26: nonempty contents fill the column by relative proportion, not tank capacity.
 */
public final class CoolantColumn {
    public record Slice(float bottom,float top) { }
    public static Slice slice(double cold,double hot,int index,int height,boolean heated) {
        cold=Double.isFinite(cold)?Math.clamp(cold,0,1):0;
        hot=Double.isFinite(hot)?Math.clamp(hot,0,1):0;
        double scale=cold+hot>0?cold+hot:1;
        double low=heated?0:height*(1-cold/scale);
        double high=heated?height*hot/scale:height;
        return new Slice((float)Math.clamp(low-index,0,1),(float)Math.clamp(high-index,0,1));
    }
    private CoolantColumn() { }
}
