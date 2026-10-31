package pe.edu.sgfa.ocr;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import pe.edu.sgfa.estudiante.repository.YmclEstudianteRepository;
import pe.edu.sgfa.institucion.service.RacaInstitucionService;
import pe.edu.sgfa.nota.service.YmclRegistroAcademicoService;
import pe.edu.sgfa.ocr.service.RahpLectorReportesPdf;
import pe.edu.sgfa.ocr.service.RahpLectorReportesPdf.RahpHojaLeida;
import pe.edu.sgfa.ocr.service.RahpResolutorReporte;
import pe.edu.sgfa.ocr.service.RahpResolutorReporte.RahpEstado;
import pe.edu.sgfa.ocr.service.RahpResolutorReporte.RahpPropuesta;
import pe.edu.sgfa.periodo.service.YmclPeriodoAcademicoService;
import pe.edu.sgfa.programa.domain.RacaCiclo;
import pe.edu.sgfa.programa.service.RacaCicloService;
import pe.edu.sgfa.programa.service.RacaCursoService;
import pe.edu.sgfa.programa.service.RacaPlanCursoService;
import pe.edu.sgfa.programa.service.RacaPlanEstudioService;
import pe.edu.sgfa.programa.service.RacaProgramaEstudioService;
import pe.edu.sgfa.trayectoria.service.YmclTrayectoriaService;
import pe.edu.sgfa.usuario.service.RyeAdminBootstrapResult;
import pe.edu.sgfa.usuario.service.RyeAuthService;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RahpLotePdfDePuntaAPuntaTest {

    private static final float ALTO_HNC = PDRectangle.A4.getHeight();

    @BeforeAll
    static void baseAislada() throws IOException {
        System.setProperty("sgfa.home-dir", Files.createTempDirectory("sgfa-lote-test-home-").toString());
    }

    @Autowired
    private RacaInstitucionService institucionServiceHnc;
    @Autowired
    private RacaProgramaEstudioService programaEstudioServiceHnc;
    @Autowired
    private RacaPlanEstudioService planEstudioServiceHnc;
    @Autowired
    private RacaCicloService cicloServiceHnc;
    @Autowired
    private RacaCursoService cursoServiceHnc;
    @Autowired
    private RacaPlanCursoService planCursoServiceHnc;
    @Autowired
    private YmclPeriodoAcademicoService periodoAcademicoServiceHnc;
    @Autowired
    private RahpLectorReportesPdf lectorHnc;
    @Autowired
    private RahpResolutorReporte resolutorHnc;
    @Autowired
    private YmclEstudianteRepository estudianteRepositoryHnc;
    @Autowired
    private YmclTrayectoriaService trayectoriaServiceHnc;
    @Autowired
    private YmclRegistroAcademicoService registroAcademicoServiceHnc;
    @Autowired
    private RyeAuthService authServiceHnc;
    @Autowired
    private RyeAdminBootstrapResult adminBootstrapResultHnc;

    private static RyeAdminBootstrapResult.RyeCredenciales credencialesHnc;
    private static RacaCiclo ciclo1Hnc;

    @BeforeEach
    void preparar() {
        if (credencialesHnc == null) {
            credencialesHnc = adminBootstrapResultHnc.ryeConsumir().orElseThrow();
        }
        authServiceHnc.ryeCerrarSesion();
        authServiceHnc.ryeAutenticar(credencialesHnc.username(), credencialesHnc.password());
        if (ciclo1Hnc == null) {
            institucionServiceHnc.racaGuardar("IESPP Juliaca", "JULI", "Av. Principal 123", null, null);
            var programaHnc = programaEstudioServiceHnc.racaCrear("AST", "Educacion Astronomia Aplicada", null, 10);
            var planHnc = planEstudioServiceHnc.racaCrear(programaHnc.getId(), "PA-1", "Plan astronomia", null, null);
            ciclo1Hnc = cicloServiceHnc.racaCrear(planHnc.getId(), 1, "Ciclo I", 1);
            int ordenHnc = 1;
            for (Object[] c : new Object[][] {{"LECTURA Y ESCRITURA EN LA EDUCACION SUPERIOR", 3},
                    {"RESOLUCION DE PROBLEMAS MATEMATICOS I", 3}, {"DESARROLLO PERSONAL I", 5}}) {
                var cursoHnc = cursoServiceHnc.racaCrear("AST-I_CM0" + ordenHnc, (String) c[0], (Integer) c[1], null, null);
                planCursoServiceHnc.racaAsignar(planHnc.getId(), ciclo1Hnc.getId(), cursoHnc.getId(), ordenHnc++, true);
            }
            periodoAcademicoServiceHnc.ymclCrear("2026-I", 2026, null, null, null);
        }
    }

    @Test
    void elPdfSeLeeSeClasificaYSoloSeGuardanLasHojasListas() throws Exception {
        File pdfHnc = File.createTempFile("lote-", ".pdf");
        pdfHnc.deleteOnExit();
        try (PDDocument docHnc = new PDDocument()) {
            hoja(docHnc, "76916587", "APAZA CONDORCALLO, Eynar", "15.45", 0);
            hoja(docHnc, "70123456", "QUISPE MAMANI, Rosa", "15.45", 90);
            hoja(docHnc, "70999999", "FLORES TICONA, Luis", "17.00", 0);
            docHnc.addPage(new PDPage(PDRectangle.A4));
            docHnc.save(pdfHnc);
        }

        List<RahpHojaLeida> hojasHnc = lectorHnc.rahpLeer(pdfHnc, null);
        List<RahpPropuesta> propuestasHnc = resolutorHnc.rahpProponerTodos(hojasHnc.stream().map(RahpHojaLeida::reporte).toList());

        assertThat(propuestasHnc).as("avisos: %s", propuestasHnc.stream().map(RahpPropuesta::avisos).toList())
                .extracting(RahpPropuesta::rahpEstado)
                .containsExactly(RahpEstado.LISTO, RahpEstado.LISTO, RahpEstado.REVISAR, RahpEstado.REVISAR);
        assertThat(propuestasHnc.get(0).promedioCalculado()).isBetween(15.45, 15.46);
        assertThat(propuestasHnc.get(2).avisos()).anyMatch(a -> a.contains("promedio impreso"));

        for (RahpPropuesta p : propuestasHnc) {
            if (p.rahpEstado() == RahpEstado.LISTO) {
                resolutorHnc.rahpGuardar(p);
            }
        }

        assertThat(estudianteRepositoryHnc.findByDni("76916587")).isPresent();
        assertThat(estudianteRepositoryHnc.findByDni("70123456")).isPresent();
        assertThat(estudianteRepositoryHnc.findByDni("70999999")).isEmpty();
        for (String dniHnc : List.of("76916587", "70123456")) {
            var estudianteHnc = estudianteRepositoryHnc.findByDni(dniHnc).orElseThrow();
            var trayectoriaHnc = trayectoriaServiceHnc.ymclListarPorEstudiante(estudianteHnc.getId()).get(0);
            var filasHnc = registroAcademicoServiceHnc.ymclListarVigentesPorCiclo(trayectoriaHnc.getId(), ciclo1Hnc.getId());
            assertThat(filasHnc).filteredOn(f -> f.registro() != null).hasSize(3);
            assertThat(registroAcademicoServiceHnc.ymclCalcularPromedioCiclo(trayectoriaHnc.getId(), ciclo1Hnc.getId()).promedio())
                    .isBetween(15.45, 15.46);
        }
    }

    private static void hoja(PDDocument docHnc, String matriculaHnc, String nombreHnc, String promedioHnc, int rotacionHnc) throws IOException {
        PDPage paginaHnc = new PDPage(PDRectangle.A4);
        paginaHnc.setRotation(rotacionHnc);
        docHnc.addPage(paginaHnc);
        try (PDPageContentStream c = new PDPageContentStream(docHnc, paginaHnc)) {
            texto(c, 12, 60, 55, "REPORTE DE CALIFICACIONES DE CURSOS Y MODULOS");
            texto(c, 10, 40, 90, "Programa de Estudios:");
            texto(c, 11, 270, 90, "EDUCACION ASTRONOMIA APLICADA");
            texto(c, 10, 40, 115, "Apellidos y Nombres del Estudiante:");
            texto(c, 11, 270, 115, nombreHnc);
            texto(c, 10, 40, 140, "Periodo Academico:");
            texto(c, 11, 270, 140, "2026 - I");
            texto(c, 10, 40, 160, "Ciclo - Seccion:");
            texto(c, 11, 270, 160, "CICLO I");
            texto(c, 10, 40, 185, "Nro. de Matricula:");
            texto(c, 11, 270, 185, matriculaHnc);

            texto(c, 9, 40, 235, "N");
            texto(c, 9, 150, 235, "CURSOS");
            texto(c, 9, 300, 235, "COMPETENCIA");
            texto(c, 9, 400, 235, "DEL CURSO");
            texto(c, 9, 480, 235, "SISTEMA");
            fila(c, 280, "1", "LECTURA Y ESCRITURA EN LA EDUCACION SUPERIOR", "LOGRADO", "16");
            fila(c, 320, "2", "RESOLUCION DE PROBLEMAS MATEMATICOS I", "EN PROCESO", "14");
            fila(c, 360, "3", "DESARROLLO PERSONAL I", "LOGRADO", "16");
            texto(c, 9, 60, 420, "PROMEDIO PONDERADO DEL PERIODO ACADEMICO");
            texto(c, 10, 500, 420, promedioHnc);
        }
    }

    private static void fila(PDPageContentStream c, float y, String nroHnc, String cursoHnc, String situacionHnc, String notaHnc)
            throws IOException {
        texto(c, 9, 45, y, nroHnc);
        texto(c, 9, 70, y, cursoHnc);
        texto(c, 8, 300, y, "Competencia 3");
        texto(c, 9, 405, y, situacionHnc);
        texto(c, 10, 500, y, notaHnc);
    }

    private static void texto(PDPageContentStream c, float tamHnc, float x, float yDesdeArribaHnc, String textoHnc) throws IOException {
        c.beginText();
        c.setFont(PDType1Font.HELVETICA, tamHnc);
        c.newLineAtOffset(x, ALTO_HNC - yDesdeArribaHnc);
        c.showText(textoHnc);
        c.endText();
    }
}
