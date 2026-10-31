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

import pe.edu.sgfa.common.exception.RyeValidacionException;
import pe.edu.sgfa.estudiante.domain.YmclEstudiante;
import pe.edu.sgfa.estudiante.service.YmclEstudianteService;
import pe.edu.sgfa.institucion.service.RacaInstitucionService;
import pe.edu.sgfa.nota.domain.YmclSituacionAcademica;
import pe.edu.sgfa.nota.repository.YmclSituacionAcademicaRepository;
import pe.edu.sgfa.nota.service.YmclFilaRegistro;
import pe.edu.sgfa.nota.service.YmclItemNota;
import pe.edu.sgfa.nota.service.YmclRegistroAcademicoService;
import pe.edu.sgfa.nota.service.YmclResultadoPromedio;
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
import pe.edu.sgfa.trayectoria.domain.YmclTrayectoria;
import pe.edu.sgfa.trayectoria.service.YmclTrayectoriaService;
import pe.edu.sgfa.usuario.service.RyeAdminBootstrapResult;
import pe.edu.sgfa.usuario.service.RyeAuthService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class YmclRegistroAcademicoServiceTest {

    @BeforeAll
    static void useIsolatedHomeDir() throws IOException {
        Path tempHomeChp = Files.createTempDirectory("sgfa-nota-test-home-");
        System.setProperty("sgfa.home-dir", tempHomeChp.toString());
    }

    @Autowired
    private RacaInstitucionService institucionServiceChp;
    @Autowired
    private RacaProgramaEstudioService programaEstudioServiceChp;
    @Autowired
    private RacaPlanEstudioService planEstudioServiceChp;
    @Autowired
    private RacaCicloService cicloServiceChp;
    @Autowired
    private RacaCursoService cursoServiceChp;
    @Autowired
    private RacaPlanCursoService planCursoServiceChp;
    @Autowired
    private YmclPeriodoAcademicoService periodoAcademicoServiceChp;
    @Autowired
    private YmclEstudianteService estudianteServiceChp;
    @Autowired
    private YmclTrayectoriaService trayectoriaServiceChp;
    @Autowired
    private YmclRegistroAcademicoService registroAcademicoServiceChp;
    @Autowired
    private YmclSituacionAcademicaRepository situacionAcademicaRepositoryChp;
    @Autowired
    private RyeAuthService authServiceChp;
    @Autowired
    private RyeAdminBootstrapResult adminBootstrapResultChp;
    @Autowired
    private JdbcTemplate jdbcTemplateChp;

    private static RyeAdminBootstrapResult.RyeCredenciales credencialesAdminChp;

    private RacaCiclo cicloChp;
    private RacaCurso curso1Chp;
    private RacaCurso curso2Chp;
    private YmclTrayectoria trayectoriaChp;
    private YmclPeriodoAcademico periodoChp;
    private YmclSituacionAcademica aprobadoChp;
    private YmclSituacionAcademica convalidadoChp;

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

        RacaProgramaEstudio programaChp = programaEstudioServiceChp.racaCrear("COM" + System.nanoTime(), "Comunicacion", null, 10);
        RacaPlanEstudio planChp = planEstudioServiceChp.racaCrear(programaChp.getId(), "P" + System.nanoTime(), "Plan", null, null);
        cicloChp = cicloServiceChp.racaCrear(planChp.getId(), 1, "Primer ciclo", 1);
        curso1Chp = cursoServiceChp.racaCrear("MAT" + System.nanoTime(), "Matematica", 4, null, null);
        curso2Chp = cursoServiceChp.racaCrear("COM" + System.nanoTime(), "Comunicacion", 3, null, null);
        planCursoServiceChp.racaAsignar(planChp.getId(), cicloChp.getId(), curso1Chp.getId(), 1, true);
        planCursoServiceChp.racaAsignar(planChp.getId(), cicloChp.getId(), curso2Chp.getId(), 2, true);

        periodoChp = periodoAcademicoServiceChp.ymclCrear("PER" + System.nanoTime(), 2026, "I", null, null);

        YmclEstudiante estudianteChp = estudianteServiceChp.ymclCrear(null, null, "Juan", "Perez" + System.nanoTime(),
                null, null, null, null, null, null);
        trayectoriaChp = trayectoriaServiceChp.ymclIniciar(estudianteChp.getId(), programaChp.getId(), planChp.getId(), null, null);

        aprobadoChp = situacionAcademicaRepositoryChp.findAll().stream()
                .filter(s -> s.getCodigo().equals("APROBADO")).findFirst().orElseThrow();
        convalidadoChp = situacionAcademicaRepositoryChp.findAll().stream()
                .filter(s -> s.getCodigo().equals("CONVALIDADO")).findFirst().orElseThrow();
    }

    @Test
    void listarParaRegistroDevuelveFilasSinRegistroCuandoNoHayNotas() {
        List<YmclFilaRegistro> filasChp = registroAcademicoServiceChp.ymclListarParaRegistro(
                trayectoriaChp.getId(), cicloChp.getId(), periodoChp.getId());

        assertThat(filasChp).hasSize(2);
        assertThat(filasChp).allSatisfy(f -> assertThat(f.registro()).isNull());
    }

    @Test
    void guardarMasivoSoloCreaFilasConSituacionElegida() {
        registroAcademicoServiceChp.ymclGuardarMasivo(trayectoriaChp.getId(), periodoChp.getId(), List.of(
                new YmclItemNota(planCursoIdDe(curso1Chp), 15.0, aprobadoChp.getId(), null),
                new YmclItemNota(planCursoIdDe(curso2Chp), null, null, null)));

        List<YmclFilaRegistro> filasChp = registroAcademicoServiceChp.ymclListarParaRegistro(
                trayectoriaChp.getId(), cicloChp.getId(), periodoChp.getId());

        assertThat(filasChp).filteredOn(f -> f.planCurso().getCurso().getId().equals(curso1Chp.getId()))
                .allSatisfy(f -> assertThat(f.registro()).isNotNull());
        assertThat(filasChp).filteredOn(f -> f.planCurso().getCurso().getId().equals(curso2Chp.getId()))
                .allSatisfy(f -> assertThat(f.registro()).isNull());
    }

    @Test
    void notaFueraDeRangoFalla() {
        Long planCursoIdChp = planCursoIdDe(curso1Chp);

        assertThatThrownBy(() -> registroAcademicoServiceChp.ymclGuardarMasivo(trayectoriaChp.getId(), periodoChp.getId(),
                List.of(new YmclItemNota(planCursoIdChp, 21.0, aprobadoChp.getId(), null))))
                .isInstanceOf(RyeValidacionException.class);
    }

    @Test
    void editarUnaNotaYaGuardadaQuedaAuditadaConValorAnteriorYNuevo() {
        Long planCursoIdChp = planCursoIdDe(curso1Chp);
        registroAcademicoServiceChp.ymclGuardarMasivo(trayectoriaChp.getId(), periodoChp.getId(),
                List.of(new YmclItemNota(planCursoIdChp, 12.0, aprobadoChp.getId(), null)));

        registroAcademicoServiceChp.ymclGuardarMasivo(trayectoriaChp.getId(), periodoChp.getId(),
                List.of(new YmclItemNota(planCursoIdChp, 17.0, aprobadoChp.getId(), "correccion")));

        Integer countEditarChp = jdbcTemplateChp.queryForObject(
                "SELECT COUNT(*) FROM auditoria WHERE accion = 'EDITAR_NOTA' AND detalle LIKE '%nota_anterior=12.0%nota_nueva=17.0%'",
                Integer.class);
        assertThat(countEditarChp).isEqualTo(1);
    }

    @Test
    void calcularPromedioExcluyeSituacionesQueNoCuentan() {
        registroAcademicoServiceChp.ymclGuardarMasivo(trayectoriaChp.getId(), periodoChp.getId(), List.of(
                new YmclItemNota(planCursoIdDe(curso1Chp), 16.0, aprobadoChp.getId(), null),
                new YmclItemNota(planCursoIdDe(curso2Chp), 18.0, convalidadoChp.getId(), null)));

        YmclResultadoPromedio resultadoChp = registroAcademicoServiceChp.ymclCalcularPromedio(
                trayectoriaChp.getId(), cicloChp.getId(), periodoChp.getId());

        assertThat(resultadoChp.totalCreditos()).isEqualTo(curso1Chp.getCreditos());
        assertThat(resultadoChp.promedio()).isEqualTo(16.0);
    }

    @Test
    void guardarNotaDeUnPlanCursoDeOtroPlanFalla() {
        RacaProgramaEstudio otroProgramaChp = programaEstudioServiceChp.racaCrear("OTR" + System.nanoTime(), "Otro programa", null, 6);
        RacaPlanEstudio otroPlanChp = planEstudioServiceChp.racaCrear(otroProgramaChp.getId(), "OP" + System.nanoTime(), "Otro plan", null, null);
        RacaCiclo otroCicloChp = cicloServiceChp.racaCrear(otroPlanChp.getId(), 1, "Ciclo otro plan", 1);
        RacaCurso otroCursoChp = cursoServiceChp.racaCrear("OTC" + System.nanoTime(), "Curso de otro plan", 3, null, null);
        var otraAsignacionChp = planCursoServiceChp.racaAsignar(otroPlanChp.getId(), otroCicloChp.getId(), otroCursoChp.getId(), 1, true);

        assertThatThrownBy(() -> registroAcademicoServiceChp.ymclGuardarMasivo(trayectoriaChp.getId(), periodoChp.getId(),
                List.of(new YmclItemNota(otraAsignacionChp.getId(), 15.0, aprobadoChp.getId(), null))))
                .isInstanceOf(RyeValidacionException.class);
    }

    private Long planCursoIdDe(RacaCurso cursoChp) {
        return registroAcademicoServiceChp.ymclListarParaRegistro(trayectoriaChp.getId(), cicloChp.getId(), periodoChp.getId())
                .stream()
                .filter(f -> f.planCurso().getCurso().getId().equals(cursoChp.getId()))
                .findFirst()
                .orElseThrow()
                .planCurso()
                .getId();
    }
}
