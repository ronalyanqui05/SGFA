package pe.edu.sgfa.programa.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.sgfa.auditoria.service.AuditoriaService;
import pe.edu.sgfa.common.exception.EntidadNoEncontradaException;
import pe.edu.sgfa.common.exception.ValidacionException;
import pe.edu.sgfa.institucion.domain.Institucion;
import pe.edu.sgfa.institucion.service.InstitucionService;
import pe.edu.sgfa.programa.domain.ProgramaEstudio;
import pe.edu.sgfa.programa.repository.ProgramaEstudioRepository;
import pe.edu.sgfa.usuario.domain.RolUsuario;
import pe.edu.sgfa.usuario.service.SesionActual;

@Service
public class ProgramaEstudioService {

    private final ProgramaEstudioRepository programaEstudioRepository;
    private final InstitucionService institucionService;
    private final SesionActual sesionActual;
    private final AuditoriaService auditoriaService;

    public ProgramaEstudioService(ProgramaEstudioRepository programaEstudioRepository,
                                   InstitucionService institucionService, SesionActual sesionActual,
                                   AuditoriaService auditoriaService) {
        this.programaEstudioRepository = programaEstudioRepository;
        this.institucionService = institucionService;
        this.sesionActual = sesionActual;
        this.auditoriaService = auditoriaService;
    }

    public List<ProgramaEstudio> listar() {
        sesionActual.requerirRol(RolUsuario.ADMIN);
        return programaEstudioRepository.findAll();
    }

    @Transactional
    public ProgramaEstudio crear(String cdg, String nmbr, String dscrpcn, int drcnCcls) {
        sesionActual.requerirRol(RolUsuario.ADMIN);
        Institucion institucion = institucionPrincipalOLanzar();
        validarDatos(cdg, nmbr, drcnCcls);
        if (programaEstudioRepository.existsByInstitucionIdAndCodigo(institucion.getId(), cdg)) {
            throw new ValidacionException("Ya existe un programa con el codigo '" + cdg + "'.");
        }

        ProgramaEstudio programa = new ProgramaEstudio(institucion, cdg, nmbr, dscrpcn, drcnCcls);
        programaEstudioRepository.save(programa);
        registrarAuditoria("CREAR_PROGRAMA", programa, "codigo=" + cdg);
        return programa;
    }

    @Transactional
    public ProgramaEstudio editar(Long id, String cdg, String nmbr, String dscrpcn, int drcnCcls) {
        sesionActual.requerirRol(RolUsuario.ADMIN);
        validarDatos(cdg, nmbr, drcnCcls);
        ProgramaEstudio programa = obtenerOLanzar(id);
        if (programaEstudioRepository.existsByInstitucionIdAndCodigoAndIdNot(
                programa.getInstitucion().getId(), cdg, id)) {
            throw new ValidacionException("Ya existe un programa con el codigo '" + cdg + "'.");
        }

        programa.setCodigo(cdg);
        programa.setNombre(nmbr);
        programa.setDescripcion(dscrpcn);
        programa.setDuracionCiclos(drcnCcls);
        registrarAuditoria("EDITAR_PROGRAMA", programa, "codigo=" + cdg);
        return programa;
    }

    @Transactional
    public ProgramaEstudio activarDesactivar(Long id, boolean actv) {
        sesionActual.requerirRol(RolUsuario.ADMIN);
        ProgramaEstudio programa = obtenerOLanzar(id);
        programa.setActivo(actv);
        registrarAuditoria(actv ? "ACTIVAR_PROGRAMA" : "DESACTIVAR_PROGRAMA", programa, null);
        return programa;
    }

    private Institucion institucionPrincipalOLanzar() {
        return institucionService.obtenerPrincipal()
                .orElseThrow(() -> new ValidacionException("Configure primero los datos de la institucion."));
    }

    private ProgramaEstudio obtenerOLanzar(Long id) {
        return programaEstudioRepository.findById(id)
                .orElseThrow(() -> new EntidadNoEncontradaException("Programa no encontrado: id=" + id));
    }

    private void validarDatos(String cdg, String nmbr, int drcnCcls) {
        if (cdg == null || cdg.isBlank()) {
            throw new ValidacionException("El codigo del programa es obligatorio.");
        }
        if (nmbr == null || nmbr.isBlank()) {
            throw new ValidacionException("El nombre del programa es obligatorio.");
        }
        if (drcnCcls <= 0) {
            throw new ValidacionException("La duracion en ciclos debe ser mayor que cero.");
        }
    }

    private void registrarAuditoria(String accn, ProgramaEstudio programa, String dtll) {
        var actr = sesionActual.obtenerUsuarioActual().orElse(null);
        auditoriaService.registrar(actr, accn, "programas_estudio", programa.getId().toString(), dtll);
    }
}
