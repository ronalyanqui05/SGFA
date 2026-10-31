package pe.edu.sgfa.trayectoria.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.sgfa.auditoria.service.RahpAuditoriaService;
import pe.edu.sgfa.common.exception.RyeEntidadNoEncontradaException;
import pe.edu.sgfa.common.exception.RyeValidacionException;
import pe.edu.sgfa.estudiante.domain.YmclEstudiante;
import pe.edu.sgfa.estudiante.repository.YmclEstudianteRepository;
import pe.edu.sgfa.nota.repository.YmclRegistroAcademicoRepository;
import pe.edu.sgfa.programa.domain.RacaEstadoPlanEstudio;
import pe.edu.sgfa.programa.domain.RacaPlanEstudio;
import pe.edu.sgfa.programa.domain.RacaProgramaEstudio;
import pe.edu.sgfa.programa.repository.RacaPlanEstudioRepository;
import pe.edu.sgfa.programa.repository.RacaProgramaEstudioRepository;
import pe.edu.sgfa.trayectoria.domain.YmclEstadoTrayectoria;
import pe.edu.sgfa.trayectoria.domain.YmclTrayectoria;
import pe.edu.sgfa.trayectoria.repository.YmclTrayectoriaRepository;
import pe.edu.sgfa.usuario.service.RyeSesionActual;

@Service
public class YmclTrayectoriaService {

    private final YmclTrayectoriaRepository trayectoriaRepositoryChp;
    private final YmclEstudianteRepository estudianteRepositoryChp;
    private final RacaProgramaEstudioRepository programaEstudioRepositoryChp;
    private final RacaPlanEstudioRepository planEstudioRepositoryChp;
    private final YmclRegistroAcademicoRepository registroAcademicoRepositoryChp;
    private final RyeSesionActual sesionActualChp;
    private final RahpAuditoriaService auditoriaServiceChp;

    public YmclTrayectoriaService(YmclTrayectoriaRepository trayectoriaRepositoryChp, YmclEstudianteRepository estudianteRepositoryChp,
                               RacaProgramaEstudioRepository programaEstudioRepositoryChp,
                               RacaPlanEstudioRepository planEstudioRepositoryChp,
                               YmclRegistroAcademicoRepository registroAcademicoRepositoryChp, RyeSesionActual sesionActualChp,
                               RahpAuditoriaService auditoriaServiceChp) {
        this.trayectoriaRepositoryChp = trayectoriaRepositoryChp;
        this.estudianteRepositoryChp = estudianteRepositoryChp;
        this.programaEstudioRepositoryChp = programaEstudioRepositoryChp;
        this.planEstudioRepositoryChp = planEstudioRepositoryChp;
        this.registroAcademicoRepositoryChp = registroAcademicoRepositoryChp;
        this.sesionActualChp = sesionActualChp;
        this.auditoriaServiceChp = auditoriaServiceChp;
    }

    public List<YmclTrayectoria> ymclListarPorEstudiante(Long estudianteIdChp) {
        sesionActualChp.ryeRequerirAutenticacion();
        return trayectoriaRepositoryChp.findByEstudianteIdOrderByFechaInicioDesc(estudianteIdChp);
    }

    public Optional<YmclTrayectoria> ymclObtenerActiva(Long estudianteIdChp) {
        sesionActualChp.ryeRequerirAutenticacion();
        return trayectoriaRepositoryChp.findByEstudianteIdAndEstado(estudianteIdChp, YmclEstadoTrayectoria.ACTIVA);
    }

    @Transactional
    public YmclTrayectoria ymclIniciar(Long estudianteIdChp, Long programaIdChp, Long planEstudioIdChp, LocalDate fechaInicioChp,
                                String observacionChp) {
        sesionActualChp.ryeRequerirAutenticacion();
        if (ymclObtenerActiva(estudianteIdChp).isPresent()) {
            throw new RyeValidacionException(
                    "El estudiante ya tiene una trayectoria activa. Use \"Cambiar de plan\" en vez de iniciar otra.");
        }

        YmclEstudiante estudianteChp = estudianteRepositoryChp.findById(estudianteIdChp)
                .orElseThrow(() -> new RyeEntidadNoEncontradaException("Estudiante no encontrado: id=" + estudianteIdChp));
        RacaPlanEstudio planChp = ymclValidarProgramaYPlan(programaIdChp, planEstudioIdChp);

        YmclTrayectoria trayectoriaChp = new YmclTrayectoria(estudianteChp, planChp.getPrograma(), planChp,
                fechaInicioChp != null ? fechaInicioChp : LocalDate.now(), observacionChp);
        trayectoriaRepositoryChp.save(trayectoriaChp);
        ymclRegistrarAuditoria("INICIAR_TRAYECTORIA", trayectoriaChp, "plan=" + planChp.getCodigo());
        return trayectoriaChp;
    }

