package pe.edu.sgfa.usuario.service;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.sgfa.auditoria.service.AuditoriaService;
import pe.edu.sgfa.usuario.domain.Usuario;
import pe.edu.sgfa.usuario.repository.UsuarioRepository;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final SesionActual sesionActual;
    private final AuditoriaService auditoriaService;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                        SesionActual sesionActual, AuditoriaService auditoriaService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.sesionActual = sesionActual;
        this.auditoriaService = auditoriaService;
    }

    @Transactional
    public Usuario autenticar(String usrnm, String rawPsswrd) {
        Optional<Usuario> usrOpt = usuarioRepository.findByUsername(usrnm);
        if (usrOpt.isEmpty() || !passwordEncoder.matches(rawPsswrd, usrOpt.get().getPasswordHash())) {
            auditoriaService.registrar(null, "LOGIN_FALLIDO", "usuarios", usrnm, "Credenciales invalidas");
            throw new CredencialesInvalidasException();
        }

        Usuario usuario = usrOpt.get();
        if (!usuario.isActivo()) {
            auditoriaService.registrar(usuario, "LOGIN_FALLIDO", "usuarios", usrnm, "Usuario inactivo");
            throw new UsuarioInactivoException();
        }

        sesionActual.iniciar(usuario);
        auditoriaService.registrar(usuario, "LOGIN", "usuarios", usuario.getId().toString(), null);
        return usuario;
    }

    public void cerrarSesion() {
        sesionActual.obtenerUsuarioActual().ifPresent(usuario ->
                auditoriaService.registrar(usuario, "LOGOUT", "usuarios", usuario.getId().toString(), null));
        sesionActual.cerrar();
    }
}
