package biggerreactorsrework.compat.cctweaked;

// Added 2026-10-31: native CC:Tweaked callbacks; all live state stays on the server thread.
import biggerreactorsrework.multiblocks.reactor.tiles.ReactorComputerPortTile;
import biggerreactorsrework.compat.computer.access.ReactorAccess;
import dan200.computercraft.api.lua.*;

public final class ReactorPeripheral extends ComputerPeripheral {
    private final ReactorAccess access;
    public ReactorPeripheral(ReactorComputerPortTile tile) {
        super(tile, "biggerreactorsrework_reactor");
        access = ReactorAccess.create(tile::nullableController);
    }
    @LuaFunction(mainThread = true)
    public Object[] connected(IArguments args) throws LuaException {
        try {
        checkThread(); return result(available() && access.connected());
        } catch (IllegalArgumentException | IllegalStateException error) { throw new LuaException(error.getMessage()); }
    }

    @LuaFunction(mainThread = true)
    public Object[] active(IArguments args) throws LuaException {
        try {
        checkAvailable(); return result(access.active());
        } catch (IllegalArgumentException | IllegalStateException error) { throw new LuaException(error.getMessage()); }
    }

    @LuaFunction(mainThread = true)
    public Object[] setActive(IArguments args) throws LuaException {
        try {
        checkAvailable(); access.setActive(args.getBoolean(0)); tile.setChanged(); return result(true);
        } catch (IllegalArgumentException | IllegalStateException error) { throw new LuaException(error.getMessage()); }
    }

    @LuaFunction(mainThread = true)
    public Object[] battery(IArguments args) throws LuaException {
        try {
        checkAvailable(); var b = access.battery(); if (b == null) return result(null); return result(table("stored", b.stored(), "capacity", b.capacity(), "producedLastTick", b.producedLastTick()));
        } catch (IllegalArgumentException | IllegalStateException error) { throw new LuaException(error.getMessage()); }
    }

    @LuaFunction(mainThread = true)
    public Object[] coolantTank(IArguments args) throws LuaException {
        try {
        checkAvailable(); var t = access.coolantTank(); if (t == null) return result(null); return result(table("coldFluidAmount", t.coldFluidAmount(), "hotFluidAmount", t.hotFluidAmount(), "capacity", t.capacity(), "transitionedLastTick", t.transitionedLastTick(), "maxTransitionedLastTick", t.maxTransitionedLastTick()));
        } catch (IllegalArgumentException | IllegalStateException error) { throw new LuaException(error.getMessage()); }
    }

    @LuaFunction(mainThread = true)
    public Object[] dumpCoolant(IArguments args) throws LuaException {
        try {
        checkAvailable(); var t = access.coolantTank(); if (t == null) throw new IllegalArgumentException("Reactor is not liquid cooled"); t.dump(); tile.setChanged(); return result(true);
        } catch (IllegalArgumentException | IllegalStateException error) { throw new LuaException(error.getMessage()); }
    }

    @LuaFunction(mainThread = true)
    public Object[] fuelTank(IArguments args) throws LuaException {
        try {
        checkAvailable(); var t = access.fuelTank(); return result(table("capacity", t.capacity(), "totalReactant", t.totalReactant(), "fuel", t.fuel(), "waste", t.waste(), "fuelReactivity", t.fuelReactivity(), "burnedLastTick", t.burnedLastTick()));
        } catch (IllegalArgumentException | IllegalStateException error) { throw new LuaException(error.getMessage()); }
    }

    @LuaFunction(mainThread = true)
    public Object[] ejectWaste(IArguments args) throws LuaException {
        try {
        checkAvailable(); access.fuelTank().ejectWaste(); tile.setChanged(); return result(true);
        } catch (IllegalArgumentException | IllegalStateException error) { throw new LuaException(error.getMessage()); }
    }

    @LuaFunction(mainThread = true)
    public Object[] fuelTemperature(IArguments args) throws LuaException {
        try {
        checkAvailable(); return result(access.fuelTemperature());
        } catch (IllegalArgumentException | IllegalStateException error) { throw new LuaException(error.getMessage()); }
    }

    @LuaFunction(mainThread = true)
    public Object[] stackTemperature(IArguments args) throws LuaException {
        try {
        checkAvailable(); return result(access.stackTemperature());
        } catch (IllegalArgumentException | IllegalStateException error) { throw new LuaException(error.getMessage()); }
    }

    @LuaFunction(mainThread = true)
    public Object[] ambientTemperature(IArguments args) throws LuaException {
        try {
        checkAvailable(); return result(access.ambientTemperature());
        } catch (IllegalArgumentException | IllegalStateException error) { throw new LuaException(error.getMessage()); }
    }

    @LuaFunction(mainThread = true)
    public Object[] controlRodCount(IArguments args) throws LuaException {
        try {
        checkAvailable(); return result(access.controlRodCount());
        } catch (IllegalArgumentException | IllegalStateException error) { throw new LuaException(error.getMessage()); }
    }

    @LuaFunction(mainThread = true)
    public Object[] getControlRod(IArguments args) throws LuaException {
        try {
        checkAvailable(); var rod = access.getControlRod(rodIndex(args)); return result(table("index", rod.index(), "name", rod.name(), "level", rod.level()));
        } catch (IllegalArgumentException | IllegalStateException error) { throw new LuaException(error.getMessage()); }
    }

    @LuaFunction(mainThread = true)
    public Object[] setControlRodLevel(IArguments args) throws LuaException {
        try {
        checkAvailable(); var rod = access.getControlRod(rodIndex(args)); rod.setLevel(percentage(args, 1)); tile.setChanged(); return result(true);
        } catch (IllegalArgumentException | IllegalStateException error) { throw new LuaException(error.getMessage()); }
    }

    @LuaFunction(mainThread = true)
    public Object[] setControlRodName(IArguments args) throws LuaException {
        try {
        checkAvailable(); var rod = access.getControlRod(rodIndex(args)); rod.setName(args.getString(1)); tile.setChanged(); return result(true);
        } catch (IllegalArgumentException | IllegalStateException error) { throw new LuaException(error.getMessage()); }
    }

    @LuaFunction(mainThread = true)
    public Object[] setAllControlRodLevels(IArguments args) throws LuaException {
        try {
        checkAvailable(); access.setAllControlRodLevels(percentage(args, 0)); tile.setChanged(); return result(true);
        } catch (IllegalArgumentException | IllegalStateException error) { throw new LuaException(error.getMessage()); }
    }
}
