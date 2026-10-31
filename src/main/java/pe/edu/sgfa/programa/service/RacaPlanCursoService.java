package pe.edu.sgfa.programa.service;

import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.sgfa.auditoria.service.RahpAuditoriaService;
import pe.edu.sgfa.common.exception.RyeEntidadNoEncontradaException;
import pe.edu.sgfa.common.exception.RyeValidacionException;
import pe.edu.sgfa.programa.domain.RacaCiclo;
import pe.edu.sgfa.programa.domain.RacaCurso;
import pe.edu.sgfa.programa.domain.RacaPlanCurso;
import pe.edu.sgfa.programa.domain.RacaPlanEstudio;
import pe.edu.sgfa.programa.repository.RacaCicloRepository;
import pe.edu.sgfa.programa.repository.RacaCursoRepository;
import pe.edu.sgfa.programa.repository.RacaPlanCursoRepository;
import pe.edu.sgfa.programa.repository.RacaPlanEstudioRepository;
import pe.edu.sgfa.usuario.domain.RyeRolUsuario;
import pe.edu.sgfa.usuario.service.RyeSesionActual;

@Service
public class RacaPlanCursoService {

    private final RacaPlanCursoRepository planCursoRepositoryCrr;
    private final RacaPlanEstudioRepository planEstudioRepositoryCrr;
    private final RacaCicloRepository cicloRepositoryCrr;
    private final RacaCursoRepository cursoRepositoryCrr;
    private final RyeSesionActual sesionActualCrr;
    private final RahpAuditoriaService auditoriaServiceCrr;

    public RacaPlanCursoService(RacaPlanCursoRepository planCursoRepositoryCrr, RacaPlanEstudioRepository planEstudioRepositoryCrr,
                             RacaCicloRepository cicloRepositoryCrr, RacaCursoRepository cursoRepositoryCrr,
                             RyeSesionActual sesionActualCrr, RahpAuditoriaService auditoriaServiceCrr) {
        this.planCursoRepositoryCrr = planCursoRepositoryCrr;
        this.planEstudioRepositoryCrr = planEstudioRepositoryCrr;
        this.cicloRepositoryCrr = cicloRepositoryCrr;
        this.cursoRepositoryCrr = cursoRepositoryCrr;
        this.sesionActualCrr = sesionActualCrr;
        this.auditoriaServiceCrr = auditoriaServiceCrr;
    }

    public List<RacaPlanCurso> racaListarPorCiclo(Long cicloIdCrr) {
        sesionActualCrr.ryeRequerirRol(RyeRolUsuario.ADMIN);
        return planCursoRepositoryCrr.findByCicloIdOrderByOrdenAsc(cicloIdCrr);
    }

    @Transactional
    public RacaPlanCurso racaAsignar(Long planEstudioIdCrr, Long cicloIdCrr, Long cursoIdCrr, int ordenCrr, boolean obligatorioCrr) {
        sesionActualCrr.ryeRequerirRol(RyeRolUsuario.ADMIN);

        RacaPlanEstudio planCrr = planEstudioRepositoryCrr.findById(planEstudioIdCrr)
                .orElseThrow(() -> new RyeEntidadNoEncontradaException("Plan de estudio no encontrado: id=" + planEstudioIdCrr));
        RacaCiclo cicloCrr = cicloRepositoryCrr.findById(cicloIdCrr)
                .orElseThrow(() -> new RyeEntidadNoEncontradaException("Ciclo no encontrado: id=" + cicloIdCrr));
        RacaCurso cursoCrr = cursoRepositoryCrr.findById(cursoIdCrr)
                .orElseThrow(() -> new RyeEntidadNoEncontradaException("Curso no encontrado: id=" + cursoIdCrr));

        if (!cicloCrr.getPlanEstudio().getId().equals(planEstudioIdCrr)) {
            throw new RyeValidacionException("El ciclo seleccionado no pertenece al plan de estudio indicado.");
        }
        if (planCursoRepositoryCrr.existsByPlanEstudioIdAndCicloIdAndCursoId(planEstudioIdCrr, cicloIdCrr, cursoIdCrr)) {
            throw new RyeValidacionException("Ese curso ya esta asignado a este ciclo.");
        }

        RacaPlanCurso planCursoCrr = new RacaPlanCurso(planCrr, cicloCrr, cursoCrr, ordenCrr, obligatorioCrr);
        planCursoRepositoryCrr.save(planCursoCrr);
        racaRegistrarAuditoria("ASIGNAR_CURSO_CICLO", planCursoCrr, "curso=" + cursoCrr.getCodigo() + ", ciclo=" + cicloCrr.getNumero());
        return planCursoCrr;
    }

    @Transactional
    public void racaQuitar(Long id) {
        sesionActualCrr.ryeRequerirRol(RyeRolUsuario.ADMIN);
        RacaPlanCurso planCursoCrr = planCursoRepositoryCrr.findById(id)
                .orElseThrow(() -> new RyeEntidadNoEncontradaException("Asignacion no encontrada: id=" + id));

        try {
            planCursoRepositoryCrr.delete(planCursoCrr);
            planCursoRepositoryCrr.flush();
        } catch (DataAccessException e) {

            throw new RyeValidacionException(
                    "No se puede quitar: ya existen notas registradas para este curso en este ciclo.");
        }

        var actorCrr = sesionActualCrr.ryeObtenerUsuarioActual().orElse(null);
        auditoriaServiceCrr.rahpRegistrar(actorCrr, "QUITAR_CURSO_CICLO", "plan_cursos", id.toString(), null);
    }

    private void racaRegistrarAuditoria(String accionCrr, RacaPlanCurso planCursoCrr, String detalleCrr) {
        var actorCrr = sesionActualCrr.ryeObtenerUsuarioActual().orElse(null);
        auditoriaServiceCrr.rahpRegistrar(actorCrr, accionCrr, "plan_cursos", planCursoCrr.getId().toString(), detalleCrr);
    }
}
