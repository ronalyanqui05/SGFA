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

import pe.edu.sgfa.ocr.service.RahpLectorReportesPdf.RahpHojaLeida;

class RahpLectorReportesPdfTest {

    private static final float ALTO_HNC = PDRectangle.A4.getHeight();

    @Test
    void leeUnaHojaPorEstudianteYEnderezaLasGiradas() throws Exception {
        File pdfHnc = File.createTempFile("lote-", ".pdf");
        pdfHnc.deleteOnExit();
        try (PDDocument docHnc = new PDDocument()) {
            hojaDeReporte(docHnc, "76916587", "APAZA CONDORCALLO, Eynar", 0);
            hojaDeReporte(docHnc, "70123456", "QUISPE MAMANI, Rosa", 90);
            docHnc.addPage(new PDPage(PDRectangle.A4));
            docHnc.save(pdfHnc);
        }
        List<int[]> avanceHnc = new ArrayList<>();
        RahpLectorReportesPdf lectorHnc = new RahpLectorReportesPdf(new RahpOcrService(Files.createTempDirectory("tess-").toString()));

        List<RahpHojaLeida> hojasHnc = lectorHnc.rahpLeer(pdfHnc, new RahpLectorReportesPdf.RahpProgreso() {
            @Override
            public void hoja(int actualHnc, int totalHnc) {
                avanceHnc.add(new int[] {actualHnc, totalHnc});
            }

            @Override
            public boolean cancelado() {
                return false;
            }
        });

        assertThat(hojasHnc).hasSize(3);
        assertThat(avanceHnc).extracting(a -> a[0] + "/" + a[1]).containsExactly("1/3", "2/3", "3/3");

        var primeraHnc = hojasHnc.get(0).reporte();
        assertThat(hojasHnc.get(0).giroGrados()).isZero();
        assertThat(primeraHnc.matricula()).isEqualTo("76916587");
        assertThat(primeraHnc.apellidoPaterno()).isEqualTo("APAZA");
        assertThat(primeraHnc.apellidoMaterno()).isEqualTo("CONDORCALLO");
        assertThat(primeraHnc.periodo()).isEqualTo("2026-I");
        assertThat(primeraHnc.ciclo()).isEqualTo(1);
        assertThat(primeraHnc.cursos()).hasSize(3);
        assertThat(primeraHnc.cursos()).extracting(c -> c.nota()).containsExactly(16.0, 14.0, 16.0);
        assertThat(primeraHnc.cursos().get(1).situacion()).isEqualTo("PROCESO");

        var segundaHnc = hojasHnc.get(1);
        assertThat(segundaHnc.giroGrados()).isNotZero();
        assertThat(segundaHnc.reporte().matricula()).isEqualTo("70123456");
        assertThat(segundaHnc.reporte().cursos()).hasSize(3);
        assertThat(segundaHnc.rahpImagen().getWidth()).isLessThanOrEqualTo(1600);

        var enBlancoHnc = hojasHnc.get(2).reporte();
        assertThat(enBlancoHnc.matricula()).isNull();
        assertThat(enBlancoHnc.cursos()).isEmpty();
        assertThat(enBlancoHnc.advertencias()).isNotEmpty();
    }

    @Test
    void sePuedeCancelarEntreHojas() throws Exception {
        File pdfHnc = File.createTempFile("lote-", ".pdf");
        pdfHnc.deleteOnExit();
        try (PDDocument docHnc = new PDDocument()) {
            docHnc.addPage(new PDPage(PDRectangle.A4));
            docHnc.addPage(new PDPage(PDRectangle.A4));
            docHnc.save(pdfHnc);
        }
        RahpLectorReportesPdf lectorHnc = new RahpLectorReportesPdf(new RahpOcrService(Files.createTempDirectory("tess-").toString()));

        List<RahpHojaLeida> hojasHnc = lectorHnc.rahpLeer(pdfHnc, new RahpLectorReportesPdf.RahpProgreso() {
            @Override
            public void hoja(int actualHnc, int totalHnc) {
            }

            @Override
            public boolean cancelado() {
                return true;
            }
        });

        assertThat(hojasHnc).isEmpty();
    }

    private static void hojaDeReporte(PDDocument docHnc, String matriculaHnc, String nombreHnc, int rotacionHnc) throws IOException {
        PDPage paginaHnc = new PDPage(PDRectangle.A4);
        paginaHnc.setRotation(rotacionHnc);
        docHnc.addPage(paginaHnc);
        try (PDPageContentStream c = new PDPageContentStream(docHnc, paginaHnc)) {
            texto(c, 12, 60, 60, "REPORTE DE CALIFICACIONES DE CURSOS Y MODULOS");
            texto(c, 10, 40, 110, "Apellidos y Nombres del Estudiante:");
            texto(c, 11, 270, 110, nombreHnc);
            texto(c, 10, 40, 135, "Periodo Academico:");
            texto(c, 11, 270, 135, "2026 - I");
            texto(c, 10, 40, 155, "Ciclo - Seccion:");
            texto(c, 11, 270, 155, "CICLO I");
            texto(c, 10, 40, 180, "Nro. de Matricula:");
            texto(c, 11, 270, 180, matriculaHnc);

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

    private static void fila(PDPageContentStream c, float y, String nroHnc, String cursoHnc, String situacionHnc, String notaHnc)
            throws IOException {
        texto(c, 9, 45, y, nroHnc);
        texto(c, 9, 70, y, cursoHnc);
        texto(c, 8, 300, y, "Competencia 3");
        texto(c, 9, 405, y, situacionHnc);
        texto(c, 10, 500, y, notaHnc);
    }

    private static void texto(PDPageContentStream c, float tamHnc, float x, float yDesdeArribaHnc, String textoHnc)
            throws IOException {
        c.beginText();
        c.setFont(PDType1Font.HELVETICA, tamHnc);
        c.newLineAtOffset(x, ALTO_HNC - yDesdeArribaHnc);
        c.showText(textoHnc);
        c.endText();
    }
}
