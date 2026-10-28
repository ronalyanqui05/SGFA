package pe.edu.sgfa.config;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class AppPathsTest {

    @TempDir
    Path tempDir;

    @Test
    void ensureDirectoriesCreatesAllExpectedSubfolders() {
        Path home = tempDir.resolve(".sgfa");

        var pths = AppPaths.ensureDirectories(home);

        assertThat(Files.isDirectory(pths.home())).isTrue();
        assertThat(Files.isDirectory(pths.data())).isTrue();
        assertThat(Files.isDirectory(pths.backups())).isTrue();
        assertThat(Files.isDirectory(pths.documents())).isTrue();
        assertThat(Files.isDirectory(pths.logs())).isTrue();
        assertThat(Files.isDirectory(pths.config())).isTrue();
        assertThat(Files.isDirectory(pths.tessdata())).isTrue();
    }

    @Test
    void resolveHomeDirUsesOverrideWhenProvided() {
        Path rslvd = AppPaths.resolveHomeDir(tempDir.resolve("custom").toString());

        assertThat(rslvd).isEqualTo(tempDir.resolve("custom"));
    }

    @Test
    void resolveHomeDirFallsBackToUserHomeWhenNoOverride() {
        Path rslvd = AppPaths.resolveHomeDir(null);

        assertThat(rslvd.toString()).endsWith(AppPaths.DEFAULT_FOLDER_NAME);
    }
}
