//? if =26.1.2 || =26.3 {
package biggerreactorsrework.core.modular.api;



import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
//?} else if =1.21.1 {
/*package biggerreactorsrework.core.modular.api;


import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
*/
//?}
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
