package pe.edu.sgfa.institucion.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.sgfa.auditoria.service.AuditoriaService;
import pe.edu.sgfa.common.exception.ValidacionException;
import pe.edu.sgfa.institucion.domain.Institucion;
import pe.edu.sgfa.institucion.repository.InstitucionRepository;
import pe.edu.sgfa.usuario.domain.RolUsuario;
import pe.edu.sgfa.usuario.service.SesionActual;

@Service
public class InstitucionService {

    private static final List<String> EXTENSIONES_LOGO_PERMITIDAS = List.of(".png", ".jpg", ".jpeg");

    private final InstitucionRepository institucionRepository;
    private final SesionActual sesionActual;
    private final AuditoriaService auditoriaService;
    private final Path configDir;

    public InstitucionService(InstitucionRepository institucionRepository, SesionActual sesionActual,
                               AuditoriaService auditoriaService, @Value("${sgfa.config-dir}") String configDir) {
        this.institucionRepository = institucionRepository;
        this.sesionActual = sesionActual;
        this.auditoriaService = auditoriaService;
        this.configDir = Path.of(configDir);
    }

    public Optional<Institucion> obtenerPrincipal() {
        return institucionRepository.findAll().stream().findFirst();
    }

    @Transactional
    public Institucion guardar(String nmbr, String sgls, String drccn, String tlfn, String crr) {
        sesionActual.requerirRol(RolUsuario.ADMIN);
        validarNombre(nmbr);

        Institucion institucion = obtenerPrincipal().orElseGet(() -> new Institucion(nmbr));
        institucion.setNombre(nmbr);
        institucion.setSiglas(sgls);
        institucion.setDireccion(drccn);
        institucion.setTelefono(tlfn);
        institucion.setCorreo(crr);
        institucionRepository.save(institucion);

        registrarAuditoria("CONFIGURAR_INSTITUCION", institucion, "nombre=" + nmbr);
        return institucion;
    }

    @Transactional
    public Institucion actualizarLogo(Path archvOrgn) {
        sesionActual.requerirRol(RolUsuario.ADMIN);
        String extnsn = extensionDe(archvOrgn)
                .filter(EXTENSIONES_LOGO_PERMITIDAS::contains)
                .orElseThrow(() -> new ValidacionException(
                        "El logo debe ser una imagen " + EXTENSIONES_LOGO_PERMITIDAS + "."));

        Institucion institucion = obtenerPrincipal()
                .orElseThrow(() -> new ValidacionException("Configure primero los datos de la institucion."));

        Path dstn = configDir.resolve("logo" + extnsn);
        try {
            Files.copy(archvOrgn, dstn, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el logo en " + dstn, e);
        }

        institucion.setLogoPath(dstn.toString());
        institucionRepository.save(institucion);

        registrarAuditoria("CAMBIAR_LOGO_INSTITUCION", institucion, "logoPath=" + dstn);
        return institucion;
    }

    private static final long TAMANO_MAXIMO_FONDO = 15L * 1024 * 1024;
    private static final int ANCHO_MINIMO_FONDO = 640;
    private static final int ALTO_MINIMO_FONDO = 360;

    @Transactional
    public Institucion actualizarFondoLogin(Path archvOrgn) {
        sesionActual.requerirRol(RolUsuario.ADMIN);
        String extnsn = extensionDe(archvOrgn)
                .filter(EXTENSIONES_LOGO_PERMITIDAS::contains)
                .orElseThrow(() -> new ValidacionException(
                        "El fondo debe ser una imagen " + EXTENSIONES_LOGO_PERMITIDAS + "."));
        try {
            if (Files.size(archvOrgn) > TAMANO_MAXIMO_FONDO) {
                throw new ValidacionException("La imagen de fondo pesa mas de 15 MB: use una mas liviana.");
            }
            java.awt.image.BufferedImage imagen = javax.imageio.ImageIO.read(archvOrgn.toFile());
            if (imagen == null) {
                throw new ValidacionException("El archivo no es una imagen valida.");
            }
            if (imagen.getWidth() < ANCHO_MINIMO_FONDO || imagen.getHeight() < ALTO_MINIMO_FONDO) {
                throw new ValidacionException("La imagen de fondo es muy chica (" + imagen.getWidth() + " x "
                        + imagen.getHeight() + "): debe medir al menos " + ANCHO_MINIMO_FONDO + " x " + ALTO_MINIMO_FONDO + " px.");
            }
        } catch (IOException e) {
            throw new ValidacionException("No se pudo leer la imagen de fondo.");
        }

        Institucion institucion = obtenerPrincipal()
                .orElseThrow(() -> new ValidacionException("Configure primero los datos de la institucion."));
        borrarArchivosDeFondo();
        Path dstn = configDir.resolve("fondo-login" + extnsn);
        try {
            Files.copy(archvOrgn, dstn, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el fondo en " + dstn, e);
        }
        institucion.setFondoLoginPath(dstn.toString());
        institucionRepository.save(institucion);
        registrarAuditoria("CAMBIAR_FONDO_LOGIN", institucion, "fondoLoginPath=" + dstn);
        return institucion;
    }

    @Transactional
    public Institucion quitarFondoLogin() {
        sesionActual.requerirRol(RolUsuario.ADMIN);
        Institucion institucion = obtenerPrincipal()
                .orElseThrow(() -> new ValidacionException("Configure primero los datos de la institucion."));
        borrarArchivosDeFondo();
        institucion.setFondoLoginPath(null);
        institucionRepository.save(institucion);
        registrarAuditoria("QUITAR_FONDO_LOGIN", institucion, "fondo por defecto");
        return institucion;
    }

    private void borrarArchivosDeFondo() {
        for (String extnsn : EXTENSIONES_LOGO_PERMITIDAS) {
            try {
                Files.deleteIfExists(configDir.resolve("fondo-login" + extnsn));
            } catch (IOException e) {
                throw new UncheckedIOException("No se pudo reemplazar el fondo anterior.", e);
            }
        }
    }

    private Optional<String> extensionDe(Path archv) {
        String nmbrArchv = archv.getFileName().toString().toLowerCase(Locale.ROOT);
        int pntIdx = nmbrArchv.lastIndexOf('.');
        return pntIdx < 0 ? Optional.empty() : Optional.of(nmbrArchv.substring(pntIdx));
    }

    private void validarNombre(String nmbr) {
        if (nmbr == null || nmbr.isBlank()) {
            throw new ValidacionException("El nombre de la institucion es obligatorio.");
        }
    }

    private void registrarAuditoria(String accn, Institucion institucion, String dtll) {
        var actr = sesionActual.obtenerUsuarioActual().orElse(null);
        auditoriaService.registrar(actr, accn, "instituciones", institucion.getId().toString(), dtll);
    }
}
