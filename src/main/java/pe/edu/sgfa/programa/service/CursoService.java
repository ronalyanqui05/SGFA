package pe.edu.sgfa.programa.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.sgfa.auditoria.service.AuditoriaService;
import pe.edu.sgfa.common.exception.EntidadNoEncontradaException;
import pe.edu.sgfa.common.exception.ValidacionException;
import pe.edu.sgfa.programa.domain.Curso;
import pe.edu.sgfa.programa.repository.CursoRepository;
import pe.edu.sgfa.usuario.domain.RolUsuario;
import pe.edu.sgfa.usuario.service.SesionActual;

@Service
public class CursoService {

    private final CursoRepository cursoRepository;
    private final SesionActual sesionActual;
    private final AuditoriaService auditoriaService;

    public CursoService(CursoRepository cursoRepository, SesionActual sesionActual,
                         AuditoriaService auditoriaService) {
        this.cursoRepository = cursoRepository;
        this.sesionActual = sesionActual;
        this.auditoriaService = auditoriaService;
    }

    public List<Curso> listar() {
        sesionActual.requerirRol(RolUsuario.ADMIN);
        return cursoRepository.findAll();
    }

    @Transactional
    public Curso crear(String cdg, String nmbr, int crdts, Integer hrs, String tp) {
        sesionActual.requerirRol(RolUsuario.ADMIN);
        validarDatos(cdg, nmbr, crdts);
        if (cursoRepository.existsByCodigo(cdg)) {
            throw new ValidacionException("Ya existe un curso con el codigo '" + cdg + "'.");
        }

        Curso crs = new Curso(cdg, nmbr, crdts, hrs, tp);
        cursoRepository.save(crs);
        registrarAuditoria("CREAR_CURSO", crs, "codigo=" + cdg);
        return crs;
    }

    @Transactional
    public Curso editar(Long id, String cdg, String nmbr, int crdts, Integer hrs, String tp) {
        sesionActual.requerirRol(RolUsuario.ADMIN);
        validarDatos(cdg, nmbr, crdts);
        Curso crs = obtenerOLanzar(id);
        if (cursoRepository.existsByCodigoAndIdNot(cdg, id)) {
            throw new ValidacionException("Ya existe un curso con el codigo '" + cdg + "'.");
        }

        crs.setCodigo(cdg);
        crs.setNombre(nmbr);
        crs.setCreditos(crdts);
        crs.setHoras(hrs);
        crs.setTipo(tp);
        registrarAuditoria("EDITAR_CURSO", crs, "codigo=" + cdg);
        return crs;
    }

    @Transactional
    public Curso activarDesactivar(Long id, boolean actv) {
        sesionActual.requerirRol(RolUsuario.ADMIN);
        Curso crs = obtenerOLanzar(id);
        crs.setActivo(actv);
        registrarAuditoria(actv ? "ACTIVAR_CURSO" : "DESACTIVAR_CURSO", crs, null);
        return crs;
    }

    private Curso obtenerOLanzar(Long id) {
        return cursoRepository.findById(id)
                .orElseThrow(() -> new EntidadNoEncontradaException("Curso no encontrado: id=" + id));
    }

    private void validarDatos(String cdg, String nmbr, int crdts) {
        if (cdg == null || cdg.isBlank()) {
            throw new ValidacionException("El codigo del curso es obligatorio.");
        }
        if (nmbr == null || nmbr.isBlank()) {
            throw new ValidacionException("El nombre del curso es obligatorio.");
        }
        if (crdts <= 0) {
            throw new ValidacionException("Los creditos deben ser mayores que cero.");
        }
    }

    private void registrarAuditoria(String accn, Curso crs, String dtll) {
        var actr = sesionActual.obtenerUsuarioActual().orElse(null);
        auditoriaService.registrar(actr, accn, "cursos", crs.getId().toString(), dtll);
    }
}
