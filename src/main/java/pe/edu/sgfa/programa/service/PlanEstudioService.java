package pe.edu.sgfa.programa.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.sgfa.auditoria.service.AuditoriaService;
import pe.edu.sgfa.common.exception.EntidadNoEncontradaException;
import pe.edu.sgfa.common.exception.ValidacionException;
import pe.edu.sgfa.programa.domain.EstadoPlanEstudio;
import pe.edu.sgfa.programa.domain.PlanEstudio;
import pe.edu.sgfa.programa.domain.ProgramaEstudio;
import pe.edu.sgfa.programa.repository.CicloRepository;
import pe.edu.sgfa.programa.repository.PlanEstudioRepository;
import pe.edu.sgfa.programa.repository.ProgramaEstudioRepository;
import pe.edu.sgfa.trayectoria.repository.TrayectoriaRepository;
import pe.edu.sgfa.usuario.domain.RolUsuario;
import pe.edu.sgfa.usuario.service.SesionActual;

@Service
public class PlanEstudioService {

    private final PlanEstudioRepository planEstudioRepository;
    private final ProgramaEstudioRepository programaEstudioRepository;
    private final CicloRepository cicloRepository;
    private final TrayectoriaRepository trayectoriaRepository;
    private final SesionActual sesionActual;
    private final AuditoriaService auditoriaService;

    public PlanEstudioService(PlanEstudioRepository planEstudioRepository,
                               ProgramaEstudioRepository programaEstudioRepository, CicloRepository cicloRepository,
                               TrayectoriaRepository trayectoriaRepository, SesionActual sesionActual,
                               AuditoriaService auditoriaService) {
        this.planEstudioRepository = planEstudioRepository;
        this.programaEstudioRepository = programaEstudioRepository;
        this.cicloRepository = cicloRepository;
        this.trayectoriaRepository = trayectoriaRepository;
        this.sesionActual = sesionActual;
        this.auditoriaService = auditoriaService;
    }

    public List<PlanEstudio> listarPorPrograma(Long prgrmId) {
        sesionActual.requerirRol(RolUsuario.ADMIN);
        return planEstudioRepository.findByProgramaId(prgrmId);
    }

    @Transactional
    public PlanEstudio crear(Long prgrmId, String cdg, String nmbr, LocalDate fchInc, LocalDate fchFin) {
        sesionActual.requerirRol(RolUsuario.ADMIN);
        ProgramaEstudio programa = programaEstudioRepository.findById(prgrmId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Programa no encontrado: id=" + prgrmId));
        validarDatos(cdg, nmbr, fchInc, fchFin);
        if (planEstudioRepository.existsByProgramaIdAndCodigo(prgrmId, cdg)) {
            throw new ValidacionException("Ya existe un plan con el codigo '" + cdg + "' en este programa.");
        }

        PlanEstudio pln = new PlanEstudio(programa, cdg, nmbr, fchInc, fchFin);
        planEstudioRepository.save(pln);
        registrarAuditoria("CREAR_PLAN_ESTUDIO", pln, "codigo=" + cdg);
        return pln;
    }

    @Transactional
    public PlanEstudio editar(Long id, String cdg, String nmbr, LocalDate fchInc, LocalDate fchFin) {
        sesionActual.requerirRol(RolUsuario.ADMIN);
        validarDatos(cdg, nmbr, fchInc, fchFin);
        PlanEstudio pln = obtenerOLanzar(id);
        if (planEstudioRepository.existsByProgramaIdAndCodigoAndIdNot(pln.getPrograma().getId(), cdg, id)) {
            throw new ValidacionException("Ya existe un plan con el codigo '" + cdg + "' en este programa.");
        }

        pln.setCodigo(cdg);
        pln.setNombre(nmbr);
        pln.setFechaInicio(fchInc);
        pln.setFechaFin(fchFin);
        registrarAuditoria("EDITAR_PLAN_ESTUDIO", pln, "codigo=" + cdg);
        return pln;
    }

    @Transactional
    public PlanEstudio cambiarEstado(Long id, EstadoPlanEstudio estd) {
        sesionActual.requerirRol(RolUsuario.ADMIN);
        PlanEstudio pln = obtenerOLanzar(id);
        pln.setEstado(estd);
        registrarAuditoria("CAMBIAR_ESTADO_PLAN_ESTUDIO", pln, "estado=" + estd);
        return pln;
    }

    @Transactional
    public void eliminar(Long id) {
        sesionActual.requerirRol(RolUsuario.ADMIN);
        PlanEstudio pln = obtenerOLanzar(id);
        if (!cicloRepository.findByPlanEstudioIdOrderByOrdenAsc(id).isEmpty()) {
            throw new ValidacionException(
                    "No se puede eliminar: el plan tiene ciclos registrados. Elimine primero sus ciclos.");
        }
        if (trayectoriaRepository.existsByPlanEstudioId(id)) {
            throw new ValidacionException(
                    "No se puede eliminar: hay estudiantes con trayectoria en este plan.");
        }
        registrarAuditoria("ELIMINAR_PLAN_ESTUDIO", pln, "codigo=" + pln.getCodigo());
        planEstudioRepository.delete(pln);
    }

    private PlanEstudio obtenerOLanzar(Long id) {
        return planEstudioRepository.findById(id)
                .orElseThrow(() -> new EntidadNoEncontradaException("Plan de estudio no encontrado: id=" + id));
    }

    private void validarDatos(String cdg, String nmbr, LocalDate fchInc, LocalDate fchFin) {
        if (cdg == null || cdg.isBlank()) {
            throw new ValidacionException("El codigo del plan es obligatorio.");
        }
        if (nmbr == null || nmbr.isBlank()) {
            throw new ValidacionException("El nombre del plan es obligatorio.");
        }
        if (fchInc != null && fchFin != null && fchFin.isBefore(fchInc)) {
            throw new ValidacionException("La fecha de fin no puede ser anterior a la fecha de inicio.");
        }
    }

    private void registrarAuditoria(String accn, PlanEstudio pln, String dtll) {
        var actr = sesionActual.obtenerUsuarioActual().orElse(null);
        auditoriaService.registrar(actr, accn, "planes_estudio", pln.getId().toString(), dtll);
    }
}
