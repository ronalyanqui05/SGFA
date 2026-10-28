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

import pe.edu.sgfa.estudiante.repository.EstudianteRepository;
import pe.edu.sgfa.institucion.service.InstitucionService;
import pe.edu.sgfa.nota.service.RegistroAcademicoService;
import pe.edu.sgfa.ocr.service.LectorReportesPdf;
import pe.edu.sgfa.ocr.service.LectorReportesPdf.HojaLeida;
import pe.edu.sgfa.ocr.service.ResolutorReporte;
import pe.edu.sgfa.ocr.service.ResolutorReporte.Estado;
import pe.edu.sgfa.ocr.service.ResolutorReporte.Propuesta;
import pe.edu.sgfa.periodo.service.PeriodoAcademicoService;
import pe.edu.sgfa.programa.domain.Ciclo;
import pe.edu.sgfa.programa.service.CicloService;
import pe.edu.sgfa.programa.service.CursoService;
import pe.edu.sgfa.programa.service.PlanCursoService;
import pe.edu.sgfa.programa.service.PlanEstudioService;
import pe.edu.sgfa.programa.service.ProgramaEstudioService;
import pe.edu.sgfa.trayectoria.service.TrayectoriaService;
import pe.edu.sgfa.usuario.service.AdminBootstrapResult;
import pe.edu.sgfa.usuario.service.AuthService;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LotePdfDePuntaAPuntaTest {

    private static final float ALTO = PDRectangle.A4.getHeight();

    @BeforeAll
    static void baseAislada() throws IOException {
        System.setProperty("sgfa.home-dir", Files.createTempDirectory("sgfa-lote-test-home-").toString());
    }

    @Autowired
    private InstitucionService institucionService;
    @Autowired
    private ProgramaEstudioService programaEstudioService;
    @Autowired
    private PlanEstudioService planEstudioService;
    @Autowired
    private CicloService cicloService;
    @Autowired
    private CursoService cursoService;
    @Autowired
    private PlanCursoService planCursoService;
    @Autowired
    private PeriodoAcademicoService periodoAcademicoService;
    @Autowired
    private LectorReportesPdf lector;
    @Autowired
    private ResolutorReporte resolutor;
    @Autowired
    private EstudianteRepository estudianteRepository;
    @Autowired
    private TrayectoriaService trayectoriaService;
    @Autowired
    private RegistroAcademicoService registroAcademicoService;
    @Autowired
    private AuthService authService;
    @Autowired
    private AdminBootstrapResult adminBootstrapResult;

    private static AdminBootstrapResult.Credenciales credenciales;
    private static Ciclo ciclo1;

    @BeforeEach
    void preparar() {
        if (credenciales == null) {
            credenciales = adminBootstrapResult.consumir().orElseThrow();
        }
        authService.cerrarSesion();
        authService.autenticar(credenciales.username(), credenciales.password());
        if (ciclo1 == null) {
            institucionService.guardar("IESPP Juliaca", "JULI", "Av. Principal 123", null, null);
            var programa = programaEstudioService.crear("AST", "Educacion Astronomia Aplicada", null, 10);
            var pln = planEstudioService.crear(programa.getId(), "PA-1", "Plan astronomia", null, null);
            ciclo1 = cicloService.crear(pln.getId(), 1, "Ciclo I", 1);
            int ordn = 1;
            for (Object[] c : new Object[][] {{"LECTURA Y ESCRITURA EN LA EDUCACION SUPERIOR", 3},
                    {"RESOLUCION DE PROBLEMAS MATEMATICOS I", 3}, {"DESARROLLO PERSONAL I", 5}}) {
                var crs = cursoService.crear("AST-I_CM0" + ordn, (String) c[0], (Integer) c[1], null, null);
                planCursoService.asignar(pln.getId(), ciclo1.getId(), crs.getId(), ordn++, true);
            }
            periodoAcademicoService.crear("2026-I", 2026, null, null, null);
        }
    }

    @Test
    void elPdfSeLeeSeClasificaYSoloSeGuardanLasHojasListas() throws Exception {
        File pdf = File.createTempFile("lote-", ".pdf");
        pdf.deleteOnExit();
        try (PDDocument doc = new PDDocument()) {
            hoja(doc, "76916587", "APAZA CONDORCALLO, Eynar", "15.45", 0);
            hoja(doc, "70123456", "QUISPE MAMANI, Rosa", "15.45", 90);
            hoja(doc, "70999999", "FLORES TICONA, Luis", "17.00", 0);
            doc.addPage(new PDPage(PDRectangle.A4));
            doc.save(pdf);
        }

        List<HojaLeida> hjs = lector.leer(pdf, null);
        List<Propuesta> prpsts = resolutor.proponerTodos(hjs.stream().map(HojaLeida::reporte).toList());

        assertThat(prpsts).as("avisos: %s", prpsts.stream().map(Propuesta::avisos).toList())
                .extracting(Propuesta::estado)
                .containsExactly(Estado.LISTO, Estado.LISTO, Estado.REVISAR, Estado.REVISAR);
        assertThat(prpsts.get(0).promedioCalculado()).isBetween(15.45, 15.46);
        assertThat(prpsts.get(2).avisos()).anyMatch(a -> a.contains("promedio impreso"));

        for (Propuesta p : prpsts) {
            if (p.estado() == Estado.LISTO) {
                resolutor.guardar(p);
            }
        }

        assertThat(estudianteRepository.findByDni("76916587")).isPresent();
        assertThat(estudianteRepository.findByDni("70123456")).isPresent();
        assertThat(estudianteRepository.findByDni("70999999")).isEmpty();
        for (String dni : List.of("76916587", "70123456")) {
            var estudiante = estudianteRepository.findByDni(dni).orElseThrow();
            var trayectoria = trayectoriaService.listarPorEstudiante(estudiante.getId()).get(0);
            var fls = registroAcademicoService.listarVigentesPorCiclo(trayectoria.getId(), ciclo1.getId());
            assertThat(fls).filteredOn(f -> f.registro() != null).hasSize(3);
            assertThat(registroAcademicoService.calcularPromedioCiclo(trayectoria.getId(), ciclo1.getId()).promedio())
                    .isBetween(15.45, 15.46);
        }
    }

    private static void hoja(PDDocument doc, String mtrcl, String nmbr, String promedio, int rtcn) throws IOException {
        PDPage pgn = new PDPage(PDRectangle.A4);
        pgn.setRotation(rtcn);
        doc.addPage(pgn);
        try (PDPageContentStream c = new PDPageContentStream(doc, pgn)) {
            texto(c, 12, 60, 55, "REPORTE DE CALIFICACIONES DE CURSOS Y MODULOS");
            texto(c, 10, 40, 90, "Programa de Estudios:");
            texto(c, 11, 270, 90, "EDUCACION ASTRONOMIA APLICADA");
            texto(c, 10, 40, 115, "Apellidos y Nombres del Estudiante:");
            texto(c, 11, 270, 115, nmbr);
            texto(c, 10, 40, 140, "Periodo Academico:");
            texto(c, 11, 270, 140, "2026 - I");
            texto(c, 10, 40, 160, "Ciclo - Seccion:");
            texto(c, 11, 270, 160, "CICLO I");
            texto(c, 10, 40, 185, "Nro. de Matricula:");
            texto(c, 11, 270, 185, mtrcl);

            texto(c, 9, 40, 235, "N");
            texto(c, 9, 150, 235, "CURSOS");
            texto(c, 9, 300, 235, "COMPETENCIA");
            texto(c, 9, 400, 235, "DEL CURSO");
            texto(c, 9, 480, 235, "SISTEMA");
            fila(c, 280, "1", "LECTURA Y ESCRITURA EN LA EDUCACION SUPERIOR", "LOGRADO", "16");
            fila(c, 320, "2", "RESOLUCION DE PROBLEMAS MATEMATICOS I", "EN PROCESO", "14");
            fila(c, 360, "3", "DESARROLLO PERSONAL I", "LOGRADO", "16");
            texto(c, 9, 60, 420, "PROMEDIO PONDERADO DEL PERIODO ACADEMICO");
            texto(c, 10, 500, 420, promedio);
        }
    }

    private static void fila(PDPageContentStream c, float y, String nro, String crs, String stcn, String nota)
            throws IOException {
        texto(c, 9, 45, y, nro);
        texto(c, 9, 70, y, crs);
        texto(c, 8, 300, y, "Competencia 3");
        texto(c, 9, 405, y, stcn);
        texto(c, 10, 500, y, nota);
    }

    private static void texto(PDPageContentStream c, float tam, float x, float yDsdArrb, String texto) throws IOException {
        c.beginText();
        c.setFont(PDType1Font.HELVETICA, tam);
        c.newLineAtOffset(x, ALTO - yDsdArrb);
        c.showText(texto);
        c.endText();
    }
}
