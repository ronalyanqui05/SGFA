package pe.edu.sgfa.usuario.service;

import java.util.Optional;

import org.springframework.stereotype.Component;

import pe.edu.sgfa.common.exception.AccesoDenegadoException;
import pe.edu.sgfa.usuario.domain.RolUsuario;
import pe.edu.sgfa.usuario.domain.Usuario;

@Component
public class SesionActual {

    private Usuario usuarioActual;

    public void iniciar(Usuario usuario) {
        this.usuarioActual = usuario;
    }

    public void cerrar() {
        this.usuarioActual = null;
    }

    public Optional<Usuario> obtenerUsuarioActual() {
        return Optional.ofNullable(usuarioActual);
    }

    public boolean estaAutenticado() {
        return usuarioActual != null;
    }

    public void requerirRol(RolUsuario rol) {
        if (usuarioActual == null || usuarioActual.getRol() != rol) {
            throw new AccesoDenegadoException("Esta accion requiere el rol " + rol + ".");
        }
    }

    public void requerirAutenticacion() {
        if (usuarioActual == null) {
            throw new AccesoDenegadoException("Debe iniciar sesion para realizar esta accion.");
        }
    }
}
