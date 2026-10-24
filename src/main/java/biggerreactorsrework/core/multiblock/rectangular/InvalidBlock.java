package biggerreactorsrework.core.multiblock.rectangular;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import biggerreactorsrework.core.multiblock.ValidationException;
import biggerreactorsrework.core.util.NonnullDefault;
import org.joml.Vector3ic;

@NonnullDefault
@SuppressWarnings("unused")
public class InvalidBlock extends ValidationException {
    
    public InvalidBlock(String s) {
        super(s);
    }
    
    public InvalidBlock(Block block, Vector3ic worldPosition, String multiblockPosition) {
        super(Component.translatable(
                "multiblock.error.biggerreactorsrework.invalid_block." + multiblockPosition,
                Component.translatable(block.getDescriptionId()),
                "(x: " + worldPosition.x() + "; y: " + worldPosition.y() + "; z: " + worldPosition.z() + ")")
        );
    }
}
