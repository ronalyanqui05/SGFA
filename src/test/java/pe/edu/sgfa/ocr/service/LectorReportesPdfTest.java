package pe.edu.sgfa.ocr.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;

import pe.edu.sgfa.ocr.service.LectorReportesPdf.HojaLeida;

class LectorReportesPdfTest {

    private static final float ALTO = PDRectangle.A4.getHeight();

    @Test
    void leeUnaHojaPorEstudianteYEnderezaLasGiradas() throws Exception {
        File pdf = File.createTempFile("lote-", ".pdf");
        pdf.deleteOnExit();
        try (PDDocument doc = new PDDocument()) {
            hojaDeReporte(doc, "76916587", "APAZA CONDORCALLO, Eynar", 0);
            hojaDeReporte(doc, "70123456", "QUISPE MAMANI, Rosa", 90);
            doc.addPage(new PDPage(PDRectangle.A4));
            doc.save(pdf);
        }
        List<int[]> avnc = new ArrayList<>();
        LectorReportesPdf lctr = new LectorReportesPdf(new OcrService(Files.createTempDirectory("tess-").toString()));

        List<HojaLeida> hjs = lctr.leer(pdf, new LectorReportesPdf.Progreso() {
            @Override
            public void hoja(int actl, int ttl) {
                avnc.add(new int[] {actl, ttl});
            }

            @Override
            public boolean cancelado() {
                return false;
            }
        });

        assertThat(hjs).hasSize(3);
        assertThat(avnc).extracting(a -> a[0] + "/" + a[1]).containsExactly("1/3", "2/3", "3/3");

        var prmr = hjs.get(0).reporte();
        assertThat(hjs.get(0).giroGrados()).isZero();
        assertThat(prmr.matricula()).isEqualTo("76916587");
        assertThat(prmr.apellidoPaterno()).isEqualTo("APAZA");
        assertThat(prmr.apellidoMaterno()).isEqualTo("CONDORCALLO");
        assertThat(prmr.periodo()).isEqualTo("2026-I");
        assertThat(prmr.ciclo()).isEqualTo(1);
        assertThat(prmr.cursos()).hasSize(3);
        assertThat(prmr.cursos()).extracting(c -> c.nota()).containsExactly(16.0, 14.0, 16.0);
        assertThat(prmr.cursos().get(1).situacion()).isEqualTo("PROCESO");

        var sgnd = hjs.get(1);
        assertThat(sgnd.giroGrados()).isNotZero();
        assertThat(sgnd.reporte().matricula()).isEqualTo("70123456");
        assertThat(sgnd.reporte().cursos()).hasSize(3);
        assertThat(sgnd.imagen().getWidth()).isLessThanOrEqualTo(1600);

        var enBlnc = hjs.get(2).reporte();
        assertThat(enBlnc.matricula()).isNull();
        assertThat(enBlnc.cursos()).isEmpty();
        assertThat(enBlnc.advertencias()).isNotEmpty();
    }

    @Test
    void sePuedeCancelarEntreHojas() throws Exception {
        File pdf = File.createTempFile("lote-", ".pdf");
        pdf.deleteOnExit();
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage(PDRectangle.A4));
            doc.addPage(new PDPage(PDRectangle.A4));
            doc.save(pdf);
        }
        LectorReportesPdf lctr = new LectorReportesPdf(new OcrService(Files.createTempDirectory("tess-").toString()));

        List<HojaLeida> hjs = lctr.leer(pdf, new LectorReportesPdf.Progreso() {
            @Override
            public void hoja(int actl, int ttl) {
            }

            @Override
            public boolean cancelado() {
                return true;
            }
        });

        assertThat(hjs).isEmpty();
    }

    private static void hojaDeReporte(PDDocument doc, String matricula, String nmbr, int rtcn) throws IOException {
        PDPage pgn = new PDPage(PDRectangle.A4);
        pgn.setRotation(rtcn);
        doc.addPage(pgn);
        try (PDPageContentStream c = new PDPageContentStream(doc, pgn)) {
            texto(c, 12, 60, 60, "REPORTE DE CALIFICACIONES DE CURSOS Y MODULOS");
            texto(c, 10, 40, 110, "Apellidos y Nombres del Estudiante:");
            texto(c, 11, 270, 110, nmbr);
            texto(c, 10, 40, 135, "Periodo Academico:");
            texto(c, 11, 270, 135, "2026 - I");
            texto(c, 10, 40, 155, "Ciclo - Seccion:");
            texto(c, 11, 270, 155, "CICLO I");
            texto(c, 10, 40, 180, "Nro. de Matricula:");
            texto(c, 11, 270, 180, matricula);

            texto(c, 9, 40, 235, "N");
            texto(c, 9, 150, 235, "CURSOS");
            texto(c, 9, 300, 235, "COMPETENCIA");
            texto(c, 9, 400, 235, "DEL CURSO");
            texto(c, 9, 480, 235, "SISTEMA");

            fila(c, 280, "1", "LECTURA Y ESCRITURA EN LA EDUCACION SUPERIOR", "LOGRADO", "16");
            fila(c, 320, "2", "RESOLUCION DE PROBLEMAS MATEMATICOS I", "EN PROCESO", "14");
            fila(c, 360, "3", "DESARROLLO PERSONAL I", "LOGRADO", "16");
            texto(c, 9, 60, 420, "PROMEDIO PONDERADO DEL PERIODO ACADEMICO");
        }
    }

    private static void fila(PDPageContentStream c, float y, String nro, String crs, String situacion, String nota)
            throws IOException {
        texto(c, 9, 45, y, nro);
        texto(c, 9, 70, y, crs);
        texto(c, 8, 300, y, "Competencia 3");
        texto(c, 9, 405, y, situacion);
        texto(c, 10, 500, y, nota);
    }

    private static void texto(PDPageContentStream c, float tam, float x, float yDsdArrb, String texto)
            throws IOException {
        c.beginText();
        c.setFont(PDType1Font.HELVETICA, tam);
        c.newLineAtOffset(x, ALTO - yDsdArrb);
        c.showText(texto);
        c.endText();
    }
}
