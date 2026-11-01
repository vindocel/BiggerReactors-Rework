package biggerreactorsrework.compat.opencomputers;

// Added 2026-10-30: native OpenComputers callbacks; all live state stays on the server thread.
import biggerreactorsrework.multiblocks.reactor.tiles.ReactorComputerPortTile;
import biggerreactorsrework.compat.computer.access.ReactorAccess;
import li.cil.oc.api.machine.*;

public final class ReactorComponent extends PortComponent {
    private final ReactorAccess access;
    public ReactorComponent(ReactorComputerPortTile tile) {
        super(tile, "biggerreactorsrework_reactor");
        access = ReactorAccess.create(tile::nullableController);
    }
    @Callback(doc = "function():boolean -- Whether the port belongs to an assembled machine.")
    public Object[] connected(Context context, Arguments args) {
        checkThread(); return result(available() && access.connected());
    }
    @Callback(doc = "function():boolean -- Whether the reactor is active.")
    public Object[] active(Context context, Arguments args) {
        checkAvailable(); return result(access.active());
    }
    @Callback(doc = "function(active:boolean):boolean -- Enable or disable the reactor.")
    public Object[] setActive(Context context, Arguments args) {
        checkAvailable(); access.setActive(args.checkBoolean(0)); tile.setChanged(); return result(true);
    }
    @Callback(doc = "function():table -- Current battery snapshot.")
    public Object[] battery(Context context, Arguments args) {
        checkAvailable(); var b = access.battery(); if (b == null) return result(null); return result(table("stored", b.stored(), "capacity", b.capacity(), "producedLastTick", b.producedLastTick()));
    }
    @Callback(doc = "function():table -- Current coolantTank snapshot.")
    public Object[] coolantTank(Context context, Arguments args) {
        checkAvailable(); var t = access.coolantTank(); if (t == null) return result(null); return result(table("coldFluidAmount", t.coldFluidAmount(), "hotFluidAmount", t.hotFluidAmount(), "capacity", t.capacity(), "transitionedLastTick", t.transitionedLastTick(), "maxTransitionedLastTick", t.maxTransitionedLastTick()));
    }
    @Callback(doc = "function():boolean -- Discard both coolant phases.")
    public Object[] dumpCoolant(Context context, Arguments args) {
        checkAvailable(); var t = access.coolantTank(); if (t == null) throw new IllegalArgumentException("Reactor is not liquid cooled"); t.dump(); tile.setChanged(); return result(true);
    }
    @Callback(doc = "function():table -- Current fuelTank snapshot.")
    public Object[] fuelTank(Context context, Arguments args) {
        checkAvailable(); var t = access.fuelTank(); return result(table("capacity", t.capacity(), "totalReactant", t.totalReactant(), "fuel", t.fuel(), "waste", t.waste(), "fuelReactivity", t.fuelReactivity(), "burnedLastTick", t.burnedLastTick()));
    }
    @Callback(doc = "function():boolean -- Eject waste through configured access ports.")
    public Object[] ejectWaste(Context context, Arguments args) {
        checkAvailable(); access.fuelTank().ejectWaste(); tile.setChanged(); return result(true);
    }
    @Callback(doc = "function():number -- Current fuelTemperature.")
    public Object[] fuelTemperature(Context context, Arguments args) {
        checkAvailable(); return result(access.fuelTemperature());
    }
    @Callback(doc = "function():number -- Current stackTemperature.")
    public Object[] stackTemperature(Context context, Arguments args) {
        checkAvailable(); return result(access.stackTemperature());
    }
    @Callback(doc = "function():number -- Current ambientTemperature.")
    public Object[] ambientTemperature(Context context, Arguments args) {
        checkAvailable(); return result(access.ambientTemperature());
    }
    @Callback(doc = "function():number -- Current controlRodCount.")
    public Object[] controlRodCount(Context context, Arguments args) {
        checkAvailable(); return result(access.controlRodCount());
    }
    @Callback(doc = "function(index:number):table -- Rod snapshot; indices start at zero.")
    public Object[] getControlRod(Context context, Arguments args) {
        checkAvailable(); var rod = access.getControlRod(rodIndex(args)); return result(table("index", rod.index(), "name", rod.name(), "level", rod.level()));
    }
    @Callback(doc = "function(index:number, level:number):boolean -- Set insertion (0 to 100 percent).")
    public Object[] setControlRodLevel(Context context, Arguments args) {
        checkAvailable(); var rod = access.getControlRod(rodIndex(args)); rod.setLevel(percentage(args, 1)); tile.setChanged(); return result(true);
    }
    @Callback(doc = "function(index:number, name:string):boolean -- Name a rod (up to 128 characters).")
    public Object[] setControlRodName(Context context, Arguments args) {
        checkAvailable(); var rod = access.getControlRod(rodIndex(args)); rod.setName(args.checkString(1)); tile.setChanged(); return result(true);
    }
    @Callback(doc = "function(level:number):boolean -- Set all rod insertions (0 to 100 percent).")
    public Object[] setAllControlRodLevels(Context context, Arguments args) {
        checkAvailable(); access.setAllControlRodLevels(percentage(args, 0)); tile.setChanged(); return result(true);
    }
}
