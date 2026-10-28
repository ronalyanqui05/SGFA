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

import pe.edu.sgfa.common.exception.AccesoDenegadoException;
import pe.edu.sgfa.common.exception.ValidacionException;
import pe.edu.sgfa.estudiante.domain.EstadoEstudiante;
import pe.edu.sgfa.estudiante.domain.Estudiante;
import pe.edu.sgfa.estudiante.service.EstudianteService;
import pe.edu.sgfa.usuario.domain.RolUsuario;
import pe.edu.sgfa.usuario.service.AdminBootstrapResult;
import pe.edu.sgfa.usuario.service.AuthService;
import pe.edu.sgfa.usuario.service.UsuarioService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EstudianteServiceTest {

    @BeforeAll
    static void useIsolatedHomeDir() throws IOException {
        Path tmpHm = Files.createTempDirectory("sgfa-estudiante-test-home-");
        System.setProperty("sgfa.home-dir", tmpHm.toString());
    }

    @Autowired
    private EstudianteService estudianteService;
    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private AuthService authService;
    @Autowired
    private AdminBootstrapResult adminBootstrapResult;

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
    void crearConDatosMinimosFunciona() {
        Estudiante estudiante = estudianteService.crear(
                null, null, "Juan Carlos", "Perez", null, null, null, null, null, null);

        assertThat(estudiante.getId()).isNotNull();
        assertThat(estudiante.getEstado()).isEqualTo(EstadoEstudiante.ACTIVO);
        assertThat(estudiante.getFechaRegistro()).isNotNull();
    }

    @Test
    void crearSinNombresFalla() {
        assertThatThrownBy(() -> estudianteService.crear(null, null, "  ", "Perez", null, null, null, null, null, null))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void crearConDniInvalidoFalla() {
        assertThatThrownBy(() -> estudianteService.crear(
                null, "123", "Maria", "Lopez", null, null, null, null, null, null))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void crearConCodigoODniDuplicadoFalla() {
        estudianteService.crear("EST001", "12345678", "Ana", "Torres", null, null, null, null, null, null);

        assertThatThrownBy(() -> estudianteService.crear(
                "EST001", null, "Otra", "Persona", null, null, null, null, null, null))
                .isInstanceOf(ValidacionException.class);
        assertThatThrownBy(() -> estudianteService.crear(
                null, "12345678", "Otra", "Persona", null, null, null, null, null, null))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void buscarEncuentraPorDniCodigoNombresYApellidos() {
        estudianteService.crear("BUS100", "87654321", "Rosa Elena", "Quispe", "Mamani", null, null, null, null, null);

        assertThat(estudianteService.buscar("87654321")).extracting(Estudiante::getNombres).contains("Rosa Elena");
        assertThat(estudianteService.buscar("BUS100")).extracting(Estudiante::getNombres).contains("Rosa Elena");
        assertThat(estudianteService.buscar("Rosa Elena")).extracting(Estudiante::getApellidoPaterno).contains("Quispe");
        assertThat(estudianteService.buscar("Quispe")).extracting(Estudiante::getApellidoMaterno).contains("Mamani");
        assertThat(estudianteService.buscar("Mamani")).extracting(Estudiante::getApellidoPaterno).contains("Quispe");
    }

    @Test
    void buscarEsTolerante_sinTildesNiMayusculasNiOrdenYAceptaFragmentos() {
        estudianteService.crear("BUS200", "76543210", "María José", "Pérez", "Condori", null, null, null, null, null);

        assertThat(estudianteService.buscar("maria jose")).extracting(Estudiante::getDni).contains("76543210");
        assertThat(estudianteService.buscar("PEREZ")).extracting(Estudiante::getDni).contains("76543210");
        assertThat(estudianteService.buscar("condori perez")).extracting(Estudiante::getDni).contains("76543210");
        assertThat(estudianteService.buscar("per con")).extracting(Estudiante::getDni).contains("76543210");
        assertThat(estudianteService.buscar("7654")).extracting(Estudiante::getDni).contains("76543210");

        assertThat(estudianteService.buscar("perez zzzz")).extracting(Estudiante::getDni).doesNotContain("76543210");

        assertThat(estudianteService.buscar("  ")).extracting(Estudiante::getDni).contains("76543210");
    }

    @Test
    void cambiarEstadoInactivaYReactiva() {
        Estudiante estudiante = estudianteService.crear(
                null, null, "Luis", "Flores", null, null, null, null, null, null);

        estudianteService.cambiarEstado(estudiante.getId(), EstadoEstudiante.INACTIVO);
        assertThat(estudianteService.listar(false)).noneMatch(e -> e.getId().equals(estudiante.getId()));
        assertThat(estudianteService.listar(true)).anyMatch(e -> e.getId().equals(estudiante.getId()));

        estudianteService.cambiarEstado(estudiante.getId(), EstadoEstudiante.ACTIVO);
        assertThat(estudianteService.listar(false)).anyMatch(e -> e.getId().equals(estudiante.getId()));
    }

    @Test
    void unOperadorPuedeCrearYEditarEstudiantes() {
        usuarioService.crear("operador.estudiantes", "claveSegura123", "Operador Estudiantes", RolUsuario.OPERADOR);
        authService.cerrarSesion();
        authService.autenticar("operador.estudiantes", "claveSegura123");

        Estudiante crd = estudianteService.crear(null, null, "Pedro", "Ramos", null, null, null, null, null, null);
        Estudiante edtd = estudianteService.editar(crd.getId(), null, null, "Pedro Antonio", "Ramos",
                null, null, null, null, null, null);

        assertThat(edtd.getNombres()).isEqualTo("Pedro Antonio");
    }

    @Test
    void crearEstudianteSinSesionFalla() {
        authService.cerrarSesion();

        assertThatThrownBy(() -> estudianteService.crear(
                null, null, "Sin", "Sesion", null, null, null, null, null, null))
                .isInstanceOf(AccesoDenegadoException.class);
    }
}
