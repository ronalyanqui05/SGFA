package pe.edu.sgfa.ficha.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.sgfa.auditoria.service.RahpAuditoriaService;
import pe.edu.sgfa.common.exception.RyeEntidadNoEncontradaException;
import pe.edu.sgfa.common.exception.RyeSgfaException;
import pe.edu.sgfa.common.exception.RyeValidacionException;
import pe.edu.sgfa.estudiante.domain.YmclEstudiante;
import pe.edu.sgfa.ficha.domain.RacaFichaGenerada;
import pe.edu.sgfa.ficha.repository.RacaFichaGeneradaRepository;
import pe.edu.sgfa.institucion.domain.RacaInstitucion;
import pe.edu.sgfa.institucion.service.RacaInstitucionService;
import pe.edu.sgfa.nota.service.YmclFilaRegistro;
import pe.edu.sgfa.nota.service.YmclRegistroAcademicoService;
import pe.edu.sgfa.nota.service.YmclResultadoPromedio;
import pe.edu.sgfa.programa.domain.RacaCiclo;
import pe.edu.sgfa.programa.domain.RacaCurso;
import pe.edu.sgfa.programa.service.RacaCicloService;
import pe.edu.sgfa.trayectoria.domain.YmclTrayectoria;
import pe.edu.sgfa.trayectoria.repository.YmclTrayectoriaRepository;
import pe.edu.sgfa.usuario.domain.RyeRolUsuario;
import pe.edu.sgfa.usuario.service.RyeSesionActual;

@Service
public class RacaFichaService {

    private static final DateTimeFormatter FORMATO_ARCHIVO_CRR = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter FORMATO_FECHA_FICHA_CRR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final YmclTrayectoriaRepository trayectoriaRepositoryCrr;
    private final RacaCicloService cicloServiceCrr;
    private final YmclRegistroAcademicoService registroAcademicoServiceCrr;
    private final RacaInstitucionService institucionServiceCrr;
    private final RacaFichaGeneradaRepository fichaGeneradaRepositoryCrr;
    private final RyeSesionActual sesionActualCrr;
    private final RahpAuditoriaService auditoriaServiceCrr;
    private final Path documentsDirCrr;

    public RacaFichaService(YmclTrayectoriaRepository trayectoriaRepositoryCrr, RacaCicloService cicloServiceCrr,
                         YmclRegistroAcademicoService registroAcademicoServiceCrr, RacaInstitucionService institucionServiceCrr,
                         RacaFichaGeneradaRepository fichaGeneradaRepositoryCrr, RyeSesionActual sesionActualCrr,
                         RahpAuditoriaService auditoriaServiceCrr,
                         @Value("${sgfa.documents-dir}") String documentsDirCrr) {
        this.trayectoriaRepositoryCrr = trayectoriaRepositoryCrr;
        this.cicloServiceCrr = cicloServiceCrr;
        this.registroAcademicoServiceCrr = registroAcademicoServiceCrr;
        this.institucionServiceCrr = institucionServiceCrr;
        this.fichaGeneradaRepositoryCrr = fichaGeneradaRepositoryCrr;
        this.sesionActualCrr = sesionActualCrr;
        this.auditoriaServiceCrr = auditoriaServiceCrr;
        this.documentsDirCrr = Path.of(documentsDirCrr);
    }

    public RacaDatosFicha racaConstruirDatos(Long trayectoriaIdCrr) {
        sesionActualCrr.ryeRequerirAutenticacion();
        YmclTrayectoria trayectoriaCrr = trayectoriaRepositoryCrr.findById(trayectoriaIdCrr)
                .orElseThrow(() -> new RyeEntidadNoEncontradaException("Trayectoria no encontrada: id=" + trayectoriaIdCrr));
        RacaInstitucion institucionCrr = institucionServiceCrr.racaObtenerPrincipal()
                .orElseThrow(() -> new RyeValidacionException("Configure primero los datos de la institucion."));

        List<RacaCiclo> ciclosCrr = cicloServiceCrr.racaListarPorPlan(trayectoriaCrr.getPlanEstudio().getId());
        List<RacaDatosCiclo> datosCiclosCrr = ciclosCrr.stream()
                .map(cicloCrr -> racaConstruirDatosCiclo(trayectoriaCrr, cicloCrr))
                .toList();

        YmclEstudiante estudianteCrr = trayectoriaCrr.getEstudiante();
        String nombreCompletoCrr = estudianteCrr.getApellidoPaterno()
                + (estudianteCrr.getApellidoMaterno() == null ? "" : " " + estudianteCrr.getApellidoMaterno())
                + ", " + estudianteCrr.getNombres();

        return new RacaDatosFicha(institucionCrr.getNombre(), institucionCrr.getSiglas(), institucionCrr.getDireccion(),
                institucionCrr.getLogoPath(), nombreCompletoCrr, estudianteCrr.getDni(), estudianteCrr.getCodigo(),
                trayectoriaCrr.getPrograma().getNombre(), trayectoriaCrr.getPlanEstudio().getCodigo(),
                trayectoriaCrr.getPlanEstudio().getNombre(), datosCiclosCrr);
    }

