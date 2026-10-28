package pe.edu.sgfa.usuario.service;

import pe.edu.sgfa.common.exception.SgfaException;

public class UsuarioInactivoException extends SgfaException {

    public UsuarioInactivoException() {
        super("Este usuario esta inactivo. Contacte al administrador.");
    }
}
