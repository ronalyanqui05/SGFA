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

import pe.edu.sgfa.backup.service.BackupService;
import pe.edu.sgfa.backup.service.InfoBackup;
import pe.edu.sgfa.common.exception.AccesoDenegadoException;
import pe.edu.sgfa.institucion.service.InstitucionService;
import pe.edu.sgfa.usuario.domain.RolUsuario;
import pe.edu.sgfa.usuario.service.AdminBootstrapResult;
import pe.edu.sgfa.usuario.service.AuthService;
import pe.edu.sgfa.usuario.service.UsuarioService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BackupServiceTest {

    @BeforeAll
    static void useIsolatedHomeDir() throws IOException {
        Path tmpHm = Files.createTempDirectory("sgfa-backup-test-home-");
        System.setProperty("sgfa.home-dir", tmpHm.toString());
    }

    @Autowired
    private InstitucionService institucionService;
    @Autowired
    private BackupService backupService;
    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private AuthService authService;
    @Autowired
    private AdminBootstrapResult adminBootstrapResult;

    private static AdminBootstrapResult.Credenciales credencialesAdmin;

    @BeforeEach
    void autenticarComoAdmin() {
        if (credencialesAdmin == null) {
            credencialesAdmin = adminBootstrapResult.consumir()
                    .orElseThrow(() -> new IllegalStateException("El admin inicial no se genero"));
        }
        authService.cerrarSesion();
        authService.autenticar(credencialesAdmin.username(), credencialesAdmin.password());

        if (institucionService.obtenerPrincipal().isEmpty()) {
            institucionService.guardar("IESPP Juliaca", "JULI", "Av. Principal 123", null, null);
        }
    }

    @Test
    void respaldarCreaUnArchivoYListarLoIncluye() {
        Path archivo = backupService.respaldar();

        assertThat(Files.exists(archivo)).isTrue();
        assertThat(archivo.getFileName().toString()).startsWith("sgfa_").endsWith(".db");

        List<InfoBackup> bckps = backupService.listar();
        assertThat(bckps).anySatisfy(b -> assertThat(b.archivo()).isEqualTo(archivo));
    }

    @Test
    void requiereRolAdmin() {
        String usrnmOprdr = "operador" + System.nanoTime();
        usuarioService.crear(usrnmOprdr, "clave1234", "Operador de prueba", RolUsuario.OPERADOR);
        authService.cerrarSesion();
        authService.autenticar(usrnmOprdr, "clave1234");

        assertThatThrownBy(backupService::respaldar).isInstanceOf(AccesoDenegadoException.class);
        assertThatThrownBy(backupService::listar).isInstanceOf(AccesoDenegadoException.class);
    }
}
