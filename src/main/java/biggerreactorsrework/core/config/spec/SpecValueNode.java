package biggerreactorsrework.core.config.spec;

import biggerreactorsrework.core.config.ConfigValue;
import biggerreactorsrework.core.util.NonnullDefault;

import javax.annotation.Nullable;
import java.lang.reflect.Field;

@NonnullDefault
public abstract class SpecValueNode extends SpecNode {
    public final Field field;
    public final SpecObjectNode parent;
    
    protected SpecValueNode(SpecObjectNode parent, Field field, ConfigOptionsDefaults defaults) {
        super(field.getName(), field.getAnnotation(ConfigValue.class), defaults);
        field.setAccessible(true);
        this.field = field;
        this.parent = parent;
    }
    
    protected Object currentValueObject() {
        try {
            return field.get(parent.object());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    
    protected void writeObject(@Nullable Object object) {
        try {
            field.set(parent.object(), object);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    
    public abstract String defaultValueAsString();
    
    public abstract String currentValueAsString();
    
    public abstract void writeFromString(String string);
    
    public abstract boolean isValueValid(String valueString);
    
    public abstract String generateComment();
}
