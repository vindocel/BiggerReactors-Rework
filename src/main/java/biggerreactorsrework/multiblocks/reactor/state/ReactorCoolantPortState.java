package biggerreactorsrework.multiblocks.reactor.state;

import biggerreactorsrework.multiblocks.reactor.tiles.ReactorCoolantPortTile;
import biggerreactorsrework.core.client.gui.GuiSync;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class ReactorCoolantPortState implements GuiSync.IGUIPacket {

    /**
     * The direction of the port. True for input, false for output.
     */
    public boolean direction = false;

    /**
     * The tile whose information this belongs to.
     */
    ReactorCoolantPortTile reactorCoolantPortTile;

    public ReactorCoolantPortState(ReactorCoolantPortTile reactorCoolantPortTile) {
        this.reactorCoolantPortTile = reactorCoolantPortTile;
    }

    @Override
    public void read(@Nonnull Map<?, ?> data) {
        // Modified 2026-10-30: resource ports share the terminal's appearance, not its menu origin.
        reactorCoolantPortTile.readAppearance(data);
        direction = (Boolean) data.get("direction");
    }

    @Override
    @Nullable
    public Map<?, ?> write() {
        reactorCoolantPortTile.updateState();
        HashMap<String, Object> data = new HashMap<>();
        data.put("appearancePos", reactorCoolantPortTile.writeAppearance());

        data.put("direction", direction);

        return data;
    }
}
