package biggerreactorsrework.multiblocks.battery.state;

import biggerreactorsrework.core.client.gui.GuiSync;
import biggerreactorsrework.multiblocks.battery.tiles.BatteryEnergyPortTile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class BatteryEnergyPortState implements GuiSync.IGUIPacket {
    
    /**
     * True for an inlet.
     */
    public boolean direction = true;
    public boolean connected = false;
    
    private final BatteryEnergyPortTile port;
    
    public BatteryEnergyPortState(BatteryEnergyPortTile port) {
        this.port = port;
    }
    
    @Override
    public void read(@Nonnull Map<?, ?> data) {
        direction = (Boolean) data.get("direction");
        connected = (Boolean) data.get("connected");
    }
    
    @Nullable
    @Override
    public Map<?, ?> write() {
        port.updateState();
        HashMap<String, Object> data = new HashMap<>();
        data.put("direction", direction);
        data.put("connected", connected);
        return data;
    }
}
