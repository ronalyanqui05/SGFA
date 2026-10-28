package pe.edu.sgfa.backup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import pe.edu.sgfa.backup.service.BackupService;
import pe.edu.sgfa.estudiante.service.EstudianteService;
import pe.edu.sgfa.institucion.service.InstitucionService;
import pe.edu.sgfa.usuario.service.AdminBootstrapResult;
import pe.edu.sgfa.usuario.service.AuthService;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BackupServiceRestaurarTest {

    @BeforeAll
    static void useIsolatedHomeDir() throws IOException {
        Path tmpHm = Files.createTempDirectory("sgfa-backup-restaurar-test-home-");
        System.setProperty("sgfa.home-dir", tmpHm.toString());
    }

    @Autowired
    private InstitucionService institucionService;
    @Autowired
    private EstudianteService estudianteService;
    @Autowired
    private BackupService backupService;
    @Autowired
    private AuthService authService;
    @Autowired
    private AdminBootstrapResult adminBootstrapResult;
    @Value("${sgfa.data-dir}")
    private String dataDir;

    @Test
    void restaurarReemplazaElContenidoPorElDelRespaldo() throws Exception {
        AdminBootstrapResult.Credenciales crdncls = adminBootstrapResult.consumir()
                .orElseThrow(() -> new IllegalStateException("El admin inicial no se genero"));
        authService.autenticar(crdncls.username(), crdncls.password());
        institucionService.guardar("IESPP Juliaca", "JULI", "Av. Principal 123", null, null);
        estudianteService.crear("PRE" + System.nanoTime(), null, "Antes", "DelBackup", null, null, null, null, null,
                null);

        Path backup = backupService.respaldar();

        estudianteService.crear("POST" + System.nanoTime(), null, "Despues", "DelBackup", null, null, null, null,
                null, null);
        assertThat(contarEstudiantes()).isEqualTo(2);

        backupService.restaurar(backup);

        assertThat(contarEstudiantes()).isEqualTo(1);
    }

    private int contarEstudiantes() throws Exception {
        String url = "jdbc:sqlite:" + Path.of(dataDir).resolve("sgfa.db");
        try (Connection cnxn = DriverManager.getConnection(url);
             Statement sntnc = cnxn.createStatement();
             ResultSet rsltd = sntnc.executeQuery("SELECT COUNT(*) FROM estudiantes")) {
            rsltd.next();
            return rsltd.getInt(1);
        }
    }
}
