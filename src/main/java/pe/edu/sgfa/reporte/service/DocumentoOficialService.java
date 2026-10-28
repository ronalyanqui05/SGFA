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

import pe.edu.sgfa.auditoria.service.AuditoriaService;
import pe.edu.sgfa.common.exception.EntidadNoEncontradaException;
import pe.edu.sgfa.common.exception.SgfaException;
import pe.edu.sgfa.common.exception.ValidacionException;
import pe.edu.sgfa.institucion.domain.Institucion;
import pe.edu.sgfa.institucion.service.InstitucionService;
import pe.edu.sgfa.reporte.domain.DocumentoEmitido;
import pe.edu.sgfa.reporte.domain.TipoDocumento;
import pe.edu.sgfa.reporte.repository.DocumentoEmitidoRepository;
import pe.edu.sgfa.usuario.domain.RolUsuario;
import pe.edu.sgfa.usuario.service.SesionActual;

@Service
public class DocumentoOficialService {

    private static final DateTimeFormatter FORMATO_ARCHIVO = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final DocumentoEmitidoRepository repositorio;
    private final ExportadorPdf exportadorPdf;
    private final InstitucionService institucionService;
    private final SesionActual sesionActual;
    private final AuditoriaService auditoriaService;
    private final Path carpeta;

    public DocumentoOficialService(DocumentoEmitidoRepository repositorio, ExportadorPdf exportadorPdf,
                                    InstitucionService institucionService, SesionActual sesionActual,
                                    AuditoriaService auditoriaService, @Value("${sgfa.documents-dir}") String dcmntsDir) {
        this.repositorio = repositorio;
        this.exportadorPdf = exportadorPdf;
        this.institucionService = institucionService;
        this.sesionActual = sesionActual;
        this.auditoriaService = auditoriaService;
        this.carpeta = Path.of(dcmntsDir).resolve("oficiales");
    }

    @Transactional
    public DocumentoEmitido emitir(TipoDocumento tp, TablaReporte tbl) {
        sesionActual.requerirAutenticacion();
        Institucion institucion = institucionService.obtenerPrincipal()
                .orElseThrow(() -> new ValidacionException("Configure primero los datos de la institucion."));

        int an = LocalDate.now().getYear();
        int nmr = repositorio.findTopByTipoAndAnioOrderByNumeroSecuencialDesc(tp, an)
                .map(d -> d.getNumeroSecuencial() + 1).orElse(1);
        String crrltv = String.format("%s-%06d-%d", tp.prefijo(), nmr, an);
        String cdg = codigoDe(tbl, crrltv);

        Path archivo;
        try {
            Files.createDirectories(carpeta);
            archivo = carpeta.resolve(crrltv.toLowerCase(Locale.ROOT) + "_" + LocalDateTime.now().format(FORMATO_ARCHIVO) + ".pdf");
        } catch (IOException e) {
            throw new SgfaException("No se pudo preparar la carpeta de documentos oficiales.", e);
        }
        Path lg = institucion.getLogoPath() == null || institucion.getLogoPath().isBlank() ? null : Path.of(institucion.getLogoPath());
        exportadorPdf.escribir(tbl, institucion.getNombre(), archivo, new ExportadorPdf.Oficial(crrltv, cdg, lg));

        var actr = sesionActual.obtenerUsuarioActual().orElse(null);
        DocumentoEmitido dcmnt = new DocumentoEmitido(tp, crrltv, nmr, an, tbl.titulo(), descripcion(tbl),
                cdg, archivo.toString(), actr);
        repositorio.save(dcmnt);
        auditoriaService.registrar(actr, "EMITIR_DOCUMENTO", "documentos_emitidos", dcmnt.getId().toString(),
                "correlativo=" + crrltv + " archivo=" + archivo.getFileName());
        return dcmnt;
    }

    public List<DocumentoEmitido> listar() {
        sesionActual.requerirAutenticacion();
        return repositorio.findAllByOrderByIdDesc();
    }

    @Transactional
    public DocumentoEmitido anular(Long id, String mtv) {
        sesionActual.requerirRol(RolUsuario.ADMIN);
        if (mtv == null || mtv.isBlank()) {
            throw new ValidacionException("Indique el motivo de la anulacion.");
        }
        DocumentoEmitido dcmnt = repositorio.findById(id)
                .orElseThrow(() -> new EntidadNoEncontradaException("Documento no encontrado: id=" + id));
        dcmnt.anular(mtv);
        repositorio.save(dcmnt);
        auditoriaService.registrar(sesionActual.obtenerUsuarioActual().orElse(null), "ANULAR_DOCUMENTO", "documentos_emitidos",
                id.toString(), "correlativo=" + dcmnt.getCorrelativo() + " motivo=" + mtv);
        return dcmnt;
    }

    public Optional<DocumentoEmitido> verificar(String crrltv, String cdg) {
        sesionActual.requerirAutenticacion();
        if (crrltv == null || cdg == null) {
            return Optional.empty();
        }
        String bscd = cdg.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        return repositorio.findByCorrelativo(crrltv.strip().toUpperCase(Locale.ROOT))
                .filter(d -> d.getCodigoVerificacion().replace("-", "").equals(bscd));
    }

    public Path archivo(DocumentoEmitido dcmnt) {
        return Path.of(dcmnt.getArchivoPath());
    }

    private static String descripcion(TablaReporte tbl) {
        return tbl.subtitulo().stream().filter(l -> !l.startsWith("Emitido:")).reduce((a, b) -> a + " | " + b).orElse("");
    }

    static String codigoDe(TablaReporte tbl, String crrltv) {
        StringBuilder cntnd = new StringBuilder(tbl.titulo()).append('\n').append(crrltv).append('\n');
        tbl.subtitulo().stream().filter(l -> !l.startsWith("Emitido:")).forEach(l -> cntnd.append(l).append('\n'));
        cntnd.append(String.join("|", tbl.columnas())).append('\n');
        tbl.filas().forEach(f -> cntnd.append(String.join("|", f)).append('\n'));
        try {
            byte[] hsh = MessageDigest.getInstance("SHA-256").digest(cntnd.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hsh) {
                hex.append(String.format("%02X", b));
            }
            String dc = hex.substring(0, 12);
            return dc.substring(0, 4) + "-" + dc.substring(4, 8) + "-" + dc.substring(8, 12);
        } catch (NoSuchAlgorithmException e) {
            throw new SgfaException("No se pudo calcular el codigo de verificacion.", e);
        }
    }
}
