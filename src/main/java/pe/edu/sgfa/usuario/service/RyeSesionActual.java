package pe.edu.sgfa.usuario.service;

import java.util.Optional;

import org.springframework.stereotype.Component;

import pe.edu.sgfa.common.exception.RyeAccesoDenegadoException;
import pe.edu.sgfa.usuario.domain.RyeRolUsuario;
import pe.edu.sgfa.usuario.domain.RyeUsuario;

@Component
public class RyeSesionActual {

    private RyeUsuario usuarioActualYnq;

    public void ryeIniciar(RyeUsuario usuarioYnq) {
        this.usuarioActualYnq = usuarioYnq;
    }

    public void ryeCerrar() {
        this.usuarioActualYnq = null;
    }

    public Optional<RyeUsuario> ryeObtenerUsuarioActual() {
        return Optional.ofNullable(usuarioActualYnq);
    }

    public boolean ryeEstaAutenticado() {
        return usuarioActualYnq != null;
    }

    public void ryeRequerirRol(RyeRolUsuario rolYnq) {
        if (usuarioActualYnq == null || usuarioActualYnq.getRol() != rolYnq) {
            throw new RyeAccesoDenegadoException("Esta accion requiere el rol " + rolYnq + ".");
        }
    }

    public void ryeRequerirAutenticacion() {
        if (usuarioActualYnq == null) {
            throw new RyeAccesoDenegadoException("Debe iniciar sesion para realizar esta accion.");
        }
    }
}
