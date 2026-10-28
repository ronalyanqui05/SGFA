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

import pe.edu.sgfa.auditoria.service.AuditoriaService;
import pe.edu.sgfa.common.exception.SgfaException;
import pe.edu.sgfa.common.exception.ValidacionException;
import pe.edu.sgfa.usuario.domain.RolUsuario;
import pe.edu.sgfa.usuario.service.SesionActual;

@Service
public class BackupService {

    private static final DateTimeFormatter FORMATO_ARCHIVO = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmssSSS");

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final SesionActual sesionActual;
    private final AuditoriaService auditoriaService;
    private final Path backupsDir;
    private final Path dbFile;

    public BackupService(DataSource dataSource, JdbcTemplate jdbcTemplate, SesionActual sesionActual,
                          AuditoriaService auditoriaService, @Value("${sgfa.backups-dir}") String backupsDir,
                          @Value("${sgfa.data-dir}") String dtDir) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
        this.sesionActual = sesionActual;
        this.auditoriaService = auditoriaService;
        this.backupsDir = Path.of(backupsDir);
        this.dbFile = Path.of(dtDir).resolve("sgfa.db");
    }

    public List<InfoBackup> listar() {
        sesionActual.requerirRol(RolUsuario.ADMIN);
        try (Stream<Path> archvs = Files.list(backupsDir)) {
            return archvs.filter(p -> p.getFileName().toString().endsWith(".db"))
                    .map(this::describir)
                    .sorted(Comparator.comparing(InfoBackup::fecha).reversed())
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo listar los respaldos.", e);
        }
    }

    public Path respaldar() {
        sesionActual.requerirRol(RolUsuario.ADMIN);
        checkpoint();

        String nmbr = "sgfa_" + java.time.LocalDateTime.now().format(FORMATO_ARCHIVO)
                + "_" + (System.nanoTime() % 1_000_000) + ".db";
        Path dstn = backupsDir.resolve(nmbr);
        try {
            Files.copy(dbFile, dstn, StandardCopyOption.COPY_ATTRIBUTES);
        } catch (IOException e) {
            throw new SgfaException("No se pudo crear el respaldo.", e);
        }

        var actr = sesionActual.obtenerUsuarioActual().orElse(null);
        auditoriaService.registrar(actr, "CREAR_BACKUP", "sistema", null, "archivo=" + nmbr);
        return dstn;
    }

    public void restaurar(Path archvBckp) {
        sesionActual.requerirRol(RolUsuario.ADMIN);
        if (!Files.exists(archvBckp) || !archvBckp.getFileName().toString().endsWith(".db")) {
            throw new ValidacionException("El archivo seleccionado no es un respaldo valido (.db).");
        }

        var actr = sesionActual.obtenerUsuarioActual().orElse(null);
        respaldar();
        auditoriaService.registrar(actr, "RESTAURAR_BACKUP", "sistema", null,
                "archivo=" + archvBckp.getFileName());

        cerrarConexiones();
        try {
            Files.copy(archvBckp, dbFile, StandardCopyOption.REPLACE_EXISTING);
            Files.deleteIfExists(Path.of(dbFile + "-wal"));
            Files.deleteIfExists(Path.of(dbFile + "-shm"));
        } catch (IOException e) {
            throw new SgfaException("No se pudo restaurar el respaldo. Cierre SGFA y, si el problema persiste, "
                    + "copie manualmente el archivo de respaldo sobre " + dbFile, e);
        }
    }

    private void checkpoint() {
        jdbcTemplate.execute("PRAGMA wal_checkpoint(FULL);");
    }

    private void cerrarConexiones() {
        if (dataSource instanceof HikariDataSource hikari) {
            hikari.close();
        }
    }

    private InfoBackup describir(Path archv) {
        try {
            Instant fecha = Files.getLastModifiedTime(archv).toInstant();
            long tmn = Files.size(archv);
            return new InfoBackup(archv, fecha, tmn);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el respaldo " + archv, e);
        }
    }
}
