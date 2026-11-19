//? if =26.1.2 || =26.3 {
package biggerreactorsrework.core.transfer;

/** State a block can copy and put back, so a cancelled transfer transaction leaves it untouched. */
public interface TransferSnapshots {
    Object transferSnapshot();

    void restoreTransferSnapshot(Object snapshot);
}
//?} else if =1.21.1 {
//?}
