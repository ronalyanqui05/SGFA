package pe.edu.sgfa.programa.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.sgfa.auditoria.service.RahpAuditoriaService;
import pe.edu.sgfa.common.exception.RyeEntidadNoEncontradaException;
import pe.edu.sgfa.common.exception.RyeValidacionException;
import pe.edu.sgfa.programa.domain.RacaEstadoPlanEstudio;
import pe.edu.sgfa.programa.domain.RacaPlanEstudio;
import pe.edu.sgfa.programa.domain.RacaProgramaEstudio;
import pe.edu.sgfa.programa.repository.RacaCicloRepository;
import pe.edu.sgfa.programa.repository.RacaPlanEstudioRepository;
import pe.edu.sgfa.programa.repository.RacaProgramaEstudioRepository;
import pe.edu.sgfa.trayectoria.repository.YmclTrayectoriaRepository;
import pe.edu.sgfa.usuario.domain.RyeRolUsuario;
import pe.edu.sgfa.usuario.service.RyeSesionActual;

@Service
public class RacaPlanEstudioService {

    private final RacaPlanEstudioRepository planEstudioRepositoryCrr;
    private final RacaProgramaEstudioRepository programaEstudioRepositoryCrr;
    private final RacaCicloRepository cicloRepositoryCrr;
    private final YmclTrayectoriaRepository trayectoriaRepositoryCrr;
    private final RyeSesionActual sesionActualCrr;
    private final RahpAuditoriaService auditoriaServiceCrr;

    public RacaPlanEstudioService(RacaPlanEstudioRepository planEstudioRepositoryCrr,
                               RacaProgramaEstudioRepository programaEstudioRepositoryCrr, RacaCicloRepository cicloRepositoryCrr,
                               YmclTrayectoriaRepository trayectoriaRepositoryCrr, RyeSesionActual sesionActualCrr,
                               RahpAuditoriaService auditoriaServiceCrr) {
        this.planEstudioRepositoryCrr = planEstudioRepositoryCrr;
        this.programaEstudioRepositoryCrr = programaEstudioRepositoryCrr;
        this.cicloRepositoryCrr = cicloRepositoryCrr;
        this.trayectoriaRepositoryCrr = trayectoriaRepositoryCrr;
        this.sesionActualCrr = sesionActualCrr;
        this.auditoriaServiceCrr = auditoriaServiceCrr;
    }

    public List<RacaPlanEstudio> racaListarPorPrograma(Long programaIdCrr) {
        sesionActualCrr.ryeRequerirRol(RyeRolUsuario.ADMIN);
        return planEstudioRepositoryCrr.findByProgramaId(programaIdCrr);
    }

    @Transactional
    public RacaPlanEstudio racaCrear(Long programaIdCrr, String codigoCrr, String nombreCrr, LocalDate fechaInicioCrr, LocalDate fechaFinCrr) {
        sesionActualCrr.ryeRequerirRol(RyeRolUsuario.ADMIN);
        RacaProgramaEstudio programaCrr = programaEstudioRepositoryCrr.findById(programaIdCrr)
                .orElseThrow(() -> new RyeEntidadNoEncontradaException("Programa no encontrado: id=" + programaIdCrr));
        racaValidarDatos(codigoCrr, nombreCrr, fechaInicioCrr, fechaFinCrr);
        if (planEstudioRepositoryCrr.existsByProgramaIdAndCodigo(programaIdCrr, codigoCrr)) {
            throw new RyeValidacionException("Ya existe un plan con el codigo '" + codigoCrr + "' en este programa.");
        }

        RacaPlanEstudio planCrr = new RacaPlanEstudio(programaCrr, codigoCrr, nombreCrr, fechaInicioCrr, fechaFinCrr);
        planEstudioRepositoryCrr.save(planCrr);
        racaRegistrarAuditoria("CREAR_PLAN_ESTUDIO", planCrr, "codigo=" + codigoCrr);
        return planCrr;
    }

