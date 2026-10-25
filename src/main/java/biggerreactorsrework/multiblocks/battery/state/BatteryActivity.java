package biggerreactorsrework.multiblocks.battery.state;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import javax.annotation.Nonnull;
import java.util.Locale;

public enum BatteryActivity implements StringRepresentable {
    ACTIVE,
    IDLE;
    
    @SuppressWarnings("SpellCheckingInspection")
    public static final EnumProperty<BatteryActivity> BATTERY_STATE_ENUM_PROPERTY = EnumProperty.create("batterystate", BatteryActivity.class);
    
    @Override
    @Nonnull
    public String getSerializedName() {
        return toString().toLowerCase(Locale.US);
    }
}
