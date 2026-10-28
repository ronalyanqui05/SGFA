package pe.edu.sgfa.estudiante.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.sgfa.auditoria.service.AuditoriaService;
import pe.edu.sgfa.common.exception.EntidadNoEncontradaException;
import pe.edu.sgfa.common.exception.ValidacionException;
import pe.edu.sgfa.estudiante.domain.EstadoEstudiante;
import pe.edu.sgfa.estudiante.domain.Estudiante;
import pe.edu.sgfa.estudiante.domain.Sexo;
import pe.edu.sgfa.estudiante.repository.EstudianteRepository;
import pe.edu.sgfa.usuario.service.SesionActual;

@Service
public class EstudianteService {

    private final EstudianteRepository estudianteRepository;
    private final SesionActual sesionActual;
    private final AuditoriaService auditoriaService;

    public EstudianteService(EstudianteRepository estudianteRepository, SesionActual sesionActual,
                              AuditoriaService auditoriaService) {
        this.estudianteRepository = estudianteRepository;
        this.sesionActual = sesionActual;
        this.auditoriaService = auditoriaService;
    }

    public List<Estudiante> listar(boolean inclrInctvs) {
        sesionActual.requerirAutenticacion();
        return inclrInctvs
                ? estudianteRepository.findAllByOrderByApellidoPaternoAscApellidoMaternoAscNombresAsc()
                : estudianteRepository.findByEstadoOrderByApellidoPaternoAscApellidoMaternoAscNombresAsc(EstadoEstudiante.ACTIVO);
    }

    public List<Estudiante> buscar(String trmn) {
        sesionActual.requerirAutenticacion();
        if (trmn == null || trmn.isBlank()) {
            return listar(true);
        }

        List<String> plbrs = java.util.Arrays.stream(paraBuscar(trmn).split("\\s+")).filter(p -> !p.isEmpty()).toList();
        return listar(true).stream().filter(e -> {
            String dts = paraBuscar(e.getNombres() + " " + e.getApellidoPaterno() + " " + e.getApellidoMaterno() + " "
                    + e.getDni() + " " + e.getCodigo());
            return plbrs.stream().allMatch(dts::contains);
        }).toList();
    }

    static String paraBuscar(String txt) {
        if (txt == null) {
            return "";
        }
        return java.text.Normalizer.normalize(txt, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(java.util.Locale.ROOT).trim();
    }

    @Transactional
    public Estudiante crear(String cdg, String dni, String nmbrs, String aplldPtrn, String aplldMtrn,
                             LocalDate fchNcmnt, Sexo sx, String drccn, String tlfn, String crr) {
        sesionActual.requerirAutenticacion();
        validarDatos(nmbrs, aplldPtrn, dni);
        validarDuplicados(cdg, dni, null);

        Estudiante estudiante = new Estudiante(normalizar(cdg), normalizar(dni), nmbrs.trim(),
                aplldPtrn.trim(), normalizar(aplldMtrn), fchNcmnt, sx,
                normalizar(drccn), normalizar(tlfn), normalizar(crr));
        estudianteRepository.save(estudiante);
        registrarAuditoria("CREAR_ESTUDIANTE", estudiante, "nombres=" + nmbrs + " " + aplldPtrn);
        return estudiante;
    }

    @Transactional
    public Estudiante editar(Long id, String cdg, String dni, String nmbrs, String aplldPtrn,
                              String aplldMtrn, LocalDate fchNcmnt, Sexo sx, String drccn,
                              String tlfn, String crr) {
        sesionActual.requerirAutenticacion();
        validarDatos(nmbrs, aplldPtrn, dni);
        validarDuplicados(cdg, dni, id);

        Estudiante estudiante = obtenerOLanzar(id);
        estudiante.setCodigo(normalizar(cdg));
        estudiante.setDni(normalizar(dni));
        estudiante.setNombres(nmbrs.trim());
        estudiante.setApellidoPaterno(aplldPtrn.trim());
        estudiante.setApellidoMaterno(normalizar(aplldMtrn));
        estudiante.setFechaNacimiento(fchNcmnt);
        estudiante.setSexo(sx);
        estudiante.setDireccion(normalizar(drccn));
        estudiante.setTelefono(normalizar(tlfn));
        estudiante.setCorreo(normalizar(crr));
        registrarAuditoria("EDITAR_ESTUDIANTE", estudiante, null);
        return estudiante;
    }

    @Transactional
    public Estudiante cambiarEstado(Long id, EstadoEstudiante estd) {
        sesionActual.requerirAutenticacion();
        Estudiante estudiante = obtenerOLanzar(id);
        estudiante.setEstado(estd);
        registrarAuditoria("CAMBIAR_ESTADO_ESTUDIANTE", estudiante, "estado=" + estd);
        return estudiante;
    }

    private Estudiante obtenerOLanzar(Long id) {
        return estudianteRepository.findById(id)
                .orElseThrow(() -> new EntidadNoEncontradaException("Estudiante no encontrado: id=" + id));
    }

    private void validarDatos(String nmbrs, String aplldPtrn, String dni) {
        if (nmbrs == null || nmbrs.isBlank()) {
            throw new ValidacionException("Los nombres son obligatorios.");
        }
        if (aplldPtrn == null || aplldPtrn.isBlank()) {
            throw new ValidacionException("El apellido paterno es obligatorio.");
        }
        if (dni != null && !dni.isBlank() && !dni.trim().matches("\\d{8}")) {
            throw new ValidacionException("El DNI debe tener 8 digitos numericos.");
        }
    }

    private void validarDuplicados(String cdg, String dni, Long idActl) {
        String cdgNrmlzd = normalizar(cdg);
        if (cdgNrmlzd != null) {
            boolean dplcd = idActl == null
                    ? estudianteRepository.existsByCodigo(cdgNrmlzd)
                    : estudianteRepository.existsByCodigoAndIdNot(cdgNrmlzd, idActl);
            if (dplcd) {
                throw new ValidacionException("Ya existe un estudiante con el codigo '" + cdgNrmlzd + "'.");
            }
        }

        String dniNrmlzd = normalizar(dni);
        if (dniNrmlzd != null) {
            boolean dplcd = idActl == null
                    ? estudianteRepository.existsByDni(dniNrmlzd)
                    : estudianteRepository.existsByDniAndIdNot(dniNrmlzd, idActl);
            if (dplcd) {
                throw new ValidacionException("Ya existe un estudiante con el DNI '" + dniNrmlzd + "'.");
            }
        }
    }

    private String normalizar(String txt) {
        return txt == null || txt.isBlank() ? null : txt.trim();
    }

    private void registrarAuditoria(String accn, Estudiante estudiante, String dtll) {
        var actr = sesionActual.obtenerUsuarioActual().orElse(null);
        auditoriaService.registrar(actr, accn, "estudiantes", estudiante.getId().toString(), dtll);
    }
}
