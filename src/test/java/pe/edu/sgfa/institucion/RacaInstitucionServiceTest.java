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

import pe.edu.sgfa.common.exception.RyeAccesoDenegadoException;
import pe.edu.sgfa.common.exception.RyeValidacionException;
import pe.edu.sgfa.institucion.domain.RacaInstitucion;
import pe.edu.sgfa.institucion.service.RacaInstitucionService;
import pe.edu.sgfa.usuario.service.RyeAdminBootstrapResult;
import pe.edu.sgfa.usuario.service.RyeAuthService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RacaInstitucionServiceTest {

    @BeforeAll
    static void useIsolatedHomeDir() throws IOException {
        Path tempHomeCrr = Files.createTempDirectory("sgfa-institucion-test-home-");
        System.setProperty("sgfa.home-dir", tempHomeCrr.toString());
    }

    @Autowired
    private RacaInstitucionService institucionServiceCrr;

    @Autowired
    private RyeAuthService authServiceCrr;

    @Autowired
    private RyeAdminBootstrapResult adminBootstrapResultCrr;

    @TempDir
    Path tempDir;

    private static RyeAdminBootstrapResult.RyeCredenciales credencialesAdminCrr;

    @BeforeEach
    void loginComoAdmin() {
        if (credencialesAdminCrr == null) {
            credencialesAdminCrr = adminBootstrapResultCrr.ryeConsumir()
                    .orElseThrow(() -> new IllegalStateException("El admin inicial no se genero"));
        }
        authServiceCrr.ryeCerrarSesion();
        authServiceCrr.ryeAutenticar(credencialesAdminCrr.username(), credencialesAdminCrr.password());
    }

    @Test
    void obtenerPrincipalEstaVacioHastaQueSeGuardaAlgo() {
        assertThat(institucionServiceCrr.racaObtenerPrincipal()).isEmpty();

        institucionServiceCrr.racaGuardar("Escuela de prueba", null, null, null, null);

        assertThat(institucionServiceCrr.racaObtenerPrincipal()).isPresent();
    }

    @Test
    void guardarCreaLaPrimeraVezYActualizaLaSegunda() {
        RacaInstitucion creadaCrr = institucionServiceCrr.racaGuardar(
                "IESPP Juliaca", "JULI", "Av. Principal 123", "051-123456", "contacto@juliaca.edu.pe");
        Long id = creadaCrr.getId();

        RacaInstitucion actualizadaCrr = institucionServiceCrr.racaGuardar(
                "IESPP Juliaca (actualizado)", "JULI", "Av. Principal 456", "051-654321", "nuevo@juliaca.edu.pe");

        assertThat(actualizadaCrr.getId()).isEqualTo(id);
        assertThat(actualizadaCrr.getNombre()).isEqualTo("IESPP Juliaca (actualizado)");
        assertThat(actualizadaCrr.getDireccion()).isEqualTo("Av. Principal 456");
        assertThat(institucionServiceCrr.racaObtenerPrincipal()).hasValueSatisfying(
                i -> assertThat(i.getNombre()).isEqualTo("IESPP Juliaca (actualizado)"));
    }

    @Test
    void guardarSinSesionAdminFalla() {
        authServiceCrr.ryeCerrarSesion();

        assertThatThrownBy(() -> institucionServiceCrr.racaGuardar("Cualquiera", null, null, null, null))
                .isInstanceOf(RyeAccesoDenegadoException.class);
    }

    @Test
    void guardarConNombreVacioFalla() {
        assertThatThrownBy(() -> institucionServiceCrr.racaGuardar("   ", null, null, null, null))
                .isInstanceOf(RyeValidacionException.class);
    }

    @Test
    void actualizarLogoCopiaElArchivoAlDirectorioDeConfiguracion() throws IOException {
        institucionServiceCrr.racaGuardar("Institucion con logo", null, null, null, null);

        Path logoOrigenCrr = tempDir.resolve("logo-original.png");
        Files.write(logoOrigenCrr, new byte[]{(byte) 0x89, 'P', 'N', 'G'});

        RacaInstitucion actualizadaCrr = institucionServiceCrr.racaActualizarLogo(logoOrigenCrr);

        assertThat(actualizadaCrr.getLogoPath()).isNotBlank();
        Path logoDestinoCrr = Path.of(actualizadaCrr.getLogoPath());
        assertThat(Files.exists(logoDestinoCrr)).isTrue();
        assertThat(logoDestinoCrr.getFileName().toString()).isEqualTo("logo.png");
    }

    @Test
    void actualizarLogoConExtensionNoPermitidaFalla() {
        institucionServiceCrr.racaGuardar("Institucion con logo invalido", null, null, null, null);
        Path archivoInvalidoCrr = tempDir.resolve("logo.gif");

        assertThatThrownBy(() -> institucionServiceCrr.racaActualizarLogo(archivoInvalidoCrr))
                .isInstanceOf(RyeValidacionException.class);
    }
}
