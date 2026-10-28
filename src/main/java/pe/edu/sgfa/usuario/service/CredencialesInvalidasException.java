package pe.edu.sgfa.usuario.service;

import pe.edu.sgfa.common.exception.SgfaException;

public class CredencialesInvalidasException extends SgfaException {

    public CredencialesInvalidasException() {
        super("Usuario o contrasena incorrectos.");
    }
}
