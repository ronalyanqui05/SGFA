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

import pe.edu.sgfa.common.exception.RyeAccesoDenegadoException;
import pe.edu.sgfa.common.exception.RyeValidacionException;
import pe.edu.sgfa.institucion.service.RacaInstitucionService;
import pe.edu.sgfa.programa.domain.RacaCiclo;
import pe.edu.sgfa.programa.domain.RacaCurso;
import pe.edu.sgfa.programa.domain.RacaEstadoPlanEstudio;
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
class RacaProgramaPlanCicloServiceTest {

    @BeforeAll
    static void useIsolatedHomeDir() throws IOException {
        Path tempHomeCrr = Files.createTempDirectory("sgfa-programa-test-home-");
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
    private RyeAuthService authServiceCrr;
    @Autowired
    private RyeAdminBootstrapResult adminBootstrapResultCrr;

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
    void crearJerarquiaCompletaProgramaPlanCiclo() {
        RacaProgramaEstudio programaCrr = programaEstudioServiceCrr.racaCrear(
                "COM", "Educacion Secundaria - Comunicacion", "Programa de formacion docente", 10);
        assertThat(programaCrr.getId()).isNotNull();
        assertThat(programaCrr.getInstitucion()).isNotNull();

        RacaPlanEstudio planCrr = planEstudioServiceCrr.racaCrear(
                programaCrr.getId(), "PLAN2024", "Plan 2024", LocalDate.of(2024, 3, 1), null);
        assertThat(planCrr.getPrograma().getId()).isEqualTo(programaCrr.getId());

        RacaCiclo ciclo1Crr = cicloServiceCrr.racaCrear(planCrr.getId(), 1, "Primer ciclo", 1);
        RacaCiclo ciclo2Crr = cicloServiceCrr.racaCrear(planCrr.getId(), 2, "Segundo ciclo", 2);

        assertThat(cicloServiceCrr.racaListarPorPlan(planCrr.getId())).extracting(RacaCiclo::getNumero)
                .containsExactly(ciclo1Crr.getNumero(), ciclo2Crr.getNumero());
    }

    @Test
    void crearProgramaConCodigoDuplicadoFalla() {
        programaEstudioServiceCrr.racaCrear("DUP", "Programa uno", null, 8);

        assertThatThrownBy(() -> programaEstudioServiceCrr.racaCrear("DUP", "Programa dos", null, 8))
                .isInstanceOf(RyeValidacionException.class);
    }

    @Test
    void crearCicloConNumeroDuplicadoEnElMismoPlanFalla() {
        RacaProgramaEstudio programaCrr = programaEstudioServiceCrr.racaCrear("CIC", "Programa ciclos", null, 6);
        RacaPlanEstudio planCrr = planEstudioServiceCrr.racaCrear(programaCrr.getId(), "P1", "Plan uno", null, null);
        cicloServiceCrr.racaCrear(planCrr.getId(), 1, "Primer ciclo", 1);

        assertThatThrownBy(() -> cicloServiceCrr.racaCrear(planCrr.getId(), 1, "Otro primer ciclo", 2))
                .isInstanceOf(RyeValidacionException.class);
    }

    @Test
    void crearProgramaSinSesionAdminFalla() {
        authServiceCrr.ryeCerrarSesion();

        assertThatThrownBy(() -> programaEstudioServiceCrr.racaCrear("X", "Programa X", null, 6))
                .isInstanceOf(RyeAccesoDenegadoException.class);
    }

    @Test
    void planConFechaFinAnteriorAFechaInicioFalla() {
        RacaProgramaEstudio programaCrr = programaEstudioServiceCrr.racaCrear("FEC", "Programa fechas", null, 6);

        assertThatThrownBy(() -> planEstudioServiceCrr.racaCrear(programaCrr.getId(), "PF", "Plan fechas",
                LocalDate.of(2024, 6, 1), LocalDate.of(2024, 1, 1)))
                .isInstanceOf(RyeValidacionException.class);
    }

    @Test
    void eliminarCicloSinCursosAsignadosFunciona() {
        RacaProgramaEstudio programaCrr = programaEstudioServiceCrr.racaCrear("ELC", "Programa eliminar ciclo", null, 6);
        RacaPlanEstudio planCrr = planEstudioServiceCrr.racaCrear(programaCrr.getId(), "PELC", "Plan eliminar ciclo", null, null);
        RacaCiclo cicloCrr = cicloServiceCrr.racaCrear(planCrr.getId(), 1, "Ciclo a eliminar", 1);

        cicloServiceCrr.racaEliminar(cicloCrr.getId());

        assertThat(cicloServiceCrr.racaListarPorPlan(planCrr.getId())).isEmpty();
    }

    @Test
    void eliminarPlanSinCiclosNiTrayectoriasFunciona() {
        RacaProgramaEstudio programaCrr = programaEstudioServiceCrr.racaCrear("ELP", "Programa eliminar plan", null, 6);
        RacaPlanEstudio planCrr = planEstudioServiceCrr.racaCrear(programaCrr.getId(), "PELP", "Plan a eliminar", null, null);

        planEstudioServiceCrr.racaEliminar(planCrr.getId());

        assertThat(planEstudioServiceCrr.racaListarPorPrograma(programaCrr.getId())).isEmpty();
    }

    @Test
    void eliminarPlanConCiclosFalla() {
        RacaProgramaEstudio programaCrr = programaEstudioServiceCrr.racaCrear("ELPC", "Programa plan con ciclos", null, 6);
        RacaPlanEstudio planCrr = planEstudioServiceCrr.racaCrear(programaCrr.getId(), "PELPC", "Plan con ciclos", null, null);
        cicloServiceCrr.racaCrear(planCrr.getId(), 1, "Ciclo bloqueante", 1);

        assertThatThrownBy(() -> planEstudioServiceCrr.racaEliminar(planCrr.getId()))
                .isInstanceOf(RyeValidacionException.class);
    }

    @Test
    void eliminarCicloConCursosAsignadosFalla() {
        RacaProgramaEstudio programaCrr = programaEstudioServiceCrr.racaCrear("ELCC", "Programa ciclo con cursos", null, 6);
        RacaPlanEstudio planCrr = planEstudioServiceCrr.racaCrear(programaCrr.getId(), "PELCC", "Plan ciclo con cursos", null, null);
        RacaCiclo cicloCrr = cicloServiceCrr.racaCrear(planCrr.getId(), 1, "Ciclo bloqueante", 1);
        RacaCurso cursoCrr = cursoServiceCrr.racaCrear("CELCC", "Curso bloqueante", 3, null, null);
        planCursoServiceCrr.racaAsignar(planCrr.getId(), cicloCrr.getId(), cursoCrr.getId(), 1, true);

        assertThatThrownBy(() -> cicloServiceCrr.racaEliminar(cicloCrr.getId()))
                .isInstanceOf(RyeValidacionException.class);
    }

    @Test
    void reabrirUnPlanCerradoLoDejaActivo() {
        RacaProgramaEstudio programaCrr = programaEstudioServiceCrr.racaCrear("REAB", "Programa reabrir plan", null, 6);
        RacaPlanEstudio planCrr = planEstudioServiceCrr.racaCrear(programaCrr.getId(), "PREAB", "Plan a reabrir", null, null);
        planEstudioServiceCrr.racaCambiarEstado(planCrr.getId(), RacaEstadoPlanEstudio.CERRADO);

        RacaPlanEstudio reabiertoCrr = planEstudioServiceCrr.racaCambiarEstado(planCrr.getId(), RacaEstadoPlanEstudio.ACTIVO);

        assertThat(reabiertoCrr.getEstado()).isEqualTo(RacaEstadoPlanEstudio.ACTIVO);
    }
}