    public JasperPrint racaPrevisualizar(Long trayectoriaIdCrr) {
        sesionActualCrr.ryeRequerirAutenticacion();
        return racaRenderizar(trayectoriaIdCrr, null).print();
    }

    @Transactional
    public RacaFichaConfirmada racaConfirmarYGuardar(Long trayectoriaIdCrr) {
        sesionActualCrr.ryeRequerirAutenticacion();
        YmclTrayectoria trayectoriaCrr = trayectoriaRepositoryCrr.findById(trayectoriaIdCrr)
                .orElseThrow(() -> new RyeEntidadNoEncontradaException("Trayectoria no encontrada: id=" + trayectoriaIdCrr));

        int anioCrr = LocalDate.now().getYear();
        int numeroSecuencialCrr = fichaGeneradaRepositoryCrr.findTopByAnioOrderByNumeroSecuencialDesc(anioCrr)
                .map(f -> f.getNumeroSecuencial() + 1)
                .orElse(1);
        String correlativoCrr = String.format("%06d-%d", numeroSecuencialCrr, anioCrr);

        RacaRenderResult renderCrr = racaRenderizar(trayectoriaIdCrr, correlativoCrr);
        Path archivoCrr = racaGuardarArchivo(renderCrr.datos(), renderCrr.pdf());

        var actorCrr = sesionActualCrr.ryeObtenerUsuarioActual().orElse(null);
        RacaFichaGenerada fichaCrr = new RacaFichaGenerada(correlativoCrr, numeroSecuencialCrr, anioCrr, trayectoriaCrr,
                archivoCrr.toString(), actorCrr);
        fichaGeneradaRepositoryCrr.save(fichaCrr);

        auditoriaServiceCrr.rahpRegistrar(actorCrr, "GENERAR_FICHA", "trayectorias_academicas", trayectoriaIdCrr.toString(),
                "correlativo=" + correlativoCrr + " archivo=" + archivoCrr.getFileName());

        return new RacaFichaConfirmada(fichaCrr, renderCrr.print());
    }

    public List<RacaFichaGenerada> racaListarPorTrayectoria(Long trayectoriaIdCrr) {
        sesionActualCrr.ryeRequerirAutenticacion();
        return fichaGeneradaRepositoryCrr.findByTrayectoriaIdOrderByFechaGeneracionDesc(trayectoriaIdCrr);
    }

    @Transactional
    public RacaFichaGenerada racaAnular(Long fichaGeneradaIdCrr, String motivoCrr) {
        sesionActualCrr.ryeRequerirRol(RyeRolUsuario.ADMIN);
        if (motivoCrr == null || motivoCrr.isBlank()) {
            throw new RyeValidacionException("Indique el motivo de la anulacion.");
        }
        RacaFichaGenerada fichaCrr = fichaGeneradaRepositoryCrr.findById(fichaGeneradaIdCrr)
                .orElseThrow(() -> new RyeEntidadNoEncontradaException("Ficha no encontrada: id=" + fichaGeneradaIdCrr));
        fichaCrr.racaAnular(motivoCrr);
        fichaGeneradaRepositoryCrr.save(fichaCrr);

        var actorCrr = sesionActualCrr.ryeObtenerUsuarioActual().orElse(null);
        auditoriaServiceCrr.rahpRegistrar(actorCrr, "ANULAR_FICHA", "fichas_generadas", fichaGeneradaIdCrr.toString(),
                "motivo=" + motivoCrr);
        return fichaCrr;
    }

    private String racaResolverFechaEmision() {
        return LocalDate.now().format(FORMATO_FECHA_FICHA_CRR);
    }

