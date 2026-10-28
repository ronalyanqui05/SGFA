package pe.edu.sgfa.periodo;

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
import pe.edu.sgfa.periodo.domain.EstadoPeriodoAcademico;
import pe.edu.sgfa.periodo.domain.PeriodoAcademico;
import pe.edu.sgfa.periodo.service.PeriodoAcademicoService;
import pe.edu.sgfa.usuario.service.AdminBootstrapResult;
import pe.edu.sgfa.usuario.service.AuthService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PeriodoAcademicoServiceTest {

    @BeforeAll
    static void useIsolatedHomeDir() throws IOException {
        Path tmpHm = Files.createTempDirectory("sgfa-periodo-test-home-");
        System.setProperty("sgfa.home-dir", tmpHm.toString());
    }

    @Autowired
    private PeriodoAcademicoService periodoAcademicoService;
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
    void crearYEditarPeriodoFunciona() {
        PeriodoAcademico periodo = periodoAcademicoService.crear(
                "2026-I", 2026, "I", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 7, 15));
        assertThat(periodo.getEstado()).isEqualTo(EstadoPeriodoAcademico.ACTIVO);

        PeriodoAcademico edtd = periodoAcademicoService.editar(
                periodo.getId(), "2026-I", 2026, "I", LocalDate.of(2026, 3, 5), LocalDate.of(2026, 7, 20));
        assertThat(edtd.getFechaInicio()).isEqualTo(LocalDate.of(2026, 3, 5));
    }

    @Test
    void crearPeriodoConNombreDuplicadoFalla() {
        periodoAcademicoService.crear("2026-II", 2026, "II", null, null);

        assertThatThrownBy(() -> periodoAcademicoService.crear("2026-II", 2026, "II", null, null))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void cambiarEstadoACerradoFunciona() {
        PeriodoAcademico periodo = periodoAcademicoService.crear("2025-II", 2025, "II", null, null);

        PeriodoAcademico crrd = periodoAcademicoService.cambiarEstado(periodo.getId(), EstadoPeriodoAcademico.CERRADO);

        assertThat(crrd.getEstado()).isEqualTo(EstadoPeriodoAcademico.CERRADO);
    }

    @Test
    void crearPeriodoSinSesionAdminFalla() {
        authService.cerrarSesion();

        assertThatThrownBy(() -> periodoAcademicoService.crear("2027-I", 2027, "I", null, null))
                .isInstanceOf(AccesoDenegadoException.class);
    }
}