    @Transactional
    public RacaPlanEstudio racaEditar(Long id, String codigoCrr, String nombreCrr, LocalDate fechaInicioCrr, LocalDate fechaFinCrr) {
        sesionActualCrr.ryeRequerirRol(RyeRolUsuario.ADMIN);
        racaValidarDatos(codigoCrr, nombreCrr, fechaInicioCrr, fechaFinCrr);
        RacaPlanEstudio planCrr = racaObtenerOLanzar(id);
        if (planEstudioRepositoryCrr.existsByProgramaIdAndCodigoAndIdNot(planCrr.getPrograma().getId(), codigoCrr, id)) {
            throw new RyeValidacionException("Ya existe un plan con el codigo '" + codigoCrr + "' en este programa.");
        }

        planCrr.setCodigo(codigoCrr);
        planCrr.setNombre(nombreCrr);
        planCrr.setFechaInicio(fechaInicioCrr);
        planCrr.setFechaFin(fechaFinCrr);
        racaRegistrarAuditoria("EDITAR_PLAN_ESTUDIO", planCrr, "codigo=" + codigoCrr);
        return planCrr;
    }

    @Transactional
    public RacaPlanEstudio racaCambiarEstado(Long id, RacaEstadoPlanEstudio estadoCrr) {
        sesionActualCrr.ryeRequerirRol(RyeRolUsuario.ADMIN);
        RacaPlanEstudio planCrr = racaObtenerOLanzar(id);
        planCrr.setEstado(estadoCrr);
        racaRegistrarAuditoria("CAMBIAR_ESTADO_PLAN_ESTUDIO", planCrr, "estado=" + estadoCrr);
        return planCrr;
    }

    @Transactional
    public void racaEliminar(Long id) {
        sesionActualCrr.ryeRequerirRol(RyeRolUsuario.ADMIN);
        RacaPlanEstudio planCrr = racaObtenerOLanzar(id);
        if (!cicloRepositoryCrr.findByPlanEstudioIdOrderByOrdenAsc(id).isEmpty()) {
            throw new RyeValidacionException(
                    "No se puede eliminar: el plan tiene ciclos registrados. Elimine primero sus ciclos.");
        }
        if (trayectoriaRepositoryCrr.existsByPlanEstudioId(id)) {
            throw new RyeValidacionException(
                    "No se puede eliminar: hay estudiantes con trayectoria en este plan.");
        }
        racaRegistrarAuditoria("ELIMINAR_PLAN_ESTUDIO", planCrr, "codigo=" + planCrr.getCodigo());
        planEstudioRepositoryCrr.delete(planCrr);
    }

    private RacaPlanEstudio racaObtenerOLanzar(Long id) {
        return planEstudioRepositoryCrr.findById(id)
                .orElseThrow(() -> new RyeEntidadNoEncontradaException("Plan de estudio no encontrado: id=" + id));
    }

    private void racaValidarDatos(String codigoCrr, String nombreCrr, LocalDate fechaInicioCrr, LocalDate fechaFinCrr) {
        if (codigoCrr == null || codigoCrr.isBlank()) {
            throw new RyeValidacionException("El codigo del plan es obligatorio.");
        }
        if (nombreCrr == null || nombreCrr.isBlank()) {
            throw new RyeValidacionException("El nombre del plan es obligatorio.");
        }
        if (fechaInicioCrr != null && fechaFinCrr != null && fechaFinCrr.isBefore(fechaInicioCrr)) {
            throw new RyeValidacionException("La fecha de fin no puede ser anterior a la fecha de inicio.");
        }
    }

    private void racaRegistrarAuditoria(String accionCrr, RacaPlanEstudio planCrr, String detalleCrr) {
        var actorCrr = sesionActualCrr.ryeObtenerUsuarioActual().orElse(null);
        auditoriaServiceCrr.rahpRegistrar(actorCrr, accionCrr, "planes_estudio", planCrr.getId().toString(), detalleCrr);
    }
}
