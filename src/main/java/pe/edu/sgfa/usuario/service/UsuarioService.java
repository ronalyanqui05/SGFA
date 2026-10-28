package pe.edu.sgfa.usuario.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.sgfa.auditoria.service.AuditoriaService;
import pe.edu.sgfa.common.exception.EntidadNoEncontradaException;
import pe.edu.sgfa.common.exception.ValidacionException;
import pe.edu.sgfa.usuario.domain.RolUsuario;
import pe.edu.sgfa.usuario.domain.Usuario;
import pe.edu.sgfa.usuario.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private static final int LONGITUD_MINIMA_PASSWORD = 8;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final SesionActual sesionActual;
    private final AuditoriaService auditoriaService;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                           SesionActual sesionActual, AuditoriaService auditoriaService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.sesionActual = sesionActual;
        this.auditoriaService = auditoriaService;
    }

    public List<Usuario> listar() {
        sesionActual.requerirRol(RolUsuario.ADMIN);
        return usuarioRepository.findAll();
    }

    @Transactional
    public Usuario crear(String usrnm, String psswrdPln, String nmbrs, RolUsuario rol) {
        sesionActual.requerirRol(RolUsuario.ADMIN);
        validarUsername(usrnm);
        validarPassword(psswrdPln);
        if (usuarioRepository.existsByUsername(usrnm)) {
            throw new ValidacionException("Ya existe un usuario con el nombre '" + usrnm + "'.");
        }

        Usuario usuario = new Usuario(usrnm, passwordEncoder.encode(psswrdPln), nmbrs, rol);
        usuarioRepository.save(usuario);
        registrarAuditoria("CREAR_USUARIO", usuario, "username=" + usrnm + ", rol=" + rol);
        return usuario;
    }

    @Transactional
    public Usuario editar(Long id, String nmbrs, RolUsuario rol) {
        sesionActual.requerirRol(RolUsuario.ADMIN);
        Usuario usuario = obtenerOLanzar(id);
        usuario.setNombres(nmbrs);
        usuario.setRol(rol);
        registrarAuditoria("EDITAR_USUARIO", usuario, "nombres=" + nmbrs + ", rol=" + rol);
        return usuario;
    }

    @Transactional
    public Usuario activarDesactivar(Long id, boolean actv) {
        sesionActual.requerirRol(RolUsuario.ADMIN);
        Usuario usuario = obtenerOLanzar(id);
        usuario.setActivo(actv);
        registrarAuditoria(actv ? "ACTIVAR_USUARIO" : "DESACTIVAR_USUARIO", usuario, null);
        return usuario;
    }

    @Transactional
    public void cambiarPassword(Long id, String nvPsswrdPln) {
        sesionActual.requerirRol(RolUsuario.ADMIN);
        validarPassword(nvPsswrdPln);
        Usuario usuario = obtenerOLanzar(id);
        usuario.setPasswordHash(passwordEncoder.encode(nvPsswrdPln));
        registrarAuditoria("CAMBIAR_PASSWORD", usuario, null);
    }

    private Usuario obtenerOLanzar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new EntidadNoEncontradaException("Usuario no encontrado: id=" + id));
    }

    private void validarUsername(String usrnm) {
        if (usrnm == null || usrnm.isBlank()) {
            throw new ValidacionException("El nombre de usuario es obligatorio.");
        }
    }

    private void validarPassword(String password) {
        if (password == null || password.length() < LONGITUD_MINIMA_PASSWORD) {
            throw new ValidacionException("La contrasena debe tener al menos " + LONGITUD_MINIMA_PASSWORD + " caracteres.");
        }
    }

    private void registrarAuditoria(String accn, Usuario usrAfctd, String dtll) {
        Usuario actr = sesionActual.obtenerUsuarioActual().orElse(null);
        String dtllCmplt = "usuario_afectado_id=" + usrAfctd.getId()
                + (dtll != null ? ", " + dtll : "");
        auditoriaService.registrar(actr, accn, "usuarios", usrAfctd.getId().toString(), dtllCmplt);
    }
}
