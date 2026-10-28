package biggerreactorsrework.core.util;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Modified 2026-10-26: persistent output stock limits operation; normal transfer batches do not. */
public final class OutputBackpressure {
    public static final int WINDOW_TICKS=20;
    private final double[] history=new double[WINDOW_TICKS];
    private int cursor;
    private double limit=1;

    public double tick(long stored,long capacity) {
        history[cursor]=occupancy(stored,capacity);
        cursor=(cursor+1)%history.length;
        double target=1-retainedFraction();
        limit=capacity<=0 || stored>=capacity?0:Math.min(target,Math.min(1,limit+.05));
        return limit;
    }
    public void released(long remaining,long capacity) {
        int current=(cursor+history.length-1)%history.length;
        history[current]=Math.min(history[current],occupancy(remaining,capacity));
    }
    public double limit(){return limit;}
    public double retainedFraction(){return Arrays.stream(history).min().orElse(0);}
    public void reset(){Arrays.fill(history,0);cursor=0;limit=1;}
    private static double occupancy(long stored,long capacity) {
        return capacity<=0?0:Math.clamp((double)stored/capacity,0,1);
    }
    public Map<String,Object> save() {
        return Map.of("history",Arrays.stream(history).boxed().toList(),"cursor",cursor,"limit",limit);
    }
    public void load(Map<String,Object> saved) {
        reset();
        if(!(saved.get("history") instanceof List<?> values) || values.size()!=history.length)return;
        for(int i=0;i<history.length;i++) {
            if(!(values.get(i) instanceof Number number) || !Double.isFinite(number.doubleValue())){reset();return;}
            history[i]=Math.clamp(number.doubleValue(),0,1);
        }
        if(saved.get("cursor") instanceof Number n)cursor=Math.floorMod(n.intValue(),history.length);
        if(saved.get("limit") instanceof Number n && Double.isFinite(n.doubleValue()))limit=Math.clamp(n.doubleValue(),0,1);
    }
}
