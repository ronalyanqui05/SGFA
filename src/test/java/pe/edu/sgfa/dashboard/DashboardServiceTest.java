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

import pe.edu.sgfa.common.exception.AccesoDenegadoException;
import pe.edu.sgfa.dashboard.service.DashboardService;
import pe.edu.sgfa.dashboard.service.ResumenDashboard;
import pe.edu.sgfa.estudiante.domain.EstadoEstudiante;
import pe.edu.sgfa.estudiante.domain.Estudiante;
import pe.edu.sgfa.estudiante.service.EstudianteService;
import pe.edu.sgfa.institucion.service.InstitucionService;
import pe.edu.sgfa.periodo.service.PeriodoAcademicoService;
import pe.edu.sgfa.programa.service.PlanEstudioService;
import pe.edu.sgfa.programa.service.ProgramaEstudioService;
import pe.edu.sgfa.usuario.service.AdminBootstrapResult;
import pe.edu.sgfa.usuario.service.AuthService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DashboardServiceTest {

    @BeforeAll
    static void useIsolatedHomeDir() throws IOException {
        Path tmpHm = Files.createTempDirectory("sgfa-dashboard-test-home-");
        System.setProperty("sgfa.home-dir", tmpHm.toString());
    }

    @Autowired
    private InstitucionService institucionService;
    @Autowired
    private ProgramaEstudioService programaEstudioService;
    @Autowired
    private PlanEstudioService planEstudioService;
    @Autowired
    private PeriodoAcademicoService periodoAcademicoService;
    @Autowired
    private EstudianteService estudianteService;
    @Autowired
    private DashboardService dashboardService;
    @Autowired
    private AuthService authService;
    @Autowired
    private AdminBootstrapResult adminBootstrapResult;

    private static AdminBootstrapResult.Credenciales credencialesAdmin;

    @BeforeEach
    void loginComoAdmin() {
        if (credencialesAdmin == null) {
            credencialesAdmin = adminBootstrapResult.consumir()
                    .orElseThrow(() -> new IllegalStateException("El admin inicial no se genero"));
        }
        authService.cerrarSesion();
        authService.autenticar(credencialesAdmin.username(), credencialesAdmin.password());
    }

    @Test
    void resumenCuentaEstudiantesProgramasPlanesYPeriodoActivo() {
        if (institucionService.obtenerPrincipal().isEmpty()) {
            institucionService.guardar("IESPP Juliaca", "JULI", null, null, null);
        }
        var programa = programaEstudioService.crear("DAS" + System.nanoTime(), "Programa dashboard", null, 6);
        planEstudioService.crear(programa.getId(), "PD" + System.nanoTime(), "Plan dashboard", null, null);
        periodoAcademicoService.crear("DASH" + System.nanoTime(), 2026, "I", null, null);

        estudianteService.crear(null, null, "Ana", "Vega" + System.nanoTime(),
                null, null, null, null, null, null);
        Estudiante inctv = estudianteService.crear(null, null, "Luis", "Mamani" + System.nanoTime(),
                null, null, null, null, null, null);
        estudianteService.cambiarEstado(inctv.getId(), EstadoEstudiante.INACTIVO);

        ResumenDashboard rsmnAnts = dashboardService.obtenerResumen();

        assertThat(rsmnAnts.totalEstudiantes()).isGreaterThanOrEqualTo(2);
        assertThat(rsmnAnts.estudiantesActivos()).isGreaterThanOrEqualTo(1);
        assertThat(rsmnAnts.totalProgramas()).isGreaterThanOrEqualTo(1);
        assertThat(rsmnAnts.totalPlanes()).isGreaterThanOrEqualTo(1);
        assertThat(rsmnAnts.periodoActualNombre()).isNotNull();
    }

    @Test
    void obtenerResumenSinSesionFalla() {
        authService.cerrarSesion();

        assertThatThrownBy(() -> dashboardService.obtenerResumen())
                .isInstanceOf(AccesoDenegadoException.class);
    }
}
