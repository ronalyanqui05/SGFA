package pe.edu.sgfa.reporte.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.sgfa.auditoria.service.RahpAuditoriaService;
import pe.edu.sgfa.common.exception.RyeEntidadNoEncontradaException;
import pe.edu.sgfa.common.exception.RyeSgfaException;
import pe.edu.sgfa.common.exception.RyeValidacionException;
import pe.edu.sgfa.institucion.domain.RacaInstitucion;
import pe.edu.sgfa.institucion.service.RacaInstitucionService;
import pe.edu.sgfa.reporte.domain.CphDocumentoEmitido;
import pe.edu.sgfa.reporte.domain.CphTipoDocumento;
import pe.edu.sgfa.reporte.repository.CphDocumentoEmitidoRepository;
import pe.edu.sgfa.usuario.domain.RyeRolUsuario;
import pe.edu.sgfa.usuario.service.RyeSesionActual;

@Service
public class CphDocumentoOficialService {

    private static final DateTimeFormatter FORMATO_ARCHIVO_PCH = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final CphDocumentoEmitidoRepository repositorioPch;
    private final CphExportadorPdf exportadorPdfPch;
    private final RacaInstitucionService institucionServicePch;
    private final RyeSesionActual sesionActualPch;
    private final RahpAuditoriaService auditoriaServicePch;
    private final Path carpetaPch;

    public CphDocumentoOficialService(CphDocumentoEmitidoRepository repositorioPch, CphExportadorPdf exportadorPdfPch,
                                    RacaInstitucionService institucionServicePch, RyeSesionActual sesionActualPch,
                                    RahpAuditoriaService auditoriaServicePch, @Value("${sgfa.documents-dir}") String documentsDirPch) {
        this.repositorioPch = repositorioPch;
        this.exportadorPdfPch = exportadorPdfPch;
        this.institucionServicePch = institucionServicePch;
        this.sesionActualPch = sesionActualPch;
        this.auditoriaServicePch = auditoriaServicePch;
        this.carpetaPch = Path.of(documentsDirPch).resolve("oficiales");
    }

    @Transactional
    public CphDocumentoEmitido cphEmitir(CphTipoDocumento tipoPch, CphTablaReporte tablaPch) {
        sesionActualPch.ryeRequerirAutenticacion();
        RacaInstitucion institucionPch = institucionServicePch.racaObtenerPrincipal()
                .orElseThrow(() -> new RyeValidacionException("Configure primero los datos de la institucion."));

        int anioPch = LocalDate.now().getYear();
        int numeroPch = repositorioPch.findTopByTipoAndAnioOrderByNumeroSecuencialDesc(tipoPch, anioPch)
                .map(d -> d.getNumeroSecuencial() + 1).orElse(1);
        String correlativoPch = String.format("%s-%06d-%d", tipoPch.cphPrefijo(), numeroPch, anioPch);
        String codigoPch = cphCodigoDe(tablaPch, correlativoPch);

        Path archivoPch;
        try {
            Files.createDirectories(carpetaPch);
            archivoPch = carpetaPch.resolve(correlativoPch.toLowerCase(Locale.ROOT) + "_" + LocalDateTime.now().format(FORMATO_ARCHIVO_PCH) + ".pdf");
        } catch (IOException e) {
            throw new RyeSgfaException("No se pudo preparar la carpeta de documentos oficiales.", e);
        }
        Path logoPch = institucionPch.getLogoPath() == null || institucionPch.getLogoPath().isBlank() ? null : Path.of(institucionPch.getLogoPath());
        exportadorPdfPch.cphEscribir(tablaPch, institucionPch.getNombre(), archivoPch, new CphExportadorPdf.CphOficial(correlativoPch, codigoPch, logoPch));

        var actorPch = sesionActualPch.ryeObtenerUsuarioActual().orElse(null);
        CphDocumentoEmitido documentoPch = new CphDocumentoEmitido(tipoPch, correlativoPch, numeroPch, anioPch, tablaPch.titulo(), cphDescripcion(tablaPch),
                codigoPch, archivoPch.toString(), actorPch);
        repositorioPch.save(documentoPch);
        auditoriaServicePch.rahpRegistrar(actorPch, "EMITIR_DOCUMENTO", "documentos_emitidos", documentoPch.getId().toString(),
                "correlativo=" + correlativoPch + " archivo=" + archivoPch.getFileName());
        return documentoPch;
    }

    public List<CphDocumentoEmitido> cphListar() {
        sesionActualPch.ryeRequerirAutenticacion();
        return repositorioPch.findAllByOrderByIdDesc();
    }

    @Transactional
    public CphDocumentoEmitido cphAnular(Long id, String motivoPch) {
        sesionActualPch.ryeRequerirRol(RyeRolUsuario.ADMIN);
        if (motivoPch == null || motivoPch.isBlank()) {
            throw new RyeValidacionException("Indique el motivo de la anulacion.");
        }
        CphDocumentoEmitido documentoPch = repositorioPch.findById(id)
                .orElseThrow(() -> new RyeEntidadNoEncontradaException("Documento no encontrado: id=" + id));
        documentoPch.cphAnular(motivoPch);
        repositorioPch.save(documentoPch);
        auditoriaServicePch.rahpRegistrar(sesionActualPch.ryeObtenerUsuarioActual().orElse(null), "ANULAR_DOCUMENTO", "documentos_emitidos",
                id.toString(), "correlativo=" + documentoPch.getCorrelativo() + " motivo=" + motivoPch);
        return documentoPch;
    }

    public Optional<CphDocumentoEmitido> cphVerificar(String correlativoPch, String codigoPch) {
        sesionActualPch.ryeRequerirAutenticacion();
        if (correlativoPch == null || codigoPch == null) {
            return Optional.empty();
        }
        String buscadoPch = codigoPch.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        return repositorioPch.findByCorrelativo(correlativoPch.strip().toUpperCase(Locale.ROOT))
                .filter(d -> d.getCodigoVerificacion().replace("-", "").equals(buscadoPch));
    }

    public Path cphArchivo(CphDocumentoEmitido documentoPch) {
        return Path.of(documentoPch.getArchivoPath());
    }

    private static String cphDescripcion(CphTablaReporte tablaPch) {
        return tablaPch.subtitulo().stream().filter(l -> !l.startsWith("Emitido:")).reduce((a, b) -> a + " | " + b).orElse("");
    }

    static String cphCodigoDe(CphTablaReporte tablaPch, String correlativoPch) {
        StringBuilder contenidoPch = new StringBuilder(tablaPch.titulo()).append('\n').append(correlativoPch).append('\n');
        tablaPch.subtitulo().stream().filter(l -> !l.startsWith("Emitido:")).forEach(l -> contenidoPch.append(l).append('\n'));
        contenidoPch.append(String.join("|", tablaPch.columnas())).append('\n');
        tablaPch.filas().forEach(f -> contenidoPch.append(String.join("|", f)).append('\n'));
        try {
            byte[] hashPch = MessageDigest.getInstance("SHA-256").digest(contenidoPch.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexPch = new StringBuilder();
            for (byte b : hashPch) {
                hexPch.append(String.format("%02X", b));
            }
            String docePch = hexPch.substring(0, 12);
            return docePch.substring(0, 4) + "-" + docePch.substring(4, 8) + "-" + docePch.substring(8, 12);
        } catch (NoSuchAlgorithmException e) {
            throw new RyeSgfaException("No se pudo calcular el codigo de verificacion.", e);
        }
    }
}
