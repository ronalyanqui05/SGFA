package pe.edu.sgfa.trayectoria.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.sgfa.auditoria.service.AuditoriaService;
import pe.edu.sgfa.common.exception.EntidadNoEncontradaException;
import pe.edu.sgfa.common.exception.ValidacionException;
import pe.edu.sgfa.estudiante.domain.Estudiante;
import pe.edu.sgfa.estudiante.repository.EstudianteRepository;
import pe.edu.sgfa.nota.repository.RegistroAcademicoRepository;
import pe.edu.sgfa.programa.domain.EstadoPlanEstudio;
import pe.edu.sgfa.programa.domain.PlanEstudio;
import pe.edu.sgfa.programa.domain.ProgramaEstudio;
import pe.edu.sgfa.programa.repository.PlanEstudioRepository;
import pe.edu.sgfa.programa.repository.ProgramaEstudioRepository;
import pe.edu.sgfa.trayectoria.domain.EstadoTrayectoria;
import pe.edu.sgfa.trayectoria.domain.Trayectoria;
import pe.edu.sgfa.trayectoria.repository.TrayectoriaRepository;
import pe.edu.sgfa.usuario.service.SesionActual;

@Service
public class TrayectoriaService {

    private final TrayectoriaRepository trayectoriaRepository;
    private final EstudianteRepository estudianteRepository;
    private final ProgramaEstudioRepository programaEstudioRepository;
    private final PlanEstudioRepository planEstudioRepository;
    private final RegistroAcademicoRepository registroAcademicoRepository;
    private final SesionActual sesionActual;
    private final AuditoriaService auditoriaService;

    public TrayectoriaService(TrayectoriaRepository trayectoriaRepository, EstudianteRepository estudianteRepository,
                               ProgramaEstudioRepository programaEstudioRepository,
                               PlanEstudioRepository planEstudioRepository,
                               RegistroAcademicoRepository registroAcademicoRepository, SesionActual sesionActual,
                               AuditoriaService auditoriaService) {
        this.trayectoriaRepository = trayectoriaRepository;
        this.estudianteRepository = estudianteRepository;
        this.programaEstudioRepository = programaEstudioRepository;
        this.planEstudioRepository = planEstudioRepository;
        this.registroAcademicoRepository = registroAcademicoRepository;
        this.sesionActual = sesionActual;
        this.auditoriaService = auditoriaService;
    }

    public List<Trayectoria> listarPorEstudiante(Long estdntId) {
        sesionActual.requerirAutenticacion();
        return trayectoriaRepository.findByEstudianteIdOrderByFechaInicioDesc(estdntId);
    }

    public Optional<Trayectoria> obtenerActiva(Long estdntId) {
        sesionActual.requerirAutenticacion();
        return trayectoriaRepository.findByEstudianteIdAndEstado(estdntId, EstadoTrayectoria.ACTIVA);
    }

