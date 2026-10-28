package pe.edu.sgfa.institucion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import pe.edu.sgfa.common.exception.AccesoDenegadoException;
import pe.edu.sgfa.common.exception.ValidacionException;
import pe.edu.sgfa.institucion.domain.Institucion;
import pe.edu.sgfa.institucion.service.InstitucionService;
import pe.edu.sgfa.usuario.service.AdminBootstrapResult;
import pe.edu.sgfa.usuario.service.AuthService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class InstitucionServiceTest {

    @BeforeAll
    static void useIsolatedHomeDir() throws IOException {
        Path tmpHm = Files.createTempDirectory("sgfa-institucion-test-home-");
        System.setProperty("sgfa.home-dir", tmpHm.toString());
    }

    @Autowired
    private InstitucionService institucionService;

    @Autowired
    private AuthService authService;

    @Autowired
    private AdminBootstrapResult adminBootstrapResult;

    @TempDir
    Path tempDir;

    private static AdminBootstrapResult.Credenciales credencialesAdmin;

    @BeforeEach
    void loginComoAdmin() {
        if (credencialesAdmin == null) {
            credencialesAdmin = adminBootstrapResult.consumir()
                    .orElseThrow(() -> new IllegalStateException("El admin inicial no se genero"));
        }
        authService.cerrarSesion();
        authService.autenticar(credencialesAdmin.username(), credencialesAdmin.password());
    }

    @Test
    void obtenerPrincipalEstaVacioHastaQueSeGuardaAlgo() {
        assertThat(institucionService.obtenerPrincipal()).isEmpty();

        institucionService.guardar("Escuela de prueba", null, null, null, null);

        assertThat(institucionService.obtenerPrincipal()).isPresent();
    }

    @Test
    void guardarCreaLaPrimeraVezYActualizaLaSegunda() {
        Institucion crd = institucionService.guardar(
                "IESPP Juliaca", "JULI", "Av. Principal 123", "051-123456", "contacto@juliaca.edu.pe");
        Long id = crd.getId();

        Institucion actlzd = institucionService.guardar(
                "IESPP Juliaca (actualizado)", "JULI", "Av. Principal 456", "051-654321", "nuevo@juliaca.edu.pe");

        assertThat(actlzd.getId()).isEqualTo(id);
        assertThat(actlzd.getNombre()).isEqualTo("IESPP Juliaca (actualizado)");
        assertThat(actlzd.getDireccion()).isEqualTo("Av. Principal 456");
        assertThat(institucionService.obtenerPrincipal()).hasValueSatisfying(
                i -> assertThat(i.getNombre()).isEqualTo("IESPP Juliaca (actualizado)"));
    }

    @Test
    void guardarSinSesionAdminFalla() {
        authService.cerrarSesion();

        assertThatThrownBy(() -> institucionService.guardar("Cualquiera", null, null, null, null))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    void guardarConNombreVacioFalla() {
        assertThatThrownBy(() -> institucionService.guardar("   ", null, null, null, null))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void actualizarLogoCopiaElArchivoAlDirectorioDeConfiguracion() throws IOException {
        institucionService.guardar("Institucion con logo", null, null, null, null);

        Path lgOrgn = tempDir.resolve("logo-original.png");
        Files.write(lgOrgn, new byte[]{(byte) 0x89, 'P', 'N', 'G'});

        Institucion actlzd = institucionService.actualizarLogo(lgOrgn);

        assertThat(actlzd.getLogoPath()).isNotBlank();
        Path lgDstn = Path.of(actlzd.getLogoPath());
        assertThat(Files.exists(lgDstn)).isTrue();
        assertThat(lgDstn.getFileName().toString()).isEqualTo("logo.png");
    }

    @Test
    void actualizarLogoConExtensionNoPermitidaFalla() {
        institucionService.guardar("Institucion con logo invalido", null, null, null, null);
        Path archvInvld = tempDir.resolve("logo.gif");

        assertThatThrownBy(() -> institucionService.actualizarLogo(archvInvld))
                .isInstanceOf(ValidacionException.class);
    }
}
