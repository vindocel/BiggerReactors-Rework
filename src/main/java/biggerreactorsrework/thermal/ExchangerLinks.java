package biggerreactorsrework.thermal;

import java.util.Collection;

// Added 2026-11-02: assembly-only straight face links, shared by controllers and designs.
public final class ExchangerLinks {
    public record Position(int x,int y,int z) { }
    /** 0 = air, 1 = primary channel, 2 = secondary channel, 3 = obstruction/outside. */
    @FunctionalInterface public interface Cells { int at(int x,int y,int z); }
    public record Links(int count,double weighted) { }
    private static final int[][] DIRECTIONS={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
    public static Links scan(Collection<Position> primary,Cells cells) {
        int count=0;double weighted=0;
        for(var p:primary)for(var d:DIRECTIONS)for(int distance=1;distance<=4;distance++) {
            int cell=cells.at(p.x+d[0]*distance,p.y+d[1]*distance,p.z+d[2]*distance);
            if(cell==0)continue;
            if(cell==2) { count++;weighted+=ThermalModel.gapFactor(distance-1); }
            break;
        }
        return new Links(count,weighted);
    }
    private ExchangerLinks() { }
}