    @Transactional
    public Trayectoria iniciar(Long estdntId, Long prgrmId, Long plnEstdId, LocalDate fchInc,
                                String obsrvcn) {
        sesionActual.requerirAutenticacion();
        if (obtenerActiva(estdntId).isPresent()) {
            throw new ValidacionException(
                    "El estudiante ya tiene una trayectoria activa. Use \"Cambiar de plan\" en vez de iniciar otra.");
        }

        Estudiante estudiante = estudianteRepository.findById(estdntId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Estudiante no encontrado: id=" + estdntId));
        PlanEstudio pln = validarProgramaYPlan(prgrmId, plnEstdId);

        Trayectoria trayectoria = new Trayectoria(estudiante, pln.getPrograma(), pln,
                fchInc != null ? fchInc : LocalDate.now(), obsrvcn);
        trayectoriaRepository.save(trayectoria);
        registrarAuditoria("INICIAR_TRAYECTORIA", trayectoria, "plan=" + pln.getCodigo());
        return trayectoria;
    }

    @Transactional
    public Trayectoria cambiarPlan(Long estdntId, Long nvPrgrmId, Long nvPlnEstdId,
                                    LocalDate fchCmb, String obsrvcn) {
        sesionActual.requerirAutenticacion();
        Trayectoria actl = obtenerActiva(estdntId)
                .orElseThrow(() -> new ValidacionException("El estudiante no tiene una trayectoria activa que cambiar."));
        PlanEstudio nvPln = validarProgramaYPlan(nvPrgrmId, nvPlnEstdId);
        LocalDate fch = fchCmb != null ? fchCmb : LocalDate.now();

        actl.setEstado(EstadoTrayectoria.TRASLADADA);
        actl.setFechaFin(fch);
        registrarAuditoria("CAMBIAR_PLAN_TRAYECTORIA_ANTERIOR", actl, "nuevo_plan=" + nvPln.getCodigo());

        Trayectoria nv = new Trayectoria(actl.getEstudiante(), nvPln.getPrograma(), nvPln, fch, obsrvcn);
        trayectoriaRepository.save(nv);
        registrarAuditoria("CAMBIAR_PLAN_TRAYECTORIA_NUEVA", nv, "plan_anterior_id=" + actl.getId());
        return nv;
    }

    @Transactional
    public Trayectoria finalizar(Long id, LocalDate fchFin, String obsrvcn) {
        return cerrar(id, EstadoTrayectoria.FINALIZADA, fchFin, obsrvcn, "FINALIZAR_TRAYECTORIA");
    }

    @Transactional
    public Trayectoria retirar(Long id, LocalDate fchFin, String obsrvcn) {
        return cerrar(id, EstadoTrayectoria.RETIRADA, fchFin, obsrvcn, "RETIRAR_TRAYECTORIA");
    }

    @Transactional
    public Trayectoria editar(Long id, Long prgrmId, Long plnEstdId, LocalDate fchInc, String obsrvcn) {
        sesionActual.requerirAutenticacion();
        Trayectoria trayectoria = obtenerOLanzar(id);
        PlanEstudio pln = validarProgramaYPlan(prgrmId, plnEstdId);

        trayectoria.setPrograma(pln.getPrograma());
        trayectoria.setPlanEstudio(pln);
        trayectoria.setFechaInicio(fchInc != null ? fchInc : trayectoria.getFechaInicio());
        trayectoria.setObservacion(obsrvcn);
        registrarAuditoria("EDITAR_TRAYECTORIA", trayectoria, "plan=" + pln.getCodigo());
        return trayectoria;
    }

    @Transactional
    public Trayectoria reactivar(Long id) {
        sesionActual.requerirAutenticacion();
        Trayectoria trayectoria = obtenerOLanzar(id);
        if (trayectoria.getEstado() == EstadoTrayectoria.ACTIVA) {
            return trayectoria;
        }
        trayectoriaRepository.findByEstudianteIdAndEstado(trayectoria.getEstudiante().getId(), EstadoTrayectoria.ACTIVA)
                .ifPresent(otra -> {
                    throw new ValidacionException(
                            "El estudiante ya tiene otra trayectoria activa (plan '" + otra.getPlanEstudio().getCodigo()
                                    + "'). Finalicela o retirela antes de reactivar esta.");
                });

        trayectoria.setEstado(EstadoTrayectoria.ACTIVA);
        trayectoria.setFechaFin(null);
        registrarAuditoria("REACTIVAR_TRAYECTORIA", trayectoria, null);
        return trayectoria;
    }

    @Transactional
    public void eliminar(Long id) {
        sesionActual.requerirAutenticacion();
        Trayectoria trayectoria = obtenerOLanzar(id);
        long ntsElmnds = registroAcademicoRepository.deleteByTrayectoriaId(id);
        registrarAuditoria("ELIMINAR_TRAYECTORIA", trayectoria,
                "plan=" + trayectoria.getPlanEstudio().getCodigo() + " notas_eliminadas=" + ntsElmnds);
        trayectoriaRepository.delete(trayectoria);
    }

    private Trayectoria obtenerOLanzar(Long id) {
        return trayectoriaRepository.findById(id)
                .orElseThrow(() -> new EntidadNoEncontradaException("Trayectoria no encontrada: id=" + id));
    }

    private Trayectoria cerrar(Long id, EstadoTrayectoria estd, LocalDate fchFin, String obsrvcn, String accn) {
        sesionActual.requerirAutenticacion();
        Trayectoria trayectoria = trayectoriaRepository.findById(id)
                .orElseThrow(() -> new EntidadNoEncontradaException("Trayectoria no encontrada: id=" + id));
        if (trayectoria.getEstado() != EstadoTrayectoria.ACTIVA) {
            throw new ValidacionException("Solo se puede cerrar una trayectoria activa.");
        }

        trayectoria.setEstado(estd);
        trayectoria.setFechaFin(fchFin != null ? fchFin : LocalDate.now());
        if (obsrvcn != null && !obsrvcn.isBlank()) {
            trayectoria.setObservacion(obsrvcn);
        }
        registrarAuditoria(accn, trayectoria, null);
        return trayectoria;
    }

    private PlanEstudio validarProgramaYPlan(Long prgrmId, Long plnEstdId) {
        ProgramaEstudio programa = programaEstudioRepository.findById(prgrmId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Programa no encontrado: id=" + prgrmId));
        PlanEstudio pln = planEstudioRepository.findById(plnEstdId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Plan de estudio no encontrado: id=" + plnEstdId));
        if (!pln.getPrograma().getId().equals(prgrmId)) {
            throw new ValidacionException("El plan seleccionado no pertenece al programa indicado.");
        }
        if (!programa.isActivo()) {
            throw new ValidacionException("El programa seleccionado esta inactivo.");
        }
        if (pln.getEstado() != EstadoPlanEstudio.ACTIVO) {
            throw new ValidacionException("El plan seleccionado no esta activo.");
        }
        return pln;
    }

    private void registrarAuditoria(String accn, Trayectoria trayectoria, String dtll) {
        var actr = sesionActual.obtenerUsuarioActual().orElse(null);
        auditoriaService.registrar(actr, accn, "trayectorias_academicas", trayectoria.getId().toString(), dtll);
    }
}
