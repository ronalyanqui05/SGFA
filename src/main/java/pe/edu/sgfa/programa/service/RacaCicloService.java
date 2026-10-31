package pe.edu.sgfa.programa.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.sgfa.auditoria.service.RahpAuditoriaService;
import pe.edu.sgfa.common.exception.RyeEntidadNoEncontradaException;
import pe.edu.sgfa.common.exception.RyeValidacionException;
import pe.edu.sgfa.programa.domain.RacaCiclo;
import pe.edu.sgfa.programa.domain.RacaPlanEstudio;
import pe.edu.sgfa.programa.repository.RacaCicloRepository;
import pe.edu.sgfa.programa.repository.RacaPlanCursoRepository;
import pe.edu.sgfa.programa.repository.RacaPlanEstudioRepository;
import pe.edu.sgfa.usuario.domain.RyeRolUsuario;
import pe.edu.sgfa.usuario.service.RyeSesionActual;

@Service
public class RacaCicloService {

    private final RacaCicloRepository cicloRepositoryCrr;
    private final RacaPlanEstudioRepository planEstudioRepositoryCrr;
    private final RacaPlanCursoRepository planCursoRepositoryCrr;
    private final RyeSesionActual sesionActualCrr;
    private final RahpAuditoriaService auditoriaServiceCrr;

    public RacaCicloService(RacaCicloRepository cicloRepositoryCrr, RacaPlanEstudioRepository planEstudioRepositoryCrr,
                         RacaPlanCursoRepository planCursoRepositoryCrr, RyeSesionActual sesionActualCrr,
                         RahpAuditoriaService auditoriaServiceCrr) {
        this.cicloRepositoryCrr = cicloRepositoryCrr;
        this.planEstudioRepositoryCrr = planEstudioRepositoryCrr;
        this.planCursoRepositoryCrr = planCursoRepositoryCrr;
        this.sesionActualCrr = sesionActualCrr;
        this.auditoriaServiceCrr = auditoriaServiceCrr;
    }

    public List<RacaCiclo> racaListarPorPlan(Long planEstudioIdCrr) {
        sesionActualCrr.ryeRequerirRol(RyeRolUsuario.ADMIN);
        return cicloRepositoryCrr.findByPlanEstudioIdOrderByOrdenAsc(planEstudioIdCrr);
    }

    @Transactional
    public RacaCiclo racaCrear(Long planEstudioIdCrr, int numeroCrr, String nombreCrr, int ordenCrr) {
        sesionActualCrr.ryeRequerirRol(RyeRolUsuario.ADMIN);
        RacaPlanEstudio planCrr = planEstudioRepositoryCrr.findById(planEstudioIdCrr)
                .orElseThrow(() -> new RyeEntidadNoEncontradaException("Plan de estudio no encontrado: id=" + planEstudioIdCrr));
        racaValidarDatos(numeroCrr, nombreCrr);
        if (cicloRepositoryCrr.existsByPlanEstudioIdAndNumero(planEstudioIdCrr, numeroCrr)) {
            throw new RyeValidacionException("Ya existe el ciclo numero " + numeroCrr + " en este plan.");
        }

        RacaCiclo cicloCrr = new RacaCiclo(planCrr, numeroCrr, nombreCrr, ordenCrr);
        cicloRepositoryCrr.save(cicloCrr);
        racaRegistrarAuditoria("CREAR_CICLO", cicloCrr, "numero=" + numeroCrr);
        return cicloCrr;
    }

    @Transactional
    public RacaCiclo racaEditar(Long id, int numeroCrr, String nombreCrr, int ordenCrr) {
        sesionActualCrr.ryeRequerirRol(RyeRolUsuario.ADMIN);
        racaValidarDatos(numeroCrr, nombreCrr);
        RacaCiclo cicloCrr = racaObtenerOLanzar(id);
        if (cicloRepositoryCrr.existsByPlanEstudioIdAndNumeroAndIdNot(cicloCrr.getPlanEstudio().getId(), numeroCrr, id)) {
            throw new RyeValidacionException("Ya existe el ciclo numero " + numeroCrr + " en este plan.");
        }

        cicloCrr.setNumero(numeroCrr);
        cicloCrr.setNombre(nombreCrr);
        cicloCrr.setOrden(ordenCrr);
        racaRegistrarAuditoria("EDITAR_CICLO", cicloCrr, "numero=" + numeroCrr);
        return cicloCrr;
    }

    @Transactional
    public void racaEliminar(Long id) {
        sesionActualCrr.ryeRequerirRol(RyeRolUsuario.ADMIN);
        RacaCiclo cicloCrr = racaObtenerOLanzar(id);
        if (!planCursoRepositoryCrr.findByCicloIdOrderByOrdenAsc(id).isEmpty()) {
            throw new RyeValidacionException(
                    "No se puede eliminar: el ciclo tiene cursos asignados. Quitelos primero en \"Cursos por ciclo\".");
        }
        racaRegistrarAuditoria("ELIMINAR_CICLO", cicloCrr, "numero=" + cicloCrr.getNumero());
        cicloRepositoryCrr.delete(cicloCrr);
    }

    private RacaCiclo racaObtenerOLanzar(Long id) {
        return cicloRepositoryCrr.findById(id)
                .orElseThrow(() -> new RyeEntidadNoEncontradaException("Ciclo no encontrado: id=" + id));
    }

    private void racaValidarDatos(int numeroCrr, String nombreCrr) {
        if (numeroCrr <= 0) {
            throw new RyeValidacionException("El numero de ciclo debe ser mayor que cero.");
        }
        if (nombreCrr == null || nombreCrr.isBlank()) {
            throw new RyeValidacionException("El nombre del ciclo es obligatorio.");
        }
    }

    private void racaRegistrarAuditoria(String accionCrr, RacaCiclo cicloCrr, String detalleCrr) {
        var actorCrr = sesionActualCrr.ryeObtenerUsuarioActual().orElse(null);
        auditoriaServiceCrr.rahpRegistrar(actorCrr, accionCrr, "ciclos", cicloCrr.getId().toString(), detalleCrr);
    }
}
