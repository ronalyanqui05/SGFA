package pe.edu.sgfa.usuario;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import pe.edu.sgfa.usuario.domain.RyeRolUsuario;
import pe.edu.sgfa.usuario.service.RyeAdminBootstrapResult;
import pe.edu.sgfa.usuario.service.RyeAuthService;
import pe.edu.sgfa.usuario.service.RyeCredencialesInvalidasException;
import pe.edu.sgfa.usuario.service.RyeSesionActual;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RyeAuthServiceTest {

    @BeforeAll
    static void useIsolatedHomeDir() throws IOException {
        Path tempHomeYnq = Files.createTempDirectory("sgfa-auth-test-home-");
        System.setProperty("sgfa.home-dir", tempHomeYnq.toString());
    }

    @Autowired
    private RyeAuthService authServiceYnq;

    @Autowired
    private RyeSesionActual sesionActualYnq;

    @Autowired
    private RyeAdminBootstrapResult adminBootstrapResultYnq;

    @Test
    void loginConCredencialesDelAdminInicialFunciona() {
        RyeAdminBootstrapResult.RyeCredenciales credencialesYnq = adminBootstrapResultYnq.ryeConsumir()
                .orElseThrow(() -> new IllegalStateException("El admin inicial no se genero"));

        authServiceYnq.ryeAutenticar(credencialesYnq.username(), credencialesYnq.password());

        assertThat(sesionActualYnq.ryeEstaAutenticado()).isTrue();
        assertThat(sesionActualYnq.ryeObtenerUsuarioActual()).hasValueSatisfying(usuarioYnq -> {
            assertThat(usuarioYnq.getUsername()).isEqualTo("admin");
            assertThat(usuarioYnq.getRol()).isEqualTo(RyeRolUsuario.ADMIN);
        });

        authServiceYnq.ryeCerrarSesion();
        assertThat(sesionActualYnq.ryeEstaAutenticado()).isFalse();
    }

    @Test
    void loginConPasswordIncorrectaFalla() {
        assertThatThrownBy(() -> authServiceYnq.ryeAutenticar("admin", "password-incorrecta-segura"))
                .isInstanceOf(RyeCredencialesInvalidasException.class);
        assertThat(sesionActualYnq.ryeEstaAutenticado()).isFalse();
    }

    @Test
    void loginConUsuarioInexistenteFalla() {
        assertThatThrownBy(() -> authServiceYnq.ryeAutenticar("no-existe", "cualquier-cosa"))
                .isInstanceOf(RyeCredencialesInvalidasException.class);
    }
}
