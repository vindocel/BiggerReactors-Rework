package biggerreactorsrework.core.serialization;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ISerializable {
    @Nullable
    DataCompound save();
    
    void load(@Nonnull DataCompound compound);
}
