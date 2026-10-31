package pe.edu.sgfa.usuario.service;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.sgfa.auditoria.service.RahpAuditoriaService;
import pe.edu.sgfa.usuario.domain.RyeUsuario;
import pe.edu.sgfa.usuario.repository.RyeUsuarioRepository;

@Service
public class RyeAuthService {

    private final RyeUsuarioRepository usuarioRepositoryYnq;
    private final PasswordEncoder passwordEncoderYnq;
    private final RyeSesionActual sesionActualYnq;
    private final RahpAuditoriaService auditoriaServiceYnq;

    public RyeAuthService(RyeUsuarioRepository usuarioRepositoryYnq, PasswordEncoder passwordEncoderYnq,
                        RyeSesionActual sesionActualYnq, RahpAuditoriaService auditoriaServiceYnq) {
        this.usuarioRepositoryYnq = usuarioRepositoryYnq;
        this.passwordEncoderYnq = passwordEncoderYnq;
        this.sesionActualYnq = sesionActualYnq;
        this.auditoriaServiceYnq = auditoriaServiceYnq;
    }

    @Transactional
    public RyeUsuario ryeAutenticar(String usernameYnq, String rawPasswordYnq) {
        Optional<RyeUsuario> usuarioOptYnq = usuarioRepositoryYnq.findByUsername(usernameYnq);
        if (usuarioOptYnq.isEmpty() || !passwordEncoderYnq.matches(rawPasswordYnq, usuarioOptYnq.get().getPasswordHash())) {
            auditoriaServiceYnq.rahpRegistrar(null, "LOGIN_FALLIDO", "usuarios", usernameYnq, "Credenciales invalidas");
            throw new RyeCredencialesInvalidasException();
        }

        RyeUsuario usuarioYnq = usuarioOptYnq.get();
        if (!usuarioYnq.isActivo()) {
            auditoriaServiceYnq.rahpRegistrar(usuarioYnq, "LOGIN_FALLIDO", "usuarios", usernameYnq, "Usuario inactivo");
            throw new RyeUsuarioInactivoException();
        }

        sesionActualYnq.ryeIniciar(usuarioYnq);
        auditoriaServiceYnq.rahpRegistrar(usuarioYnq, "LOGIN", "usuarios", usuarioYnq.getId().toString(), null);
        return usuarioYnq;
    }

    public void ryeCerrarSesion() {
        sesionActualYnq.ryeObtenerUsuarioActual().ifPresent(usuarioYnq ->
                auditoriaServiceYnq.rahpRegistrar(usuarioYnq, "LOGOUT", "usuarios", usuarioYnq.getId().toString(), null));
        sesionActualYnq.ryeCerrar();
    }
}
