package pe.edu.sgfa.programa.service;

import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.sgfa.auditoria.service.AuditoriaService;
import pe.edu.sgfa.common.exception.EntidadNoEncontradaException;
import pe.edu.sgfa.common.exception.ValidacionException;
import pe.edu.sgfa.programa.domain.Ciclo;
import pe.edu.sgfa.programa.domain.Curso;
import pe.edu.sgfa.programa.domain.PlanCurso;
import pe.edu.sgfa.programa.domain.PlanEstudio;
import pe.edu.sgfa.programa.repository.CicloRepository;
import pe.edu.sgfa.programa.repository.CursoRepository;
import pe.edu.sgfa.programa.repository.PlanCursoRepository;
import pe.edu.sgfa.programa.repository.PlanEstudioRepository;
import pe.edu.sgfa.usuario.domain.RolUsuario;
import pe.edu.sgfa.usuario.service.SesionActual;

@Service
public class PlanCursoService {

    private final PlanCursoRepository planCursoRepository;
    private final PlanEstudioRepository planEstudioRepository;
    private final CicloRepository cicloRepository;
    private final CursoRepository cursoRepository;
    private final SesionActual sesionActual;
    private final AuditoriaService auditoriaService;

    public PlanCursoService(PlanCursoRepository planCursoRepository, PlanEstudioRepository planEstudioRepository,
                             CicloRepository cicloRepository, CursoRepository cursoRepository,
                             SesionActual sesionActual, AuditoriaService auditoriaService) {
        this.planCursoRepository = planCursoRepository;
        this.planEstudioRepository = planEstudioRepository;
        this.cicloRepository = cicloRepository;
        this.cursoRepository = cursoRepository;
        this.sesionActual = sesionActual;
        this.auditoriaService = auditoriaService;
    }

    public List<PlanCurso> listarPorCiclo(Long cclId) {
        sesionActual.requerirRol(RolUsuario.ADMIN);
        return planCursoRepository.findByCicloIdOrderByOrdenAsc(cclId);
    }

    @Transactional
    public PlanCurso asignar(Long plnEstdId, Long cclId, Long crsId, int ordn, boolean oblgtr) {
        sesionActual.requerirRol(RolUsuario.ADMIN);

        PlanEstudio pln = planEstudioRepository.findById(plnEstdId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Plan de estudio no encontrado: id=" + plnEstdId));
        Ciclo ccl = cicloRepository.findById(cclId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Ciclo no encontrado: id=" + cclId));
        Curso crs = cursoRepository.findById(crsId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Curso no encontrado: id=" + crsId));

        if (!ccl.getPlanEstudio().getId().equals(plnEstdId)) {
            throw new ValidacionException("El ciclo seleccionado no pertenece al plan de estudio indicado.");
        }
        if (planCursoRepository.existsByPlanEstudioIdAndCicloIdAndCursoId(plnEstdId, cclId, crsId)) {
            throw new ValidacionException("Ese curso ya esta asignado a este ciclo.");
        }

        PlanCurso plnCrs = new PlanCurso(pln, ccl, crs, ordn, oblgtr);
        planCursoRepository.save(plnCrs);
        registrarAuditoria("ASIGNAR_CURSO_CICLO", plnCrs, "curso=" + crs.getCodigo() + ", ciclo=" + ccl.getNumero());
        return plnCrs;
    }

    @Transactional
    public void quitar(Long id) {
        sesionActual.requerirRol(RolUsuario.ADMIN);
        PlanCurso plnCrs = planCursoRepository.findById(id)
                .orElseThrow(() -> new EntidadNoEncontradaException("Asignacion no encontrada: id=" + id));

        try {
            planCursoRepository.delete(plnCrs);
            planCursoRepository.flush();
        } catch (DataAccessException e) {

            throw new ValidacionException(
                    "No se puede quitar: ya existen notas registradas para este curso en este ciclo.");
        }

        var actr = sesionActual.obtenerUsuarioActual().orElse(null);
        auditoriaService.registrar(actr, "QUITAR_CURSO_CICLO", "plan_cursos", id.toString(), null);
    }

    private void registrarAuditoria(String accn, PlanCurso plnCrs, String dtll) {
        var actr = sesionActual.obtenerUsuarioActual().orElse(null);
        auditoriaService.registrar(actr, accn, "plan_cursos", plnCrs.getId().toString(), dtll);
    }
}
