package pe.edu.sgfa.usuario.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.sgfa.auditoria.service.RahpAuditoriaService;
import pe.edu.sgfa.common.exception.RyeEntidadNoEncontradaException;
import pe.edu.sgfa.common.exception.RyeValidacionException;
import pe.edu.sgfa.usuario.domain.RyeRolUsuario;
import pe.edu.sgfa.usuario.domain.RyeUsuario;
import pe.edu.sgfa.usuario.repository.RyeUsuarioRepository;

@Service
public class RyeUsuarioService {

    private static final int LONGITUD_MINIMA_PASSWORD_YNQ = 8;

    private final RyeUsuarioRepository usuarioRepositoryYnq;
    private final PasswordEncoder passwordEncoderYnq;
    private final RyeSesionActual sesionActualYnq;
    private final RahpAuditoriaService auditoriaServiceYnq;

    public RyeUsuarioService(RyeUsuarioRepository usuarioRepositoryYnq, PasswordEncoder passwordEncoderYnq,
                           RyeSesionActual sesionActualYnq, RahpAuditoriaService auditoriaServiceYnq) {
        this.usuarioRepositoryYnq = usuarioRepositoryYnq;
        this.passwordEncoderYnq = passwordEncoderYnq;
        this.sesionActualYnq = sesionActualYnq;
        this.auditoriaServiceYnq = auditoriaServiceYnq;
    }

    public List<RyeUsuario> ryeListar() {
        sesionActualYnq.ryeRequerirRol(RyeRolUsuario.ADMIN);
        return usuarioRepositoryYnq.findAll();
    }

    @Transactional
    public RyeUsuario ryeCrear(String usernameYnq, String passwordPlanoYnq, String nombresYnq, RyeRolUsuario rolYnq) {
        sesionActualYnq.ryeRequerirRol(RyeRolUsuario.ADMIN);
        ryeValidarUsername(usernameYnq);
        ryeValidarPassword(passwordPlanoYnq);
        if (usuarioRepositoryYnq.existsByUsername(usernameYnq)) {
            throw new RyeValidacionException("Ya existe un usuario con el nombre '" + usernameYnq + "'.");
        }

        RyeUsuario usuarioYnq = new RyeUsuario(usernameYnq, passwordEncoderYnq.encode(passwordPlanoYnq), nombresYnq, rolYnq);
        usuarioRepositoryYnq.save(usuarioYnq);
        ryeRegistrarAuditoria("CREAR_USUARIO", usuarioYnq, "username=" + usernameYnq + ", rol=" + rolYnq);
        return usuarioYnq;
    }

    @Transactional
    public RyeUsuario ryeEditar(Long id, String nombresYnq, RyeRolUsuario rolYnq) {
        sesionActualYnq.ryeRequerirRol(RyeRolUsuario.ADMIN);
        RyeUsuario usuarioYnq = ryeObtenerOLanzar(id);
        usuarioYnq.setNombres(nombresYnq);
        usuarioYnq.setRol(rolYnq);
        ryeRegistrarAuditoria("EDITAR_USUARIO", usuarioYnq, "nombres=" + nombresYnq + ", rol=" + rolYnq);
        return usuarioYnq;
    }

    @Transactional
    public RyeUsuario ryeActivarDesactivar(Long id, boolean activoYnq) {
        sesionActualYnq.ryeRequerirRol(RyeRolUsuario.ADMIN);
        RyeUsuario usuarioYnq = ryeObtenerOLanzar(id);
        usuarioYnq.setActivo(activoYnq);
        ryeRegistrarAuditoria(activoYnq ? "ACTIVAR_USUARIO" : "DESACTIVAR_USUARIO", usuarioYnq, null);
        return usuarioYnq;
    }

    @Transactional
    public void ryeCambiarPassword(Long id, String nuevaPasswordPlanoYnq) {
        sesionActualYnq.ryeRequerirRol(RyeRolUsuario.ADMIN);
        ryeValidarPassword(nuevaPasswordPlanoYnq);
        RyeUsuario usuarioYnq = ryeObtenerOLanzar(id);
        usuarioYnq.setPasswordHash(passwordEncoderYnq.encode(nuevaPasswordPlanoYnq));
        ryeRegistrarAuditoria("CAMBIAR_PASSWORD", usuarioYnq, null);
    }

    private RyeUsuario ryeObtenerOLanzar(Long id) {
        return usuarioRepositoryYnq.findById(id)
                .orElseThrow(() -> new RyeEntidadNoEncontradaException("Usuario no encontrado: id=" + id));
    }

    private void ryeValidarUsername(String usernameYnq) {
        if (usernameYnq == null || usernameYnq.isBlank()) {
            throw new RyeValidacionException("El nombre de usuario es obligatorio.");
        }
    }

    private void ryeValidarPassword(String passwordYnq) {
        if (passwordYnq == null || passwordYnq.length() < LONGITUD_MINIMA_PASSWORD_YNQ) {
            throw new RyeValidacionException("La contrasena debe tener al menos " + LONGITUD_MINIMA_PASSWORD_YNQ + " caracteres.");
        }
    }

    private void ryeRegistrarAuditoria(String accionYnq, RyeUsuario usuarioAfectadoYnq, String detalleYnq) {
        RyeUsuario actorYnq = sesionActualYnq.ryeObtenerUsuarioActual().orElse(null);
        String detalleCompletoYnq = "usuario_afectado_id=" + usuarioAfectadoYnq.getId()
                + (detalleYnq != null ? ", " + detalleYnq : "");
        auditoriaServiceYnq.rahpRegistrar(actorYnq, accionYnq, "usuarios", usuarioAfectadoYnq.getId().toString(), detalleCompletoYnq);
    }
}
