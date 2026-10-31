package pe.edu.sgfa.trayectoria;

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

import pe.edu.sgfa.common.exception.RyeValidacionException;
import pe.edu.sgfa.estudiante.domain.YmclEstudiante;
import pe.edu.sgfa.estudiante.service.YmclEstudianteService;
import pe.edu.sgfa.institucion.service.RacaInstitucionService;
import pe.edu.sgfa.nota.repository.YmclRegistroAcademicoRepository;
import pe.edu.sgfa.nota.service.YmclItemNota;
import pe.edu.sgfa.nota.service.YmclRegistroAcademicoService;
import pe.edu.sgfa.periodo.domain.YmclPeriodoAcademico;
import pe.edu.sgfa.periodo.service.YmclPeriodoAcademicoService;
import pe.edu.sgfa.programa.domain.RacaCiclo;
import pe.edu.sgfa.programa.domain.RacaCurso;
import pe.edu.sgfa.programa.domain.RacaPlanEstudio;
import pe.edu.sgfa.programa.domain.RacaProgramaEstudio;
import pe.edu.sgfa.programa.service.RacaCicloService;
import pe.edu.sgfa.programa.service.RacaCursoService;
import pe.edu.sgfa.programa.service.RacaPlanCursoService;
import pe.edu.sgfa.programa.service.RacaPlanEstudioService;
import pe.edu.sgfa.programa.service.RacaProgramaEstudioService;
import pe.edu.sgfa.trayectoria.domain.YmclEstadoTrayectoria;
import pe.edu.sgfa.trayectoria.domain.YmclTrayectoria;
import pe.edu.sgfa.trayectoria.service.YmclTrayectoriaService;
import pe.edu.sgfa.usuario.service.RyeAdminBootstrapResult;
import pe.edu.sgfa.usuario.service.RyeAuthService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class YmclTrayectoriaServiceTest {

    @BeforeAll
    static void useIsolatedHomeDir() throws IOException {
        Path tempHomeChp = Files.createTempDirectory("sgfa-trayectoria-test-home-");
        System.setProperty("sgfa.home-dir", tempHomeChp.toString());
    }

    @Autowired
    private RacaInstitucionService institucionServiceChp;
    @Autowired
    private RacaProgramaEstudioService programaEstudioServiceChp;
    @Autowired
    private RacaPlanEstudioService planEstudioServiceChp;
    @Autowired
    private YmclEstudianteService estudianteServiceChp;
    @Autowired
    private YmclTrayectoriaService trayectoriaServiceChp;
    @Autowired
    private RacaCicloService cicloServiceChp;
    @Autowired
    private RacaCursoService cursoServiceChp;
    @Autowired
    private RacaPlanCursoService planCursoServiceChp;
    @Autowired
    private YmclPeriodoAcademicoService periodoAcademicoServiceChp;
    @Autowired
    private YmclRegistroAcademicoService registroAcademicoServiceChp;
    @Autowired
    private YmclRegistroAcademicoRepository registroAcademicoRepositoryChp;
    @Autowired
    private RyeAuthService authServiceChp;
    @Autowired
    private RyeAdminBootstrapResult adminBootstrapResultChp;

    private static RyeAdminBootstrapResult.RyeCredenciales credencialesAdminChp;
    private RacaProgramaEstudio programa;
    private RacaPlanEstudio plan2020Chp;
    private RacaPlanEstudio plan2024Chp;

    @BeforeEach
    void prepararDatosBase() {
        if (credencialesAdminChp == null) {
            credencialesAdminChp = adminBootstrapResultChp.ryeConsumir()
                    .orElseThrow(() -> new IllegalStateException("El admin inicial no se genero"));
        }
        authServiceChp.ryeCerrarSesion();
        authServiceChp.ryeAutenticar(credencialesAdminChp.username(), credencialesAdminChp.password());

        if (institucionServiceChp.racaObtenerPrincipal().isEmpty()) {
            institucionServiceChp.racaGuardar("IESPP Juliaca", "JULI", null, null, null);
        }
        programa = programaEstudioServiceChp.racaCrear("COM" + System.nanoTime(), "Comunicacion", null, 10);
        plan2020Chp = planEstudioServiceChp.racaCrear(programa.getId(), "PLAN2020", "Plan 2020", null, null);
        plan2024Chp = planEstudioServiceChp.racaCrear(programa.getId(), "PLAN2024", "Plan 2024", null, null);
    }

    private YmclEstudiante crearEstudiante() {
        return estudianteServiceChp.ymclCrear(null, null, "Juan", "Perez" + System.nanoTime(), null, null, null, null, null, null);
    }

    @Test
    void iniciarTrayectoriaFunciona() {
        YmclEstudiante estudianteChp = crearEstudiante();

        YmclTrayectoria trayectoriaChp = trayectoriaServiceChp.ymclIniciar(
                estudianteChp.getId(), programa.getId(), plan2020Chp.getId(), null, "Ingreso regular");

        assertThat(trayectoriaChp.getEstado()).isEqualTo(YmclEstadoTrayectoria.ACTIVA);
        assertThat(trayectoriaServiceChp.ymclObtenerActiva(estudianteChp.getId())).isPresent();
    }

    @Test
    void iniciarSegundaTrayectoriaSinCerrarLaActivaFalla() {
        YmclEstudiante estudianteChp = crearEstudiante();
        trayectoriaServiceChp.ymclIniciar(estudianteChp.getId(), programa.getId(), plan2020Chp.getId(), null, null);

        assertThatThrownBy(() -> trayectoriaServiceChp.ymclIniciar(
                estudianteChp.getId(), programa.getId(), plan2024Chp.getId(), null, null))
                .isInstanceOf(RyeValidacionException.class);
    }

    @Test
    void cambiarPlanCierraLaAnteriorYCreaUnaNuevaSinPerderHistorial() {
        YmclEstudiante estudianteChp = crearEstudiante();
        YmclTrayectoria originalChp = trayectoriaServiceChp.ymclIniciar(
                estudianteChp.getId(), programa.getId(), plan2020Chp.getId(), null, null);

        YmclTrayectoria nuevaChp = trayectoriaServiceChp.ymclCambiarPlan(
                estudianteChp.getId(), programa.getId(), plan2024Chp.getId(), null, "Cambio de plan de estudios");

        assertThat(nuevaChp.getEstado()).isEqualTo(YmclEstadoTrayectoria.ACTIVA);
        assertThat(nuevaChp.getPlanEstudio().getId()).isEqualTo(plan2024Chp.getId());

        verificarHistorialConservado(estudianteChp, originalChp, nuevaChp);
    }

    private void verificarHistorialConservado(YmclEstudiante estudianteChp, YmclTrayectoria originalChp, YmclTrayectoria nuevaChp) {
        List<YmclTrayectoria> historialChp = trayectoriaServiceChp.ymclListarPorEstudiante(estudianteChp.getId());
        assertThat(historialChp).hasSize(2);
        assertThat(historialChp).anySatisfy(t -> {
            assertThat(t.getId()).isEqualTo(originalChp.getId());
            assertThat(t.getEstado()).isEqualTo(YmclEstadoTrayectoria.TRASLADADA);
        });
        assertThat(historialChp).anySatisfy(t -> assertThat(t.getId()).isEqualTo(nuevaChp.getId()));
    }

    @Test
    void cambiarPlanSinTrayectoriaActivaFalla() {
        YmclEstudiante estudianteChp = crearEstudiante();

        assertThatThrownBy(() -> trayectoriaServiceChp.ymclCambiarPlan(
                estudianteChp.getId(), programa.getId(), plan2024Chp.getId(), null, null))
                .isInstanceOf(RyeValidacionException.class);
    }

    @Test
    void finalizarYRetirarFuncionan() {
        YmclEstudiante estudiante1Chp = crearEstudiante();
        YmclTrayectoria t1 = trayectoriaServiceChp.ymclIniciar(estudiante1Chp.getId(), programa.getId(), plan2020Chp.getId(), null, null);
        trayectoriaServiceChp.ymclFinalizar(t1.getId(), null, "Egreso");
        assertThat(trayectoriaServiceChp.ymclObtenerActiva(estudiante1Chp.getId())).isEmpty();

        YmclEstudiante estudiante2Chp = crearEstudiante();
        YmclTrayectoria t2 = trayectoriaServiceChp.ymclIniciar(estudiante2Chp.getId(), programa.getId(), plan2020Chp.getId(), null, null);
        trayectoriaServiceChp.ymclRetirar(t2.getId(), null, "Retiro voluntario");
        assertThat(trayectoriaServiceChp.ymclObtenerActiva(estudiante2Chp.getId())).isEmpty();
    }

    @Test
    void planDeOtroProgramaFalla() {
        YmclEstudiante estudianteChp = crearEstudiante();
        RacaProgramaEstudio otroProgramaChp = programaEstudioServiceChp.racaCrear("OTR" + System.nanoTime(), "Otro programa", null, 8);

        assertThatThrownBy(() -> trayectoriaServiceChp.ymclIniciar(
                estudianteChp.getId(), otroProgramaChp.getId(), plan2020Chp.getId(), null, null))
                .isInstanceOf(RyeValidacionException.class);
    }

    @Test
    void editarCorrigeProgramaYPlanAunqueEsteRetiradaYTengaNotas() {
        YmclEstudiante estudianteChp = crearEstudiante();
        YmclTrayectoria trayectoriaChp = trayectoriaServiceChp.ymclIniciar(
                estudianteChp.getId(), programa.getId(), plan2020Chp.getId(), null, "Plan equivocado");
        registrarUnaNota(trayectoriaChp, plan2020Chp);
        trayectoriaServiceChp.ymclRetirar(trayectoriaChp.getId(), null, "Retiro por error");

        YmclTrayectoria corregidaChp = trayectoriaServiceChp.ymclEditar(
                trayectoriaChp.getId(), programa.getId(), plan2024Chp.getId(), null, "Corregido: era plan2024");

        assertThat(corregidaChp.getPlanEstudio().getId()).isEqualTo(plan2024Chp.getId());
        assertThat(corregidaChp.getEstado()).isEqualTo(YmclEstadoTrayectoria.RETIRADA);
        assertThat(corregidaChp.getObservacion()).isEqualTo("Corregido: era plan2024");
    }

    @Test
    void reactivarVuelveAActivaYLimpiaFechaFin() {
        YmclEstudiante estudianteChp = crearEstudiante();
        YmclTrayectoria trayectoriaChp = trayectoriaServiceChp.ymclIniciar(estudianteChp.getId(), programa.getId(), plan2020Chp.getId(), null, null);
        trayectoriaServiceChp.ymclRetirar(trayectoriaChp.getId(), null, "Me equivoque al retirar");

        YmclTrayectoria reactivadaChp = trayectoriaServiceChp.ymclReactivar(trayectoriaChp.getId());

        assertThat(reactivadaChp.getEstado()).isEqualTo(YmclEstadoTrayectoria.ACTIVA);
        assertThat(reactivadaChp.getFechaFin()).isNull();
        assertThat(trayectoriaServiceChp.ymclObtenerActiva(estudianteChp.getId())).isPresent();
    }

    @Test
    void reactivarFallaSiYaHayOtraTrayectoriaActiva() {
        YmclEstudiante estudianteChp = crearEstudiante();
        YmclTrayectoria primeraChp = trayectoriaServiceChp.ymclIniciar(estudianteChp.getId(), programa.getId(), plan2020Chp.getId(), null, null);
        trayectoriaServiceChp.ymclRetirar(primeraChp.getId(), null, null);
        trayectoriaServiceChp.ymclIniciar(estudianteChp.getId(), programa.getId(), plan2024Chp.getId(), null, null);

        assertThatThrownBy(() -> trayectoriaServiceChp.ymclReactivar(primeraChp.getId()))
                .isInstanceOf(RyeValidacionException.class);
    }

    @Test
    void eliminarBorraLaTrayectoriaYSusNotasSinDejarHuerfanas() {
        YmclEstudiante estudianteChp = crearEstudiante();
        YmclTrayectoria trayectoriaChp = trayectoriaServiceChp.ymclIniciar(
                estudianteChp.getId(), programa.getId(), plan2020Chp.getId(), null, "Trayectoria mal creada");
        registrarUnaNota(trayectoriaChp, plan2020Chp);

        trayectoriaServiceChp.ymclEliminar(trayectoriaChp.getId());

        assertThat(trayectoriaServiceChp.ymclListarPorEstudiante(estudianteChp.getId())).isEmpty();
        assertThat(registroAcademicoRepositoryChp.existsByTrayectoriaId(trayectoriaChp.getId())).isFalse();
    }

    private void registrarUnaNota(YmclTrayectoria trayectoriaChp, RacaPlanEstudio planChp) {
        RacaCiclo cicloChp = cicloServiceChp.racaCrear(planChp.getId(), 1 + (int) (System.nanoTime() % 1000), "Ciclo nota", 1);
        RacaCurso cursoChp = cursoServiceChp.racaCrear("TRY" + System.nanoTime(), "Curso para nota", 3, null, null);
        var planCursoChp = planCursoServiceChp.racaAsignar(planChp.getId(), cicloChp.getId(), cursoChp.getId(), 1, true);
        YmclPeriodoAcademico periodoChp = periodoAcademicoServiceChp.ymclCrear("TRYP" + System.nanoTime(), 2026, "I", null, null);
        Long aprobadoIdChp = registroAcademicoServiceChp.ymclListarSituaciones().stream()
                .filter(s -> s.getCodigo().equals("APROBADO")).findFirst().orElseThrow().getId();
        registroAcademicoServiceChp.ymclGuardarMasivo(trayectoriaChp.getId(), periodoChp.getId(),
                List.of(new YmclItemNota(planCursoChp.getId(), 15.0, aprobadoIdChp, null)));
    }
}
