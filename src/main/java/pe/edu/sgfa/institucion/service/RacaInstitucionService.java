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

import pe.edu.sgfa.auditoria.service.RahpAuditoriaService;
import pe.edu.sgfa.common.exception.RyeValidacionException;
import pe.edu.sgfa.institucion.domain.RacaInstitucion;
import pe.edu.sgfa.institucion.repository.RacaInstitucionRepository;
import pe.edu.sgfa.usuario.domain.RyeRolUsuario;
import pe.edu.sgfa.usuario.service.RyeSesionActual;

@Service
public class RacaInstitucionService {

    private static final List<String> EXTENSIONES_LOGO_PERMITIDAS_CRR = List.of(".png", ".jpg", ".jpeg");

    private final RacaInstitucionRepository institucionRepositoryCrr;
    private final RyeSesionActual sesionActualCrr;
    private final RahpAuditoriaService auditoriaServiceCrr;
    private final Path configDirCrr;

    public RacaInstitucionService(RacaInstitucionRepository institucionRepositoryCrr, RyeSesionActual sesionActualCrr,
                               RahpAuditoriaService auditoriaServiceCrr, @Value("${sgfa.config-dir}") String configDirCrr) {
        this.institucionRepositoryCrr = institucionRepositoryCrr;
        this.sesionActualCrr = sesionActualCrr;
        this.auditoriaServiceCrr = auditoriaServiceCrr;
        this.configDirCrr = Path.of(configDirCrr);
    }

    public Optional<RacaInstitucion> racaObtenerPrincipal() {
        return institucionRepositoryCrr.findAll().stream().findFirst();
    }

    @Transactional
    public RacaInstitucion racaGuardar(String nombreCrr, String siglasCrr, String direccionCrr, String telefonoCrr, String correoCrr) {
        sesionActualCrr.ryeRequerirRol(RyeRolUsuario.ADMIN);
        racaValidarNombre(nombreCrr);

        RacaInstitucion institucionCrr = racaObtenerPrincipal().orElseGet(() -> new RacaInstitucion(nombreCrr));
        institucionCrr.setNombre(nombreCrr);
        institucionCrr.setSiglas(siglasCrr);
        institucionCrr.setDireccion(direccionCrr);
        institucionCrr.setTelefono(telefonoCrr);
        institucionCrr.setCorreo(correoCrr);
        institucionRepositoryCrr.save(institucionCrr);

        racaRegistrarAuditoria("CONFIGURAR_INSTITUCION", institucionCrr, "nombre=" + nombreCrr);
        return institucionCrr;
    }

