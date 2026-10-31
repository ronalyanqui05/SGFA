package pe.edu.sgfa.estudiante.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.sgfa.auditoria.service.RahpAuditoriaService;
import pe.edu.sgfa.common.exception.RyeEntidadNoEncontradaException;
import pe.edu.sgfa.common.exception.RyeValidacionException;
import pe.edu.sgfa.estudiante.domain.YmclEstadoEstudiante;
import pe.edu.sgfa.estudiante.domain.YmclEstudiante;
import pe.edu.sgfa.estudiante.domain.YmclSexo;
import pe.edu.sgfa.estudiante.repository.YmclEstudianteRepository;
import pe.edu.sgfa.usuario.service.RyeSesionActual;

@Service
public class YmclEstudianteService {

    private final YmclEstudianteRepository estudianteRepositoryChp;
    private final RyeSesionActual sesionActualChp;
    private final RahpAuditoriaService auditoriaServiceChp;

    public YmclEstudianteService(YmclEstudianteRepository estudianteRepositoryChp, RyeSesionActual sesionActualChp,
                              RahpAuditoriaService auditoriaServiceChp) {
        this.estudianteRepositoryChp = estudianteRepositoryChp;
        this.sesionActualChp = sesionActualChp;
        this.auditoriaServiceChp = auditoriaServiceChp;
    }

    public List<YmclEstudiante> ymclListar(boolean incluirInactivosChp) {
        sesionActualChp.ryeRequerirAutenticacion();
        return incluirInactivosChp
                ? estudianteRepositoryChp.findAllByOrderByApellidoPaternoAscApellidoMaternoAscNombresAsc()
                : estudianteRepositoryChp.findByEstadoOrderByApellidoPaternoAscApellidoMaternoAscNombresAsc(YmclEstadoEstudiante.ACTIVO);
    }

    public List<YmclEstudiante> ymclBuscar(String terminoChp) {
        sesionActualChp.ryeRequerirAutenticacion();
        if (terminoChp == null || terminoChp.isBlank()) {
            return ymclListar(true);
        }

        List<String> palabrasChp = java.util.Arrays.stream(ymclParaBuscar(terminoChp).split("\\s+")).filter(p -> !p.isEmpty()).toList();
        return ymclListar(true).stream().filter(e -> {
            String datosChp = ymclParaBuscar(e.getNombres() + " " + e.getApellidoPaterno() + " " + e.getApellidoMaterno() + " "
                    + e.getDni() + " " + e.getCodigo());
            return palabrasChp.stream().allMatch(datosChp::contains);
        }).toList();
    }

