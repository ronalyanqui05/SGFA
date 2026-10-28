package pe.edu.sgfa.programa.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.sgfa.auditoria.service.AuditoriaService;
import pe.edu.sgfa.common.exception.EntidadNoEncontradaException;
import pe.edu.sgfa.common.exception.ValidacionException;
import pe.edu.sgfa.programa.domain.Ciclo;
import pe.edu.sgfa.programa.domain.PlanEstudio;
import pe.edu.sgfa.programa.repository.CicloRepository;
import pe.edu.sgfa.programa.repository.PlanCursoRepository;
import pe.edu.sgfa.programa.repository.PlanEstudioRepository;
import pe.edu.sgfa.usuario.domain.RolUsuario;
import pe.edu.sgfa.usuario.service.SesionActual;

@Service
public class CicloService {

    private final CicloRepository cicloRepository;
    private final PlanEstudioRepository planEstudioRepository;
    private final PlanCursoRepository planCursoRepository;
    private final SesionActual sesionActual;
    private final AuditoriaService auditoriaService;

    public CicloService(CicloRepository cicloRepository, PlanEstudioRepository planEstudioRepository,
                         PlanCursoRepository planCursoRepository, SesionActual sesionActual,
                         AuditoriaService auditoriaService) {
        this.cicloRepository = cicloRepository;
        this.planEstudioRepository = planEstudioRepository;
        this.planCursoRepository = planCursoRepository;
        this.sesionActual = sesionActual;
        this.auditoriaService = auditoriaService;
    }

    public List<Ciclo> listarPorPlan(Long plnEstdId) {
        sesionActual.requerirRol(RolUsuario.ADMIN);
        return cicloRepository.findByPlanEstudioIdOrderByOrdenAsc(plnEstdId);
    }

    @Transactional
    public Ciclo crear(Long plnEstdId, int nmr, String nmbr, int ordn) {
        sesionActual.requerirRol(RolUsuario.ADMIN);
        PlanEstudio pln = planEstudioRepository.findById(plnEstdId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Plan de estudio no encontrado: id=" + plnEstdId));
        validarDatos(nmr, nmbr);
        if (cicloRepository.existsByPlanEstudioIdAndNumero(plnEstdId, nmr)) {
            throw new ValidacionException("Ya existe el ciclo numero " + nmr + " en este plan.");
        }

        Ciclo ccl = new Ciclo(pln, nmr, nmbr, ordn);
        cicloRepository.save(ccl);
        registrarAuditoria("CREAR_CICLO", ccl, "numero=" + nmr);
        return ccl;
    }

    @Transactional
    public Ciclo editar(Long id, int nmr, String nmbr, int ordn) {
        sesionActual.requerirRol(RolUsuario.ADMIN);
        validarDatos(nmr, nmbr);
        Ciclo ccl = obtenerOLanzar(id);
        if (cicloRepository.existsByPlanEstudioIdAndNumeroAndIdNot(ccl.getPlanEstudio().getId(), nmr, id)) {
            throw new ValidacionException("Ya existe el ciclo numero " + nmr + " en este plan.");
        }

        ccl.setNumero(nmr);
        ccl.setNombre(nmbr);
        ccl.setOrden(ordn);
        registrarAuditoria("EDITAR_CICLO", ccl, "numero=" + nmr);
        return ccl;
    }

    @Transactional
    public void eliminar(Long id) {
        sesionActual.requerirRol(RolUsuario.ADMIN);
        Ciclo ccl = obtenerOLanzar(id);
        if (!planCursoRepository.findByCicloIdOrderByOrdenAsc(id).isEmpty()) {
            throw new ValidacionException(
                    "No se puede eliminar: el ciclo tiene cursos asignados. Quitelos primero en \"Cursos por ciclo\".");
        }
        registrarAuditoria("ELIMINAR_CICLO", ccl, "numero=" + ccl.getNumero());
        cicloRepository.delete(ccl);
    }

    private Ciclo obtenerOLanzar(Long id) {
        return cicloRepository.findById(id)
                .orElseThrow(() -> new EntidadNoEncontradaException("Ciclo no encontrado: id=" + id));
    }

    private void validarDatos(int nmr, String nmbr) {
        if (nmr <= 0) {
            throw new ValidacionException("El numero de ciclo debe ser mayor que cero.");
        }
        if (nmbr == null || nmbr.isBlank()) {
            throw new ValidacionException("El nombre del ciclo es obligatorio.");
        }
    }

    private void registrarAuditoria(String accn, Ciclo ccl, String dtll) {
        var actr = sesionActual.obtenerUsuarioActual().orElse(null);
        auditoriaService.registrar(actr, accn, "ciclos", ccl.getId().toString(), dtll);
    }
}
