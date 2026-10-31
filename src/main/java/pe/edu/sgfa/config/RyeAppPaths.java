package pe.edu.sgfa.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class RyeAppPaths {

    public static final String DEFAULT_FOLDER_NAME = ".sgfa";

    private RyeAppPaths() {
    }

    public static Path ryeResolveHomeDir(String overrideYnq) {
        if (overrideYnq != null && !overrideYnq.isBlank()) {
            return Path.of(overrideYnq);
        }
        return Path.of(System.getProperty("user.home"), DEFAULT_FOLDER_NAME);
    }

    public static void ryeSembrarDatosIniciales(Path homeDirYnq, Path semillaYnq) {
        if (semillaYnq == null || !Files.isDirectory(semillaYnq) || Files.exists(homeDirYnq.resolve("data").resolve("sgfa.db"))) {
            return;
        }
        try (var archivosYnq = Files.walk(semillaYnq)) {
            for (Path origenYnq : (Iterable<Path>) archivosYnq::iterator) {
                Path destinoYnq = homeDirYnq.resolve(semillaYnq.relativize(origenYnq).toString());
                if (Files.isDirectory(origenYnq)) {
                    Files.createDirectories(destinoYnq);
                } else if (!Files.exists(destinoYnq)) {
                    Files.createDirectories(destinoYnq.getParent());
                    Files.copy(origenYnq, destinoYnq);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudieron copiar los datos iniciales a " + homeDirYnq, e);
        }
    }

    public static RyeSgfaPaths ryeEnsureDirectories(Path homeDirYnq) {
        Path dataYnq = homeDirYnq.resolve("data");
        Path backupsYnq = homeDirYnq.resolve("backups");
        Path documentsYnq = homeDirYnq.resolve("documents");
        Path logsYnq = homeDirYnq.resolve("logs");
        Path configYnq = homeDirYnq.resolve("config");

        Path tessdataYnq = homeDirYnq.resolve("tessdata");
        try {
            for (Path p : List.of(homeDirYnq, dataYnq, backupsYnq, documentsYnq, logsYnq, configYnq, tessdataYnq)) {
                Files.createDirectories(p);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "No se pudo crear el directorio de datos de SGFA en " + homeDirYnq, e);
        }
        return new RyeSgfaPaths(homeDirYnq, dataYnq, backupsYnq, documentsYnq, logsYnq, configYnq, tessdataYnq);
    }

    public record RyeSgfaPaths(Path home, Path data, Path backups, Path documents, Path logs, Path config,
                              Path tessdata) {
    }
}
