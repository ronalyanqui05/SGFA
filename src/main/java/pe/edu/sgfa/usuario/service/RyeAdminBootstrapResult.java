package pe.edu.sgfa.usuario.service;

import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class RyeAdminBootstrapResult {

    private String usernameYnq;
    private String passwordYnq;

    public synchronized void ryeEstablecerCredenciales(String usernameYnq, String passwordYnq) {
        this.usernameYnq = usernameYnq;
        this.passwordYnq = passwordYnq;
    }

    public synchronized Optional<RyeCredenciales> ryeConsumir() {
        if (passwordYnq == null) {
            return Optional.empty();
        }
        RyeCredenciales credencialesYnq = new RyeCredenciales(usernameYnq, passwordYnq);
        usernameYnq = null;
        passwordYnq = null;
        return Optional.of(credencialesYnq);
    }

    public record RyeCredenciales(String username, String password) {
    }
}
