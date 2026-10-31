package pe.edu.sgfa.backup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import pe.edu.sgfa.backup.service.RahpBackupService;
import pe.edu.sgfa.backup.service.RahpInfoBackup;
import pe.edu.sgfa.common.exception.RyeAccesoDenegadoException;
import pe.edu.sgfa.institucion.service.RacaInstitucionService;
import pe.edu.sgfa.usuario.domain.RyeRolUsuario;
import pe.edu.sgfa.usuario.service.RyeAdminBootstrapResult;
import pe.edu.sgfa.usuario.service.RyeAuthService;
import pe.edu.sgfa.usuario.service.RyeUsuarioService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RahpBackupServiceTest {

    @BeforeAll
    static void useIsolatedHomeDir() throws IOException {
        Path tempHomeHnc = Files.createTempDirectory("sgfa-backup-test-home-");
        System.setProperty("sgfa.home-dir", tempHomeHnc.toString());
    }

    @Autowired
    private RacaInstitucionService institucionServiceHnc;
    @Autowired
    private RahpBackupService backupServiceHnc;
    @Autowired
    private RyeUsuarioService usuarioServiceHnc;
    @Autowired
    private RyeAuthService authServiceHnc;
    @Autowired
    private RyeAdminBootstrapResult adminBootstrapResultHnc;

    private static RyeAdminBootstrapResult.RyeCredenciales credencialesAdminHnc;

    @BeforeEach
    void autenticarComoAdmin() {
        if (credencialesAdminHnc == null) {
            credencialesAdminHnc = adminBootstrapResultHnc.ryeConsumir()
                    .orElseThrow(() -> new IllegalStateException("El admin inicial no se genero"));
        }
        authServiceHnc.ryeCerrarSesion();
        authServiceHnc.ryeAutenticar(credencialesAdminHnc.username(), credencialesAdminHnc.password());

        if (institucionServiceHnc.racaObtenerPrincipal().isEmpty()) {
            institucionServiceHnc.racaGuardar("IESPP Juliaca", "JULI", "Av. Principal 123", null, null);
        }
    }

    @Test
    void respaldarCreaUnArchivoYListarLoIncluye() {
        Path archivoHnc = backupServiceHnc.rahpRespaldar();

        assertThat(Files.exists(archivoHnc)).isTrue();
        assertThat(archivoHnc.getFileName().toString()).startsWith("sgfa_").endsWith(".db");

        List<RahpInfoBackup> backupsHnc = backupServiceHnc.rahpListar();
        assertThat(backupsHnc).anySatisfy(b -> assertThat(b.archivo()).isEqualTo(archivoHnc));
    }

    @Test
    void requiereRolAdmin() {
        String usernameOperadorHnc = "operador" + System.nanoTime();
        usuarioServiceHnc.ryeCrear(usernameOperadorHnc, "clave1234", "Operador de prueba", RyeRolUsuario.OPERADOR);
        authServiceHnc.ryeCerrarSesion();
        authServiceHnc.ryeAutenticar(usernameOperadorHnc, "clave1234");

        assertThatThrownBy(backupServiceHnc::rahpRespaldar).isInstanceOf(RyeAccesoDenegadoException.class);
        assertThatThrownBy(backupServiceHnc::rahpListar).isInstanceOf(RyeAccesoDenegadoException.class);
    }
}
