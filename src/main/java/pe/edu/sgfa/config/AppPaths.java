package pe.edu.sgfa.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class AppPaths {

    public static final String DEFAULT_FOLDER_NAME = ".sgfa";

    private AppPaths() {
    }

    public static Path resolveHomeDir(String ovrrd) {
        if (ovrrd != null && !ovrrd.isBlank()) {
            return Path.of(ovrrd);
        }
        return Path.of(System.getProperty("user.home"), DEFAULT_FOLDER_NAME);
    }

    public static void sembrarDatosIniciales(Path hmDir, Path smll) {
        if (smll == null || !Files.isDirectory(smll) || Files.exists(hmDir.resolve("data").resolve("sgfa.db"))) {
            return;
        }
        try (var archvs = Files.walk(smll)) {
            for (Path orgn : (Iterable<Path>) archvs::iterator) {
                Path dstn = hmDir.resolve(smll.relativize(orgn).toString());
                if (Files.isDirectory(orgn)) {
                    Files.createDirectories(dstn);
                } else if (!Files.exists(dstn)) {
                    Files.createDirectories(dstn.getParent());
                    Files.copy(orgn, dstn);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudieron copiar los datos iniciales a " + hmDir, e);
        }
    }

    public static SgfaPaths ensureDirectories(Path hmDir) {
        Path data = hmDir.resolve("data");
        Path backups = hmDir.resolve("backups");
        Path documents = hmDir.resolve("documents");
        Path logs = hmDir.resolve("logs");
        Path config = hmDir.resolve("config");

        Path tessdata = hmDir.resolve("tessdata");
        try {
            for (Path p : List.of(hmDir, data, backups, documents, logs, config, tessdata)) {
                Files.createDirectories(p);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "No se pudo crear el directorio de datos de SGFA en " + hmDir, e);
        }
        return new SgfaPaths(hmDir, data, backups, documents, logs, config, tessdata);
    }

    public record SgfaPaths(Path home, Path data, Path backups, Path documents, Path logs, Path config,
                              Path tessdata) {
    }
}
