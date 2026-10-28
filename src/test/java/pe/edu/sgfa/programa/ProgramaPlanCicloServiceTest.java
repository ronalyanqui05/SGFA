package pe.edu.sgfa.programa;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import pe.edu.sgfa.common.exception.AccesoDenegadoException;
import pe.edu.sgfa.common.exception.ValidacionException;
import pe.edu.sgfa.institucion.service.InstitucionService;
import pe.edu.sgfa.programa.domain.Ciclo;
import pe.edu.sgfa.programa.domain.Curso;
import pe.edu.sgfa.programa.domain.EstadoPlanEstudio;
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
class ProgramaPlanCicloServiceTest {

    @BeforeAll
    static void useIsolatedHomeDir() throws IOException {
        Path tmpHm = Files.createTempDirectory("sgfa-programa-test-home-");
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
    private AuthService authService;
    @Autowired
    private AdminBootstrapResult adminBootstrapResult;

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
    void crearJerarquiaCompletaProgramaPlanCiclo() {
        ProgramaEstudio programa = programaEstudioService.crear(
                "COM", "Educacion Secundaria - Comunicacion", "Programa de formacion docente", 10);
        assertThat(programa.getId()).isNotNull();
        assertThat(programa.getInstitucion()).isNotNull();

        PlanEstudio pln = planEstudioService.crear(
                programa.getId(), "PLAN2024", "Plan 2024", LocalDate.of(2024, 3, 1), null);
        assertThat(pln.getPrograma().getId()).isEqualTo(programa.getId());

        Ciclo ccl1 = cicloService.crear(pln.getId(), 1, "Primer ciclo", 1);
        Ciclo ccl2 = cicloService.crear(pln.getId(), 2, "Segundo ciclo", 2);

        assertThat(cicloService.listarPorPlan(pln.getId())).extracting(Ciclo::getNumero)
                .containsExactly(ccl1.getNumero(), ccl2.getNumero());
    }

    @Test
    void crearProgramaConCodigoDuplicadoFalla() {
        programaEstudioService.crear("DUP", "Programa uno", null, 8);

        assertThatThrownBy(() -> programaEstudioService.crear("DUP", "Programa dos", null, 8))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void crearCicloConNumeroDuplicadoEnElMismoPlanFalla() {
        ProgramaEstudio programa = programaEstudioService.crear("CIC", "Programa ciclos", null, 6);
        PlanEstudio pln = planEstudioService.crear(programa.getId(), "P1", "Plan uno", null, null);
        cicloService.crear(pln.getId(), 1, "Primer ciclo", 1);

        assertThatThrownBy(() -> cicloService.crear(pln.getId(), 1, "Otro primer ciclo", 2))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void crearProgramaSinSesionAdminFalla() {
        authService.cerrarSesion();

        assertThatThrownBy(() -> programaEstudioService.crear("X", "Programa X", null, 6))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    void planConFechaFinAnteriorAFechaInicioFalla() {
        ProgramaEstudio programa = programaEstudioService.crear("FEC", "Programa fechas", null, 6);

        assertThatThrownBy(() -> planEstudioService.crear(programa.getId(), "PF", "Plan fechas",
                LocalDate.of(2024, 6, 1), LocalDate.of(2024, 1, 1)))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void eliminarCicloSinCursosAsignadosFunciona() {
        ProgramaEstudio programa = programaEstudioService.crear("ELC", "Programa eliminar ciclo", null, 6);
        PlanEstudio pln = planEstudioService.crear(programa.getId(), "PELC", "Plan eliminar ciclo", null, null);
        Ciclo ccl = cicloService.crear(pln.getId(), 1, "Ciclo a eliminar", 1);

        cicloService.eliminar(ccl.getId());

        assertThat(cicloService.listarPorPlan(pln.getId())).isEmpty();
    }

    @Test
    void eliminarPlanSinCiclosNiTrayectoriasFunciona() {
        ProgramaEstudio programa = programaEstudioService.crear("ELP", "Programa eliminar plan", null, 6);
        PlanEstudio pln = planEstudioService.crear(programa.getId(), "PELP", "Plan a eliminar", null, null);

        planEstudioService.eliminar(pln.getId());

        assertThat(planEstudioService.listarPorPrograma(programa.getId())).isEmpty();
    }

    @Test
    void eliminarPlanConCiclosFalla() {
        ProgramaEstudio programa = programaEstudioService.crear("ELPC", "Programa plan con ciclos", null, 6);
        PlanEstudio pln = planEstudioService.crear(programa.getId(), "PELPC", "Plan con ciclos", null, null);
        cicloService.crear(pln.getId(), 1, "Ciclo bloqueante", 1);

        assertThatThrownBy(() -> planEstudioService.eliminar(pln.getId()))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void eliminarCicloConCursosAsignadosFalla() {
        ProgramaEstudio programa = programaEstudioService.crear("ELCC", "Programa ciclo con cursos", null, 6);
        PlanEstudio pln = planEstudioService.crear(programa.getId(), "PELCC", "Plan ciclo con cursos", null, null);
        Ciclo ccl = cicloService.crear(pln.getId(), 1, "Ciclo bloqueante", 1);
        Curso crs = cursoService.crear("CELCC", "Curso bloqueante", 3, null, null);
        planCursoService.asignar(pln.getId(), ccl.getId(), crs.getId(), 1, true);

        assertThatThrownBy(() -> cicloService.eliminar(ccl.getId()))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void reabrirUnPlanCerradoLoDejaActivo() {
        ProgramaEstudio programa = programaEstudioService.crear("REAB", "Programa reabrir plan", null, 6);
        PlanEstudio pln = planEstudioService.crear(programa.getId(), "PREAB", "Plan a reabrir", null, null);
        planEstudioService.cambiarEstado(pln.getId(), EstadoPlanEstudio.CERRADO);

        PlanEstudio rbrt = planEstudioService.cambiarEstado(pln.getId(), EstadoPlanEstudio.ACTIVO);

        assertThat(rbrt.getEstado()).isEqualTo(EstadoPlanEstudio.ACTIVO);
    }
}
