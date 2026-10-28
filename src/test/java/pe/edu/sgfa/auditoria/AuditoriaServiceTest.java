package pe.edu.sgfa.auditoria;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.annotation.DirtiesContext;

import pe.edu.sgfa.auditoria.domain.Auditoria;
import pe.edu.sgfa.auditoria.service.AuditoriaService;
import pe.edu.sgfa.common.exception.AccesoDenegadoException;
import pe.edu.sgfa.estudiante.domain.Estudiante;
import pe.edu.sgfa.estudiante.service.EstudianteService;
import pe.edu.sgfa.institucion.service.InstitucionService;
import pe.edu.sgfa.programa.domain.ProgramaEstudio;
import pe.edu.sgfa.programa.service.ProgramaEstudioService;
import pe.edu.sgfa.usuario.domain.RolUsuario;
import pe.edu.sgfa.usuario.service.AdminBootstrapResult;
import pe.edu.sgfa.usuario.service.AuthService;
import pe.edu.sgfa.usuario.service.UsuarioService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuditoriaServiceTest {

    @BeforeAll
    static void useIsolatedHomeDir() throws IOException {
        Path tmpHm = Files.createTempDirectory("sgfa-auditoria-test-home-");
        System.setProperty("sgfa.home-dir", tmpHm.toString());
    }

    @Autowired
    private InstitucionService institucionService;
    @Autowired
    private EstudianteService estudianteService;
    @Autowired
    private ProgramaEstudioService programaEstudioService;
    @Autowired
    private AuditoriaService auditoriaService;
    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private AuthService authService;
    @Autowired
    private AdminBootstrapResult adminBootstrapResult;

    private static AdminBootstrapResult.Credenciales credencialesAdmin;

    private Estudiante estudiante;
    private ProgramaEstudio programa;

    @BeforeEach
    void prepararDatosBase() {
        if (credencialesAdmin == null) {
            credencialesAdmin = adminBootstrapResult.consumir()
                    .orElseThrow(() -> new IllegalStateException("El admin inicial no se genero"));
        }
        authService.cerrarSesion();
        authService.autenticar(credencialesAdmin.username(), credencialesAdmin.password());

        if (institucionService.obtenerPrincipal().isEmpty()) {
            institucionService.guardar("IESPP Juliaca", "JULI", "Av. Principal 123", null, null);
        }

        long mrc = System.nanoTime();
        estudiante = estudianteService.crear("AUD" + mrc, null, "Carla", "Rios", null, null, null, null, null, null);
        programa = programaEstudioService.crear("AUDP" + mrc, "Programa auditoria", null, 10);
    }

    @Test
    void buscarSinFiltrosIncluyeLosRegistrosRecienCreados() {
        Page<Auditoria> pgn = auditoriaService.buscar(null, null, null, null, null, 0, 50);

        assertThat(pgn.getContent()).anySatisfy(a -> assertThat(a.getAccion()).isEqualTo("CREAR_ESTUDIANTE"));
        assertThat(pgn.getContent()).anySatisfy(a -> assertThat(a.getAccion()).isEqualTo("CREAR_PROGRAMA"));

        List<Auditoria> cntnd = pgn.getContent();
        for (int i = 0; i + 1 < cntnd.size(); i++) {
            assertThat(cntnd.get(i).getFecha()).isAfterOrEqualTo(cntnd.get(i + 1).getFecha());
        }
    }

    @Test
    void filtraPorAccionEntidadYRangoDeFechas() {
        Page<Auditoria> porAccn = auditoriaService.buscar(null, null, null, "CREAR_ESTUDIANTE", null, 0, 50);
        assertThat(porAccn.getContent()).allSatisfy(a -> assertThat(a.getAccion()).isEqualTo("CREAR_ESTUDIANTE"));
        assertThat(porAccn.getContent()).anySatisfy(a -> assertThat(a.getEntidadId()).isEqualTo(estudiante.getId().toString()));

        Page<Auditoria> porEntdd = auditoriaService.buscar(null, null, null, null, "programas_estudio", 0, 50);
        assertThat(porEntdd.getContent()).allSatisfy(a -> assertThat(a.getEntidad()).isEqualTo("programas_estudio"));

        Instant ftr = Instant.now().plusSeconds(3600);
        Page<Auditoria> frDeRng = auditoriaService.buscar(ftr, null, null, null, null, 0, 50);
        assertThat(frDeRng.getContent()).isEmpty();
    }

    @Test
    void listarAccionesYEntidadesIncluyeLoRecienGenerado() {
        assertThat(auditoriaService.listarAcciones()).contains("CREAR_ESTUDIANTE", "CREAR_PROGRAMA");
        assertThat(auditoriaService.listarEntidades()).contains("estudiantes", "programas_estudio");
    }

    @Test
    void soloAdminPuedeLeerElLog() {
        String usrnmOprdr = "operador" + System.nanoTime();
        usuarioService.crear(usrnmOprdr, "clave1234", "Operador de prueba", RolUsuario.OPERADOR);
        authService.cerrarSesion();
        authService.autenticar(usrnmOprdr, "clave1234");

        assertThatThrownBy(() -> auditoriaService.buscar(null, null, null, null, null, 0, 50))
                .isInstanceOf(AccesoDenegadoException.class);
        assertThatThrownBy(auditoriaService::listarAcciones).isInstanceOf(AccesoDenegadoException.class);
        assertThatThrownBy(auditoriaService::listarEntidades).isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    void registrarFuncionaSinSesionActiva() {
        authService.cerrarSesion();

        auditoriaService.registrar(null, "LOGIN_FALLIDO", "usuarios", "alguien", "Credenciales invalidas");

        authService.autenticar(credencialesAdmin.username(), credencialesAdmin.password());
        Page<Auditoria> pgn = auditoriaService.buscar(null, null, null, "LOGIN_FALLIDO", null, 0, 50);
        assertThat(pgn.getContent()).anySatisfy(a -> assertThat(a.getUsuarioId()).isNull());
    }
}
