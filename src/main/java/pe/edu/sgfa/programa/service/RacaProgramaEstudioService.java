package pe.edu.sgfa.programa.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.sgfa.auditoria.service.RahpAuditoriaService;
import pe.edu.sgfa.common.exception.RyeEntidadNoEncontradaException;
import pe.edu.sgfa.common.exception.RyeValidacionException;
import pe.edu.sgfa.institucion.domain.RacaInstitucion;
import pe.edu.sgfa.institucion.service.RacaInstitucionService;
import pe.edu.sgfa.programa.domain.RacaProgramaEstudio;
import pe.edu.sgfa.programa.repository.RacaProgramaEstudioRepository;
import pe.edu.sgfa.usuario.domain.RyeRolUsuario;
import pe.edu.sgfa.usuario.service.RyeSesionActual;

@Service
public class RacaProgramaEstudioService {

    private final RacaProgramaEstudioRepository programaEstudioRepositoryCrr;
    private final RacaInstitucionService institucionServiceCrr;
    private final RyeSesionActual sesionActualCrr;
    private final RahpAuditoriaService auditoriaServiceCrr;

    public RacaProgramaEstudioService(RacaProgramaEstudioRepository programaEstudioRepositoryCrr,
                                   RacaInstitucionService institucionServiceCrr, RyeSesionActual sesionActualCrr,
                                   RahpAuditoriaService auditoriaServiceCrr) {
        this.programaEstudioRepositoryCrr = programaEstudioRepositoryCrr;
        this.institucionServiceCrr = institucionServiceCrr;
        this.sesionActualCrr = sesionActualCrr;
        this.auditoriaServiceCrr = auditoriaServiceCrr;
    }

    public List<RacaProgramaEstudio> racaListar() {
        sesionActualCrr.ryeRequerirRol(RyeRolUsuario.ADMIN);
        return programaEstudioRepositoryCrr.findAll();
    }

    @Transactional
    public RacaProgramaEstudio racaCrear(String codigoCrr, String nombreCrr, String descripcionCrr, int duracionCiclosCrr) {
        sesionActualCrr.ryeRequerirRol(RyeRolUsuario.ADMIN);
        RacaInstitucion institucionCrr = racaInstitucionPrincipalOLanzar();
        racaValidarDatos(codigoCrr, nombreCrr, duracionCiclosCrr);
        if (programaEstudioRepositoryCrr.existsByInstitucionIdAndCodigo(institucionCrr.getId(), codigoCrr)) {
            throw new RyeValidacionException("Ya existe un programa con el codigo '" + codigoCrr + "'.");
        }

        RacaProgramaEstudio programaCrr = new RacaProgramaEstudio(institucionCrr, codigoCrr, nombreCrr, descripcionCrr, duracionCiclosCrr);
        programaEstudioRepositoryCrr.save(programaCrr);
        racaRegistrarAuditoria("CREAR_PROGRAMA", programaCrr, "codigo=" + codigoCrr);
        return programaCrr;
    }

    @Transactional
    public RacaProgramaEstudio racaEditar(Long id, String codigoCrr, String nombreCrr, String descripcionCrr, int duracionCiclosCrr) {
        sesionActualCrr.ryeRequerirRol(RyeRolUsuario.ADMIN);
        racaValidarDatos(codigoCrr, nombreCrr, duracionCiclosCrr);
        RacaProgramaEstudio programaCrr = racaObtenerOLanzar(id);
        if (programaEstudioRepositoryCrr.existsByInstitucionIdAndCodigoAndIdNot(
                programaCrr.getInstitucion().getId(), codigoCrr, id)) {
            throw new RyeValidacionException("Ya existe un programa con el codigo '" + codigoCrr + "'.");
        }

        programaCrr.setCodigo(codigoCrr);
        programaCrr.setNombre(nombreCrr);
        programaCrr.setDescripcion(descripcionCrr);
        programaCrr.setDuracionCiclos(duracionCiclosCrr);
        racaRegistrarAuditoria("EDITAR_PROGRAMA", programaCrr, "codigo=" + codigoCrr);
        return programaCrr;
    }

    @Transactional
    public RacaProgramaEstudio racaActivarDesactivar(Long id, boolean activoCrr) {
        sesionActualCrr.ryeRequerirRol(RyeRolUsuario.ADMIN);
        RacaProgramaEstudio programaCrr = racaObtenerOLanzar(id);
        programaCrr.setActivo(activoCrr);
        racaRegistrarAuditoria(activoCrr ? "ACTIVAR_PROGRAMA" : "DESACTIVAR_PROGRAMA", programaCrr, null);
        return programaCrr;
    }

    private RacaInstitucion racaInstitucionPrincipalOLanzar() {
        return institucionServiceCrr.racaObtenerPrincipal()
                .orElseThrow(() -> new RyeValidacionException("Configure primero los datos de la institucion."));
    }

    private RacaProgramaEstudio racaObtenerOLanzar(Long id) {
        return programaEstudioRepositoryCrr.findById(id)
                .orElseThrow(() -> new RyeEntidadNoEncontradaException("Programa no encontrado: id=" + id));
    }

    private void racaValidarDatos(String codigoCrr, String nombreCrr, int duracionCiclosCrr) {
        if (codigoCrr == null || codigoCrr.isBlank()) {
            throw new RyeValidacionException("El codigo del programa es obligatorio.");
        }
        if (nombreCrr == null || nombreCrr.isBlank()) {
            throw new RyeValidacionException("El nombre del programa es obligatorio.");
        }
        if (duracionCiclosCrr <= 0) {
            throw new RyeValidacionException("La duracion en ciclos debe ser mayor que cero.");
        }
    }

    private void racaRegistrarAuditoria(String accionCrr, RacaProgramaEstudio programaCrr, String detalleCrr) {
        var actorCrr = sesionActualCrr.ryeObtenerUsuarioActual().orElse(null);
        auditoriaServiceCrr.rahpRegistrar(actorCrr, accionCrr, "programas_estudio", programaCrr.getId().toString(), detalleCrr);
    }
}
