package pe.edu.sgfa.periodo.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.sgfa.auditoria.service.AuditoriaService;
import pe.edu.sgfa.common.exception.EntidadNoEncontradaException;
import pe.edu.sgfa.common.exception.ValidacionException;
import pe.edu.sgfa.periodo.domain.EstadoPeriodoAcademico;
import pe.edu.sgfa.periodo.domain.PeriodoAcademico;
import pe.edu.sgfa.periodo.repository.PeriodoAcademicoRepository;
import pe.edu.sgfa.usuario.domain.RolUsuario;
import pe.edu.sgfa.usuario.service.SesionActual;

@Service
public class PeriodoAcademicoService {

    private final PeriodoAcademicoRepository periodoAcademicoRepository;
    private final SesionActual sesionActual;
    private final AuditoriaService auditoriaService;

    public PeriodoAcademicoService(PeriodoAcademicoRepository periodoAcademicoRepository,
                                    SesionActual sesionActual, AuditoriaService auditoriaService) {
        this.periodoAcademicoRepository = periodoAcademicoRepository;
        this.sesionActual = sesionActual;
        this.auditoriaService = auditoriaService;
    }

    public List<PeriodoAcademico> listar() {
        sesionActual.requerirRol(RolUsuario.ADMIN);
        return periodoAcademicoRepository.findAll();
    }

    @Transactional
    public PeriodoAcademico crear(String nmbr, int an, String tp, LocalDate fchInc, LocalDate fchFin) {
        sesionActual.requerirRol(RolUsuario.ADMIN);
        validarDatos(nmbr, an, fchInc, fchFin);
        if (periodoAcademicoRepository.existsByNombre(nmbr)) {
            throw new ValidacionException("Ya existe un periodo con el nombre '" + nmbr + "'.");
        }

        PeriodoAcademico periodo = new PeriodoAcademico(nmbr, an, tp, fchInc, fchFin);
        periodoAcademicoRepository.save(periodo);
        registrarAuditoria("CREAR_PERIODO", periodo, "nombre=" + nmbr);
        return periodo;
    }

    @Transactional
    public PeriodoAcademico editar(Long id, String nmbr, int an, String tp, LocalDate fchInc, LocalDate fchFin) {
        sesionActual.requerirRol(RolUsuario.ADMIN);
        validarDatos(nmbr, an, fchInc, fchFin);
        PeriodoAcademico periodo = obtenerOLanzar(id);
        if (periodoAcademicoRepository.existsByNombreAndIdNot(nmbr, id)) {
            throw new ValidacionException("Ya existe un periodo con el nombre '" + nmbr + "'.");
        }

        periodo.setNombre(nmbr);
        periodo.setAnio(an);
        periodo.setTipo(tp);
        periodo.setFechaInicio(fchInc);
        periodo.setFechaFin(fchFin);
        registrarAuditoria("EDITAR_PERIODO", periodo, "nombre=" + nmbr);
        return periodo;
    }

    @Transactional
    public PeriodoAcademico cambiarEstado(Long id, EstadoPeriodoAcademico estd) {
        sesionActual.requerirRol(RolUsuario.ADMIN);
        PeriodoAcademico periodo = obtenerOLanzar(id);
        periodo.setEstado(estd);
        registrarAuditoria("CAMBIAR_ESTADO_PERIODO", periodo, "estado=" + estd);
        return periodo;
    }

    private PeriodoAcademico obtenerOLanzar(Long id) {
        return periodoAcademicoRepository.findById(id)
                .orElseThrow(() -> new EntidadNoEncontradaException("Periodo academico no encontrado: id=" + id));
    }

    private void validarDatos(String nmbr, int an, LocalDate fchInc, LocalDate fchFin) {
        if (nmbr == null || nmbr.isBlank()) {
            throw new ValidacionException("El nombre del periodo es obligatorio.");
        }
        if (an <= 0) {
            throw new ValidacionException("El anio debe ser valido.");
        }
        if (fchInc != null && fchFin != null && fchFin.isBefore(fchInc)) {
            throw new ValidacionException("La fecha de fin no puede ser anterior a la fecha de inicio.");
        }
    }

    private void registrarAuditoria(String accn, PeriodoAcademico periodo, String dtll) {
        var actr = sesionActual.obtenerUsuarioActual().orElse(null);
        auditoriaService.registrar(actr, accn, "periodos_academicos", periodo.getId().toString(), dtll);
    }
}
