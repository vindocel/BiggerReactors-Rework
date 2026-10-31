package biggerreactorsrework.core.multiblock.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import biggerreactorsrework.core.debug.DebugInfo;
import biggerreactorsrework.core.multiblock.MultiblockController;
import biggerreactorsrework.core.multiblock.modular.MultiblockControllerModule;
import biggerreactorsrework.core.multiblock.modular.MultiblockControllerModuleRegistry;
import biggerreactorsrework.core.multiblock.rectangular.IRectangularMultiblockBlock;
import biggerreactorsrework.core.multiblock.validated.IValidatedMultiblock;
import biggerreactorsrework.core.multiblock.validated.IValidatedMultiblockControllerModule;
import biggerreactorsrework.core.registry.OnModLoad;
import biggerreactorsrework.core.util.NonnullDefault;
import biggerreactorsrework.core.util.Util;

import javax.annotation.Nullable;

@NonnullDefault
public interface IPersistentMultiblock<
        TileType extends BlockEntity & IPersistentMultiblockTile<TileType, BlockType, ControllerType>,
        BlockType extends Block & IRectangularMultiblockBlock,
        ControllerType extends MultiblockController<TileType, BlockType, ControllerType> & IPersistentMultiblock<TileType, BlockType, ControllerType>
        > extends IValidatedMultiblock<TileType, BlockType, ControllerType> {
    
    CompoundTag mergeNBTs(CompoundTag nbtA, CompoundTag nbtB);
    
    void read(CompoundTag nbt);
    
    @Nullable
    CompoundTag write();
    
    default Module<TileType, BlockType, ControllerType> persistentModule() {
        //noinspection unchecked,ConstantConditions
        return module(IPersistentMultiblock.class, IPersistentMultiblock.Module.class);
    }
    
    default void dirty() {
        final var module = module(IPersistentMultiblock.class, Module.class);
        assert module != null;
        module.dirty();
    }
    
    final class Module<
            TileType extends BlockEntity & IPersistentMultiblockTile<TileType, BlockType, ControllerType>,
            BlockType extends Block & IRectangularMultiblockBlock,
            ControllerType extends MultiblockController<TileType, BlockType, ControllerType> & IPersistentMultiblock<TileType, BlockType, ControllerType>
            > extends MultiblockControllerModule<TileType, BlockType, ControllerType> implements IValidatedMultiblockControllerModule {
        @Nullable
        private TileType saveDelegate;
        private boolean hasSaveDelegate = false;
        @Nullable private java.util.UUID origin;
        public java.util.UUID origin() { return origin; }
        public boolean hasPendingContents() {
            return shouldReadNBT && nbt!=null && storedData(nbt);
        }
        private static boolean storedData(Object value) {
            var keys=java.util.Set.of("fuel","waste","liquidAmount","gasAmount","inAmount","outAmount","storedPower");
            if(value instanceof CompoundTag tag) {
                for(String key:tag.keySet()) {
                    if(keys.contains(key) && tag.get(key) instanceof net.minecraft.nbt.NumericTag n && n.asDouble().orElse(0D)>0)return true;
                    if(key.equals("Items") && tag.get(key) instanceof net.minecraft.nbt.ListTag list && !list.isEmpty())return true;
                    if(key.equals("simulationData") && tag.get(key) instanceof net.minecraft.nbt.ByteArrayTag array) {
                        try {if(storedData(new biggerreactorsrework.core.serialization.DataCompound(array.getAsByteArray()).toROBNMap()))return true;}
                        catch(IllegalArgumentException invalid){return true;}
                    }
                    if(tag.get(key) instanceof CompoundTag child && storedData(child))return true;
                }
            } else if(value instanceof java.util.Map<?,?> map) {
                for(var entry:map.entrySet()) {
                    if(keys.contains(entry.getKey()) && entry.getValue() instanceof Number number && number.doubleValue()>0)return true;
                    if(entry.getValue() instanceof java.util.Map<?,?> && storedData(entry.getValue()))return true;
                }
            }
            return false;
        }
        @Nullable
        private IPersistentMultiblockTile.Module<TileType, BlockType, ControllerType> saveDelegateModule;
        @Nullable
        private CompoundTag nbt;
        private boolean shouldReadNBT = false;
        private int expectedBlocks = 0;
        @Nullable
        private IValidatedMultiblock.AssemblyState lastAssemblyState;
        
        @OnModLoad
        public static void register() {
            MultiblockControllerModuleRegistry.registerModule(IPersistentMultiblock.class, Module::new);
        }
        
        public Module(IPersistentMultiblock<TileType, BlockType, ControllerType> controller) {
            super(controller);
        }
        
        private void partAdded(TileType newPart) {
            final var persistentModule = newPart.module(IPersistentMultiblockTile.class, IPersistentMultiblockTile.Module.class);
            assert persistentModule != null;
            if (origin == null) origin = persistentModule.origin;
            if (expectedBlocks == 0) {
                expectedBlocks = persistentModule.expectedBlocks;
            }
            if (persistentModule.expectedBlocks != 0 && persistentModule.expectedBlocks != expectedBlocks) {
                // merging controllers, reset to zero
                expectedBlocks = 0;
            }
            if (lastAssemblyState == null) {
                lastAssemblyState = persistentModule.lastAssemblyState;
            }
            
            final var newNBT = persistentModule.controllerNBT;
            persistentModule.controllerNBT = null;
            if (newNBT == null) {
                return;
            }
            if (saveDelegate == null) {
                saveDelegate = newPart;
                //noinspection unchecked
                saveDelegateModule = persistentModule;
            }
            if (nbt != null) {
                if (nbt.equals(newNBT)) {
                    return;
                }
                nbt = controller.mergeNBTs(nbt, newNBT);
            } else {
                nbt = newNBT;
            }
            shouldReadNBT = true;
        }
        
        private void partRemoved(TileType oldPart) {
            if (oldPart == saveDelegate) {
                // Modified 2026-10-26: allow a surviving part to take over persistence.
                hasSaveDelegate = false;
                saveDelegate = null;
                saveDelegateModule = null;
            }
        }
        
        @Override
        public void merge(ControllerType other) {
            final var otherPersistentModule = other.module(IPersistentMultiblock.class, Module.class);
            assert otherPersistentModule != null;
            if (otherPersistentModule.nbt != null) {
                if (controller.assemblyState() != AssemblyState.DISASSEMBLED) {
                    // we are currently assembled, this will probably change, but this is on the implementation to handle
                    return;
                }
                if (nbt == null) {
                    nbt = otherPersistentModule.nbt;
                    shouldReadNBT = true;
                    return;
                }
                nbt = controller.mergeNBTs(nbt, otherPersistentModule.nbt);
                shouldReadNBT = true;
            }
        }
        
        public void onPartLoaded(TileType tile) {
            partAdded(tile);
        }
        
        public void onPartUnloaded(TileType tile) {
            partRemoved(tile);
        }
        
        public void onPartAttached(TileType tile) {
            partAdded(tile);
        }
        
        public void onPartDetached(TileType tile) {
            partRemoved(tile);
        }
        
        public void onPartPlaced(TileType tile) {
            // cannot have nbt when first placed
            expectedBlocks = 0;
        }
        
        public void onPartBroken(TileType tile) {
            expectedBlocks = 0;
            partRemoved(tile);
        }
        
        private void pickDelegate() {
            if (hasSaveDelegate) {
                return;
            }
            saveDelegate = controller.randomTile();
            //noinspection unchecked
            saveDelegateModule = saveDelegate.module(IPersistentMultiblockTile.class, IPersistentMultiblockTile.Module.class);
            if (saveDelegateModule != null) {
                saveDelegateModule.controllerNBT = null;
            }
            hasSaveDelegate = true;
        }
        
        @Override
        public boolean canValidate() {
            return expectedBlocks <= controller.blocks.size();
        }
    
        @Override
        public boolean canTick() {
            return canValidate();
        }
        
        @Override
        public void onStateTransition(AssemblyState oldAssemblyState, AssemblyState newAssemblyState) {
            if (newAssemblyState == AssemblyState.ASSEMBLED) {
                if (origin == null) origin = java.util.UUID.randomUUID();
                for (var tile : controller.blocks.tileElements()) {
                    if (tile == null) continue;
                    var part = tile.module(IPersistentMultiblockTile.class, IPersistentMultiblockTile.Module.class);
                    if (part != null) part.origin = origin;
                }
            }
            if (shouldReadNBT) {
                shouldReadNBT = false;
                if (nbt != null) {
                    controller.read(nbt);
                }
            }
        }
        
        void dirty() {
            pickDelegate();
            nbt = null;
            if (saveDelegateModule != null) {
                saveDelegateModule.controllerNBT = null;
            }
            Util.markRangeDirty(controller.level, controller.min(), controller.max());
        }
        
        boolean isSaveDelegate(TileType tile) {
            pickDelegate();
            return tile == saveDelegate;
        }
        
        @Nullable
        CompoundTag getNBT() {
            if (nbt == null) {
                nbt = controller.write();
            }
            return nbt;
        }
    
        @Nullable
        @Override
        public DebugInfo getDebugInfo() {
            final var debugInfo = new DebugInfo("PersistentMultiblock");
            return null;
        }
    }
}
