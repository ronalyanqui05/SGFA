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

import pe.edu.sgfa.common.exception.RyeAccesoDenegadoException;
import pe.edu.sgfa.common.exception.RyeValidacionException;
import pe.edu.sgfa.periodo.domain.YmclEstadoPeriodoAcademico;
import pe.edu.sgfa.periodo.domain.YmclPeriodoAcademico;
import pe.edu.sgfa.periodo.service.YmclPeriodoAcademicoService;
import pe.edu.sgfa.usuario.service.RyeAdminBootstrapResult;
import pe.edu.sgfa.usuario.service.RyeAuthService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class YmclPeriodoAcademicoServiceTest {

    @BeforeAll
    static void useIsolatedHomeDir() throws IOException {
        Path tempHomeChp = Files.createTempDirectory("sgfa-periodo-test-home-");
        System.setProperty("sgfa.home-dir", tempHomeChp.toString());
    }

    @Autowired
    private YmclPeriodoAcademicoService periodoAcademicoServiceChp;
    @Autowired
    private RyeAuthService authServiceChp;
    @Autowired
    private RyeAdminBootstrapResult adminBootstrapResultChp;

    private static RyeAdminBootstrapResult.RyeCredenciales credencialesAdminChp;

    @BeforeEach
    void loginComoAdmin() {
        if (credencialesAdminChp == null) {
            credencialesAdminChp = adminBootstrapResultChp.ryeConsumir()
                    .orElseThrow(() -> new IllegalStateException("El admin inicial no se genero"));
        }
        authServiceChp.ryeCerrarSesion();
        authServiceChp.ryeAutenticar(credencialesAdminChp.username(), credencialesAdminChp.password());
    }

    @Test
    void crearYEditarPeriodoFunciona() {
        YmclPeriodoAcademico periodoChp = periodoAcademicoServiceChp.ymclCrear(
                "2026-I", 2026, "I", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 7, 15));
        assertThat(periodoChp.getEstado()).isEqualTo(YmclEstadoPeriodoAcademico.ACTIVO);

        YmclPeriodoAcademico editadoChp = periodoAcademicoServiceChp.ymclEditar(
                periodoChp.getId(), "2026-I", 2026, "I", LocalDate.of(2026, 3, 5), LocalDate.of(2026, 7, 20));
        assertThat(editadoChp.getFechaInicio()).isEqualTo(LocalDate.of(2026, 3, 5));
    }

    @Test
    void crearPeriodoConNombreDuplicadoFalla() {
        periodoAcademicoServiceChp.ymclCrear("2026-II", 2026, "II", null, null);

        assertThatThrownBy(() -> periodoAcademicoServiceChp.ymclCrear("2026-II", 2026, "II", null, null))
                .isInstanceOf(RyeValidacionException.class);
    }

    @Test
    void cambiarEstadoACerradoFunciona() {
        YmclPeriodoAcademico periodoChp = periodoAcademicoServiceChp.ymclCrear("2025-II", 2025, "II", null, null);

        YmclPeriodoAcademico cerradoChp = periodoAcademicoServiceChp.ymclCambiarEstado(periodoChp.getId(), YmclEstadoPeriodoAcademico.CERRADO);

        assertThat(cerradoChp.getEstado()).isEqualTo(YmclEstadoPeriodoAcademico.CERRADO);
    }

    @Test
    void crearPeriodoSinSesionAdminFalla() {
        authServiceChp.ryeCerrarSesion();

        assertThatThrownBy(() -> periodoAcademicoServiceChp.ymclCrear("2027-I", 2027, "I", null, null))
                .isInstanceOf(RyeAccesoDenegadoException.class);
    }
}
