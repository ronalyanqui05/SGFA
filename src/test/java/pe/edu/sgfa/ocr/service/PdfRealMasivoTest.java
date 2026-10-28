package pe.edu.sgfa.ocr.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import pe.edu.sgfa.ocr.service.ReconocedorReporteCalificaciones.ReporteDetectado;

class PdfRealMasivoTest {

    private static final File PDF = new File(System.getProperty("user.home"), "Downloads/pruebas ocr/pdf_masivo.pdf");

    private record Esperado(String matricula, String paterno, double[] notas, double promedio, String situaciones) {
    }

    private static final List<Esperado> HOJAS = List.of(
            new Esperado("61400640", "AÑARI", new double[] {16, 14, 16, 16, 17, 17}, 16.17, "LPLLLL"),
            new Esperado("60621934", "CASTILLO", new double[] {15, 14, 13, 15, 14, 13}, 14.00, "LPPLPP"),
            new Esperado("61400120", "COLQUEHUANCA", new double[] {16, 14, 16, 15, 15, 17}, 15.54, "LPLLLL"),
            new Esperado("61536463", "CONDORI", new double[] {16, 16, 15, 16, 15, 16}, 15.67, "LLLLLL"),
            new Esperado("61598220", "COSI", new double[] {15, 14, 16, 16, 14, 15}, 15.00, "LPLLPL"),
            new Esperado("60069187", "CRUZ", new double[] {16, 14, 15, 17, 16, 16}, 15.83, "LPLLLL"),
            new Esperado("60676776", "ESTOFANERO", new double[] {15, 14, 16, 15, 15, 15}, 15.00, "LPLLLL"),
            new Esperado("62279103", "FLORES", new double[] {17, 16, 15, 15, 15, 17}, 15.79, "LLLLLL"));

    @Test
    void leeLasOchoHojasReales() throws Exception {
        Assumptions.assumeTrue(PDF.isFile(), "No esta " + PDF);
        var lctr = new LectorReportesPdf(new OcrService(Files.createTempDirectory("tess-").toString()));

        var hjs = lctr.leer(PDF, null);

        assertThat(hjs).hasSize(HOJAS.size());
        List<String> flls = new ArrayList<>();
        int ntsOk = 0;
        for (int i = 0; i < HOJAS.size(); i++) {
            Esperado e = HOJAS.get(i);
            ReporteDetectado r = hjs.get(i).reporte();
            String h = "H" + (i + 1) + ": ";
            if (!e.matricula().equals(r.matricula())) {
                flls.add(h + "matricula " + r.matricula() + " (esperada " + e.matricula() + ")");
            }
            if (r.apellidoPaterno() == null || EmparejadorNombres.similitud(r.apellidoPaterno(), e.paterno()) < 0.8) {
                flls.add(h + "apellido " + r.apellidoPaterno() + " (esperado " + e.paterno() + ")");
            }
            if (!"2026-I".equals(r.periodo())) {
                flls.add(h + "periodo " + r.periodo());
            }
            if (!Integer.valueOf(1).equals(r.ciclo())) {
                flls.add(h + "ciclo " + r.ciclo());
            }
            if (r.promedioReporte() == null || Math.abs(r.promedioReporte() - e.promedio()) > 0.001) {
                flls.add(h + "promedio " + r.promedioReporte() + " (esperado " + e.promedio() + ")");
            }
            if (r.cursos().size() != 6) {
                flls.add(h + "cursos " + r.cursos().size() + " (esperados 6)");
                continue;
            }
            for (int c = 0; c < 6; c++) {
                var crs = r.cursos().get(c);
                if (crs.nota() != null && crs.nota() == e.notas()[c]) {
                    ntsOk++;
                } else {
                    flls.add(h + "nota del curso " + (c + 1) + ": " + crs.nota() + " (esperada " + e.notas()[c] + ")");
                }
                String esprd = e.situaciones().charAt(c) == 'L' ? "LOGRADO" : "PROCESO";
                if (!esprd.equals(crs.situacion())) {
                    flls.add(h + "situacion del curso " + (c + 1) + ": " + crs.situacion() + " (esperada " + esprd + ")");
                }
            }
        }
        System.out.println("PDFREAL>> notas correctas: " + ntsOk + "/48 | fallos: " + flls.size());
        flls.forEach(f -> System.out.println("PDFREAL>>   " + f));

        assertThat(flls).as("diferencias contra lo que dicen las hojas").isEmpty();
    }
}
