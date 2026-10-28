package pe.edu.sgfa.backup.service;

import java.nio.file.Path;
import java.time.Instant;

public record InfoBackup(Path archivo, Instant fecha, long tamanoBytes) {

    public String nombreArchivo() {
        return archivo.getFileName().toString();
    }
}
