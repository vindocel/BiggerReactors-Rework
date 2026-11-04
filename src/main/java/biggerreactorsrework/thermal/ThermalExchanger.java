package biggerreactorsrework.thermal;

import biggerreactorsrework.util.FluidTransitionTank;
import net.minecraft.world.level.material.Fluids;

// Added 2026-11-02: one atomic recipe across both circuits; credit only corrects sub-batch flow resolution.
public final class ThermalExchanger {
    private double credit;
    private String recipe="";
    public long hotLastTick,steamLastTick;
    public double inputKJ,usefulKJ,processLossKJ;

    public void tick(FluidTransitionTank primary,FluidTransitionTank secondary,double rate,ThermalModel.Preset preset) {
        hotLastTick=steamLastTick=0;inputKJ=usefulKJ=processLossKJ=0;
        primary.resetThermalCounters();secondary.resetThermalCounters();
        var hot=primary.activeTransition();var water=secondary.activeTransition();
        if(primary.hasUnresolvedProfile() || secondary.hasUnresolvedProfile() || hot==null || water==null
                || !water.liquids.contains(Fluids.WATER) || !Double.isFinite(rate) || rate<=0) { credit=0;recipe="";return; }
        int hotBatch=10,steamBatch=hot.steamPerBatch;
        double primaryCost=hot.liquids.contains(Fluids.WATER)?preset.steamBudget():preset.heatPerCoolant();
        if(steamBatch<=0 || steamBatch*preset.steamBudget()>hotBatch*primaryCost) { credit=0;return; }
        String key=biggerreactorsrework.registries.CoolantComponents.fingerprint(hot)+":"+steamBatch;
        if(!key.equals(recipe))credit=0;recipe=key;
        long available=Math.min(primary.fluidAmountInTank(0),Math.max(0,primary.perSideCapacity-primary.fluidAmountInTank(1)))/hotBatch;
        available=Math.min(available,Math.min(secondary.fluidAmountInTank(0),Math.max(0,secondary.perSideCapacity-secondary.fluidAmountInTank(1)))/steamBatch);
        if(available<=0) { credit=0;return; }
        double budget=rate+credit;
        long batches=Math.min(available,ThermalModel.wholeUnits(budget/hotBatch));
        hotLastTick=batches*hotBatch;steamLastTick=batches*steamBatch;
        primary.processThermalUnits(hotLastTick,primaryCost);
        secondary.processThermalUnits(steamLastTick,preset.steamBudget());
        // Whole unused throughput is never banked, including during an input/output blockage.
        credit=budget-hotLastTick;credit=credit>=hotBatch?0:Math.max(0,credit);
        inputKJ=hotLastTick*primaryCost;usefulKJ=steamLastTick*preset.steamBudget();processLossKJ=inputKJ-usefulKJ;
    }
    public String recipe() { return recipe; }
    public void restoreRecipe(String value) { recipe=value; }
    public double credit() { return credit; }
    public void restoreCredit(double value) { credit=Double.isFinite(value)&&value>=0&&value<10?value:0; }
}
