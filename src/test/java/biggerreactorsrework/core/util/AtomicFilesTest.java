package biggerreactorsrework.core.util;

// Modified 2026-10-30: failed writes must preserve previously saved data and clean their own temporary file.

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class AtomicFilesTest {
    @TempDir Path directory;

    @Test void failedWriterCannotReplaceExistingData() throws Exception {
        var file=directory.resolve("library.dat");Files.writeString(file,"saved");
        assertThrows(IOException.class,()->AtomicFiles.replace(file,temp->{
            Files.writeString(temp,"incomplete");throw new IOException("Disk write interrupted");
        }));
        assertEquals("saved",Files.readString(file));
        try(var paths=Files.list(directory)) { assertEquals(1,paths.count()); }
    }

    @Test void completedWriteReplacesTheFile() throws Exception {
        var file=directory.resolve("nested/library.dat");
        AtomicFiles.replace(file,temp->Files.writeString(temp,"first"));
        AtomicFiles.replace(file,temp->Files.writeString(temp,"second"));
        assertEquals("second",Files.readString(file));
    }
}
