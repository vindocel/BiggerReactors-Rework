package biggerreactorsrework.core.multiblock.validated;

import biggerreactorsrework.core.util.NonnullDefault;

@NonnullDefault
public interface IAssembledTickMultiblockModule {
    
    default void preTick() {
    }
    
    default void postTick() {
    }
    
    default void preDisassembledTick() {
    }
    
    default void postDisassembledTick() {
    }
}
