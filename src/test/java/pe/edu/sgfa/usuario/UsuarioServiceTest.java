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

import pe.edu.sgfa.common.exception.AccesoDenegadoException;
import pe.edu.sgfa.common.exception.ValidacionException;
import pe.edu.sgfa.usuario.domain.RolUsuario;
import pe.edu.sgfa.usuario.domain.Usuario;
import pe.edu.sgfa.usuario.service.AdminBootstrapResult;
import pe.edu.sgfa.usuario.service.AuthService;
import pe.edu.sgfa.usuario.service.SesionActual;
import pe.edu.sgfa.usuario.service.UsuarioService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UsuarioServiceTest {

    @BeforeAll
    static void useIsolatedHomeDir() throws IOException {
        Path tmpHm = Files.createTempDirectory("sgfa-usuarios-test-home-");
        System.setProperty("sgfa.home-dir", tmpHm.toString());
    }

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private AuthService authService;

    @Autowired
    private SesionActual sesionActual;

    @Autowired
    private AdminBootstrapResult adminBootstrapResult;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
    void crearUsuarioComoAdminFunciona() {
        Usuario oprdr = usuarioService.crear("jperez", "claveSegura123", "Juan Perez", RolUsuario.OPERADOR);

        assertThat(oprdr.getId()).isNotNull();
        assertThat(oprdr.isActivo()).isTrue();
        assertThat(passwordEncoder.matches("claveSegura123", oprdr.getPasswordHash())).isTrue();
    }

    @Test
    void crearUsuarioConUsernameDuplicadoFalla() {
        usuarioService.crear("mgarcia", "claveSegura123", "Maria Garcia", RolUsuario.OPERADOR);

        assertThatThrownBy(() -> usuarioService.crear("mgarcia", "otraClaveSegura", "Otro Nombre", RolUsuario.OPERADOR))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void crearUsuarioConPasswordCortaFalla() {
        assertThatThrownBy(() -> usuarioService.crear("nuevo", "corta", "Nombre", RolUsuario.OPERADOR))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void operadorNoPuedeGestionarUsuarios() {
        Usuario oprdr = usuarioService.crear("operador1", "claveSegura123", "Operador Uno", RolUsuario.OPERADOR);
        authService.cerrarSesion();
        authService.autenticar("operador1", "claveSegura123");

        assertThatThrownBy(() -> usuarioService.crear("otro", "claveSegura123", "Otro", RolUsuario.OPERADOR))
                .isInstanceOf(AccesoDenegadoException.class);

        assertThat(oprdr.getRol()).isEqualTo(RolUsuario.OPERADOR);
    }

    @Test
    void activarDesactivarYCambiarPasswordFuncionan() {
        Usuario oprdr = usuarioService.crear("carlos", "claveSegura123", "Carlos Ruiz", RolUsuario.OPERADOR);

        usuarioService.activarDesactivar(oprdr.getId(), false);
        assertThat(usuarioService.listar())
                .filteredOn(u -> u.getId().equals(oprdr.getId()))
                .allSatisfy(u -> assertThat(u.isActivo()).isFalse());

        usuarioService.cambiarPassword(oprdr.getId(), "claveNuevaSegura456");
        Usuario actlzd = usuarioService.listar().stream()
                .filter(u -> u.getId().equals(oprdr.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(passwordEncoder.matches("claveNuevaSegura456", actlzd.getPasswordHash())).isTrue();
    }
}
