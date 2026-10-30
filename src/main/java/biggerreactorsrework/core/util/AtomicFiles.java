package biggerreactorsrework.core.util;

// Modified 2026-10-30: preserve the previous file until its replacement has been fully written.

import java.io.IOException;
import java.nio.file.*;

public final class AtomicFiles {
    private AtomicFiles() { }

    @FunctionalInterface
    public interface Writer { void write(Path path) throws IOException; }

    public static void replace(Path target, Writer writer) throws IOException {
        Path directory = target.toAbsolutePath().getParent();
        Files.createDirectories(directory);
        Path temporary = Files.createTempFile(directory, target.getFileName().toString(), ".tmp");
        try {
            writer.write(temporary);
            try { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException unavailable) { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
}
