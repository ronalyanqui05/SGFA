package pe.edu.sgfa.usuario.service;

import pe.edu.sgfa.common.exception.RyeSgfaException;

public class RyeCredencialesInvalidasException extends RyeSgfaException {

    public RyeCredencialesInvalidasException() {
        super("Usuario o contrasena incorrectos.");
    }
}
