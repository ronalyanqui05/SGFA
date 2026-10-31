package pe.edu.sgfa.estudiante;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import pe.edu.sgfa.common.exception.RyeAccesoDenegadoException;
import pe.edu.sgfa.common.exception.RyeValidacionException;
import pe.edu.sgfa.estudiante.domain.YmclEstadoEstudiante;
import pe.edu.sgfa.estudiante.domain.YmclEstudiante;
import pe.edu.sgfa.estudiante.service.YmclEstudianteService;
import pe.edu.sgfa.usuario.domain.RyeRolUsuario;
import pe.edu.sgfa.usuario.service.RyeAdminBootstrapResult;
import pe.edu.sgfa.usuario.service.RyeAuthService;
import pe.edu.sgfa.usuario.service.RyeUsuarioService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class YmclEstudianteServiceTest {

    @BeforeAll
    static void useIsolatedHomeDir() throws IOException {
        Path tempHomeChp = Files.createTempDirectory("sgfa-estudiante-test-home-");
        System.setProperty("sgfa.home-dir", tempHomeChp.toString());
    }

    @Autowired
    private YmclEstudianteService estudianteServiceChp;
    @Autowired
    private RyeUsuarioService usuarioServiceChp;
    @Autowired
    private RyeAuthService authServiceChp;
    @Autowired
    private RyeAdminBootstrapResult adminBootstrapResultChp;

    private static RyeAdminBootstrapResult.RyeCredenciales credencialesAdminChp;

    @BeforeEach
    void loginComoAdmin() {
        if (credencialesAdminChp == null) {
            credencialesAdminChp = adminBootstrapResultChp.ryeConsumir()
                    .orElseThrow(() -> new IllegalStateException("El admin inicial no se genero"));
        }
        authServiceChp.ryeCerrarSesion();
        authServiceChp.ryeAutenticar(credencialesAdminChp.username(), credencialesAdminChp.password());
    }

    @Test
    void crearConDatosMinimosFunciona() {
        YmclEstudiante estudianteChp = estudianteServiceChp.ymclCrear(
                null, null, "Juan Carlos", "Perez", null, null, null, null, null, null);

        assertThat(estudianteChp.getId()).isNotNull();
        assertThat(estudianteChp.getEstado()).isEqualTo(YmclEstadoEstudiante.ACTIVO);
        assertThat(estudianteChp.getFechaRegistro()).isNotNull();
    }

    @Test
    void crearSinNombresFalla() {
        assertThatThrownBy(() -> estudianteServiceChp.ymclCrear(null, null, "  ", "Perez", null, null, null, null, null, null))
                .isInstanceOf(RyeValidacionException.class);
    }

    @Test
    void crearConDniInvalidoFalla() {
        assertThatThrownBy(() -> estudianteServiceChp.ymclCrear(
                null, "123", "Maria", "Lopez", null, null, null, null, null, null))
                .isInstanceOf(RyeValidacionException.class);
    }

    @Test
    void crearConCodigoODniDuplicadoFalla() {
        estudianteServiceChp.ymclCrear("EST001", "12345678", "Ana", "Torres", null, null, null, null, null, null);

        assertThatThrownBy(() -> estudianteServiceChp.ymclCrear(
                "EST001", null, "Otra", "Persona", null, null, null, null, null, null))
                .isInstanceOf(RyeValidacionException.class);
        assertThatThrownBy(() -> estudianteServiceChp.ymclCrear(
                null, "12345678", "Otra", "Persona", null, null, null, null, null, null))
                .isInstanceOf(RyeValidacionException.class);
    }

    @Test
    void buscarEncuentraPorDniCodigoNombresYApellidos() {
        estudianteServiceChp.ymclCrear("BUS100", "87654321", "Rosa Elena", "Quispe", "Mamani", null, null, null, null, null);

        assertThat(estudianteServiceChp.ymclBuscar("87654321")).extracting(YmclEstudiante::getNombres).contains("Rosa Elena");
        assertThat(estudianteServiceChp.ymclBuscar("BUS100")).extracting(YmclEstudiante::getNombres).contains("Rosa Elena");
        assertThat(estudianteServiceChp.ymclBuscar("Rosa Elena")).extracting(YmclEstudiante::getApellidoPaterno).contains("Quispe");
        assertThat(estudianteServiceChp.ymclBuscar("Quispe")).extracting(YmclEstudiante::getApellidoMaterno).contains("Mamani");
        assertThat(estudianteServiceChp.ymclBuscar("Mamani")).extracting(YmclEstudiante::getApellidoPaterno).contains("Quispe");
    }

    @Test
    void buscarEsToleranteSinTildesNiMayusculasNiOrdenYAceptaFragmentos() {
        estudianteServiceChp.ymclCrear("BUS200", "76543210", "María José", "Pérez", "Condori", null, null, null, null, null);

        assertThat(estudianteServiceChp.ymclBuscar("maria jose")).extracting(YmclEstudiante::getDni).contains("76543210");
        assertThat(estudianteServiceChp.ymclBuscar("PEREZ")).extracting(YmclEstudiante::getDni).contains("76543210");
        assertThat(estudianteServiceChp.ymclBuscar("condori perez")).extracting(YmclEstudiante::getDni).contains("76543210");
        assertThat(estudianteServiceChp.ymclBuscar("per con")).extracting(YmclEstudiante::getDni).contains("76543210");
        assertThat(estudianteServiceChp.ymclBuscar("7654")).extracting(YmclEstudiante::getDni).contains("76543210");

        assertThat(estudianteServiceChp.ymclBuscar("perez zzzz")).extracting(YmclEstudiante::getDni).doesNotContain("76543210");

        assertThat(estudianteServiceChp.ymclBuscar("  ")).extracting(YmclEstudiante::getDni).contains("76543210");
    }

    @Test
    void cambiarEstadoInactivaYReactiva() {
        YmclEstudiante estudianteChp = estudianteServiceChp.ymclCrear(
                null, null, "Luis", "Flores", null, null, null, null, null, null);

        estudianteServiceChp.ymclCambiarEstado(estudianteChp.getId(), YmclEstadoEstudiante.INACTIVO);
        assertThat(estudianteServiceChp.ymclListar(false)).noneMatch(e -> e.getId().equals(estudianteChp.getId()));
        assertThat(estudianteServiceChp.ymclListar(true)).anyMatch(e -> e.getId().equals(estudianteChp.getId()));

        estudianteServiceChp.ymclCambiarEstado(estudianteChp.getId(), YmclEstadoEstudiante.ACTIVO);
        assertThat(estudianteServiceChp.ymclListar(false)).anyMatch(e -> e.getId().equals(estudianteChp.getId()));
    }

    @Test
    void unOperadorPuedeCrearYEditarEstudiantes() {
        usuarioServiceChp.ryeCrear("operador.estudiantes", "claveSegura123", "Operador Estudiantes", RyeRolUsuario.OPERADOR);
        authServiceChp.ryeCerrarSesion();
        authServiceChp.ryeAutenticar("operador.estudiantes", "claveSegura123");

        YmclEstudiante creadoChp = estudianteServiceChp.ymclCrear(null, null, "Pedro", "Ramos", null, null, null, null, null, null);
        YmclEstudiante editadoChp = estudianteServiceChp.ymclEditar(creadoChp.getId(), null, null, "Pedro Antonio", "Ramos",
                null, null, null, null, null, null);

        assertThat(editadoChp.getNombres()).isEqualTo("Pedro Antonio");
    }

    @Test
    void crearEstudianteSinSesionFalla() {
        authServiceChp.ryeCerrarSesion();

        assertThatThrownBy(() -> estudianteServiceChp.ymclCrear(
                null, null, "Sin", "Sesion", null, null, null, null, null, null))
                .isInstanceOf(RyeAccesoDenegadoException.class);
    }
}
