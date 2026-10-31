package pe.edu.sgfa.periodo.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.sgfa.auditoria.service.RahpAuditoriaService;
import pe.edu.sgfa.common.exception.RyeEntidadNoEncontradaException;
import pe.edu.sgfa.common.exception.RyeValidacionException;
import pe.edu.sgfa.periodo.domain.YmclEstadoPeriodoAcademico;
import pe.edu.sgfa.periodo.domain.YmclPeriodoAcademico;
import pe.edu.sgfa.periodo.repository.YmclPeriodoAcademicoRepository;
import pe.edu.sgfa.usuario.domain.RyeRolUsuario;
import pe.edu.sgfa.usuario.service.RyeSesionActual;

@Service
public class YmclPeriodoAcademicoService {

    private final YmclPeriodoAcademicoRepository periodoAcademicoRepositoryChp;
    private final RyeSesionActual sesionActualChp;
    private final RahpAuditoriaService auditoriaServiceChp;

    public YmclPeriodoAcademicoService(YmclPeriodoAcademicoRepository periodoAcademicoRepositoryChp,
                                    RyeSesionActual sesionActualChp, RahpAuditoriaService auditoriaServiceChp) {
        this.periodoAcademicoRepositoryChp = periodoAcademicoRepositoryChp;
        this.sesionActualChp = sesionActualChp;
        this.auditoriaServiceChp = auditoriaServiceChp;
    }

    public List<YmclPeriodoAcademico> ymclListar() {
        sesionActualChp.ryeRequerirRol(RyeRolUsuario.ADMIN);
        return periodoAcademicoRepositoryChp.findAll();
    }

    @Transactional
    public YmclPeriodoAcademico ymclCrear(String nombreChp, int anioChp, String tipoChp, LocalDate fechaInicioChp, LocalDate fechaFinChp) {
        sesionActualChp.ryeRequerirRol(RyeRolUsuario.ADMIN);
        ymclValidarDatos(nombreChp, anioChp, fechaInicioChp, fechaFinChp);
        if (periodoAcademicoRepositoryChp.existsByNombre(nombreChp)) {
            throw new RyeValidacionException("Ya existe un periodo con el nombre '" + nombreChp + "'.");
        }

        YmclPeriodoAcademico periodoChp = new YmclPeriodoAcademico(nombreChp, anioChp, tipoChp, fechaInicioChp, fechaFinChp);
        periodoAcademicoRepositoryChp.save(periodoChp);
        ymclRegistrarAuditoria("CREAR_PERIODO", periodoChp, "nombre=" + nombreChp);
        return periodoChp;
    }

    @Transactional
    public YmclPeriodoAcademico ymclEditar(Long id, String nombreChp, int anioChp, String tipoChp, LocalDate fechaInicioChp, LocalDate fechaFinChp) {
        sesionActualChp.ryeRequerirRol(RyeRolUsuario.ADMIN);
        ymclValidarDatos(nombreChp, anioChp, fechaInicioChp, fechaFinChp);
        YmclPeriodoAcademico periodoChp = ymclObtenerOLanzar(id);
        if (periodoAcademicoRepositoryChp.existsByNombreAndIdNot(nombreChp, id)) {
            throw new RyeValidacionException("Ya existe un periodo con el nombre '" + nombreChp + "'.");
        }

        periodoChp.setNombre(nombreChp);
        periodoChp.setAnio(anioChp);
        periodoChp.setTipo(tipoChp);
        periodoChp.setFechaInicio(fechaInicioChp);
        periodoChp.setFechaFin(fechaFinChp);
        ymclRegistrarAuditoria("EDITAR_PERIODO", periodoChp, "nombre=" + nombreChp);
        return periodoChp;
    }

    @Transactional
    public YmclPeriodoAcademico ymclCambiarEstado(Long id, YmclEstadoPeriodoAcademico estadoChp) {
        sesionActualChp.ryeRequerirRol(RyeRolUsuario.ADMIN);
        YmclPeriodoAcademico periodoChp = ymclObtenerOLanzar(id);
        periodoChp.setEstado(estadoChp);
        ymclRegistrarAuditoria("CAMBIAR_ESTADO_PERIODO", periodoChp, "estado=" + estadoChp);
        return periodoChp;
    }

    private YmclPeriodoAcademico ymclObtenerOLanzar(Long id) {
        return periodoAcademicoRepositoryChp.findById(id)
                .orElseThrow(() -> new RyeEntidadNoEncontradaException("Periodo academico no encontrado: id=" + id));
    }

    private void ymclValidarDatos(String nombreChp, int anioChp, LocalDate fechaInicioChp, LocalDate fechaFinChp) {
        if (nombreChp == null || nombreChp.isBlank()) {
            throw new RyeValidacionException("El nombre del periodo es obligatorio.");
        }
        if (anioChp <= 0) {
            throw new RyeValidacionException("El anio debe ser valido.");
        }
        if (fechaInicioChp != null && fechaFinChp != null && fechaFinChp.isBefore(fechaInicioChp)) {
            throw new RyeValidacionException("La fecha de fin no puede ser anterior a la fecha de inicio.");
        }
    }

    private void ymclRegistrarAuditoria(String accionChp, YmclPeriodoAcademico periodoChp, String detalleChp) {
        var actorChp = sesionActualChp.ryeObtenerUsuarioActual().orElse(null);
        auditoriaServiceChp.rahpRegistrar(actorChp, accionChp, "periodos_academicos", periodoChp.getId().toString(), detalleChp);
    }
}
