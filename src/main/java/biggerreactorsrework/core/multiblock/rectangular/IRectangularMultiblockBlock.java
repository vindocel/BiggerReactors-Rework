package biggerreactorsrework.core.multiblock.rectangular;

import biggerreactorsrework.core.multiblock.validated.IValidatedMultiblockBlock;
import biggerreactorsrework.core.util.NonnullDefault;

@NonnullDefault
public interface IRectangularMultiblockBlock extends IValidatedMultiblockBlock {
    boolean isGoodForInterior();
    
    boolean isGoodForExterior();
    
    default boolean isGoodForFrame() {
        return isGoodForExterior();
    }
    
    default boolean isGoodForCorner() {
        return isGoodForFrame();
    }
}
