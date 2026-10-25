package biggerreactorsrework.multiblocks.battery.state;

import biggerreactorsrework.core.client.gui.GuiSync;
import biggerreactorsrework.multiblocks.battery.tiles.BatteryBaseTile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class BatteryState implements GuiSync.IGUIPacket {
    
    public long energyStored = 0;
    public long energyCapacity = 0;
    public long lastInput = 0;
    public long lastOutput = 0;
    public long transferLimit = 0;
    public int cellCount = 0;
    public int inletCount = 0;
    public int outletCount = 0;
    
    private final BatteryBaseTile tile;
    
    public BatteryState(BatteryBaseTile tile) {
        this.tile = tile;
    }
    
    @Override
    public void read(@Nonnull Map<?, ?> data) {
        energyStored = ((Number) data.get("energyStored")).longValue();
        energyCapacity = ((Number) data.get("energyCapacity")).longValue();
        lastInput = ((Number) data.get("lastInput")).longValue();
        lastOutput = ((Number) data.get("lastOutput")).longValue();
        transferLimit = ((Number) data.get("transferLimit")).longValue();
        cellCount = ((Number) data.get("cellCount")).intValue();
        inletCount = ((Number) data.get("inletCount")).intValue();
        outletCount = ((Number) data.get("outletCount")).intValue();
    }
    
    @Nullable
    @Override
    public Map<?, ?> write() {
        tile.updateBatteryState();
        HashMap<String, Object> data = new HashMap<>();
        data.put("energyStored", energyStored);
        data.put("energyCapacity", energyCapacity);
        data.put("lastInput", lastInput);
        data.put("lastOutput", lastOutput);
        data.put("transferLimit", transferLimit);
        data.put("cellCount", cellCount);
        data.put("inletCount", inletCount);
        data.put("outletCount", outletCount);
        return data;
    }
}
