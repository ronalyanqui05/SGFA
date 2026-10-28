package pe.edu.sgfa.auditoria.service;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.sgfa.auditoria.domain.Auditoria;
import pe.edu.sgfa.auditoria.repository.AuditoriaRepository;
import pe.edu.sgfa.usuario.domain.RolUsuario;
import pe.edu.sgfa.usuario.domain.Usuario;
import pe.edu.sgfa.usuario.service.SesionActual;

@Service
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;
    private final SesionActual sesionActual;

    public AuditoriaService(AuditoriaRepository auditoriaRepository, SesionActual sesionActual) {
        this.auditoriaRepository = auditoriaRepository;
        this.sesionActual = sesionActual;
    }

    @Transactional
    public void registrar(Usuario actr, String accn, String entdd, String entddId, String dtll) {
        Long usrId = actr != null ? actr.getId() : null;
        auditoriaRepository.save(new Auditoria(usrId, accn, entdd, entddId, dtll, Instant.now()));
    }

    public Page<Auditoria> buscar(Instant dsd, Instant hst, Long usrId, String accn, String entdd,
                                   int pgn, int tmnPgn) {
        sesionActual.requerirRol(RolUsuario.ADMIN);
        PageRequest pgbl = PageRequest.of(pgn, tmnPgn, Sort.by(Sort.Direction.DESC, "fecha"));
        return auditoriaRepository.buscar(dsd, hst, usrId, accn, entdd, pgbl);
    }

    public List<String> listarAcciones() {
        sesionActual.requerirRol(RolUsuario.ADMIN);
        return auditoriaRepository.listarAcciones();
    }

    public List<String> listarEntidades() {
        sesionActual.requerirRol(RolUsuario.ADMIN);
        return auditoriaRepository.listarEntidades();
    }
}
