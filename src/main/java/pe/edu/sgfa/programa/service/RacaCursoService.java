package pe.edu.sgfa.programa.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.sgfa.auditoria.service.RahpAuditoriaService;
import pe.edu.sgfa.common.exception.RyeEntidadNoEncontradaException;
import pe.edu.sgfa.common.exception.RyeValidacionException;
import pe.edu.sgfa.programa.domain.RacaCurso;
import pe.edu.sgfa.programa.repository.RacaCursoRepository;
import pe.edu.sgfa.usuario.domain.RyeRolUsuario;
import pe.edu.sgfa.usuario.service.RyeSesionActual;

@Service
public class RacaCursoService {

    private final RacaCursoRepository cursoRepositoryCrr;
    private final RyeSesionActual sesionActualCrr;
    private final RahpAuditoriaService auditoriaServiceCrr;

    public RacaCursoService(RacaCursoRepository cursoRepositoryCrr, RyeSesionActual sesionActualCrr,
                         RahpAuditoriaService auditoriaServiceCrr) {
        this.cursoRepositoryCrr = cursoRepositoryCrr;
        this.sesionActualCrr = sesionActualCrr;
        this.auditoriaServiceCrr = auditoriaServiceCrr;
    }

    public List<RacaCurso> racaListar() {
        sesionActualCrr.ryeRequerirRol(RyeRolUsuario.ADMIN);
        return cursoRepositoryCrr.findAll();
    }

    @Transactional
    public RacaCurso racaCrear(String codigoCrr, String nombreCrr, int creditosCrr, Integer horasCrr, String tipoCrr) {
        sesionActualCrr.ryeRequerirRol(RyeRolUsuario.ADMIN);
        racaValidarDatos(codigoCrr, nombreCrr, creditosCrr);
        if (cursoRepositoryCrr.existsByCodigo(codigoCrr)) {
            throw new RyeValidacionException("Ya existe un curso con el codigo '" + codigoCrr + "'.");
        }

        RacaCurso cursoCrr = new RacaCurso(codigoCrr, nombreCrr, creditosCrr, horasCrr, tipoCrr);
        cursoRepositoryCrr.save(cursoCrr);
        racaRegistrarAuditoria("CREAR_CURSO", cursoCrr, "codigo=" + codigoCrr);
        return cursoCrr;
    }

    @Transactional
    public RacaCurso racaEditar(Long id, String codigoCrr, String nombreCrr, int creditosCrr, Integer horasCrr, String tipoCrr) {
        sesionActualCrr.ryeRequerirRol(RyeRolUsuario.ADMIN);
        racaValidarDatos(codigoCrr, nombreCrr, creditosCrr);
        RacaCurso cursoCrr = racaObtenerOLanzar(id);
        if (cursoRepositoryCrr.existsByCodigoAndIdNot(codigoCrr, id)) {
            throw new RyeValidacionException("Ya existe un curso con el codigo '" + codigoCrr + "'.");
        }

        cursoCrr.setCodigo(codigoCrr);
        cursoCrr.setNombre(nombreCrr);
        cursoCrr.setCreditos(creditosCrr);
        cursoCrr.setHoras(horasCrr);
        cursoCrr.setTipo(tipoCrr);
        racaRegistrarAuditoria("EDITAR_CURSO", cursoCrr, "codigo=" + codigoCrr);
        return cursoCrr;
    }

    @Transactional
    public RacaCurso racaActivarDesactivar(Long id, boolean activoCrr) {
        sesionActualCrr.ryeRequerirRol(RyeRolUsuario.ADMIN);
        RacaCurso cursoCrr = racaObtenerOLanzar(id);
        cursoCrr.setActivo(activoCrr);
        racaRegistrarAuditoria(activoCrr ? "ACTIVAR_CURSO" : "DESACTIVAR_CURSO", cursoCrr, null);
        return cursoCrr;
    }

    private RacaCurso racaObtenerOLanzar(Long id) {
        return cursoRepositoryCrr.findById(id)
                .orElseThrow(() -> new RyeEntidadNoEncontradaException("Curso no encontrado: id=" + id));
    }

    private void racaValidarDatos(String codigoCrr, String nombreCrr, int creditosCrr) {
        if (codigoCrr == null || codigoCrr.isBlank()) {
            throw new RyeValidacionException("El codigo del curso es obligatorio.");
        }
        if (nombreCrr == null || nombreCrr.isBlank()) {
            throw new RyeValidacionException("El nombre del curso es obligatorio.");
        }
        if (creditosCrr <= 0) {
            throw new RyeValidacionException("Los creditos deben ser mayores que cero.");
        }
    }

    private void racaRegistrarAuditoria(String accionCrr, RacaCurso cursoCrr, String detalleCrr) {
        var actorCrr = sesionActualCrr.ryeObtenerUsuarioActual().orElse(null);
        auditoriaServiceCrr.rahpRegistrar(actorCrr, accionCrr, "cursos", cursoCrr.getId().toString(), detalleCrr);
    }
}
