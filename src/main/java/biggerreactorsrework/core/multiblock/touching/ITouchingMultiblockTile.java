package biggerreactorsrework.core.multiblock.touching;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import biggerreactorsrework.core.modular.api.IModularTile;
import biggerreactorsrework.core.modular.api.ModuleRegistry;
import biggerreactorsrework.core.modular.api.TileModule;
import biggerreactorsrework.core.multiblock.IMultiblockTile;
import biggerreactorsrework.core.multiblock.MultiblockController;
import biggerreactorsrework.core.multiblock.common.IPersistentMultiblock;
import biggerreactorsrework.core.multiblock.common.IPersistentMultiblockTile;
import biggerreactorsrework.core.multiblock.modular.ICoreMultiblockTileModule;
import biggerreactorsrework.core.multiblock.rectangular.IRectangularMultiblock;
import biggerreactorsrework.core.multiblock.rectangular.IRectangularMultiblockBlock;
import biggerreactorsrework.core.multiblock.rectangular.IRectangularMultiblockTile;
import biggerreactorsrework.core.registry.OnModLoad;
import biggerreactorsrework.core.util.NonnullDefault;
import biggerreactorsrework.core.util.VectorUtil;
import org.joml.Vector3i;

@NonnullDefault
public interface ITouchingMultiblockTile<
        TileType extends BlockEntity & ITouchingMultiblockTile<TileType, BlockType, ControllerType> & IRectangularMultiblockTile<TileType, BlockType, ControllerType> & IPersistentMultiblockTile<TileType, BlockType, ControllerType>,
        BlockType extends Block & IRectangularMultiblockBlock,
        ControllerType extends MultiblockController<TileType, BlockType, ControllerType> & ITouchingMultiblock<TileType, BlockType, ControllerType> & IRectangularMultiblock<TileType, BlockType, ControllerType> & IPersistentMultiblock<TileType, BlockType, ControllerType>
        > extends IMultiblockTile<TileType, BlockType, ControllerType> {
    
    final class Module<
            TileType extends BlockEntity & ITouchingMultiblockTile<TileType, BlockType, ControllerType> & IRectangularMultiblockTile<TileType, BlockType, ControllerType> & IPersistentMultiblockTile<TileType, BlockType, ControllerType>,
            BlockType extends Block & IRectangularMultiblockBlock,
            ControllerType extends MultiblockController<TileType, BlockType, ControllerType> & ITouchingMultiblock<TileType, BlockType, ControllerType> & IRectangularMultiblock<TileType, BlockType, ControllerType> & IPersistentMultiblock<TileType, BlockType, ControllerType>
            > extends TileModule<TileType> implements ICoreMultiblockTileModule<TileType, BlockType, ControllerType> {
        
        boolean assembled = false;
        final Vector3i min = new Vector3i();
        final Vector3i max = new Vector3i();
        
        @OnModLoad
        public static void register() {
            ModuleRegistry.registerTileModule(ITouchingMultiblockTile.class, Module::new);
        }
        
        public Module(IModularTile iface) {
            super(iface);
        }
        
        @Override
        public boolean shouldConnectTo(TileType tile, Direction direction) {
            if (!assembled) {
                return true;
            }
            return VectorUtil.lequal(min, tile.getBlockPos()) && VectorUtil.grequal(max, tile.getBlockPos());
        }
    
        @Override
        public String saveKey() {
            return "touching_multiblock";
        }
    
        @Override
        public void readNBT(CompoundTag nbt) {
            assembled = nbt.getBooleanOr("assembled", false);
            min.set(nbt.getIntOr("minx", 0), nbt.getIntOr("miny", 0), nbt.getIntOr("minz", 0));
            max.set(nbt.getIntOr("maxx", 0), nbt.getIntOr("maxy", 0), nbt.getIntOr("maxz", 0));
        }
        
        @Override
        public CompoundTag writeNBT() {
            final var tag = new CompoundTag();
            tag.putBoolean("assembled", assembled);
            tag.putInt("minx", min.x());
            tag.putInt("miny", min.y());
            tag.putInt("minz", min.z());
            tag.putInt("maxx", max.x());
            tag.putInt("maxy", max.y());
            tag.putInt("maxz", max.z());
            return tag;
        }
    }
}
