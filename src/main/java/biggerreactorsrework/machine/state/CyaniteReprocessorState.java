package biggerreactorsrework.machine.state;

import biggerreactorsrework.machine.tiles.CyaniteReprocessorTile;
import biggerreactorsrework.core.client.gui.GuiSync;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class CyaniteReprocessorState implements GuiSync.IGUIPacket {

    /**
     * The number of ticks the current item has processed for.
     */
    public int workTime = 0;
    /**
     * The total amount of time required to process the item.
     */
    public int workTimeTotal = 0;

    /**
     * The amount of energy stored in the machine.
     */
    public int energyStored = 0;
    /**
     * The max energy capacity of the machine.
     */
    public int energyCapacity = 0;

    /**
     * The amount of water stored in the machine.
     */
    public int waterStored = 0;
    /**
     * The max water capacity of the machine.
     */
    public int waterCapacity = 0;

    /**
     * Side routes per material, packed by {@link biggerreactorsrework.machine.side.SideConfiguration#pack}.
     */
    public int itemSides = 0;
    public int fluidSides = 0;
    public int energySides = 0;

    /**
     * Automation switches.
     */
    public boolean autoInput = false;
    public boolean autoEject = false;
    public boolean autoInputFluid = false;

    /**
     * The tile whose information this belongs to.
     */
    CyaniteReprocessorTile cyaniteReprocessorTile;

    public CyaniteReprocessorState(CyaniteReprocessorTile cyaniteReprocessorTile) {
        this.cyaniteReprocessorTile = cyaniteReprocessorTile;
    }

    @Override
    public void read(@Nonnull Map<?, ?> data) {
        this.workTime = (Integer) data.get("workTime");
        this.workTimeTotal = (Integer) data.get("workTimeTotal");

        this.energyStored = (Integer) data.get("energyStored");
        this.energyCapacity = (Integer) data.get("energyCapacity");

        this.waterStored = (Integer) data.get("waterStored");
        this.waterCapacity = (Integer) data.get("waterCapacity");

        this.itemSides = (Integer) data.get("itemSides");
        this.fluidSides = (Integer) data.get("fluidSides");
        this.energySides = (Integer) data.get("energySides");
        this.autoInput = (Integer) data.get("autoInput") != 0;
        this.autoEject = (Integer) data.get("autoEject") != 0;
        this.autoInputFluid = (Integer) data.get("autoInputFluid") != 0;
    }

    @Nullable
    @Override
    public Map<?, ?> write() {
        this.cyaniteReprocessorTile.updateState();
        HashMap<String, Object> data = new HashMap<>();

        data.put("workTime", this.workTime);
        data.put("workTimeTotal", this.workTimeTotal);

        data.put("energyStored", this.energyStored);
        data.put("energyCapacity", this.energyCapacity);

        data.put("waterStored", this.waterStored);
        data.put("waterCapacity", this.waterCapacity);

        data.put("itemSides", this.itemSides);
        data.put("fluidSides", this.fluidSides);
        data.put("energySides", this.energySides);
        data.put("autoInput", this.autoInput ? 1 : 0);
        data.put("autoEject", this.autoEject ? 1 : 0);
        data.put("autoInputFluid", this.autoInputFluid ? 1 : 0);

        return data;
    }
}
