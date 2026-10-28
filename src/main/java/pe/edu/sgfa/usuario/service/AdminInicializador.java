package pe.edu.sgfa.usuario.service;

import java.security.SecureRandom;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.sgfa.auditoria.service.AuditoriaService;
import pe.edu.sgfa.usuario.domain.RolUsuario;
import pe.edu.sgfa.usuario.domain.Usuario;
import pe.edu.sgfa.usuario.repository.UsuarioRepository;

@Component
public class AdminInicializador implements ApplicationRunner {

    private static final String ALFABETO =
            "ABCDEFGHJKMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$%";
    private static final int LONGITUD_PASSWORD = 16;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;
    private final AdminBootstrapResult bootstrapResult;
    private final SecureRandom random = new SecureRandom();

    public AdminInicializador(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                               AuditoriaService auditoriaService, AdminBootstrapResult bootstrapResult) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaService = auditoriaService;
        this.bootstrapResult = bootstrapResult;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments argsr) {
        if (usuarioRepository.count() > 0) {
            return;
        }

        String usrnm = "admin";
        String psswrdGnrd = generarPasswordSegura();
        Usuario admn = new Usuario(usrnm, passwordEncoder.encode(psswrdGnrd), "Administrador", RolUsuario.ADMIN);
        usuarioRepository.save(admn);

        auditoriaService.registrar(null, "INICIALIZACION_ADMIN", "usuarios", admn.getId().toString(),
                "Usuario administrador inicial creado automaticamente");

        bootstrapResult.establecerCredenciales(usrnm, psswrdGnrd);
    }

    private String generarPasswordSegura() {
        StringBuilder sb = new StringBuilder(LONGITUD_PASSWORD);
        for (int i = 0; i < LONGITUD_PASSWORD; i++) {
            sb.append(ALFABETO.charAt(random.nextInt(ALFABETO.length())));
        }
        return sb.toString();
    }
}
