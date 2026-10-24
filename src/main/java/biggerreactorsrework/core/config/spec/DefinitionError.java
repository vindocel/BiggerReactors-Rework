package biggerreactorsrework.core.config.spec;

import biggerreactorsrework.core.util.NonnullDefault;

@NonnullDefault
public class DefinitionError extends RuntimeException {
    public DefinitionError(String message) {
        super(message);
    }
}
