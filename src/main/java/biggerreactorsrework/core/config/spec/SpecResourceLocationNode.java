//? if =26.1.2 || =26.3 {
package biggerreactorsrework.core.config.spec;

import net.minecraft.resources.Identifier;
import biggerreactorsrework.core.parsers.Element;
//?} else if =1.21.1 {
/*package biggerreactorsrework.core.config.spec;

import net.minecraft.resources.ResourceLocation;
import biggerreactorsrework.core.parsers.Element;
*/
//?}
import biggerreactorsrework.core.util.NonnullDefault;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

@NonnullDefault
//? if =26.1.2 || =26.3 {
public class SpecResourceLocationNode extends SpecValueNode {
    public final Identifier defaultValue;

    SpecResourceLocationNode(SpecObjectNode parent, Field field, ConfigOptionsDefaults defaults) {
        super(parent, field, defaults);
        this.defaultValue = (Identifier) currentValueObject();
    }
//?} else if =1.21.1 {
/*public class SpecResourceLocationNode extends SpecValueNode {
    public final ResourceLocation defaultValue;

    SpecResourceLocationNode(SpecObjectNode parent, Field field, ConfigOptionsDefaults defaults) {
        super(parent, field, defaults);
        this.defaultValue = (ResourceLocation) currentValueObject();
    }
*/
//?}

    @Override
    public String defaultValueAsString() {
        return String.valueOf(defaultValue);
    }

    @Override
    public String currentValueAsString() {
        return String.valueOf(currentValueObject());
    }

    @Override
//? if =26.1.2 || =26.3 {
    public void writeFromString(String string) {
        writeObject(string.equalsIgnoreCase("null") ? null : Identifier.parse(string));
    }
//?} else if =1.21.1 {
/*    public void writeFromString(String string) {
        writeObject(string.equalsIgnoreCase("null") ? null : ResourceLocation.parse(string));
    }
*/
//?}

    @Override
//? if =26.1.2 || =26.3 {
    public boolean isValueValid(String valueString) {
        return Identifier.tryParse(valueString) != null;
    }
//?} else if =1.21.1 {
/*    public boolean isValueValid(String valueString) {
        return ResourceLocation.tryParse(valueString) != null;
    }
*/
//?}

    @Override
    public String generateComment() {
        return baseComment;
    }

    @Override
    public void writeDefault() {
        writeObject(defaultValue);
    }

    @Nullable
    @Override
    public Element generateDefaultElement() {
        return new Element(Element.Type.String, generateComment(), name, defaultValueAsString());
    }

    @Nullable
    @Override
    public Element generateCurrentElement() {
        return new Element(Element.Type.String, generateComment(), name, currentValueAsString());
    }

    @Override
    public Element generateSyncElement() {
        return new Element(Element.Type.String, null, name, currentValueAsString());
    }

    @Nullable
    @Override
    public Element correctToValidState(Element element) {
        if (element.type != Element.Type.String || !(element.value instanceof String)) {
            return generateDefaultElement();
        }
        if (!isValueValid((String) element.value)) {
            return generateCurrentElement();
        }
        return new Element(Element.Type.String, baseComment, name, element.value);
    }

    @Override
    public void writeElement(Element element) {
        writeFromString(element.asString());
    }
}