    private RacaRenderResult racaRenderizar(Long trayectoriaIdCrr, String correlativoCrr) {
        RacaDatosFicha datosCrr = racaConstruirDatos(trayectoriaIdCrr);
        List<RacaFilaFichaCiclo> filasPlanasCrr = datosCrr.ciclos().stream()
                .flatMap(cicloCrr -> cicloCrr.filas().stream())
                .toList();

        Map<String, Object> parametrosCrr = new HashMap<>();
        parametrosCrr.put("institucionNombre", datosCrr.institucionNombre());
        parametrosCrr.put("institucionSiglas", datosCrr.institucionSiglas());
        parametrosCrr.put("institucionDireccion", datosCrr.institucionDireccion());
        parametrosCrr.put("logoPath", datosCrr.logoPath());
        parametrosCrr.put("estudianteNombreCompleto", datosCrr.estudianteNombreCompleto());
        parametrosCrr.put("estudianteDni", datosCrr.estudianteDni());
        parametrosCrr.put("estudianteCodigo", datosCrr.estudianteCodigo());
        parametrosCrr.put("programaNombre", datosCrr.programaNombre());
        parametrosCrr.put("planNombre", datosCrr.planNombre());
        parametrosCrr.put("fechaEmision", racaResolverFechaEmision());
        parametrosCrr.put("correlativo", correlativoCrr);

        try (InputStream jrxmlCrr = getClass().getResourceAsStream("/reports/ficha-academica.jrxml")) {
            if (jrxmlCrr == null) {
                throw new RyeSgfaException("No se encontro la plantilla de la ficha academica.");
            }
            JasperReport reporteCrr = JasperCompileManager.compileReport(jrxmlCrr);
            JRBeanCollectionDataSource dataSourceCrr = new JRBeanCollectionDataSource(filasPlanasCrr);
            JasperPrint printCrr = JasperFillManager.fillReport(reporteCrr, parametrosCrr, dataSourceCrr);

            ByteArrayOutputStream salidaCrr = new ByteArrayOutputStream();
            JasperExportManager.exportReportToPdfStream(printCrr, salidaCrr);
            byte[] pdfCrr = salidaCrr.toByteArray();

            return new RacaRenderResult(datosCrr, pdfCrr, printCrr);
        } catch (JRException | IOException e) {
            throw new RyeSgfaException("No se pudo generar la ficha academica.", e);
        }
    }

    private RacaDatosCiclo racaConstruirDatosCiclo(YmclTrayectoria trayectoriaCrr, RacaCiclo cicloCrr) {
        List<YmclFilaRegistro> filasCrr = registroAcademicoServiceCrr.ymclListarVigentesPorCiclo(trayectoriaCrr.getId(), cicloCrr.getId());
        YmclResultadoPromedio promedioCrr = registroAcademicoServiceCrr.ymclCalcularPromedioCiclo(trayectoriaCrr.getId(), cicloCrr.getId());

        List<RacaFilaFichaCiclo> filasFichaCrr = filasCrr.stream()
                .map(filaCrr -> racaAFilaFicha(cicloCrr, filaCrr, promedioCrr))
                .toList();
        return new RacaDatosCiclo(cicloCrr.getNumero(), cicloCrr.getNombre(), filasFichaCrr, promedioCrr);
    }

    private RacaFilaFichaCiclo racaAFilaFicha(RacaCiclo cicloCrr, YmclFilaRegistro filaCrr, YmclResultadoPromedio promedioCrr) {
        var registroCrr = filaCrr.registro();
        RacaCurso cursoCrr = filaCrr.planCurso().getCurso();
        Double notaCrr = registroCrr == null ? null : registroCrr.getNotaFinal();
        Double ponderadoCrr = notaCrr == null ? null : notaCrr * cursoCrr.getCreditos();
        String observacionCrr = registroCrr == null ? null : registroCrr.getObservacion();

        String fechaCrr = (registroCrr == null || (observacionCrr != null && !observacionCrr.isBlank())
                || registroCrr.getPeriodo().getFechaFin() == null)
                ? null : registroCrr.getPeriodo().getFechaFin().format(FORMATO_FECHA_FICHA_CRR);

        return new RacaFilaFichaCiclo(cicloCrr.getNumero(), cicloCrr.getNombre(), cursoCrr.getCodigo(), cursoCrr.getNombre(),
                cursoCrr.getCreditos(), notaCrr, ponderadoCrr, fechaCrr, observacionCrr,
                promedioCrr.promedio(), promedioCrr.totalCreditos(), promedioCrr.sumaPonderada());
    }

    private Path racaGuardarArchivo(RacaDatosFicha datosCrr, byte[] pdfCrr) {
        try {
            Files.createDirectories(documentsDirCrr);
            String baseCrr = (datosCrr.estudianteCodigo() != null ? datosCrr.estudianteCodigo() : datosCrr.estudianteNombreCompleto())
                    .toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z0-9]+", "_");
            String nombreArchivoCrr = "ficha_" + baseCrr + "_" + java.time.LocalDateTime.now().format(FORMATO_ARCHIVO_CRR) + ".pdf";
            Path archivoCrr = documentsDirCrr.resolve(nombreArchivoCrr);
            Files.write(archivoCrr, pdfCrr);
            return archivoCrr;
        } catch (IOException e) {
            throw new RyeSgfaException("No se pudo guardar el archivo de la ficha academica.", e);
        }
    }

    private record RacaRenderResult(RacaDatosFicha datos, byte[] pdf, JasperPrint print) {
    }
}
