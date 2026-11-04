package biggerreactorsrework.gametest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Added 2026-11-03: study output goes to a folder named by the environment; without one the measurement only runs. */
final class StudyReports {
    private StudyReports() { }

    static void write(String name, CharSequence content) throws IOException {
        String folder = System.getProperty("biggerreactorsrework.reports", System.getenv("BIGGERREACTORSREWORK_REPORTS"));
        if (folder == null || folder.isBlank()) return;
        var path = Path.of(folder, name);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }
}
