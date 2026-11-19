//? if =26.1.2 || =26.3 {
package biggerreactorsrework.core.client.gui.api;


import javax.annotation.ParametersAreNonnullByDefault;
//?} else if =1.21.1 {
/*package biggerreactorsrework.core.client.gui.api;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;
*/
//?}

//? if =26.1.2 || =26.3 {
@ParametersAreNonnullByDefault
public interface IHasUpdatableState<T> {
//?} else if =1.21.1 {
/*@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface IHasUpdatableState<T> {
*/
//?}
    /**
     * @return The current state of the tile.
     */
    T getState();

    /**
     * Call for an update to the current state information.
     */
    void updateState();
}
