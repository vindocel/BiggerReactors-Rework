package biggerreactorsrework.core.config;

import biggerreactorsrework.core.parsers.Element;
import biggerreactorsrework.core.parsers.JSON5;
import biggerreactorsrework.core.parsers.TOML;

public enum ConfigFormat {
    JSON5,
    TOML,
    ;
    
    public Element parse(String string) {
        return switch (this) {
            case JSON5 -> biggerreactorsrework.core.parsers.JSON5.parseString(string);
            case TOML -> biggerreactorsrework.core.parsers.TOML.parseString(string);
        };
    }
    
    public String parse(Element element) {
        return switch (this) {
            case JSON5 -> biggerreactorsrework.core.parsers.JSON5.parseElement(element);
            case TOML -> biggerreactorsrework.core.parsers.TOML.parseElement(element);
        };
    }
}
