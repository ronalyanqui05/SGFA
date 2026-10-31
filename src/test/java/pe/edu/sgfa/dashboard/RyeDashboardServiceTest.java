package pe.edu.sgfa.dashboard;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import pe.edu.sgfa.common.exception.RyeAccesoDenegadoException;
import pe.edu.sgfa.dashboard.service.RyeDashboardService;
import pe.edu.sgfa.dashboard.service.RyeResumenDashboard;
import pe.edu.sgfa.estudiante.domain.YmclEstadoEstudiante;
import pe.edu.sgfa.estudiante.domain.YmclEstudiante;
import pe.edu.sgfa.estudiante.service.YmclEstudianteService;
import pe.edu.sgfa.institucion.service.RacaInstitucionService;
import pe.edu.sgfa.periodo.service.YmclPeriodoAcademicoService;
import pe.edu.sgfa.programa.service.RacaPlanEstudioService;
import pe.edu.sgfa.programa.service.RacaProgramaEstudioService;
import pe.edu.sgfa.usuario.service.RyeAdminBootstrapResult;
import pe.edu.sgfa.usuario.service.RyeAuthService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RyeDashboardServiceTest {

    @BeforeAll
    static void useIsolatedHomeDir() throws IOException {
        Path tempHomeYnq = Files.createTempDirectory("sgfa-dashboard-test-home-");
        System.setProperty("sgfa.home-dir", tempHomeYnq.toString());
    }

    @Autowired
    private RacaInstitucionService institucionServiceYnq;
    @Autowired
    private RacaProgramaEstudioService programaEstudioServiceYnq;
    @Autowired
    private RacaPlanEstudioService planEstudioServiceYnq;
    @Autowired
    private YmclPeriodoAcademicoService periodoAcademicoServiceYnq;
    @Autowired
    private YmclEstudianteService estudianteServiceYnq;
    @Autowired
    private RyeDashboardService dashboardServiceYnq;
    @Autowired
    private RyeAuthService authServiceYnq;
    @Autowired
    private RyeAdminBootstrapResult adminBootstrapResultYnq;

    private static RyeAdminBootstrapResult.RyeCredenciales credencialesAdminYnq;

    @BeforeEach
    void loginComoAdmin() {
        if (credencialesAdminYnq == null) {
            credencialesAdminYnq = adminBootstrapResultYnq.ryeConsumir()
                    .orElseThrow(() -> new IllegalStateException("El admin inicial no se genero"));
        }
        authServiceYnq.ryeCerrarSesion();
        authServiceYnq.ryeAutenticar(credencialesAdminYnq.username(), credencialesAdminYnq.password());
    }

    @Test
    void resumenCuentaEstudiantesProgramasPlanesYPeriodoActivo() {
        if (institucionServiceYnq.racaObtenerPrincipal().isEmpty()) {
            institucionServiceYnq.racaGuardar("IESPP Juliaca", "JULI", null, null, null);
        }
        var programaYnq = programaEstudioServiceYnq.racaCrear("DAS" + System.nanoTime(), "Programa dashboard", null, 6);
        planEstudioServiceYnq.racaCrear(programaYnq.getId(), "PD" + System.nanoTime(), "Plan dashboard", null, null);
        periodoAcademicoServiceYnq.ymclCrear("DASH" + System.nanoTime(), 2026, "I", null, null);

        estudianteServiceYnq.ymclCrear(null, null, "Ana", "Vega" + System.nanoTime(),
                null, null, null, null, null, null);
        YmclEstudiante inactivoYnq = estudianteServiceYnq.ymclCrear(null, null, "Luis", "Mamani" + System.nanoTime(),
                null, null, null, null, null, null);
        estudianteServiceYnq.ymclCambiarEstado(inactivoYnq.getId(), YmclEstadoEstudiante.INACTIVO);

        RyeResumenDashboard resumenAntesYnq = dashboardServiceYnq.ryeObtenerResumen();

        assertThat(resumenAntesYnq.totalEstudiantes()).isGreaterThanOrEqualTo(2);
        assertThat(resumenAntesYnq.estudiantesActivos()).isGreaterThanOrEqualTo(1);
        assertThat(resumenAntesYnq.totalProgramas()).isGreaterThanOrEqualTo(1);
        assertThat(resumenAntesYnq.totalPlanes()).isGreaterThanOrEqualTo(1);
        assertThat(resumenAntesYnq.periodoActualNombre()).isNotNull();
    }

    @Test
    void obtenerResumenSinSesionFalla() {
        authServiceYnq.ryeCerrarSesion();

        assertThatThrownBy(() -> dashboardServiceYnq.ryeObtenerResumen())
                .isInstanceOf(RyeAccesoDenegadoException.class);
    }
}
