package biggerreactorsrework.core.client.gui.api;


import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public interface IHasUpdatableState<T> {
    /**
     * @return The current state of the tile.
     */
    T getState();

    /**
     * Call for an update to the current state information.
     */
    void updateState();
}
