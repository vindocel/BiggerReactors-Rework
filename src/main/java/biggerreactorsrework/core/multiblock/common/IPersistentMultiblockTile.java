package biggerreactorsrework.core.multiblock.common;

// Modified 2026-10-26: persist machine origin separately from delegated contents.

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import biggerreactorsrework.core.modular.api.IModularTile;
import biggerreactorsrework.core.modular.api.ModuleRegistry;
import biggerreactorsrework.core.modular.api.TileModule;
import biggerreactorsrework.core.multiblock.MultiblockController;
import biggerreactorsrework.core.multiblock.MultiblockTileModule;
import biggerreactorsrework.core.multiblock.modular.ICoreMultiblockTileModule;
import biggerreactorsrework.core.multiblock.rectangular.IRectangularMultiblockBlock;
import biggerreactorsrework.core.multiblock.validated.IValidatedMultiblock;
import biggerreactorsrework.core.multiblock.validated.IValidatedMultiblockTile;
import biggerreactorsrework.core.registry.OnModLoad;
import biggerreactorsrework.core.util.NonnullDefault;

import javax.annotation.Nullable;

@NonnullDefault
public interface IPersistentMultiblockTile<
        TileType extends BlockEntity & IPersistentMultiblockTile<TileType, BlockType, ControllerType>,
        BlockType extends Block & IRectangularMultiblockBlock,
        ControllerType extends MultiblockController<TileType, BlockType, ControllerType> & IPersistentMultiblock<TileType, BlockType, ControllerType>
        > extends IValidatedMultiblockTile<TileType, BlockType, ControllerType> {
    
    final class Module<
            TileType extends BlockEntity & IPersistentMultiblockTile<TileType, BlockType, ControllerType>,
            BlockType extends Block & IRectangularMultiblockBlock,
            ControllerType extends MultiblockController<TileType, BlockType, ControllerType> & IPersistentMultiblock<TileType, BlockType, ControllerType>
            > extends TileModule<TileType> implements ICoreMultiblockTileModule<TileType, BlockType, ControllerType> {
        
        @Nullable
        CompoundTag controllerNBT;
        @Nullable java.util.UUID origin;
        IValidatedMultiblock.AssemblyState lastAssemblyState = IValidatedMultiblock.AssemblyState.DISASSEMBLED;
        int expectedBlocks = 0;
        
        MultiblockTileModule<TileType, BlockType, ControllerType> multiblockModule;
        @Nullable
        IPersistentMultiblock.Module<TileType, BlockType, ControllerType> controllerPersistentModule;
        
        @OnModLoad
        public static void register() {
            ModuleRegistry.registerTileModule(IPersistentMultiblockTile.class, Module::new);
        }
        
        public Module(IModularTile iface) {
            super(iface);
            //noinspection ConstantConditions
            multiblockModule = null;
        }
        
        @Override
        public void postModuleConstruction() {
            multiblockModule = iface.multiblockModule();
        }
        
        @Override
        public String saveKey() {
            return "persistent_multiblock";
        }
        
        @Override
        public void readNBT(CompoundTag nbt) {
            origin = nbt.read("machine_origin", net.minecraft.core.UUIDUtil.CODEC).orElse(null);
            // backwards compat with opening older worlds
            // TODO: require this
            if (nbt.contains("last_assembly_state")) {
                // Modified 2026-10-30: invalid metadata must not prevent reconstruction of valid stored machines.
                lastAssemblyState = biggerreactorsrework.core.util.SavedValues.enumValue(IValidatedMultiblock.AssemblyState.class, nbt.getStringOr("last_assembly_state", ""), IValidatedMultiblock.AssemblyState.DISASSEMBLED);
            }
            if (nbt.contains("expected_blocks")) {
                expectedBlocks = Math.max(0, nbt.getIntOr("expected_blocks", 0));
            }
            if (nbt.contains("controller_data")) {
                this.controllerNBT = nbt.getCompoundOrEmpty("controller_data");
                return;
            }
            this.controllerNBT = null;
        }
        
        @Override
        public CompoundTag writeNBT() {
            if (controllerPersistentModule != null && controllerPersistentModule.isSaveDelegate(iface) && controllerNBT == null) {
                controllerNBT = controllerPersistentModule.getNBT();
            }
            final var toReturn = new CompoundTag();
            if (controllerPersistentModule != null && controllerPersistentModule.origin() != null) origin = controllerPersistentModule.origin();
            if (origin != null) toReturn.store("machine_origin", net.minecraft.core.UUIDUtil.CODEC, origin);
            toReturn.putString("last_assembly_state", lastAssemblyState.toString());
            if (controllerNBT != null) {
                toReturn.put("controller_data", controllerNBT);
            }
            return toReturn;
        }
        
        @Override
        public void aboutToUnloadDetach() {
            controllerNBT = null;
            writeNBT();
        }
        
        @Override
        public void onControllerChange() {
            final var controller = multiblockModule.controller();
            if (controller == null) {
                controllerPersistentModule = null;
                return;
            }
            controllerPersistentModule = controller.persistentModule();
        }
    }
}