    @Transactional
    public YmclTrayectoria ymclCambiarPlan(Long estudianteIdChp, Long nuevoProgramaIdChp, Long nuevoPlanEstudioIdChp,
                                    LocalDate fechaCambioChp, String observacionChp) {
        sesionActualChp.ryeRequerirAutenticacion();
        YmclTrayectoria actualChp = ymclObtenerActiva(estudianteIdChp)
                .orElseThrow(() -> new RyeValidacionException("El estudiante no tiene una trayectoria activa que cambiar."));
        RacaPlanEstudio nuevoPlanChp = ymclValidarProgramaYPlan(nuevoProgramaIdChp, nuevoPlanEstudioIdChp);
        LocalDate fechaChp = fechaCambioChp != null ? fechaCambioChp : LocalDate.now();

        actualChp.setEstado(YmclEstadoTrayectoria.TRASLADADA);
        actualChp.setFechaFin(fechaChp);
        ymclRegistrarAuditoria("CAMBIAR_PLAN_TRAYECTORIA_ANTERIOR", actualChp, "nuevo_plan=" + nuevoPlanChp.getCodigo());

        YmclTrayectoria nuevaChp = new YmclTrayectoria(actualChp.getEstudiante(), nuevoPlanChp.getPrograma(), nuevoPlanChp, fechaChp, observacionChp);
        trayectoriaRepositoryChp.save(nuevaChp);
        ymclRegistrarAuditoria("CAMBIAR_PLAN_TRAYECTORIA_NUEVA", nuevaChp, "plan_anterior_id=" + actualChp.getId());
        return nuevaChp;
    }

    @Transactional
    public YmclTrayectoria ymclFinalizar(Long id, LocalDate fechaFinChp, String observacionChp) {
        return ymclCerrar(id, YmclEstadoTrayectoria.FINALIZADA, fechaFinChp, observacionChp, "FINALIZAR_TRAYECTORIA");
    }

    @Transactional
    public YmclTrayectoria ymclRetirar(Long id, LocalDate fechaFinChp, String observacionChp) {
        return ymclCerrar(id, YmclEstadoTrayectoria.RETIRADA, fechaFinChp, observacionChp, "RETIRAR_TRAYECTORIA");
    }

    @Transactional
    public YmclTrayectoria ymclEditar(Long id, Long programaIdChp, Long planEstudioIdChp, LocalDate fechaInicioChp, String observacionChp) {
        sesionActualChp.ryeRequerirAutenticacion();
        YmclTrayectoria trayectoriaChp = ymclObtenerOLanzar(id);
        RacaPlanEstudio planChp = ymclValidarProgramaYPlan(programaIdChp, planEstudioIdChp);

        trayectoriaChp.setPrograma(planChp.getPrograma());
        trayectoriaChp.setPlanEstudio(planChp);
        trayectoriaChp.setFechaInicio(fechaInicioChp != null ? fechaInicioChp : trayectoriaChp.getFechaInicio());
        trayectoriaChp.setObservacion(observacionChp);
        ymclRegistrarAuditoria("EDITAR_TRAYECTORIA", trayectoriaChp, "plan=" + planChp.getCodigo());
        return trayectoriaChp;
    }

