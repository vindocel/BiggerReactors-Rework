package biggerreactorsrework.compat.cctweaked;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.compat.ComputerPortState;
import dan200.computercraft.api.lua.*;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.LinkedHashMap;
import java.util.Map;

/** Added 2026-10-31: optional native peripheral, with no world access on the Lua worker thread. */
public abstract class ComputerPeripheral implements IPeripheral {
    protected final BlockEntity tile;
    private final String type;
    protected ComputerPeripheral(BlockEntity tile, String type) { this.tile=tile; this.type=type; }
    @Override public final String getType() { return type; }
    @Override public final Object getTarget() { return tile; }
    @Override public final boolean equals(IPeripheral other) {
        return other instanceof ComputerPeripheral peripheral && peripheral.tile==tile && peripheral.type.equals(type);
    }
    protected final boolean available() {
        var level=tile.getLevel();
        return level!=null && !level.isClientSide() && ((ComputerPortState.Port)tile).computerPortState().loaded()
                && level.hasChunkAt(tile.getBlockPos()) && level.getBlockEntity(tile.getBlockPos())==tile;
    }
    protected final void checkThread() throws LuaException {
        var server=tile.getLevel()==null?null:tile.getLevel().getServer();
        if(server==null || !server.isSameThread())throw new LuaException("Computer callbacks require the server thread");
    }
    protected final void checkAvailable() throws LuaException {
        checkThread();
        if(!available())throw new LuaException("Computer port is unloaded or removed");
    }
    protected static Map<String,Object> table(Object... pairs) {
        var result=new LinkedHashMap<String,Object>();
        for(int i=0;i<pairs.length;i+=2)result.put((String)pairs[i],pairs[i+1]);
        return result;
    }
    protected static Object[] result(Object value) { return new Object[]{value}; }
    protected static double percentage(IArguments args,int index) throws LuaException {
        double value=args.getDouble(index);
        if(!Double.isFinite(value) || value<0 || value>100)throw new LuaException("Expected a finite percentage from 0 to 100");
        return value;
    }
    protected static int rodIndex(IArguments args) throws LuaException {
        double value=args.getDouble(0);
        if(!Double.isFinite(value) || value<0 || value>Integer.MAX_VALUE || value!=Math.rint(value))
            throw new LuaException("Expected a nonnegative integer rod index");
        return (int)value;
    }
    @LuaFunction(mainThread=true) public final int apiVersion() { return 1; }
    @LuaFunction(mainThread=true) public final String modVersion() { return BiggerReactors.modVersion(); }
}
