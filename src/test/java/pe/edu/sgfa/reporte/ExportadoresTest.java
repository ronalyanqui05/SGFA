package pe.edu.sgfa.reporte;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import pe.edu.sgfa.reporte.service.ExportadorExcel;
import pe.edu.sgfa.reporte.service.ExportadorPdf;
import pe.edu.sgfa.reporte.service.TablaReporte;
import pe.edu.sgfa.reporte.service.TablaReporte.Grupo;

class ExportadoresTest {

    @TempDir
    Path carpeta;

    private static TablaReporte acta(int estdnts) {
        List<List<String>> fls = new ArrayList<>();
        for (int i = 1; i <= estdnts; i++) {
            fls.add(List.of(String.valueOf(i), "7000" + String.format("%04d", i), "PEÑA", "NÚÑEZ", "María Ω",
                    "L", "16", "48", "E", "14", "56", "ALTO"));
        }
        fls.add(List.of("", "", "PROMEDIO DEL CURSO", "", "", "", "15.00", "", "", "14.00", "", ""));
        return new TablaReporte("ACTA CONSOLIDADA DE NOTAS", List.of("Programa: Educación Secundaria", "Periodo: 2026-I"),
                List.of(new Grupo("", 1), new Grupo("", 1), new Grupo("", 1), new Grupo("", 1), new Grupo("", 1),
                        new Grupo("LECTURA Y ESCRITURA EN LA EDUCACIÓN SUPERIOR (3 cred.)", 3),
                        new Grupo("RESOLUCIÓN DE PROBLEMAS MATEMÁTICOS I (4 cred.)", 3), new Grupo("", 1)),
                List.of("N°", "N° Matricula", "Apellido paterno", "Apellido materno", "Nombres", "Sit.", "Nota", "PTJ", "Sit.",
                        "Nota", "PTJ", "Riesgo"),
                List.of(4, 10, 14, 14, 18, 4, 5, 6, 4, 5, 6, 8),
                List.of(true, false, false, false, false, false, true, true, false, true, true, false), fls,
                List.of("Sit.: D Destacado, L Logrado, E En proceso."), true);
    }

    @Test
    void excelTieneEncabezadoAgrupadoYNumerosComoNumeros() throws Exception {
        Path xlsxr = carpeta.resolve("acta.xlsx");

        new ExportadorExcel().escribir(acta(3), "Escuela de Educación Superior Pedagógica Pública Juliaca", xlsxr);

        try (var lbr = new XSSFWorkbook(Files.newInputStream(xlsxr))) {
            var hj = lbr.getSheetAt(0);
            assertThat(hj.getSheetName()).isEqualTo("ACTA CONSOLIDADA DE NOTAS");
            assertThat(hj.getRow(0).getCell(0).getStringCellValue()).startsWith("Escuela de Educación");
            assertThat(hj.getRow(1).getCell(0).getStringCellValue()).isEqualTo("ACTA CONSOLIDADA DE NOTAS");

            assertThat(hj.getNumMergedRegions()).isEqualTo(2);

            assertThat(hj.getRow(5).getCell(5).getStringCellValue()).contains("LECTURA Y ESCRITURA");
            assertThat(hj.getRow(6).getCell(1).getStringCellValue()).isEqualTo("N° Matricula");
            var prmr = hj.getRow(7);
            assertThat(prmr.getCell(6).getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(prmr.getCell(6).getNumericCellValue()).isEqualTo(16.0);
            assertThat(prmr.getCell(2).getStringCellValue()).isEqualTo("PEÑA");
            assertThat(prmr.getCell(1).getCellType()).isEqualTo(CellType.STRING);
        }
    }

    @Test
    void pdfConPocasFilasCabeEnUnaHojaConFirmasYAcentos() throws Exception {
        Path pdf = carpeta.resolve("acta.pdf");

        new ExportadorPdf().escribir(acta(3), "Escuela de Educación Superior Pedagógica Pública Juliaca", pdf);

        try (PDDocument doc = PDDocument.load(pdf.toFile())) {
            assertThat(doc.getNumberOfPages()).isEqualTo(1);
            assertThat(doc.getPage(0).getMediaBox().getWidth()).isGreaterThan(doc.getPage(0).getMediaBox().getHeight());
            String txt = new PDFTextStripper().getText(doc);
            assertThat(txt).contains("ACTA CONSOLIDADA DE NOTAS", "Educación Superior", "PEÑA", "NÚÑEZ",
                    "Programa: Educación Secundaria", "Pagina 1 de 1", "Fecha de emision", "Secretario Académico", "Director General",
                    "PROMEDIO DEL CURSO", "Sit.: D Destacado");
            assertThat(txt).contains("María ?");
        }
    }

    @Test
    void pdfLargoSeParteEnHojasYRepiteElEncabezado() throws Exception {
        Path pdf = carpeta.resolve("largo.pdf");

        new ExportadorPdf().escribir(acta(80), "Institución", pdf);

        try (PDDocument doc = PDDocument.load(pdf.toFile())) {
            int pgns = doc.getNumberOfPages();
            assertThat(pgns).isGreaterThanOrEqualTo(2);
            PDFTextStripper lctr = new PDFTextStripper();
            lctr.setStartPage(pgns);
            lctr.setEndPage(pgns);
            String ultm = lctr.getText(doc);
            assertThat(ultm).contains("Pagina " + pgns + " de " + pgns, "ACTA CONSOLIDADA DE NOTAS (continuacion)",
                    "N° Matricula", "Secretario Académico");
            lctr.setStartPage(1);
            lctr.setEndPage(1);
            assertThat(lctr.getText(doc)).contains("Pagina 1 de " + pgns);
        }
    }

    @Test
    void unaTablaSinGruposNiFirmasSaleVerticalYSinLineasDeFirma() throws Exception {
        var tbl = new TablaReporte("COBERTURA DE NOTAS POR CICLO", List.of("Periodo: 2026-I"), null,
                List.of("Ciclo", "Cursos", "Estado"), List.of(14, 6, 9), List.of(false, true, false),
                List.of(List.of("PRIMER CICLO", "6", "Parcial"), List.of("SEGUNDO CICLO", "0", "Sin notas")), List.of(), false);
        Path pdf = carpeta.resolve("cobertura.pdf");

        new ExportadorPdf().escribir(tbl, "", pdf);

        try (PDDocument doc = PDDocument.load(pdf.toFile())) {
            assertThat(doc.getNumberOfPages()).isEqualTo(1);
            assertThat(doc.getPage(0).getMediaBox().getHeight()).isGreaterThan(doc.getPage(0).getMediaBox().getWidth());
            assertThat(new PDFTextStripper().getText(doc)).contains("PRIMER CICLO", "Parcial").doesNotContain("Secretario");
        }
    }
}
