package biggerreactorsrework.core.util;


import net.minecraft.core.Direction;
//? if =26.1.2 || =26.3 {
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class BlockStates {
//?} else if =1.21.1 {
/*import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

public class BlockStates {
*/
//?}
    public static final BooleanProperty PORT_DIRECTION = BooleanProperty.create("port_direction");
//? if =26.1.2 || =26.3 {
    public static final BooleanProperty ACTIVITY = BooleanProperty.create("active");
    public static final EnumProperty<Direction> FACING = EnumProperty.create("facing", Direction.class);
}
//?} else if =1.21.1 {
/*    public static final BooleanProperty ACTIVITY = BooleanProperty.create("active");
    public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.values());
}
*/
//?}
