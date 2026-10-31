package pe.edu.sgfa.auditoria.service;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.sgfa.auditoria.domain.RahpAuditoria;
import pe.edu.sgfa.auditoria.repository.RahpAuditoriaRepository;
import pe.edu.sgfa.usuario.domain.RyeRolUsuario;
import pe.edu.sgfa.usuario.domain.RyeUsuario;
import pe.edu.sgfa.usuario.service.RyeSesionActual;

@Service
public class RahpAuditoriaService {

    private final RahpAuditoriaRepository auditoriaRepositoryHnc;
    private final RyeSesionActual sesionActualHnc;

    public RahpAuditoriaService(RahpAuditoriaRepository auditoriaRepositoryHnc, RyeSesionActual sesionActualHnc) {
        this.auditoriaRepositoryHnc = auditoriaRepositoryHnc;
        this.sesionActualHnc = sesionActualHnc;
    }

    @Transactional
    public void rahpRegistrar(RyeUsuario actorHnc, String accionHnc, String entidadHnc, String entidadIdHnc, String detalleHnc) {
        Long usuarioIdHnc = actorHnc != null ? actorHnc.getId() : null;
        auditoriaRepositoryHnc.save(new RahpAuditoria(usuarioIdHnc, accionHnc, entidadHnc, entidadIdHnc, detalleHnc, Instant.now()));
    }

    public Page<RahpAuditoria> rahpBuscar(Instant desdeHnc, Instant hastaHnc, Long usuarioIdHnc, String accionHnc, String entidadHnc,
                                   int paginaHnc, int tamanoPaginaHnc) {
        sesionActualHnc.ryeRequerirRol(RyeRolUsuario.ADMIN);
        PageRequest pageableHnc = PageRequest.of(paginaHnc, tamanoPaginaHnc, Sort.by(Sort.Direction.DESC, "fecha"));
        return auditoriaRepositoryHnc.buscar(desdeHnc, hastaHnc, usuarioIdHnc, accionHnc, entidadHnc, pageableHnc);
    }

    public List<String> rahpListarAcciones() {
        sesionActualHnc.ryeRequerirRol(RyeRolUsuario.ADMIN);
        return auditoriaRepositoryHnc.listarAcciones();
    }

    public List<String> rahpListarEntidades() {
        sesionActualHnc.ryeRequerirRol(RyeRolUsuario.ADMIN);
        return auditoriaRepositoryHnc.listarEntidades();
    }
}
