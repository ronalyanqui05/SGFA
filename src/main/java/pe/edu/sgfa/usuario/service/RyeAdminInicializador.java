package pe.edu.sgfa.usuario.service;

import java.security.SecureRandom;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.sgfa.auditoria.service.RahpAuditoriaService;
import pe.edu.sgfa.usuario.domain.RyeRolUsuario;
import pe.edu.sgfa.usuario.domain.RyeUsuario;
import pe.edu.sgfa.usuario.repository.RyeUsuarioRepository;

@Component
public class RyeAdminInicializador implements ApplicationRunner {

    private static final String ALFABETO_YNQ =
            "ABCDEFGHJKMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$%";
    private static final int LONGITUD_PASSWORD_YNQ = 16;

    private final RyeUsuarioRepository usuarioRepositoryYnq;
    private final PasswordEncoder passwordEncoderYnq;
    private final RahpAuditoriaService auditoriaServiceYnq;
    private final RyeAdminBootstrapResult bootstrapResultYnq;
    private final SecureRandom randomYnq = new SecureRandom();

    public RyeAdminInicializador(RyeUsuarioRepository usuarioRepositoryYnq, PasswordEncoder passwordEncoderYnq,
                               RahpAuditoriaService auditoriaServiceYnq, RyeAdminBootstrapResult bootstrapResultYnq) {
        this.usuarioRepositoryYnq = usuarioRepositoryYnq;
        this.passwordEncoderYnq = passwordEncoderYnq;
        this.auditoriaServiceYnq = auditoriaServiceYnq;
        this.bootstrapResultYnq = bootstrapResultYnq;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments argsYnq) {
        if (usuarioRepositoryYnq.count() > 0) {
            return;
        }

        String usernameYnq = "admin";
        String passwordGeneradaYnq = ryeGenerarPasswordSegura();
        RyeUsuario adminYnq = new RyeUsuario(usernameYnq, passwordEncoderYnq.encode(passwordGeneradaYnq), "Administrador", RyeRolUsuario.ADMIN);
        usuarioRepositoryYnq.save(adminYnq);

        auditoriaServiceYnq.rahpRegistrar(null, "INICIALIZACION_ADMIN", "usuarios", adminYnq.getId().toString(),
                "Usuario administrador inicial creado automaticamente");

        bootstrapResultYnq.ryeEstablecerCredenciales(usernameYnq, passwordGeneradaYnq);
    }

    private String ryeGenerarPasswordSegura() {
        StringBuilder sb = new StringBuilder(LONGITUD_PASSWORD_YNQ);
        for (int i = 0; i < LONGITUD_PASSWORD_YNQ; i++) {
            sb.append(ALFABETO_YNQ.charAt(randomYnq.nextInt(ALFABETO_YNQ.length())));
        }
        return sb.toString();
    }
}