    @Transactional
    public YmclTrayectoria ymclReactivar(Long id) {
        sesionActualChp.ryeRequerirAutenticacion();
        YmclTrayectoria trayectoriaChp = ymclObtenerOLanzar(id);
        if (trayectoriaChp.getEstado() == YmclEstadoTrayectoria.ACTIVA) {
            return trayectoriaChp;
        }
        trayectoriaRepositoryChp.findByEstudianteIdAndEstado(trayectoriaChp.getEstudiante().getId(), YmclEstadoTrayectoria.ACTIVA)
                .ifPresent(otraChp -> {
                    throw new RyeValidacionException(
                            "El estudiante ya tiene otra trayectoria activa (plan '" + otraChp.getPlanEstudio().getCodigo()
                                    + "'). Finalicela o retirela antes de reactivar esta.");
                });

        trayectoriaChp.setEstado(YmclEstadoTrayectoria.ACTIVA);
        trayectoriaChp.setFechaFin(null);
        ymclRegistrarAuditoria("REACTIVAR_TRAYECTORIA", trayectoriaChp, null);
        return trayectoriaChp;
    }

    @Transactional
    public void ymclEliminar(Long id) {
        sesionActualChp.ryeRequerirAutenticacion();
        YmclTrayectoria trayectoriaChp = ymclObtenerOLanzar(id);
        long notasEliminadasChp = registroAcademicoRepositoryChp.deleteByTrayectoriaId(id);
        ymclRegistrarAuditoria("ELIMINAR_TRAYECTORIA", trayectoriaChp,
                "plan=" + trayectoriaChp.getPlanEstudio().getCodigo() + " notas_eliminadas=" + notasEliminadasChp);
        trayectoriaRepositoryChp.delete(trayectoriaChp);
    }

    private YmclTrayectoria ymclObtenerOLanzar(Long id) {
        return trayectoriaRepositoryChp.findById(id)
                .orElseThrow(() -> new RyeEntidadNoEncontradaException("Trayectoria no encontrada: id=" + id));
    }

    private YmclTrayectoria ymclCerrar(Long id, YmclEstadoTrayectoria estadoChp, LocalDate fechaFinChp, String observacionChp, String accionChp) {
        sesionActualChp.ryeRequerirAutenticacion();
        YmclTrayectoria trayectoriaChp = trayectoriaRepositoryChp.findById(id)
                .orElseThrow(() -> new RyeEntidadNoEncontradaException("Trayectoria no encontrada: id=" + id));
        if (trayectoriaChp.getEstado() != YmclEstadoTrayectoria.ACTIVA) {
            throw new RyeValidacionException("Solo se puede cerrar una trayectoria activa.");
        }

        trayectoriaChp.setEstado(estadoChp);
        trayectoriaChp.setFechaFin(fechaFinChp != null ? fechaFinChp : LocalDate.now());
        if (observacionChp != null && !observacionChp.isBlank()) {
            trayectoriaChp.setObservacion(observacionChp);
        }
        ymclRegistrarAuditoria(accionChp, trayectoriaChp, null);
        return trayectoriaChp;
    }

    private RacaPlanEstudio ymclValidarProgramaYPlan(Long programaIdChp, Long planEstudioIdChp) {
        RacaProgramaEstudio programaChp = programaEstudioRepositoryChp.findById(programaIdChp)
                .orElseThrow(() -> new RyeEntidadNoEncontradaException("Programa no encontrado: id=" + programaIdChp));
        RacaPlanEstudio planChp = planEstudioRepositoryChp.findById(planEstudioIdChp)
                .orElseThrow(() -> new RyeEntidadNoEncontradaException("Plan de estudio no encontrado: id=" + planEstudioIdChp));
        if (!planChp.getPrograma().getId().equals(programaIdChp)) {
            throw new RyeValidacionException("El plan seleccionado no pertenece al programa indicado.");
        }
        if (!programaChp.isActivo()) {
            throw new RyeValidacionException("El programa seleccionado esta inactivo.");
        }
        if (planChp.getEstado() != RacaEstadoPlanEstudio.ACTIVO) {
            throw new RyeValidacionException("El plan seleccionado no esta activo.");
        }
        return planChp;
    }

    private void ymclRegistrarAuditoria(String accionChp, YmclTrayectoria trayectoriaChp, String detalleChp) {
        var actorChp = sesionActualChp.ryeObtenerUsuarioActual().orElse(null);
        auditoriaServiceChp.rahpRegistrar(actorChp, accionChp, "trayectorias_academicas", trayectoriaChp.getId().toString(), detalleChp);
    }
}
