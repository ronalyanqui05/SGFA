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

import pe.edu.sgfa.auditoria.service.AuditoriaService;
import pe.edu.sgfa.common.exception.EntidadNoEncontradaException;
import pe.edu.sgfa.common.exception.SgfaException;
import pe.edu.sgfa.common.exception.ValidacionException;
import pe.edu.sgfa.estudiante.domain.Estudiante;
import pe.edu.sgfa.ficha.domain.FichaGenerada;
import pe.edu.sgfa.ficha.repository.FichaGeneradaRepository;
import pe.edu.sgfa.institucion.domain.Institucion;
import pe.edu.sgfa.institucion.service.InstitucionService;
import pe.edu.sgfa.nota.service.FilaRegistro;
import pe.edu.sgfa.nota.service.RegistroAcademicoService;
import pe.edu.sgfa.nota.service.ResultadoPromedio;
import pe.edu.sgfa.programa.domain.Ciclo;
import pe.edu.sgfa.programa.domain.Curso;
import pe.edu.sgfa.programa.service.CicloService;
import pe.edu.sgfa.trayectoria.domain.Trayectoria;
import pe.edu.sgfa.trayectoria.repository.TrayectoriaRepository;
import pe.edu.sgfa.usuario.domain.RolUsuario;
import pe.edu.sgfa.usuario.service.SesionActual;

@Service
public class FichaService {

    private static final DateTimeFormatter FORMATO_ARCHIVO = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter FORMATO_FECHA_FICHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final TrayectoriaRepository trayectoriaRepository;
    private final CicloService cicloService;
    private final RegistroAcademicoService registroAcademicoService;
    private final InstitucionService institucionService;
    private final FichaGeneradaRepository fichaGeneradaRepository;
    private final SesionActual sesionActual;
    private final AuditoriaService auditoriaService;
    private final Path documentsDir;

    public FichaService(TrayectoriaRepository trayectoriaRepository, CicloService cicloService,
                         RegistroAcademicoService registroAcademicoService, InstitucionService institucionService,
                         FichaGeneradaRepository fichaGeneradaRepository, SesionActual sesionActual,
                         AuditoriaService auditoriaService,
                         @Value("${sgfa.documents-dir}") String documentsDir) {
        this.trayectoriaRepository = trayectoriaRepository;
        this.cicloService = cicloService;
        this.registroAcademicoService = registroAcademicoService;
        this.institucionService = institucionService;
        this.fichaGeneradaRepository = fichaGeneradaRepository;
        this.sesionActual = sesionActual;
        this.auditoriaService = auditoriaService;
        this.documentsDir = Path.of(documentsDir);
    }

