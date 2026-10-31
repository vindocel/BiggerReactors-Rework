package biggerreactorsrework.core.util;


import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class BlockStates {
    public static final BooleanProperty PORT_DIRECTION = BooleanProperty.create("port_direction");
    public static final BooleanProperty ACTIVITY = BooleanProperty.create("active");
    public static final EnumProperty<Direction> FACING = EnumProperty.create("facing", Direction.class);
}
