package biggerreactorsrework.compat.opencomputers;

// Added 2026-10-30: native OpenComputers callbacks; all live state stays on the server thread.
import biggerreactorsrework.multiblocks.turbine.tiles.TurbineComputerPortTile;
import biggerreactorsrework.compat.opencomputers.access.TurbineAccess;
import li.cil.oc.api.machine.*;

public final class TurbineComponent extends PortComponent {
    private final TurbineAccess access;
    public TurbineComponent(TurbineComputerPortTile tile) {
        super(tile, "biggerreactorsrework_turbine");
        access = TurbineAccess.create(tile::nullableController);
    }
    @Callback(doc = "function():boolean -- Whether the port belongs to an assembled machine.")
    public Object[] connected(Context context, Arguments args) {
        checkThread(); return result(available() && access.connected());
    }
    @Callback(doc = "function():boolean -- Current active.")
    public Object[] active(Context context, Arguments args) {
        checkAvailable(); return result(access.active());
    }
    @Callback(doc = "function(value:boolean):boolean -- Set active.")
    public Object[] setActive(Context context, Arguments args) {
        checkAvailable(); access.setActive(args.checkBoolean(0)); tile.setChanged(); return result(true);
    }
    @Callback(doc = "function():boolean -- Current coilEngaged.")
    public Object[] coilEngaged(Context context, Arguments args) {
        checkAvailable(); return result(access.coilEngaged());
    }
    @Callback(doc = "function(value:boolean):boolean -- Set coilEngaged.")
    public Object[] setCoilEngaged(Context context, Arguments args) {
        checkAvailable(); access.setCoilEngaged(args.checkBoolean(0)); tile.setChanged(); return result(true);
    }
    @Callback(doc = "function():table -- Current battery snapshot.")
    public Object[] battery(Context context, Arguments args) {
        checkAvailable(); var b = access.battery(); return result(table("stored", b.stored(), "capacity", b.capacity(), "producedLastTick", b.producedLastTick()));
    }
    @Callback(doc = "function():table -- Current rotor snapshot.")
    public Object[] rotor(Context context, Arguments args) {
        checkAvailable(); var r = access.rotor(); return result(table("RPM", r.RPM(), "efficiencyLastTick", r.efficiencyLastTick()));
    }
    @Callback(doc = "function():table -- Current fluidTank snapshot.")
    public Object[] fluidTank(Context context, Arguments args) {
        checkAvailable(); var t = access.fluidTank(); return result(table("input", table("name", t.input().name(), "amount", t.input().amount(), "maxAmount", t.input().maxAmount()), "output", table("name", t.output().name(), "amount", t.output().amount(), "maxAmount", t.output().maxAmount()), "flowLastTick", t.flowLastTick(), "nominalFlowRate", t.nominalFlowRate(), "flowRateLimit", t.flowRateLimit()));
    }
    @Callback(doc = "function(rate:number):boolean -- Set integer flow in mB per tick, within the flow limit.")
    public Object[] setNominalFlowRate(Context context, Arguments args) {
        checkAvailable(); double rate = args.checkDouble(0); if (!Double.isFinite(rate) || rate != Math.rint(rate) || rate < 0 || rate > access.fluidTank().flowRateLimit()) throw new IllegalArgumentException("Flow rate must be an integer within the flow limit"); access.fluidTank().setNominalFlowRate((long) rate); tile.setChanged(); return result(true);
    }
    @Callback(doc = "function():string -- Current vent policy: closed, overflow or all.")
    public Object[] ventState(Context context, Arguments args) {
        checkAvailable(); return result(access.ventState());
    }
    @Callback(doc = "function(state:string):boolean -- Set vent policy: closed, overflow or all.")
    public Object[] setVentState(Context context, Arguments args) {
        checkAvailable(); access.setVentState(args.checkString(0)); tile.setChanged(); return result(true);
    }
}