    static String ymclParaBuscar(String textoChp) {
        if (textoChp == null) {
            return "";
        }
        return java.text.Normalizer.normalize(textoChp, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(java.util.Locale.ROOT).trim();
    }

    @Transactional
    public YmclEstudiante ymclCrear(String codigoChp, String dniChp, String nombresChp, String apellidoPaternoChp, String apellidoMaternoChp,
                             LocalDate fechaNacimientoChp, YmclSexo sexoChp, String direccionChp, String telefonoChp, String correoChp) {
        sesionActualChp.ryeRequerirAutenticacion();
        ymclValidarDatos(nombresChp, apellidoPaternoChp, dniChp);
        ymclValidarDuplicados(codigoChp, dniChp, null);

        YmclEstudiante estudianteChp = new YmclEstudiante(ymclNormalizar(codigoChp), ymclNormalizar(dniChp), nombresChp.trim(),
                apellidoPaternoChp.trim(), ymclNormalizar(apellidoMaternoChp), fechaNacimientoChp, sexoChp,
                ymclNormalizar(direccionChp), ymclNormalizar(telefonoChp), ymclNormalizar(correoChp));
        estudianteRepositoryChp.save(estudianteChp);
        ymclRegistrarAuditoria("CREAR_ESTUDIANTE", estudianteChp, "nombres=" + nombresChp + " " + apellidoPaternoChp);
        return estudianteChp;
    }

    @Transactional
    public YmclEstudiante ymclEditar(Long id, String codigoChp, String dniChp, String nombresChp, String apellidoPaternoChp,
                              String apellidoMaternoChp, LocalDate fechaNacimientoChp, YmclSexo sexoChp, String direccionChp,
                              String telefonoChp, String correoChp) {
        sesionActualChp.ryeRequerirAutenticacion();
        ymclValidarDatos(nombresChp, apellidoPaternoChp, dniChp);
        ymclValidarDuplicados(codigoChp, dniChp, id);

        YmclEstudiante estudianteChp = ymclObtenerOLanzar(id);
        estudianteChp.setCodigo(ymclNormalizar(codigoChp));
        estudianteChp.setDni(ymclNormalizar(dniChp));
        estudianteChp.setNombres(nombresChp.trim());
        estudianteChp.setApellidoPaterno(apellidoPaternoChp.trim());
        estudianteChp.setApellidoMaterno(ymclNormalizar(apellidoMaternoChp));
        estudianteChp.setFechaNacimiento(fechaNacimientoChp);
        estudianteChp.setSexo(sexoChp);
        estudianteChp.setDireccion(ymclNormalizar(direccionChp));
        estudianteChp.setTelefono(ymclNormalizar(telefonoChp));
        estudianteChp.setCorreo(ymclNormalizar(correoChp));
        ymclRegistrarAuditoria("EDITAR_ESTUDIANTE", estudianteChp, null);
        return estudianteChp;
    }

    @Transactional
    public YmclEstudiante ymclCambiarEstado(Long id, YmclEstadoEstudiante estadoChp) {
        sesionActualChp.ryeRequerirAutenticacion();
        YmclEstudiante estudianteChp = ymclObtenerOLanzar(id);
        estudianteChp.setEstado(estadoChp);
        ymclRegistrarAuditoria("CAMBIAR_ESTADO_ESTUDIANTE", estudianteChp, "estado=" + estadoChp);
        return estudianteChp;
    }

    private YmclEstudiante ymclObtenerOLanzar(Long id) {
        return estudianteRepositoryChp.findById(id)
                .orElseThrow(() -> new RyeEntidadNoEncontradaException("Estudiante no encontrado: id=" + id));
    }

    private void ymclValidarDatos(String nombresChp, String apellidoPaternoChp, String dniChp) {
        if (nombresChp == null || nombresChp.isBlank()) {
            throw new RyeValidacionException("Los nombres son obligatorios.");
        }
        if (apellidoPaternoChp == null || apellidoPaternoChp.isBlank()) {
            throw new RyeValidacionException("El apellido paterno es obligatorio.");
        }
        if (dniChp != null && !dniChp.isBlank() && !dniChp.trim().matches("\\d{8}")) {
            throw new RyeValidacionException("El DNI debe tener 8 digitos numericos.");
        }
    }

    private void ymclValidarDuplicados(String codigoChp, String dniChp, Long idActualChp) {
        String codigoNormalizadoChp = ymclNormalizar(codigoChp);
        if (codigoNormalizadoChp != null) {
            boolean duplicadoChp = idActualChp == null
                    ? estudianteRepositoryChp.existsByCodigo(codigoNormalizadoChp)
                    : estudianteRepositoryChp.existsByCodigoAndIdNot(codigoNormalizadoChp, idActualChp);
            if (duplicadoChp) {
                throw new RyeValidacionException("Ya existe un estudiante con el codigo '" + codigoNormalizadoChp + "'.");
            }
        }

        String dniNormalizadoChp = ymclNormalizar(dniChp);
        if (dniNormalizadoChp != null) {
            boolean duplicadoChp = idActualChp == null
                    ? estudianteRepositoryChp.existsByDni(dniNormalizadoChp)
                    : estudianteRepositoryChp.existsByDniAndIdNot(dniNormalizadoChp, idActualChp);
            if (duplicadoChp) {
                throw new RyeValidacionException("Ya existe un estudiante con el DNI '" + dniNormalizadoChp + "'.");
            }
        }
    }

    private String ymclNormalizar(String textoChp) {
        return textoChp == null || textoChp.isBlank() ? null : textoChp.trim();
    }

    private void ymclRegistrarAuditoria(String accionChp, YmclEstudiante estudianteChp, String detalleChp) {
        var actorChp = sesionActualChp.ryeObtenerUsuarioActual().orElse(null);
        auditoriaServiceChp.rahpRegistrar(actorChp, accionChp, "estudiantes", estudianteChp.getId().toString(), detalleChp);
    }
}
