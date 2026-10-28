package biggerreactorsrework.multiblocks.heatexchanger;

import biggerreactorsrework.core.util.OutputBackpressure;
import biggerreactorsrework.util.FluidTransitionTank;
import net.minecraft.nbt.CompoundTag;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Modified 2026-10-26: steam backlog limits both phase changes, never cold stock or water input. */
public final class HeatExchangerBackpressure {
    private final FluidTransitionTank primary,secondary;
    private final OutputBackpressure pressure=new OutputBackpressure();
    public HeatExchangerBackpressure(FluidTransitionTank primary,FluidTransitionTank secondary) {
        this.primary=primary;this.secondary=secondary;
        secondary.observeOutputWith(pressure);
    }
    public void tick() {
        double limit=pressure.tick(secondary.fluidAmountInTank(1),secondary.perSideCapacity);
        primary.setConversionLimit(limit);secondary.setConversionLimit(limit);
    }
    public double limit(){return pressure.limit();}
    public double retainedFraction(){return pressure.retainedFraction();}
    public CompoundTag save() {
        var state=pressure.save();var tag=new CompoundTag();
        var history=(List<?>)state.get("history");
        tag.putLongArray("history",history.stream().mapToLong(v->Double.doubleToLongBits(((Number)v).doubleValue())).toArray());
        tag.putInt("cursor",((Number)state.get("cursor")).intValue());
        tag.putDouble("limit",pressure.limit());return tag;
    }
    public void load(CompoundTag tag) {
        pressure.load(Map.of("history",Arrays.stream(tag.getLongArray("history")).mapToDouble(Double::longBitsToDouble).boxed().toList(),
                "cursor",tag.getInt("cursor"),"limit",tag.getDouble("limit")));
    }
}
