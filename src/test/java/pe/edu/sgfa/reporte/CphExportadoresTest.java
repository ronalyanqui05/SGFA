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

import pe.edu.sgfa.reporte.service.CphExportadorExcel;
import pe.edu.sgfa.reporte.service.CphExportadorPdf;
import pe.edu.sgfa.reporte.service.CphTablaReporte;
import pe.edu.sgfa.reporte.service.CphTablaReporte.CphGrupo;

class CphExportadoresTest {

    @TempDir
    Path carpeta;

    private static CphTablaReporte acta(int estudiantesPch) {
        List<List<String>> filasPch = new ArrayList<>();
        for (int i = 1; i <= estudiantesPch; i++) {
            filasPch.add(List.of(String.valueOf(i), "7000" + String.format("%04d", i), "PEÑA", "NÚÑEZ", "María Ω",
                    "L", "16", "48", "E", "14", "56", "ALTO"));
        }
        filasPch.add(List.of("", "", "PROMEDIO DEL CURSO", "", "", "", "15.00", "", "", "14.00", "", ""));
        return new CphTablaReporte("ACTA CONSOLIDADA DE NOTAS", List.of("Programa: Educación Secundaria", "Periodo: 2026-I"),
                List.of(new CphGrupo("", 1), new CphGrupo("", 1), new CphGrupo("", 1), new CphGrupo("", 1), new CphGrupo("", 1),
                        new CphGrupo("LECTURA Y ESCRITURA EN LA EDUCACIÓN SUPERIOR (3 cred.)", 3),
                        new CphGrupo("RESOLUCIÓN DE PROBLEMAS MATEMÁTICOS I (4 cred.)", 3), new CphGrupo("", 1)),
                List.of("N°", "N° Matricula", "Apellido paterno", "Apellido materno", "Nombres", "Sit.", "Nota", "PTJ", "Sit.",
                        "Nota", "PTJ", "Riesgo"),
                List.of(4, 10, 14, 14, 18, 4, 5, 6, 4, 5, 6, 8),
                List.of(true, false, false, false, false, false, true, true, false, true, true, false), filasPch,
                List.of("Sit.: D Destacado, L Logrado, E En proceso."), true);
    }

    @Test
    void excelTieneEncabezadoAgrupadoYNumerosComoNumeros() throws Exception {
        Path xlsxPch = carpeta.resolve("acta.xlsx");

        new CphExportadorExcel().cphEscribir(acta(3), "Escuela de Educación Superior Pedagógica Pública Juliaca", xlsxPch);

        try (var libroPch = new XSSFWorkbook(Files.newInputStream(xlsxPch))) {
            var hojaPch = libroPch.getSheetAt(0);
            assertThat(hojaPch.getSheetName()).isEqualTo("ACTA CONSOLIDADA DE NOTAS");
            assertThat(hojaPch.getRow(0).getCell(0).getStringCellValue()).startsWith("Escuela de Educación");
            assertThat(hojaPch.getRow(1).getCell(0).getStringCellValue()).isEqualTo("ACTA CONSOLIDADA DE NOTAS");

            assertThat(hojaPch.getNumMergedRegions()).isEqualTo(2);

            assertThat(hojaPch.getRow(5).getCell(5).getStringCellValue()).contains("LECTURA Y ESCRITURA");
            assertThat(hojaPch.getRow(6).getCell(1).getStringCellValue()).isEqualTo("N° Matricula");
            var primeraPch = hojaPch.getRow(7);
            assertThat(primeraPch.getCell(6).getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(primeraPch.getCell(6).getNumericCellValue()).isEqualTo(16.0);
            assertThat(primeraPch.getCell(2).getStringCellValue()).isEqualTo("PEÑA");
            assertThat(primeraPch.getCell(1).getCellType()).isEqualTo(CellType.STRING);
        }
    }

    @Test
    void pdfConPocasFilasCabeEnUnaHojaConFirmasYAcentos() throws Exception {
        Path pdfPch = carpeta.resolve("acta.pdf");

        new CphExportadorPdf().cphEscribir(acta(3), "Escuela de Educación Superior Pedagógica Pública Juliaca", pdfPch);

        try (PDDocument docPch = PDDocument.load(pdfPch.toFile())) {
            assertThat(docPch.getNumberOfPages()).isEqualTo(1);
            assertThat(docPch.getPage(0).getMediaBox().getWidth()).isGreaterThan(docPch.getPage(0).getMediaBox().getHeight());
            String textoPch = new PDFTextStripper().getText(docPch);
            assertThat(textoPch).contains("ACTA CONSOLIDADA DE NOTAS", "Educación Superior", "PEÑA", "NÚÑEZ",
                    "Programa: Educación Secundaria", "Pagina 1 de 1", "Fecha de emision", "Secretario Académico", "Director General",
                    "PROMEDIO DEL CURSO", "Sit.: D Destacado");
            assertThat(textoPch).contains("María ?");
        }
    }

    @Test
    void pdfLargoSeParteEnHojasYRepiteElEncabezado() throws Exception {
        Path pdfPch = carpeta.resolve("largo.pdf");

        new CphExportadorPdf().cphEscribir(acta(80), "Institución", pdfPch);

        try (PDDocument docPch = PDDocument.load(pdfPch.toFile())) {
            int paginasPch = docPch.getNumberOfPages();
            assertThat(paginasPch).isGreaterThanOrEqualTo(2);
            PDFTextStripper lectorPch = new PDFTextStripper();
            lectorPch.setStartPage(paginasPch);
            lectorPch.setEndPage(paginasPch);
            String ultimaPch = lectorPch.getText(docPch);
            assertThat(ultimaPch).contains("Pagina " + paginasPch + " de " + paginasPch, "ACTA CONSOLIDADA DE NOTAS (continuacion)",
                    "N° Matricula", "Secretario Académico");
            lectorPch.setStartPage(1);
            lectorPch.setEndPage(1);
            assertThat(lectorPch.getText(docPch)).contains("Pagina 1 de " + paginasPch);
        }
    }

    @Test
    void unaTablaSinGruposNiFirmasSaleVerticalYSinLineasDeFirma() throws Exception {
        var tablaPch = new CphTablaReporte("COBERTURA DE NOTAS POR CICLO", List.of("Periodo: 2026-I"), null,
                List.of("Ciclo", "Cursos", "Estado"), List.of(14, 6, 9), List.of(false, true, false),
                List.of(List.of("PRIMER CICLO", "6", "Parcial"), List.of("SEGUNDO CICLO", "0", "Sin notas")), List.of(), false);
        Path pdfPch = carpeta.resolve("cobertura.pdf");

        new CphExportadorPdf().cphEscribir(tablaPch, "", pdfPch);

        try (PDDocument docPch = PDDocument.load(pdfPch.toFile())) {
            assertThat(docPch.getNumberOfPages()).isEqualTo(1);
            assertThat(docPch.getPage(0).getMediaBox().getHeight()).isGreaterThan(docPch.getPage(0).getMediaBox().getWidth());
            assertThat(new PDFTextStripper().getText(docPch)).contains("PRIMER CICLO", "Parcial").doesNotContain("Secretario");
        }
    }
}
