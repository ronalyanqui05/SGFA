package pe.edu.sgfa.nota;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import pe.edu.sgfa.common.exception.ValidacionException;
import pe.edu.sgfa.estudiante.domain.Estudiante;
import pe.edu.sgfa.estudiante.service.EstudianteService;
import pe.edu.sgfa.institucion.service.InstitucionService;
import pe.edu.sgfa.nota.domain.SituacionAcademica;
import pe.edu.sgfa.nota.repository.SituacionAcademicaRepository;
import pe.edu.sgfa.nota.service.FilaRegistro;
import pe.edu.sgfa.nota.service.ItemNota;
import pe.edu.sgfa.nota.service.RegistroAcademicoService;
import pe.edu.sgfa.nota.service.ResultadoPromedio;
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
import pe.edu.sgfa.trayectoria.domain.Trayectoria;
import pe.edu.sgfa.trayectoria.service.TrayectoriaService;
import pe.edu.sgfa.usuario.service.AdminBootstrapResult;
import pe.edu.sgfa.usuario.service.AuthService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RegistroAcademicoServiceTest {

    @BeforeAll
    static void useIsolatedHomeDir() throws IOException {
        Path tmpHm = Files.createTempDirectory("sgfa-nota-test-home-");
        System.setProperty("sgfa.home-dir", tmpHm.toString());
    }

    @Autowired
    private InstitucionService institucionService;
    @Autowired
    private ProgramaEstudioService programaEstudioService;
    @Autowired
    private PlanEstudioService planEstudioService;
    @Autowired
    private CicloService cicloService;
    @Autowired
    private CursoService cursoService;
    @Autowired
    private PlanCursoService planCursoService;
    @Autowired
    private PeriodoAcademicoService periodoAcademicoService;
    @Autowired
    private EstudianteService estudianteService;
    @Autowired
    private TrayectoriaService trayectoriaService;
    @Autowired
    private RegistroAcademicoService registroAcademicoService;
    @Autowired
    private SituacionAcademicaRepository situacionAcademicaRepository;
    @Autowired
    private AuthService authService;
    @Autowired
    private AdminBootstrapResult adminBootstrapResult;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static AdminBootstrapResult.Credenciales credencialesAdmin;

    private Ciclo ciclo;
    private Curso curso1;
    private Curso curso2;
    private Trayectoria trayectoria;
    private PeriodoAcademico periodo;
    private SituacionAcademica aprobado;
    private SituacionAcademica convalidado;

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

        ProgramaEstudio programa = programaEstudioService.crear("COM" + System.nanoTime(), "Comunicacion", null, 10);
        PlanEstudio pln = planEstudioService.crear(programa.getId(), "P" + System.nanoTime(), "Plan", null, null);
        ciclo = cicloService.crear(pln.getId(), 1, "Primer ciclo", 1);
        curso1 = cursoService.crear("MAT" + System.nanoTime(), "Matematica", 4, null, null);
        curso2 = cursoService.crear("COM" + System.nanoTime(), "Comunicacion", 3, null, null);
        planCursoService.asignar(pln.getId(), ciclo.getId(), curso1.getId(), 1, true);
        planCursoService.asignar(pln.getId(), ciclo.getId(), curso2.getId(), 2, true);

        periodo = periodoAcademicoService.crear("PER" + System.nanoTime(), 2026, "I", null, null);

        Estudiante estudiante = estudianteService.crear(null, null, "Juan", "Perez" + System.nanoTime(),
                null, null, null, null, null, null);
        trayectoria = trayectoriaService.iniciar(estudiante.getId(), programa.getId(), pln.getId(), null, null);

        aprobado = situacionAcademicaRepository.findAll().stream()
                .filter(s -> s.getCodigo().equals("APROBADO")).findFirst().orElseThrow();
        convalidado = situacionAcademicaRepository.findAll().stream()
                .filter(s -> s.getCodigo().equals("CONVALIDADO")).findFirst().orElseThrow();
    }

    @Test
    void listarParaRegistroDevuelveFilasSinRegistroCuandoNoHayNotas() {
        List<FilaRegistro> fls = registroAcademicoService.listarParaRegistro(
                trayectoria.getId(), ciclo.getId(), periodo.getId());

        assertThat(fls).hasSize(2);
        assertThat(fls).allSatisfy(f -> assertThat(f.registro()).isNull());
    }

    @Test
    void guardarMasivoSoloCreaFilasConSituacionElegida() {
        registroAcademicoService.guardarMasivo(trayectoria.getId(), periodo.getId(), List.of(
                new ItemNota(planCursoIdDe(curso1), 15.0, aprobado.getId(), null),
                new ItemNota(planCursoIdDe(curso2), null, null, null)));

        List<FilaRegistro> fls = registroAcademicoService.listarParaRegistro(
                trayectoria.getId(), ciclo.getId(), periodo.getId());

        assertThat(fls).filteredOn(f -> f.planCurso().getCurso().getId().equals(curso1.getId()))
                .allSatisfy(f -> assertThat(f.registro()).isNotNull());
        assertThat(fls).filteredOn(f -> f.planCurso().getCurso().getId().equals(curso2.getId()))
                .allSatisfy(f -> assertThat(f.registro()).isNull());
    }

    @Test
    void notaFueraDeRangoFalla() {
        Long plnCrsId = planCursoIdDe(curso1);

        assertThatThrownBy(() -> registroAcademicoService.guardarMasivo(trayectoria.getId(), periodo.getId(),
                List.of(new ItemNota(plnCrsId, 21.0, aprobado.getId(), null))))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void editarUnaNotaYaGuardadaQuedaAuditadaConValorAnteriorYNuevo() {
        Long plnCrsId = planCursoIdDe(curso1);
        registroAcademicoService.guardarMasivo(trayectoria.getId(), periodo.getId(),
                List.of(new ItemNota(plnCrsId, 12.0, aprobado.getId(), null)));

        registroAcademicoService.guardarMasivo(trayectoria.getId(), periodo.getId(),
                List.of(new ItemNota(plnCrsId, 17.0, aprobado.getId(), "correccion")));

        Integer cntEdtr = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auditoria WHERE accion = 'EDITAR_NOTA' AND detalle LIKE '%nota_anterior=12.0%nota_nueva=17.0%'",
                Integer.class);
        assertThat(cntEdtr).isEqualTo(1);
    }

    @Test
    void calcularPromedioExcluyeSituacionesQueNoCuentan() {
        registroAcademicoService.guardarMasivo(trayectoria.getId(), periodo.getId(), List.of(
                new ItemNota(planCursoIdDe(curso1), 16.0, aprobado.getId(), null),
                new ItemNota(planCursoIdDe(curso2), 18.0, convalidado.getId(), null)));

        ResultadoPromedio rsltd = registroAcademicoService.calcularPromedio(
                trayectoria.getId(), ciclo.getId(), periodo.getId());

        assertThat(rsltd.totalCreditos()).isEqualTo(curso1.getCreditos());
        assertThat(rsltd.promedio()).isEqualTo(16.0);
    }

    @Test
    void guardarNotaDeUnPlanCursoDeOtroPlanFalla() {
        ProgramaEstudio otrPrgrm = programaEstudioService.crear("OTR" + System.nanoTime(), "Otro programa", null, 6);
        PlanEstudio otrPln = planEstudioService.crear(otrPrgrm.getId(), "OP" + System.nanoTime(), "Otro plan", null, null);
        Ciclo otrCcl = cicloService.crear(otrPln.getId(), 1, "Ciclo otro plan", 1);
        Curso otrCrs = cursoService.crear("OTC" + System.nanoTime(), "Curso de otro plan", 3, null, null);
        var otrAsgncn = planCursoService.asignar(otrPln.getId(), otrCcl.getId(), otrCrs.getId(), 1, true);

        assertThatThrownBy(() -> registroAcademicoService.guardarMasivo(trayectoria.getId(), periodo.getId(),
                List.of(new ItemNota(otrAsgncn.getId(), 15.0, aprobado.getId(), null))))
                .isInstanceOf(ValidacionException.class);
    }

    private Long planCursoIdDe(Curso crs) {
        return registroAcademicoService.listarParaRegistro(trayectoria.getId(), ciclo.getId(), periodo.getId())
                .stream()
                .filter(f -> f.planCurso().getCurso().getId().equals(crs.getId()))
                .findFirst()
                .orElseThrow()
                .planCurso()
                .getId();
    }
}
