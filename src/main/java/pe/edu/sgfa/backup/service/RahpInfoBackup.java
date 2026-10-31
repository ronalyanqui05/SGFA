package pe.edu.sgfa.backup.service;

import java.nio.file.Path;
import java.time.Instant;

public record RahpInfoBackup(Path archivo, Instant fecha, long tamanoBytes) {

    public String rahpNombreArchivo() {
        return archivo.getFileName().toString();
    }
}
