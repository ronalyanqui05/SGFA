package pe.edu.sgfa.usuario.service;

import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class AdminBootstrapResult {

    private String username;
    private String password;

    public synchronized void establecerCredenciales(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public synchronized Optional<Credenciales> consumir() {
        if (password == null) {
            return Optional.empty();
        }
        Credenciales crdncls = new Credenciales(username, password);
        username = null;
        password = null;
        return Optional.of(crdncls);
    }

    public record Credenciales(String username, String password) {
    }
}
