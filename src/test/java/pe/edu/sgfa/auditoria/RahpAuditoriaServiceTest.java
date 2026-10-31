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

import pe.edu.sgfa.auditoria.domain.RahpAuditoria;
import pe.edu.sgfa.auditoria.service.RahpAuditoriaService;
import pe.edu.sgfa.common.exception.RyeAccesoDenegadoException;
import pe.edu.sgfa.estudiante.domain.YmclEstudiante;
import pe.edu.sgfa.estudiante.service.YmclEstudianteService;
import pe.edu.sgfa.institucion.service.RacaInstitucionService;
import pe.edu.sgfa.programa.domain.RacaProgramaEstudio;
import pe.edu.sgfa.programa.service.RacaProgramaEstudioService;
import pe.edu.sgfa.usuario.domain.RyeRolUsuario;
import pe.edu.sgfa.usuario.service.RyeAdminBootstrapResult;
import pe.edu.sgfa.usuario.service.RyeAuthService;
import pe.edu.sgfa.usuario.service.RyeUsuarioService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RahpAuditoriaServiceTest {

    @BeforeAll
    static void useIsolatedHomeDir() throws IOException {
        Path tempHomeHnc = Files.createTempDirectory("sgfa-auditoria-test-home-");
        System.setProperty("sgfa.home-dir", tempHomeHnc.toString());
    }

    @Autowired
    private RacaInstitucionService institucionServiceHnc;
    @Autowired
    private YmclEstudianteService estudianteServiceHnc;
    @Autowired
    private RacaProgramaEstudioService programaEstudioServiceHnc;
    @Autowired
    private RahpAuditoriaService auditoriaServiceHnc;
    @Autowired
    private RyeUsuarioService usuarioServiceHnc;
    @Autowired
    private RyeAuthService authServiceHnc;
    @Autowired
    private RyeAdminBootstrapResult adminBootstrapResultHnc;

    private static RyeAdminBootstrapResult.RyeCredenciales credencialesAdminHnc;

    private YmclEstudiante estudianteHnc;
    private RacaProgramaEstudio programa;

    @BeforeEach
    void prepararDatosBase() {
        if (credencialesAdminHnc == null) {
            credencialesAdminHnc = adminBootstrapResultHnc.ryeConsumir()
                    .orElseThrow(() -> new IllegalStateException("El admin inicial no se genero"));
        }
        authServiceHnc.ryeCerrarSesion();
        authServiceHnc.ryeAutenticar(credencialesAdminHnc.username(), credencialesAdminHnc.password());

        if (institucionServiceHnc.racaObtenerPrincipal().isEmpty()) {
            institucionServiceHnc.racaGuardar("IESPP Juliaca", "JULI", "Av. Principal 123", null, null);
        }

        long marcaHnc = System.nanoTime();
        estudianteHnc = estudianteServiceHnc.ymclCrear("AUD" + marcaHnc, null, "Carla", "Rios", null, null, null, null, null, null);
        programa = programaEstudioServiceHnc.racaCrear("AUDP" + marcaHnc, "Programa auditoria", null, 10);
    }

    @Test
    void buscarSinFiltrosIncluyeLosRegistrosRecienCreados() {
        Page<RahpAuditoria> paginaHnc = auditoriaServiceHnc.rahpBuscar(null, null, null, null, null, 0, 50);

        assertThat(paginaHnc.getContent()).anySatisfy(a -> assertThat(a.getAccion()).isEqualTo("CREAR_ESTUDIANTE"));
        assertThat(paginaHnc.getContent()).anySatisfy(a -> assertThat(a.getAccion()).isEqualTo("CREAR_PROGRAMA"));

        List<RahpAuditoria> contenidoHnc = paginaHnc.getContent();
        for (int i = 0; i + 1 < contenidoHnc.size(); i++) {
            assertThat(contenidoHnc.get(i).getFecha()).isAfterOrEqualTo(contenidoHnc.get(i + 1).getFecha());
        }
    }

    @Test
    void filtraPorAccionEntidadYRangoDeFechas() {
        Page<RahpAuditoria> porAccionHnc = auditoriaServiceHnc.rahpBuscar(null, null, null, "CREAR_ESTUDIANTE", null, 0, 50);
        assertThat(porAccionHnc.getContent()).allSatisfy(a -> assertThat(a.getAccion()).isEqualTo("CREAR_ESTUDIANTE"));
        assertThat(porAccionHnc.getContent()).anySatisfy(a -> assertThat(a.getEntidadId()).isEqualTo(estudianteHnc.getId().toString()));

        Page<RahpAuditoria> porEntidadHnc = auditoriaServiceHnc.rahpBuscar(null, null, null, null, "programas_estudio", 0, 50);
        assertThat(porEntidadHnc.getContent()).allSatisfy(a -> assertThat(a.getEntidad()).isEqualTo("programas_estudio"));

        Instant futuroHnc = Instant.now().plusSeconds(3600);
        Page<RahpAuditoria> fueraDeRangoHnc = auditoriaServiceHnc.rahpBuscar(futuroHnc, null, null, null, null, 0, 50);
        assertThat(fueraDeRangoHnc.getContent()).isEmpty();
    }

    @Test
    void listarAccionesYEntidadesIncluyeLoRecienGenerado() {
        assertThat(auditoriaServiceHnc.rahpListarAcciones()).contains("CREAR_ESTUDIANTE", "CREAR_PROGRAMA");
        assertThat(auditoriaServiceHnc.rahpListarEntidades()).contains("estudiantes", "programas_estudio");
    }

    @Test
    void soloAdminPuedeLeerElLog() {
        String usernameOperadorHnc = "operador" + System.nanoTime();
        usuarioServiceHnc.ryeCrear(usernameOperadorHnc, "clave1234", "Operador de prueba", RyeRolUsuario.OPERADOR);
        authServiceHnc.ryeCerrarSesion();
        authServiceHnc.ryeAutenticar(usernameOperadorHnc, "clave1234");

        assertThatThrownBy(() -> auditoriaServiceHnc.rahpBuscar(null, null, null, null, null, 0, 50))
                .isInstanceOf(RyeAccesoDenegadoException.class);
        assertThatThrownBy(auditoriaServiceHnc::rahpListarAcciones).isInstanceOf(RyeAccesoDenegadoException.class);
        assertThatThrownBy(auditoriaServiceHnc::rahpListarEntidades).isInstanceOf(RyeAccesoDenegadoException.class);
    }

    @Test
    void registrarFuncionaSinSesionActiva() {
        authServiceHnc.ryeCerrarSesion();

        auditoriaServiceHnc.rahpRegistrar(null, "LOGIN_FALLIDO", "usuarios", "alguien", "Credenciales invalidas");

        authServiceHnc.ryeAutenticar(credencialesAdminHnc.username(), credencialesAdminHnc.password());
        Page<RahpAuditoria> paginaHnc = auditoriaServiceHnc.rahpBuscar(null, null, null, "LOGIN_FALLIDO", null, 0, 50);
        assertThat(paginaHnc.getContent()).anySatisfy(a -> assertThat(a.getUsuarioId()).isNull());
    }
}
