package pe.edu.sgfa.ocr.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import pe.edu.sgfa.ocr.service.RahpReconocedorReporteCalificaciones.RahpReporteDetectado;

class RahpPdfRealMasivoTest {

    private static final File PDF_HNC = new File(System.getProperty("user.home"), "Downloads/pruebas ocr/pdf_masivo.pdf");

    private record RahpEsperado(String matricula, String paterno, double[] notas, double promedio, String situaciones) {
    }

    private static final List<RahpEsperado> HOJAS_HNC = List.of(
            new RahpEsperado("61400640", "AÑARI", new double[] {16, 14, 16, 16, 17, 17}, 16.17, "LPLLLL"),
            new RahpEsperado("60621934", "CASTILLO", new double[] {15, 14, 13, 15, 14, 13}, 14.00, "LPPLPP"),
            new RahpEsperado("61400120", "COLQUEHUANCA", new double[] {16, 14, 16, 15, 15, 17}, 15.54, "LPLLLL"),
            new RahpEsperado("61536463", "CONDORI", new double[] {16, 16, 15, 16, 15, 16}, 15.67, "LLLLLL"),
            new RahpEsperado("61598220", "COSI", new double[] {15, 14, 16, 16, 14, 15}, 15.00, "LPLLPL"),
            new RahpEsperado("60069187", "CRUZ", new double[] {16, 14, 15, 17, 16, 16}, 15.83, "LPLLLL"),
            new RahpEsperado("60676776", "ESTOFANERO", new double[] {15, 14, 16, 15, 15, 15}, 15.00, "LPLLLL"),
            new RahpEsperado("62279103", "FLORES", new double[] {17, 16, 15, 15, 15, 17}, 15.79, "LLLLLL"));

    @Test
    void leeLasOchoHojasReales() throws Exception {
        Assumptions.assumeTrue(PDF_HNC.isFile(), "No esta " + PDF_HNC);
        var lectorHnc = new RahpLectorReportesPdf(new RahpOcrService(Files.createTempDirectory("tess-").toString()));

        var hojasHnc = lectorHnc.rahpLeer(PDF_HNC, null);

        assertThat(hojasHnc).hasSize(HOJAS_HNC.size());
        List<String> fallosHnc = new ArrayList<>();
        int notasOkHnc = 0;
        for (int i = 0; i < HOJAS_HNC.size(); i++) {
            RahpEsperado e = HOJAS_HNC.get(i);
            RahpReporteDetectado r = hojasHnc.get(i).reporte();
            String h = "H" + (i + 1) + ": ";
            if (!e.matricula().equals(r.matricula())) {
                fallosHnc.add(h + "matricula " + r.matricula() + " (esperada " + e.matricula() + ")");
            }
            if (r.apellidoPaterno() == null || RahpEmparejadorNombres.rahpSimilitud(r.apellidoPaterno(), e.paterno()) < 0.8) {
                fallosHnc.add(h + "apellido " + r.apellidoPaterno() + " (esperado " + e.paterno() + ")");
            }
            if (!"2026-I".equals(r.periodo())) {
                fallosHnc.add(h + "periodo " + r.periodo());
            }
            if (!Integer.valueOf(1).equals(r.ciclo())) {
                fallosHnc.add(h + "ciclo " + r.ciclo());
            }
            if (r.promedioReporte() == null || Math.abs(r.promedioReporte() - e.promedio()) > 0.001) {
                fallosHnc.add(h + "promedio " + r.promedioReporte() + " (esperado " + e.promedio() + ")");
            }
            if (r.cursos().size() != 6) {
                fallosHnc.add(h + "cursos " + r.cursos().size() + " (esperados 6)");
                continue;
            }
            for (int c = 0; c < 6; c++) {
                var cursoHnc = r.cursos().get(c);
                if (cursoHnc.nota() != null && cursoHnc.nota() == e.notas()[c]) {
                    notasOkHnc++;
                } else {
                    fallosHnc.add(h + "nota del curso " + (c + 1) + ": " + cursoHnc.nota() + " (esperada " + e.notas()[c] + ")");
                }
                String esperadaHnc = e.situaciones().charAt(c) == 'L' ? "LOGRADO" : "PROCESO";
                if (!esperadaHnc.equals(cursoHnc.situacion())) {
                    fallosHnc.add(h + "situacion del curso " + (c + 1) + ": " + cursoHnc.situacion() + " (esperada " + esperadaHnc + ")");
                }
            }
        }
        System.out.println("PDFREAL>> notas correctas: " + notasOkHnc + "/48 | fallos: " + fallosHnc.size());
        fallosHnc.forEach(f -> System.out.println("PDFREAL>>   " + f));

        assertThat(fallosHnc).as("diferencias contra lo que dicen las hojas").isEmpty();
    }
}
