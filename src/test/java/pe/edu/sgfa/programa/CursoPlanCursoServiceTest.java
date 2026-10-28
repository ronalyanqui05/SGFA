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

import pe.edu.sgfa.common.exception.ValidacionException;
import pe.edu.sgfa.institucion.service.InstitucionService;
import pe.edu.sgfa.periodo.domain.PeriodoAcademico;
import pe.edu.sgfa.periodo.service.PeriodoAcademicoService;
import pe.edu.sgfa.programa.domain.Ciclo;
import pe.edu.sgfa.programa.domain.Curso;
import pe.edu.sgfa.programa.domain.PlanCurso;
import pe.edu.sgfa.programa.domain.PlanEstudio;
import pe.edu.sgfa.programa.domain.ProgramaEstudio;
import pe.edu.sgfa.programa.service.CicloService;
import pe.edu.sgfa.programa.service.CursoService;
import pe.edu.sgfa.programa.service.PlanCursoService;
import pe.edu.sgfa.programa.service.PlanEstudioService;
import pe.edu.sgfa.programa.service.ProgramaEstudioService;
import pe.edu.sgfa.usuario.service.AdminBootstrapResult;
import pe.edu.sgfa.usuario.service.AuthService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CursoPlanCursoServiceTest {

    @BeforeAll
    static void useIsolatedHomeDir() throws IOException {
        Path tmpHm = Files.createTempDirectory("sgfa-curso-test-home-");
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
    private AuthService authService;
    @Autowired
    private AdminBootstrapResult adminBootstrapResult;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static AdminBootstrapResult.Credenciales credencialesAdmin;

    @BeforeEach
    void loginComoAdminYConfigurarInstitucion() {
        if (credencialesAdmin == null) {
            credencialesAdmin = adminBootstrapResult.consumir()
                    .orElseThrow(() -> new IllegalStateException("El admin inicial no se genero"));
        }
        authService.cerrarSesion();
        authService.autenticar(credencialesAdmin.username(), credencialesAdmin.password());

        if (institucionService.obtenerPrincipal().isEmpty()) {
            institucionService.guardar("IESPP Juliaca", "JULI", null, null, null);
        }
    }

    @Test
    void crearCursoYEditarloFunciona() {
        Curso crs = cursoService.crear("MAT101", "Matematica Basica", 4, 64, "OBLIGATORIO");
        assertThat(crs.getId()).isNotNull();

        Curso edtd = cursoService.editar(crs.getId(), "MAT101", "Matematica Basica I", 5, 80, "OBLIGATORIO");
        assertThat(edtd.getCreditos()).isEqualTo(5);
    }

    @Test
    void crearCursoConCodigoDuplicadoFalla() {
        cursoService.crear("DUP1", "Curso uno", 3, null, null);

        assertThatThrownBy(() -> cursoService.crear("DUP1", "Curso dos", 3, null, null))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void asignarCursoAUnCicloDeOtroPlanFalla() {
        ProgramaEstudio programa = programaEstudioService.crear("ASG", "Programa asignacion", null, 6);
        PlanEstudio plnA = planEstudioService.crear(programa.getId(), "PA", "Plan A", null, null);
        PlanEstudio plnB = planEstudioService.crear(programa.getId(), "PB", "Plan B", null, null);
        Ciclo cclDePlnA = cicloService.crear(plnA.getId(), 1, "Ciclo 1", 1);
        Curso crs = cursoService.crear("ASG101", "Curso asignacion", 3, null, null);

        assertThatThrownBy(() -> planCursoService.asignar(plnB.getId(), cclDePlnA.getId(), crs.getId(), 1, true))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void asignarYQuitarCursoDeUnCicloFunciona() {
        ProgramaEstudio programa = programaEstudioService.crear("QUI", "Programa quitar", null, 6);
        PlanEstudio pln = planEstudioService.crear(programa.getId(), "PQ", "Plan quitar", null, null);
        Ciclo ccl = cicloService.crear(pln.getId(), 1, "Ciclo 1", 1);
        Curso crs = cursoService.crear("QUI101", "Curso quitar", 3, null, null);

        PlanCurso asgncn = planCursoService.asignar(pln.getId(), ccl.getId(), crs.getId(), 1, true);
        assertThat(planCursoService.listarPorCiclo(ccl.getId())).hasSize(1);

        planCursoService.quitar(asgncn.getId());
        assertThat(planCursoService.listarPorCiclo(ccl.getId())).isEmpty();
    }

    @Test
    void quitarCursoConNotasRegistradasFalla() {
        ProgramaEstudio programa = programaEstudioService.crear("BLQ", "Programa bloqueo", null, 6);
        PlanEstudio pln = planEstudioService.crear(programa.getId(), "PB2", "Plan bloqueo", null, null);
        Ciclo ccl = cicloService.crear(pln.getId(), 1, "Ciclo 1", 1);
        Curso crs = cursoService.crear("BLQ101", "Curso bloqueo", 3, null, null);
        PlanCurso asgncn = planCursoService.asignar(pln.getId(), ccl.getId(), crs.getId(), 1, true);
        PeriodoAcademico periodo = periodoAcademicoService.crear("2026-I", 2026, "I", null, null);

        jdbcTemplate.update(
                "INSERT INTO estudiantes (nombres, apellido_paterno, estado) VALUES (?, ?, 'ACTIVO')",
                "Juan", "Perez");
        Long estdntId = jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Long.class);

        jdbcTemplate.update(
                "INSERT INTO trayectorias_academicas (estudiante_id, programa_id, plan_estudio_id, estado) "
                        + "VALUES (?, ?, ?, 'ACTIVA')",
                estdntId, programa.getId(), pln.getId());
        Long tryctrId = jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Long.class);

        Long stcnId = jdbcTemplate.queryForObject(
                "SELECT id FROM situaciones_academicas WHERE codigo = 'APROBADO'", Long.class);

        jdbcTemplate.update(
                "INSERT INTO registros_academicos (trayectoria_id, plan_curso_id, periodo_id, nota_final, situacion_id) "
                        + "VALUES (?, ?, ?, 17, ?)",
                tryctrId, asgncn.getId(), periodo.getId(), stcnId);

        assertThatThrownBy(() -> planCursoService.quitar(asgncn.getId()))
                .isInstanceOf(ValidacionException.class);
    }
}
