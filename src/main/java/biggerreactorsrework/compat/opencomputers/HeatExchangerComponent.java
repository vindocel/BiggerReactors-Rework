package biggerreactorsrework.compat.opencomputers;

// Added 2026-10-30: native OpenComputers callbacks; all live state stays on the server thread.
import biggerreactorsrework.multiblocks.heatexchanger.tiles.HeatExchangerComputerPortTile;
import biggerreactorsrework.compat.opencomputers.access.HeatExchangerAccess;
import li.cil.oc.api.machine.*;

public final class HeatExchangerComponent extends PortComponent {
    private final HeatExchangerAccess access;
    public HeatExchangerComponent(HeatExchangerComputerPortTile tile) {
        super(tile, "biggerreactorsrework_heat_exchanger");
        access = HeatExchangerAccess.create(tile::nullableController);
    }
    @Callback(doc = "function():boolean -- Whether the port belongs to an assembled machine.")
    public Object[] connected(Context context, Arguments args) {
        checkThread(); return result(available() && access.connected());
    }
    @Callback(doc = "function():table -- Current condenser snapshot.")
    public Object[] condenser(Context context, Arguments args) {
        checkAvailable(); var c = access.condenser(); return result(table("temperature", c.temperature(), "rfPerKelvin", c.rfPerKelvin(), "transitionedLastTick", c.transitionedLastTick(), "maxTransitionedLastTick", c.maxTransitionedLastTick(), "transitionEnergy", c.transitionEnergy(), "input", table("name", c.input().name(), "amount", c.input().amount(), "maxAmount", c.input().maxAmount()), "output", table("name", c.output().name(), "amount", c.output().amount(), "maxAmount", c.output().maxAmount())));
    }
    @Callback(doc = "function():boolean -- Discard both condenser fluid phases.")
    public Object[] dumpCondenser(Context context, Arguments args) {
        checkAvailable(); access.condenser().dump(); tile.setChanged(); return result(true);
    }
    @Callback(doc = "function():table -- Current evaporator snapshot.")
    public Object[] evaporator(Context context, Arguments args) {
        checkAvailable(); var c = access.evaporator(); return result(table("temperature", c.temperature(), "rfPerKelvin", c.rfPerKelvin(), "transitionedLastTick", c.transitionedLastTick(), "maxTransitionedLastTick", c.maxTransitionedLastTick(), "transitionEnergy", c.transitionEnergy(), "input", table("name", c.input().name(), "amount", c.input().amount(), "maxAmount", c.input().maxAmount()), "output", table("name", c.output().name(), "amount", c.output().amount(), "maxAmount", c.output().maxAmount())));
    }
    @Callback(doc = "function():boolean -- Discard both evaporator fluid phases.")
    public Object[] dumpEvaporator(Context context, Arguments args) {
        checkAvailable(); access.evaporator().dump(); tile.setChanged(); return result(true);
    }
    @Callback(doc = "function():table -- Current internalEnvironment snapshot.")
    public Object[] internalEnvironment(Context context, Arguments args) {
        checkAvailable(); var e = access.internalEnvironment(); return result(table("temperature", e.temperature(), "rfPerKelvin", e.rfPerKelvin()));
    }
    @Callback(doc = "function():number -- Current ambientTemperature.")
    public Object[] ambientTemperature(Context context, Arguments args) {
        checkAvailable(); return result(access.ambientTemperature());
    }
    @Callback(doc = "function():number -- Current ambientInternalRFKT.")
    public Object[] ambientInternalRFKT(Context context, Arguments args) {
        checkAvailable(); return result(access.ambientInternalRFKT());
    }
    @Callback(doc = "function():number -- Current condenserInternalRFKT.")
    public Object[] condenserInternalRFKT(Context context, Arguments args) {
        checkAvailable(); return result(access.condenserInternalRFKT());
    }
    @Callback(doc = "function():number -- Current evaporatorInternalRFKT.")
    public Object[] evaporatorInternalRFKT(Context context, Arguments args) {
        checkAvailable(); return result(access.evaporatorInternalRFKT());
    }
    @Callback(doc = "function():number -- Current condenserEvaporatorRFKT.")
    public Object[] condenserEvaporatorRFKT(Context context, Arguments args) {
        checkAvailable(); return result(access.condenserEvaporatorRFKT());
    }
}