    public DatosFicha construirDatos(Long tryctrId) {
        sesionActual.requerirAutenticacion();
        Trayectoria trayectoria = trayectoriaRepository.findById(tryctrId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Trayectoria no encontrada: id=" + tryctrId));
        Institucion institucion = institucionService.obtenerPrincipal()
                .orElseThrow(() -> new ValidacionException("Configure primero los datos de la institucion."));

        List<Ciclo> ciclos = cicloService.listarPorPlan(trayectoria.getPlanEstudio().getId());
        List<DatosCiclo> dtsCcls = ciclos.stream()
                .map(ccl -> construirDatosCiclo(trayectoria, ccl))
                .toList();

        Estudiante estudiante = trayectoria.getEstudiante();
        String nmbrCmplt = estudiante.getApellidoPaterno()
                + (estudiante.getApellidoMaterno() == null ? "" : " " + estudiante.getApellidoMaterno())
                + ", " + estudiante.getNombres();

        return new DatosFicha(institucion.getNombre(), institucion.getSiglas(), institucion.getDireccion(),
                institucion.getLogoPath(), nmbrCmplt, estudiante.getDni(), estudiante.getCodigo(),
                trayectoria.getPrograma().getNombre(), trayectoria.getPlanEstudio().getCodigo(),
                trayectoria.getPlanEstudio().getNombre(), dtsCcls);
    }

    public JasperPrint previsualizar(Long tryctrId) {
        sesionActual.requerirAutenticacion();
        return renderizar(tryctrId, null).print();
    }

    @Transactional
    public FichaConfirmada confirmarYGuardar(Long tryctrId) {
        sesionActual.requerirAutenticacion();
        Trayectoria trayectoria = trayectoriaRepository.findById(tryctrId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Trayectoria no encontrada: id=" + tryctrId));

        int an = LocalDate.now().getYear();
        int nmrScncl = fichaGeneradaRepository.findTopByAnioOrderByNumeroSecuencialDesc(an)
                .map(f -> f.getNumeroSecuencial() + 1)
                .orElse(1);
        String crrltv = String.format("%06d-%d", nmrScncl, an);

        RenderResult rndr = renderizar(tryctrId, crrltv);
        Path archv = guardarArchivo(rndr.datos(), rndr.pdf());

        var actr = sesionActual.obtenerUsuarioActual().orElse(null);
        FichaGenerada ficha = new FichaGenerada(crrltv, nmrScncl, an, trayectoria,
                archv.toString(), actr);
        fichaGeneradaRepository.save(ficha);

        auditoriaService.registrar(actr, "GENERAR_FICHA", "trayectorias_academicas", tryctrId.toString(),
                "correlativo=" + crrltv + " archivo=" + archv.getFileName());

        return new FichaConfirmada(ficha, rndr.print());
    }

    public List<FichaGenerada> listarPorTrayectoria(Long tryctrId) {
        sesionActual.requerirAutenticacion();
        return fichaGeneradaRepository.findByTrayectoriaIdOrderByFechaGeneracionDesc(tryctrId);
    }

    @Transactional
    public FichaGenerada anular(Long fchGnrdId, String mtv) {
        sesionActual.requerirRol(RolUsuario.ADMIN);
        if (mtv == null || mtv.isBlank()) {
            throw new ValidacionException("Indique el motivo de la anulacion.");
        }
        FichaGenerada ficha = fichaGeneradaRepository.findById(fchGnrdId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Ficha no encontrada: id=" + fchGnrdId));
        ficha.anular(mtv);
        fichaGeneradaRepository.save(ficha);

        var actr = sesionActual.obtenerUsuarioActual().orElse(null);
        auditoriaService.registrar(actr, "ANULAR_FICHA", "fichas_generadas", fchGnrdId.toString(),
                "motivo=" + mtv);
        return ficha;
    }

    private String resolverFechaEmision() {
        return LocalDate.now().format(FORMATO_FECHA_FICHA);
    }

    private RenderResult renderizar(Long tryctrId, String crrltv) {
        DatosFicha datos = construirDatos(tryctrId);
        List<FilaFichaCiclo> flsPlns = datos.ciclos().stream()
                .flatMap(ccl -> ccl.filas().stream())
                .toList();

        Map<String, Object> prmtrs = new HashMap<>();
        prmtrs.put("institucionNombre", datos.institucionNombre());
        prmtrs.put("institucionSiglas", datos.institucionSiglas());
        prmtrs.put("institucionDireccion", datos.institucionDireccion());
        prmtrs.put("logoPath", datos.logoPath());
        prmtrs.put("estudianteNombreCompleto", datos.estudianteNombreCompleto());
        prmtrs.put("estudianteDni", datos.estudianteDni());
        prmtrs.put("estudianteCodigo", datos.estudianteCodigo());
        prmtrs.put("programaNombre", datos.programaNombre());
        prmtrs.put("planNombre", datos.planNombre());
        prmtrs.put("fechaEmision", resolverFechaEmision());
        prmtrs.put("correlativo", crrltv);

        try (InputStream jrxmlr = getClass().getResourceAsStream("/reports/ficha-academica.jrxml")) {
            if (jrxmlr == null) {
                throw new SgfaException("No se encontro la plantilla de la ficha academica.");
            }
            JasperReport rprt = JasperCompileManager.compileReport(jrxmlr);
            JRBeanCollectionDataSource dtSrc = new JRBeanCollectionDataSource(flsPlns);
            JasperPrint print = JasperFillManager.fillReport(rprt, prmtrs, dtSrc);

            ByteArrayOutputStream sld = new ByteArrayOutputStream();
            JasperExportManager.exportReportToPdfStream(print, sld);
            byte[] pdf = sld.toByteArray();

            return new RenderResult(datos, pdf, print);
        } catch (JRException | IOException e) {
            throw new SgfaException("No se pudo generar la ficha academica.", e);
        }
    }

    private DatosCiclo construirDatosCiclo(Trayectoria trayectoria, Ciclo ccl) {
        List<FilaRegistro> filas = registroAcademicoService.listarVigentesPorCiclo(trayectoria.getId(), ccl.getId());
        ResultadoPromedio promedio = registroAcademicoService.calcularPromedioCiclo(trayectoria.getId(), ccl.getId());

        List<FilaFichaCiclo> flsFch = filas.stream()
                .map(fl -> aFilaFicha(ccl, fl, promedio))
                .toList();
        return new DatosCiclo(ccl.getNumero(), ccl.getNombre(), flsFch, promedio);
    }

    private FilaFichaCiclo aFilaFicha(Ciclo ccl, FilaRegistro fl, ResultadoPromedio promedio) {
        var registro = fl.registro();
        Curso crs = fl.planCurso().getCurso();
        Double nota = registro == null ? null : registro.getNotaFinal();
        Double pndrd = nota == null ? null : nota * crs.getCreditos();
        String obsrvcn = registro == null ? null : registro.getObservacion();

        String fch = (registro == null || (obsrvcn != null && !obsrvcn.isBlank())
                || registro.getPeriodo().getFechaFin() == null)
                ? null : registro.getPeriodo().getFechaFin().format(FORMATO_FECHA_FICHA);

        return new FilaFichaCiclo(ccl.getNumero(), ccl.getNombre(), crs.getCodigo(), crs.getNombre(),
                crs.getCreditos(), nota, pndrd, fch, obsrvcn,
                promedio.promedio(), promedio.totalCreditos(), promedio.sumaPonderada());
    }

    private Path guardarArchivo(DatosFicha datos, byte[] pdf) {
        try {
            Files.createDirectories(documentsDir);
            String bs = (datos.estudianteCodigo() != null ? datos.estudianteCodigo() : datos.estudianteNombreCompleto())
                    .toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z0-9]+", "_");
            String nmbrArchv = "ficha_" + bs + "_" + java.time.LocalDateTime.now().format(FORMATO_ARCHIVO) + ".pdf";
            Path archv = documentsDir.resolve(nmbrArchv);
            Files.write(archv, pdf);
            return archv;
        } catch (IOException e) {
            throw new SgfaException("No se pudo guardar el archivo de la ficha academica.", e);
        }
    }

    private record RenderResult(DatosFicha datos, byte[] pdf, JasperPrint print) {
    }
}
