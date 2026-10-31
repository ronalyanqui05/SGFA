package pe.edu.sgfa.usuario;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;

import pe.edu.sgfa.common.exception.RyeAccesoDenegadoException;
import pe.edu.sgfa.common.exception.RyeValidacionException;
import pe.edu.sgfa.usuario.domain.RyeRolUsuario;
import pe.edu.sgfa.usuario.domain.RyeUsuario;
import pe.edu.sgfa.usuario.service.RyeAdminBootstrapResult;
import pe.edu.sgfa.usuario.service.RyeAuthService;
import pe.edu.sgfa.usuario.service.RyeSesionActual;
import pe.edu.sgfa.usuario.service.RyeUsuarioService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RyeUsuarioServiceTest {

    @BeforeAll
    static void useIsolatedHomeDir() throws IOException {
        Path tempHomeYnq = Files.createTempDirectory("sgfa-usuarios-test-home-");
        System.setProperty("sgfa.home-dir", tempHomeYnq.toString());
    }

    @Autowired
    private RyeUsuarioService usuarioServiceYnq;

    @Autowired
    private RyeAuthService authServiceYnq;

    @Autowired
    private RyeSesionActual sesionActualYnq;

    @Autowired
    private RyeAdminBootstrapResult adminBootstrapResultYnq;

    @Autowired
    private PasswordEncoder passwordEncoderYnq;

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
    void crearUsuarioComoAdminFunciona() {
        RyeUsuario operadorYnq = usuarioServiceYnq.ryeCrear("jperez", "claveSegura123", "Juan Perez", RyeRolUsuario.OPERADOR);

        assertThat(operadorYnq.getId()).isNotNull();
        assertThat(operadorYnq.isActivo()).isTrue();
        assertThat(passwordEncoderYnq.matches("claveSegura123", operadorYnq.getPasswordHash())).isTrue();
    }

    @Test
    void crearUsuarioConUsernameDuplicadoFalla() {
        usuarioServiceYnq.ryeCrear("mgarcia", "claveSegura123", "Maria Garcia", RyeRolUsuario.OPERADOR);

        assertThatThrownBy(() -> usuarioServiceYnq.ryeCrear("mgarcia", "otraClaveSegura", "Otro Nombre", RyeRolUsuario.OPERADOR))
                .isInstanceOf(RyeValidacionException.class);
    }

    @Test
    void crearUsuarioConPasswordCortaFalla() {
        assertThatThrownBy(() -> usuarioServiceYnq.ryeCrear("nuevo", "corta", "Nombre", RyeRolUsuario.OPERADOR))
                .isInstanceOf(RyeValidacionException.class);
    }

    @Test
    void operadorNoPuedeGestionarUsuarios() {
        RyeUsuario operadorYnq = usuarioServiceYnq.ryeCrear("operador1", "claveSegura123", "Operador Uno", RyeRolUsuario.OPERADOR);
        authServiceYnq.ryeCerrarSesion();
        authServiceYnq.ryeAutenticar("operador1", "claveSegura123");

        assertThatThrownBy(() -> usuarioServiceYnq.ryeCrear("otro", "claveSegura123", "Otro", RyeRolUsuario.OPERADOR))
                .isInstanceOf(RyeAccesoDenegadoException.class);

        assertThat(operadorYnq.getRol()).isEqualTo(RyeRolUsuario.OPERADOR);
    }

    @Test
    void activarDesactivarYCambiarPasswordFuncionan() {
        RyeUsuario operadorYnq = usuarioServiceYnq.ryeCrear("carlos", "claveSegura123", "Carlos Ruiz", RyeRolUsuario.OPERADOR);

        usuarioServiceYnq.ryeActivarDesactivar(operadorYnq.getId(), false);
        assertThat(usuarioServiceYnq.ryeListar())
                .filteredOn(u -> u.getId().equals(operadorYnq.getId()))
                .allSatisfy(u -> assertThat(u.isActivo()).isFalse());

        usuarioServiceYnq.ryeCambiarPassword(operadorYnq.getId(), "claveNuevaSegura456");
        RyeUsuario actualizadoYnq = usuarioServiceYnq.ryeListar().stream()
                .filter(u -> u.getId().equals(operadorYnq.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(passwordEncoderYnq.matches("claveNuevaSegura456", actualizadoYnq.getPasswordHash())).isTrue();
    }
}
