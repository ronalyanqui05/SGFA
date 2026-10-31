package pe.edu.sgfa.config;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class RyeAppPathsTest {

    @TempDir
    Path tempDir;

    @Test
    void ensureDirectoriesCreatesAllExpectedSubfolders() {
        Path homeYnq = tempDir.resolve(".sgfa");

        var pathsYnq = RyeAppPaths.ryeEnsureDirectories(homeYnq);

        assertThat(Files.isDirectory(pathsYnq.home())).isTrue();
        assertThat(Files.isDirectory(pathsYnq.data())).isTrue();
        assertThat(Files.isDirectory(pathsYnq.backups())).isTrue();
        assertThat(Files.isDirectory(pathsYnq.documents())).isTrue();
        assertThat(Files.isDirectory(pathsYnq.logs())).isTrue();
        assertThat(Files.isDirectory(pathsYnq.config())).isTrue();
        assertThat(Files.isDirectory(pathsYnq.tessdata())).isTrue();
    }

    @Test
    void resolveHomeDirUsesOverrideWhenProvided() {
        Path resolvedYnq = RyeAppPaths.ryeResolveHomeDir(tempDir.resolve("custom").toString());

        assertThat(resolvedYnq).isEqualTo(tempDir.resolve("custom"));
    }

    @Test
    void resolveHomeDirFallsBackToUserHomeWhenNoOverride() {
        Path resolvedYnq = RyeAppPaths.ryeResolveHomeDir(null);

        assertThat(resolvedYnq.toString()).endsWith(RyeAppPaths.DEFAULT_FOLDER_NAME);
    }
}
