package pe.edu.sgfa.usuario;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import pe.edu.sgfa.usuario.domain.RolUsuario;
import pe.edu.sgfa.usuario.service.AdminBootstrapResult;
import pe.edu.sgfa.usuario.service.AuthService;
import pe.edu.sgfa.usuario.service.CredencialesInvalidasException;
import pe.edu.sgfa.usuario.service.SesionActual;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuthServiceTest {

    @BeforeAll
    static void useIsolatedHomeDir() throws IOException {
        Path tmpHm = Files.createTempDirectory("sgfa-auth-test-home-");
        System.setProperty("sgfa.home-dir", tmpHm.toString());
    }

    @Autowired
    private AuthService authService;

    @Autowired
    private SesionActual sesionActual;

    @Autowired
    private AdminBootstrapResult adminBootstrapResult;

    @Test
    void loginConCredencialesDelAdminInicialFunciona() {
        AdminBootstrapResult.Credenciales crdncls = adminBootstrapResult.consumir()
                .orElseThrow(() -> new IllegalStateException("El admin inicial no se genero"));

        authService.autenticar(crdncls.username(), crdncls.password());

        assertThat(sesionActual.estaAutenticado()).isTrue();
        assertThat(sesionActual.obtenerUsuarioActual()).hasValueSatisfying(usuario -> {
            assertThat(usuario.getUsername()).isEqualTo("admin");
            assertThat(usuario.getRol()).isEqualTo(RolUsuario.ADMIN);
        });

        authService.cerrarSesion();
        assertThat(sesionActual.estaAutenticado()).isFalse();
    }

    @Test
    void loginConPasswordIncorrectaFalla() {
        assertThatThrownBy(() -> authService.autenticar("admin", "password-incorrecta-segura"))
                .isInstanceOf(CredencialesInvalidasException.class);
        assertThat(sesionActual.estaAutenticado()).isFalse();
    }

    @Test
    void loginConUsuarioInexistenteFalla() {
        assertThatThrownBy(() -> authService.autenticar("no-existe", "cualquier-cosa"))
                .isInstanceOf(CredencialesInvalidasException.class);
    }
}
