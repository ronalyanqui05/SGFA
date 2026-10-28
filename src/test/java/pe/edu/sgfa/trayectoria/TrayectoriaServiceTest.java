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

import pe.edu.sgfa.common.exception.ValidacionException;
import pe.edu.sgfa.estudiante.domain.Estudiante;
import pe.edu.sgfa.estudiante.service.EstudianteService;
import pe.edu.sgfa.institucion.service.InstitucionService;
import pe.edu.sgfa.nota.repository.RegistroAcademicoRepository;
import pe.edu.sgfa.nota.service.ItemNota;
import pe.edu.sgfa.nota.service.RegistroAcademicoService;
import pe.edu.sgfa.periodo.domain.PeriodoAcademico;
import pe.edu.sgfa.periodo.service.PeriodoAcademicoService;
import pe.edu.sgfa.programa.domain.Ciclo;
import pe.edu.sgfa.programa.domain.Curso;
import pe.edu.sgfa.programa.domain.PlanEstudio;
import pe.edu.sgfa.programa.domain.ProgramaEstudio;
import pe.edu.sgfa.programa.service.CicloService;
import pe.edu.sgfa.programa.service.CursoService;
import pe.edu.sgfa.programa.service.PlanCursoService;
import pe.edu.sgfa.programa.service.PlanEstudioService;
import pe.edu.sgfa.programa.service.ProgramaEstudioService;
import pe.edu.sgfa.trayectoria.domain.EstadoTrayectoria;
import pe.edu.sgfa.trayectoria.domain.Trayectoria;
import pe.edu.sgfa.trayectoria.service.TrayectoriaService;
import pe.edu.sgfa.usuario.service.AdminBootstrapResult;
import pe.edu.sgfa.usuario.service.AuthService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TrayectoriaServiceTest {

    @BeforeAll
    static void useIsolatedHomeDir() throws IOException {
        Path tmpHm = Files.createTempDirectory("sgfa-trayectoria-test-home-");
        System.setProperty("sgfa.home-dir", tmpHm.toString());
    }

    @Autowired
    private InstitucionService institucionService;
    @Autowired
    private ProgramaEstudioService programaEstudioService;
    @Autowired
    private PlanEstudioService planEstudioService;
    @Autowired
    private EstudianteService estudianteService;
    @Autowired
    private TrayectoriaService trayectoriaService;
    @Autowired
    private CicloService cicloService;
    @Autowired
    private CursoService cursoService;
    @Autowired
    private PlanCursoService planCursoService;
    @Autowired
    private PeriodoAcademicoService periodoAcademicoService;
    @Autowired
    private RegistroAcademicoService registroAcademicoService;
    @Autowired
    private RegistroAcademicoRepository registroAcademicoRepository;
    @Autowired
    private AuthService authService;
    @Autowired
    private AdminBootstrapResult adminBootstrapResult;

    private static AdminBootstrapResult.Credenciales credencialesAdmin;
    private ProgramaEstudio programa;
    private PlanEstudio plan2020;
    private PlanEstudio plan2024;

    @BeforeEach
    void prepararDatosBase() {
        if (credencialesAdmin == null) {
            credencialesAdmin = adminBootstrapResult.consumir()
                    .orElseThrow(() -> new IllegalStateException("El admin inicial no se genero"));
        }
        authService.cerrarSesion();
        authService.autenticar(credencialesAdmin.username(), credencialesAdmin.password());

        if (institucionService.obtenerPrincipal().isEmpty()) {
            institucionService.guardar("IESPP Juliaca", "JULI", null, null, null);
        }
        programa = programaEstudioService.crear("COM" + System.nanoTime(), "Comunicacion", null, 10);
        plan2020 = planEstudioService.crear(programa.getId(), "PLAN2020", "Plan 2020", null, null);
        plan2024 = planEstudioService.crear(programa.getId(), "PLAN2024", "Plan 2024", null, null);
    }

    private Estudiante crearEstudiante() {
        return estudianteService.crear(null, null, "Juan", "Perez" + System.nanoTime(), null, null, null, null, null, null);
    }

    @Test
    void iniciarTrayectoriaFunciona() {
        Estudiante estudiante = crearEstudiante();

        Trayectoria trayectoria = trayectoriaService.iniciar(
                estudiante.getId(), programa.getId(), plan2020.getId(), null, "Ingreso regular");

        assertThat(trayectoria.getEstado()).isEqualTo(EstadoTrayectoria.ACTIVA);
        assertThat(trayectoriaService.obtenerActiva(estudiante.getId())).isPresent();
    }

    @Test
    void iniciarSegundaTrayectoriaSinCerrarLaActivaFalla() {
        Estudiante estudiante = crearEstudiante();
        trayectoriaService.iniciar(estudiante.getId(), programa.getId(), plan2020.getId(), null, null);

        assertThatThrownBy(() -> trayectoriaService.iniciar(
                estudiante.getId(), programa.getId(), plan2024.getId(), null, null))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void cambiarPlanCierraLaAnteriorYCreaUnaNuevaSinPerderHistorial() {
        Estudiante estudiante = crearEstudiante();
        Trayectoria orgnl = trayectoriaService.iniciar(
                estudiante.getId(), programa.getId(), plan2020.getId(), null, null);

        Trayectoria nv = trayectoriaService.cambiarPlan(
                estudiante.getId(), programa.getId(), plan2024.getId(), null, "Cambio de plan de estudios");

        assertThat(nv.getEstado()).isEqualTo(EstadoTrayectoria.ACTIVA);
        assertThat(nv.getPlanEstudio().getId()).isEqualTo(plan2024.getId());

        verificarHistorialConservado(estudiante, orgnl, nv);
    }

    private void verificarHistorialConservado(Estudiante estudiante, Trayectoria orgnl, Trayectoria nv) {
        List<Trayectoria> hstrl = trayectoriaService.listarPorEstudiante(estudiante.getId());
        assertThat(hstrl).hasSize(2);
        assertThat(hstrl).anySatisfy(t -> {
            assertThat(t.getId()).isEqualTo(orgnl.getId());
            assertThat(t.getEstado()).isEqualTo(EstadoTrayectoria.TRASLADADA);
        });
        assertThat(hstrl).anySatisfy(t -> assertThat(t.getId()).isEqualTo(nv.getId()));
    }

    @Test
    void cambiarPlanSinTrayectoriaActivaFalla() {
        Estudiante estudiante = crearEstudiante();

        assertThatThrownBy(() -> trayectoriaService.cambiarPlan(
                estudiante.getId(), programa.getId(), plan2024.getId(), null, null))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void finalizarYRetirarFuncionan() {
        Estudiante estdnt1 = crearEstudiante();
        Trayectoria t1 = trayectoriaService.iniciar(estdnt1.getId(), programa.getId(), plan2020.getId(), null, null);
        trayectoriaService.finalizar(t1.getId(), null, "Egreso");
        assertThat(trayectoriaService.obtenerActiva(estdnt1.getId())).isEmpty();

        Estudiante estdnt2 = crearEstudiante();
        Trayectoria t2 = trayectoriaService.iniciar(estdnt2.getId(), programa.getId(), plan2020.getId(), null, null);
        trayectoriaService.retirar(t2.getId(), null, "Retiro voluntario");
        assertThat(trayectoriaService.obtenerActiva(estdnt2.getId())).isEmpty();
    }

    @Test
    void planDeOtroProgramaFalla() {
        Estudiante estudiante = crearEstudiante();
        ProgramaEstudio otrPrgrm = programaEstudioService.crear("OTR" + System.nanoTime(), "Otro programa", null, 8);

        assertThatThrownBy(() -> trayectoriaService.iniciar(
                estudiante.getId(), otrPrgrm.getId(), plan2020.getId(), null, null))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void editarCorrigeProgramaYPlanAunqueEsteRetiradaYTengaNotas() {
        Estudiante estudiante = crearEstudiante();
        Trayectoria trayectoria = trayectoriaService.iniciar(
                estudiante.getId(), programa.getId(), plan2020.getId(), null, "Plan equivocado");
        registrarUnaNota(trayectoria, plan2020);
        trayectoriaService.retirar(trayectoria.getId(), null, "Retiro por error");

        Trayectoria crrgd = trayectoriaService.editar(
                trayectoria.getId(), programa.getId(), plan2024.getId(), null, "Corregido: era plan2024");

        assertThat(crrgd.getPlanEstudio().getId()).isEqualTo(plan2024.getId());
        assertThat(crrgd.getEstado()).isEqualTo(EstadoTrayectoria.RETIRADA);
        assertThat(crrgd.getObservacion()).isEqualTo("Corregido: era plan2024");
    }

    @Test
    void reactivarVuelveAActivaYLimpiaFechaFin() {
        Estudiante estudiante = crearEstudiante();
        Trayectoria trayectoria = trayectoriaService.iniciar(estudiante.getId(), programa.getId(), plan2020.getId(), null, null);
        trayectoriaService.retirar(trayectoria.getId(), null, "Me equivoque al retirar");

        Trayectoria rctvd = trayectoriaService.reactivar(trayectoria.getId());

        assertThat(rctvd.getEstado()).isEqualTo(EstadoTrayectoria.ACTIVA);
        assertThat(rctvd.getFechaFin()).isNull();
        assertThat(trayectoriaService.obtenerActiva(estudiante.getId())).isPresent();
    }

    @Test
    void reactivarFallaSiYaHayOtraTrayectoriaActiva() {
        Estudiante estudiante = crearEstudiante();
        Trayectoria prmr = trayectoriaService.iniciar(estudiante.getId(), programa.getId(), plan2020.getId(), null, null);
        trayectoriaService.retirar(prmr.getId(), null, null);
        trayectoriaService.iniciar(estudiante.getId(), programa.getId(), plan2024.getId(), null, null);

        assertThatThrownBy(() -> trayectoriaService.reactivar(prmr.getId()))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void eliminarBorraLaTrayectoriaYSusNotasSinDejarHuerfanas() {
        Estudiante estudiante = crearEstudiante();
        Trayectoria trayectoria = trayectoriaService.iniciar(
                estudiante.getId(), programa.getId(), plan2020.getId(), null, "Trayectoria mal creada");
        registrarUnaNota(trayectoria, plan2020);

        trayectoriaService.eliminar(trayectoria.getId());

        assertThat(trayectoriaService.listarPorEstudiante(estudiante.getId())).isEmpty();
        assertThat(registroAcademicoRepository.existsByTrayectoriaId(trayectoria.getId())).isFalse();
    }

    private void registrarUnaNota(Trayectoria trayectoria, PlanEstudio pln) {
        Ciclo ccl = cicloService.crear(pln.getId(), 1 + (int) (System.nanoTime() % 1000), "Ciclo nota", 1);
        Curso crs = cursoService.crear("TRY" + System.nanoTime(), "Curso para nota", 3, null, null);
        var plnCrs = planCursoService.asignar(pln.getId(), ccl.getId(), crs.getId(), 1, true);
        PeriodoAcademico periodo = periodoAcademicoService.crear("TRYP" + System.nanoTime(), 2026, "I", null, null);
        Long aprbdId = registroAcademicoService.listarSituaciones().stream()
                .filter(s -> s.getCodigo().equals("APROBADO")).findFirst().orElseThrow().getId();
        registroAcademicoService.guardarMasivo(trayectoria.getId(), periodo.getId(),
                List.of(new ItemNota(plnCrs.getId(), 15.0, aprbdId, null)));
    }
}
