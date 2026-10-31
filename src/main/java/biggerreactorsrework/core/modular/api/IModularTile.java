package biggerreactorsrework.core.modular.api;


import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
public interface IModularTile {
    
    default <Type> Type as(Class<Type> clazz) {
        //noinspection unchecked
        return (Type) this;
    }
    
    @Nullable
    TileModule<?> module(Class<?> interfaceClazz);
    
    @Nullable
    default <T extends TileModule<?>> T module(Class<?> interfaceClazz, Class<T> moduleType) {
        //noinspection unchecked
        return (T) module(interfaceClazz);
    }
    
    List<TileModule<?>> modules();
}
