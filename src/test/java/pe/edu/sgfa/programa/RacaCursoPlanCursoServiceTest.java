package pe.edu.sgfa.programa;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import pe.edu.sgfa.common.exception.RyeValidacionException;
import pe.edu.sgfa.institucion.service.RacaInstitucionService;
import pe.edu.sgfa.periodo.domain.YmclPeriodoAcademico;
import pe.edu.sgfa.periodo.service.YmclPeriodoAcademicoService;
import pe.edu.sgfa.programa.domain.RacaCiclo;
import pe.edu.sgfa.programa.domain.RacaCurso;
import pe.edu.sgfa.programa.domain.RacaPlanCurso;
import pe.edu.sgfa.programa.domain.RacaPlanEstudio;
import pe.edu.sgfa.programa.domain.RacaProgramaEstudio;
import pe.edu.sgfa.programa.service.RacaCicloService;
import pe.edu.sgfa.programa.service.RacaCursoService;
import pe.edu.sgfa.programa.service.RacaPlanCursoService;
import pe.edu.sgfa.programa.service.RacaPlanEstudioService;
import pe.edu.sgfa.programa.service.RacaProgramaEstudioService;
import pe.edu.sgfa.usuario.service.RyeAdminBootstrapResult;
import pe.edu.sgfa.usuario.service.RyeAuthService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RacaCursoPlanCursoServiceTest {

    @BeforeAll
    static void useIsolatedHomeDir() throws IOException {
        Path tempHomeCrr = Files.createTempDirectory("sgfa-curso-test-home-");
        System.setProperty("sgfa.home-dir", tempHomeCrr.toString());
    }

    @Autowired
    private RacaInstitucionService institucionServiceCrr;
    @Autowired
    private RacaProgramaEstudioService programaEstudioServiceCrr;
    @Autowired
    private RacaPlanEstudioService planEstudioServiceCrr;
    @Autowired
    private RacaCicloService cicloServiceCrr;
    @Autowired
    private RacaCursoService cursoServiceCrr;
    @Autowired
    private RacaPlanCursoService planCursoServiceCrr;
    @Autowired
    private YmclPeriodoAcademicoService periodoAcademicoServiceCrr;
    @Autowired
    private RyeAuthService authServiceCrr;
    @Autowired
    private RyeAdminBootstrapResult adminBootstrapResultCrr;
    @Autowired
    private JdbcTemplate jdbcTemplateCrr;

    private static RyeAdminBootstrapResult.RyeCredenciales credencialesAdminCrr;

    @BeforeEach
    void loginComoAdminYConfigurarInstitucion() {
        if (credencialesAdminCrr == null) {
            credencialesAdminCrr = adminBootstrapResultCrr.ryeConsumir()
                    .orElseThrow(() -> new IllegalStateException("El admin inicial no se genero"));
        }
        authServiceCrr.ryeCerrarSesion();
        authServiceCrr.ryeAutenticar(credencialesAdminCrr.username(), credencialesAdminCrr.password());

        if (institucionServiceCrr.racaObtenerPrincipal().isEmpty()) {
            institucionServiceCrr.racaGuardar("IESPP Juliaca", "JULI", null, null, null);
        }
    }

    @Test
    void crearCursoYEditarloFunciona() {
        RacaCurso cursoCrr = cursoServiceCrr.racaCrear("MAT101", "Matematica Basica", 4, 64, "OBLIGATORIO");
        assertThat(cursoCrr.getId()).isNotNull();

        RacaCurso editadoCrr = cursoServiceCrr.racaEditar(cursoCrr.getId(), "MAT101", "Matematica Basica I", 5, 80, "OBLIGATORIO");
        assertThat(editadoCrr.getCreditos()).isEqualTo(5);
    }

    @Test
    void crearCursoConCodigoDuplicadoFalla() {
        cursoServiceCrr.racaCrear("DUP1", "Curso uno", 3, null, null);

        assertThatThrownBy(() -> cursoServiceCrr.racaCrear("DUP1", "Curso dos", 3, null, null))
                .isInstanceOf(RyeValidacionException.class);
    }

    @Test
    void asignarCursoAUnCicloDeOtroPlanFalla() {
        RacaProgramaEstudio programaCrr = programaEstudioServiceCrr.racaCrear("ASG", "Programa asignacion", null, 6);
        RacaPlanEstudio planACrr = planEstudioServiceCrr.racaCrear(programaCrr.getId(), "PA", "Plan A", null, null);
        RacaPlanEstudio planBCrr = planEstudioServiceCrr.racaCrear(programaCrr.getId(), "PB", "Plan B", null, null);
        RacaCiclo cicloDePlanACrr = cicloServiceCrr.racaCrear(planACrr.getId(), 1, "Ciclo 1", 1);
        RacaCurso cursoCrr = cursoServiceCrr.racaCrear("ASG101", "Curso asignacion", 3, null, null);

        assertThatThrownBy(() -> planCursoServiceCrr.racaAsignar(planBCrr.getId(), cicloDePlanACrr.getId(), cursoCrr.getId(), 1, true))
                .isInstanceOf(RyeValidacionException.class);
    }

    @Test
    void asignarYQuitarCursoDeUnCicloFunciona() {
        RacaProgramaEstudio programaCrr = programaEstudioServiceCrr.racaCrear("QUI", "Programa quitar", null, 6);
        RacaPlanEstudio planCrr = planEstudioServiceCrr.racaCrear(programaCrr.getId(), "PQ", "Plan quitar", null, null);
        RacaCiclo cicloCrr = cicloServiceCrr.racaCrear(planCrr.getId(), 1, "Ciclo 1", 1);
        RacaCurso cursoCrr = cursoServiceCrr.racaCrear("QUI101", "Curso quitar", 3, null, null);

        RacaPlanCurso asignacionCrr = planCursoServiceCrr.racaAsignar(planCrr.getId(), cicloCrr.getId(), cursoCrr.getId(), 1, true);
        assertThat(planCursoServiceCrr.racaListarPorCiclo(cicloCrr.getId())).hasSize(1);

        planCursoServiceCrr.racaQuitar(asignacionCrr.getId());
        assertThat(planCursoServiceCrr.racaListarPorCiclo(cicloCrr.getId())).isEmpty();
    }

    @Test
    void quitarCursoConNotasRegistradasFalla() {
        RacaProgramaEstudio programaCrr = programaEstudioServiceCrr.racaCrear("BLQ", "Programa bloqueo", null, 6);
        RacaPlanEstudio planCrr = planEstudioServiceCrr.racaCrear(programaCrr.getId(), "PB2", "Plan bloqueo", null, null);
        RacaCiclo cicloCrr = cicloServiceCrr.racaCrear(planCrr.getId(), 1, "Ciclo 1", 1);
        RacaCurso cursoCrr = cursoServiceCrr.racaCrear("BLQ101", "Curso bloqueo", 3, null, null);
        RacaPlanCurso asignacionCrr = planCursoServiceCrr.racaAsignar(planCrr.getId(), cicloCrr.getId(), cursoCrr.getId(), 1, true);
        YmclPeriodoAcademico periodoCrr = periodoAcademicoServiceCrr.ymclCrear("2026-I", 2026, "I", null, null);

        jdbcTemplateCrr.update(
                "INSERT INTO estudiantes (nombres, apellido_paterno, estado) VALUES (?, ?, 'ACTIVO')",
                "Juan", "Perez");
        Long estudianteIdCrr = jdbcTemplateCrr.queryForObject("SELECT last_insert_rowid()", Long.class);

        jdbcTemplateCrr.update(
                "INSERT INTO trayectorias_academicas (estudiante_id, programa_id, plan_estudio_id, estado) "
                        + "VALUES (?, ?, ?, 'ACTIVA')",
                estudianteIdCrr, programaCrr.getId(), planCrr.getId());
        Long trayectoriaIdCrr = jdbcTemplateCrr.queryForObject("SELECT last_insert_rowid()", Long.class);

        Long situacionIdCrr = jdbcTemplateCrr.queryForObject(
                "SELECT id FROM situaciones_academicas WHERE codigo = 'APROBADO'", Long.class);

        jdbcTemplateCrr.update(
                "INSERT INTO registros_academicos (trayectoria_id, plan_curso_id, periodo_id, nota_final, situacion_id) "
                        + "VALUES (?, ?, ?, 17, ?)",
                trayectoriaIdCrr, asignacionCrr.getId(), periodoCrr.getId(), situacionIdCrr);

        assertThatThrownBy(() -> planCursoServiceCrr.racaQuitar(asignacionCrr.getId()))
                .isInstanceOf(RyeValidacionException.class);
    }
}
