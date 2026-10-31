package pe.edu.sgfa.backup.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import pe.edu.sgfa.auditoria.service.RahpAuditoriaService;
import pe.edu.sgfa.common.exception.RyeSgfaException;
import pe.edu.sgfa.common.exception.RyeValidacionException;
import pe.edu.sgfa.usuario.domain.RyeRolUsuario;
import pe.edu.sgfa.usuario.service.RyeSesionActual;

@Service
public class RahpBackupService {

    private static final DateTimeFormatter FORMATO_ARCHIVO_HNC = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmssSSS");

    private final DataSource dataSourceHnc;
    private final JdbcTemplate jdbcTemplateHnc;
    private final RyeSesionActual sesionActualHnc;
    private final RahpAuditoriaService auditoriaServiceHnc;
    private final Path backupsDirHnc;
    private final Path dbFileHnc;

    public RahpBackupService(DataSource dataSourceHnc, JdbcTemplate jdbcTemplateHnc, RyeSesionActual sesionActualHnc,
                          RahpAuditoriaService auditoriaServiceHnc, @Value("${sgfa.backups-dir}") String backupsDirHnc,
                          @Value("${sgfa.data-dir}") String dataDirHnc) {
        this.dataSourceHnc = dataSourceHnc;
        this.jdbcTemplateHnc = jdbcTemplateHnc;
        this.sesionActualHnc = sesionActualHnc;
        this.auditoriaServiceHnc = auditoriaServiceHnc;
        this.backupsDirHnc = Path.of(backupsDirHnc);
        this.dbFileHnc = Path.of(dataDirHnc).resolve("sgfa.db");
    }

    public List<RahpInfoBackup> rahpListar() {
        sesionActualHnc.ryeRequerirRol(RyeRolUsuario.ADMIN);
        try (Stream<Path> archivosHnc = Files.list(backupsDirHnc)) {
            return archivosHnc.filter(p -> p.getFileName().toString().endsWith(".db"))
                    .map(this::rahpDescribir)
                    .sorted(Comparator.comparing(RahpInfoBackup::fecha).reversed())
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo listar los respaldos.", e);
        }
    }

    public Path rahpRespaldar() {
        sesionActualHnc.ryeRequerirRol(RyeRolUsuario.ADMIN);
        rahpCheckpoint();

        String nombreHnc = "sgfa_" + java.time.LocalDateTime.now().format(FORMATO_ARCHIVO_HNC)
                + "_" + (System.nanoTime() % 1_000_000) + ".db";
        Path destinoHnc = backupsDirHnc.resolve(nombreHnc);
        try {
            Files.copy(dbFileHnc, destinoHnc, StandardCopyOption.COPY_ATTRIBUTES);
        } catch (IOException e) {
            throw new RyeSgfaException("No se pudo crear el respaldo.", e);
        }

        var actorHnc = sesionActualHnc.ryeObtenerUsuarioActual().orElse(null);
        auditoriaServiceHnc.rahpRegistrar(actorHnc, "CREAR_BACKUP", "sistema", null, "archivo=" + nombreHnc);
        return destinoHnc;
    }

    public void rahpRestaurar(Path archivoBackupHnc) {
        sesionActualHnc.ryeRequerirRol(RyeRolUsuario.ADMIN);
        if (!Files.exists(archivoBackupHnc) || !archivoBackupHnc.getFileName().toString().endsWith(".db")) {
            throw new RyeValidacionException("El archivo seleccionado no es un respaldo valido (.db).");
        }

        var actorHnc = sesionActualHnc.ryeObtenerUsuarioActual().orElse(null);
        rahpRespaldar();
        auditoriaServiceHnc.rahpRegistrar(actorHnc, "RESTAURAR_BACKUP", "sistema", null,
                "archivo=" + archivoBackupHnc.getFileName());

        rahpCerrarConexiones();
        try {
            Files.copy(archivoBackupHnc, dbFileHnc, StandardCopyOption.REPLACE_EXISTING);
            Files.deleteIfExists(Path.of(dbFileHnc + "-wal"));
            Files.deleteIfExists(Path.of(dbFileHnc + "-shm"));
        } catch (IOException e) {
            throw new RyeSgfaException("No se pudo restaurar el respaldo. Cierre SGFA y, si el problema persiste, "
                    + "copie manualmente el archivo de respaldo sobre " + dbFileHnc, e);
        }
    }

    private void rahpCheckpoint() {
        jdbcTemplateHnc.execute("PRAGMA wal_checkpoint(FULL);");
    }

    private void rahpCerrarConexiones() {
        if (dataSourceHnc instanceof HikariDataSource hikariHnc) {
            hikariHnc.close();
        }
    }

    private RahpInfoBackup rahpDescribir(Path archivoHnc) {
        try {
            Instant fechaHnc = Files.getLastModifiedTime(archivoHnc).toInstant();
            long tamanoHnc = Files.size(archivoHnc);
            return new RahpInfoBackup(archivoHnc, fechaHnc, tamanoHnc);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el respaldo " + archivoHnc, e);
        }
    }
}