    @Transactional
    public RacaInstitucion racaActualizarLogo(Path archivoOrigenCrr) {
        sesionActualCrr.ryeRequerirRol(RyeRolUsuario.ADMIN);
        String extensionCrr = racaExtensionDe(archivoOrigenCrr)
                .filter(EXTENSIONES_LOGO_PERMITIDAS_CRR::contains)
                .orElseThrow(() -> new RyeValidacionException(
                        "El logo debe ser una imagen " + EXTENSIONES_LOGO_PERMITIDAS_CRR + "."));

        RacaInstitucion institucionCrr = racaObtenerPrincipal()
                .orElseThrow(() -> new RyeValidacionException("Configure primero los datos de la institucion."));

        Path destinoCrr = configDirCrr.resolve("logo" + extensionCrr);
        try {
            Files.copy(archivoOrigenCrr, destinoCrr, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el logo en " + destinoCrr, e);
        }

        institucionCrr.setLogoPath(destinoCrr.toString());
        institucionRepositoryCrr.save(institucionCrr);

        racaRegistrarAuditoria("CAMBIAR_LOGO_INSTITUCION", institucionCrr, "logoPath=" + destinoCrr);
        return institucionCrr;
    }

    private static final long TAMANO_MAXIMO_FONDO_CRR = 15L * 1024 * 1024;
    private static final int ANCHO_MINIMO_FONDO_CRR = 640;
    private static final int ALTO_MINIMO_FONDO_CRR = 360;

    @Transactional
    public RacaInstitucion racaActualizarFondoLogin(Path archivoOrigenCrr) {
        sesionActualCrr.ryeRequerirRol(RyeRolUsuario.ADMIN);
        String extensionCrr = racaExtensionDe(archivoOrigenCrr)
                .filter(EXTENSIONES_LOGO_PERMITIDAS_CRR::contains)
                .orElseThrow(() -> new RyeValidacionException(
                        "El fondo debe ser una imagen " + EXTENSIONES_LOGO_PERMITIDAS_CRR + "."));
        try {
            if (Files.size(archivoOrigenCrr) > TAMANO_MAXIMO_FONDO_CRR) {
                throw new RyeValidacionException("La imagen de fondo pesa mas de 15 MB: use una mas liviana.");
            }
            java.awt.image.BufferedImage imagenCrr = javax.imageio.ImageIO.read(archivoOrigenCrr.toFile());
            if (imagenCrr == null) {
                throw new RyeValidacionException("El archivo no es una imagen valida.");
            }
            if (imagenCrr.getWidth() < ANCHO_MINIMO_FONDO_CRR || imagenCrr.getHeight() < ALTO_MINIMO_FONDO_CRR) {
                throw new RyeValidacionException("La imagen de fondo es muy chica (" + imagenCrr.getWidth() + " x "
                        + imagenCrr.getHeight() + "): debe medir al menos " + ANCHO_MINIMO_FONDO_CRR + " x " + ALTO_MINIMO_FONDO_CRR + " px.");
            }
        } catch (IOException e) {
            throw new RyeValidacionException("No se pudo leer la imagen de fondo.");
        }

        RacaInstitucion institucionCrr = racaObtenerPrincipal()
                .orElseThrow(() -> new RyeValidacionException("Configure primero los datos de la institucion."));
        racaBorrarArchivosDeFondo();
        Path destinoCrr = configDirCrr.resolve("fondo-login" + extensionCrr);
        try {
            Files.copy(archivoOrigenCrr, destinoCrr, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el fondo en " + destinoCrr, e);
        }
        institucionCrr.setFondoLoginPath(destinoCrr.toString());
        institucionRepositoryCrr.save(institucionCrr);
        racaRegistrarAuditoria("CAMBIAR_FONDO_LOGIN", institucionCrr, "fondoLoginPath=" + destinoCrr);
        return institucionCrr;
    }

    @Transactional
    public RacaInstitucion racaQuitarFondoLogin() {
        sesionActualCrr.ryeRequerirRol(RyeRolUsuario.ADMIN);
        RacaInstitucion institucionCrr = racaObtenerPrincipal()
                .orElseThrow(() -> new RyeValidacionException("Configure primero los datos de la institucion."));
        racaBorrarArchivosDeFondo();
        institucionCrr.setFondoLoginPath(null);
        institucionRepositoryCrr.save(institucionCrr);
        racaRegistrarAuditoria("QUITAR_FONDO_LOGIN", institucionCrr, "fondo por defecto");
        return institucionCrr;
    }

    private void racaBorrarArchivosDeFondo() {
        for (String extensionCrr : EXTENSIONES_LOGO_PERMITIDAS_CRR) {
            try {
                Files.deleteIfExists(configDirCrr.resolve("fondo-login" + extensionCrr));
            } catch (IOException e) {
                throw new UncheckedIOException("No se pudo reemplazar el fondo anterior.", e);
            }
        }
    }

    private Optional<String> racaExtensionDe(Path archivoCrr) {
        String nombreArchivoCrr = archivoCrr.getFileName().toString().toLowerCase(Locale.ROOT);
        int puntoIdxCrr = nombreArchivoCrr.lastIndexOf('.');
        return puntoIdxCrr < 0 ? Optional.empty() : Optional.of(nombreArchivoCrr.substring(puntoIdxCrr));
    }

    private void racaValidarNombre(String nombreCrr) {
        if (nombreCrr == null || nombreCrr.isBlank()) {
            throw new RyeValidacionException("El nombre de la institucion es obligatorio.");
        }
    }

    private void racaRegistrarAuditoria(String accionCrr, RacaInstitucion institucionCrr, String detalleCrr) {
        var actorCrr = sesionActualCrr.ryeObtenerUsuarioActual().orElse(null);
        auditoriaServiceCrr.rahpRegistrar(actorCrr, accionCrr, "instituciones", institucionCrr.getId().toString(), detalleCrr);
    }
}
