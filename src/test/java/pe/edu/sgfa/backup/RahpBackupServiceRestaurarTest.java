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

import pe.edu.sgfa.backup.service.RahpBackupService;
import pe.edu.sgfa.estudiante.service.YmclEstudianteService;
import pe.edu.sgfa.institucion.service.RacaInstitucionService;
import pe.edu.sgfa.usuario.service.RyeAdminBootstrapResult;
import pe.edu.sgfa.usuario.service.RyeAuthService;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RahpBackupServiceRestaurarTest {

    @BeforeAll
    static void useIsolatedHomeDir() throws IOException {
        Path tempHomeHnc = Files.createTempDirectory("sgfa-backup-restaurar-test-home-");
        System.setProperty("sgfa.home-dir", tempHomeHnc.toString());
    }

    @Autowired
    private RacaInstitucionService institucionServiceHnc;
    @Autowired
    private YmclEstudianteService estudianteServiceHnc;
    @Autowired
    private RahpBackupService backupServiceHnc;
    @Autowired
    private RyeAuthService authServiceHnc;
    @Autowired
    private RyeAdminBootstrapResult adminBootstrapResultHnc;
    @Value("${sgfa.data-dir}")
    private String dataDirHnc;

    @Test
    void restaurarReemplazaElContenidoPorElDelRespaldo() throws Exception {
        RyeAdminBootstrapResult.RyeCredenciales credencialesHnc = adminBootstrapResultHnc.ryeConsumir()
                .orElseThrow(() -> new IllegalStateException("El admin inicial no se genero"));
        authServiceHnc.ryeAutenticar(credencialesHnc.username(), credencialesHnc.password());
        institucionServiceHnc.racaGuardar("IESPP Juliaca", "JULI", "Av. Principal 123", null, null);
        estudianteServiceHnc.ymclCrear("PRE" + System.nanoTime(), null, "Antes", "DelBackup", null, null, null, null, null,
                null);

        Path backupHnc = backupServiceHnc.rahpRespaldar();

        estudianteServiceHnc.ymclCrear("POST" + System.nanoTime(), null, "Despues", "DelBackup", null, null, null, null,
                null, null);
        assertThat(contarEstudiantes()).isEqualTo(2);

        backupServiceHnc.rahpRestaurar(backupHnc);

        assertThat(contarEstudiantes()).isEqualTo(1);
    }

    private int contarEstudiantes() throws Exception {
        String urlHnc = "jdbc:sqlite:" + Path.of(dataDirHnc).resolve("sgfa.db");
        try (Connection conexionHnc = DriverManager.getConnection(urlHnc);
             Statement sentenciaHnc = conexionHnc.createStatement();
             ResultSet resultadoHnc = sentenciaHnc.executeQuery("SELECT COUNT(*) FROM estudiantes")) {
            resultadoHnc.next();
            return resultadoHnc.getInt(1);
        }
    }
}
