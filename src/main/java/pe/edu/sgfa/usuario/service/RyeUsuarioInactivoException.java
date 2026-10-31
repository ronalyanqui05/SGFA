package pe.edu.sgfa.usuario.service;

import pe.edu.sgfa.common.exception.RyeSgfaException;

public class RyeUsuarioInactivoException extends RyeSgfaException {

    public RyeUsuarioInactivoException() {
        super("Este usuario esta inactivo. Contacte al administrador.");
    }
}
