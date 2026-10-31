package biggerreactorsrework.core.modular.api;



import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
public interface IModularBlock {
    default <Type> Type as(Class<Type> clazz) {
        //noinspection unchecked
        return (Type) this;
    }
    
    BlockModule<?> module(Class<? extends IModularBlock> interfaceClazz);
    
    default <T extends BlockModule<?>> T module(Class<? extends IModularBlock> interfaceClazz, Class<T> moduleType) {
        //noinspection unchecked
        return (T) module(interfaceClazz);
    }
    
    List<BlockModule<?>> modules();
}