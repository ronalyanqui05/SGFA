package pe.edu.sgfa.reporte.service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import pe.edu.sgfa.common.exception.RyeSgfaException;
import pe.edu.sgfa.reporte.service.CphTablaReporte.CphGrupo;

@Component
public class CphExportadorExcel {

    private static final DateTimeFormatter FECHA_PCH = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public void cphEscribir(CphTablaReporte tablaPch, String institucionPch, Path destinoPch) {
        try (Workbook libroPch = new XSSFWorkbook(); OutputStream salidaPch = Files.newOutputStream(destinoPch)) {
            Sheet hojaPch = libroPch.createSheet(cphNombreHoja(tablaPch.titulo()));
            int columnasPch = tablaPch.columnas().size();
            CellStyle negritaPch = cphEstilo(libroPch, true, false, null, HorizontalAlignment.LEFT);
            CellStyle encabezadoPch = cphEstilo(libroPch, true, true, IndexedColors.GREY_25_PERCENT, HorizontalAlignment.CENTER);
            CellStyle textoPch = cphEstilo(libroPch, false, true, null, HorizontalAlignment.LEFT);
            CellStyle numeroPch = cphEstilo(libroPch, false, true, null, HorizontalAlignment.RIGHT);
            CellStyle resumenTextoPch = cphEstilo(libroPch, true, true, IndexedColors.LIGHT_CORNFLOWER_BLUE, HorizontalAlignment.LEFT);
            CellStyle resumenNumeroPch = cphEstilo(libroPch, true, true, IndexedColors.LIGHT_CORNFLOWER_BLUE, HorizontalAlignment.RIGHT);

            int filaPch = 0;
            if (institucionPch != null && !institucionPch.isBlank()) {
                cphCelda(hojaPch.createRow(filaPch++), 0, institucionPch, negritaPch);
            }
            cphCelda(hojaPch.createRow(filaPch++), 0, tablaPch.titulo(), negritaPch);
            for (String lineaPch : tablaPch.subtitulo()) {
                cphCelda(hojaPch.createRow(filaPch++), 0, lineaPch, null);
            }
            filaPch++;

            if (tablaPch.cphTieneGrupos()) {
                Row filaGruposPch = hojaPch.createRow(filaPch);
                int colPch = 0;
                for (CphGrupo g : tablaPch.grupos()) {
                    for (int k = 0; k < g.columnas(); k++) {
                        cphCelda(filaGruposPch, colPch + k, k == 0 ? g.texto() : "", encabezadoPch);
                    }
                    if (g.columnas() > 1) {
                        hojaPch.addMergedRegion(new CellRangeAddress(filaPch, filaPch, colPch, colPch + g.columnas() - 1));
                    }
                    colPch += g.columnas();
                }
                filaPch++;
            }
            Row filaColumnasPch = hojaPch.createRow(filaPch);
            for (int c = 0; c < columnasPch; c++) {
                cphCelda(filaColumnasPch, c, tablaPch.columnas().get(c), encabezadoPch);
            }
            hojaPch.createFreezePane(0, filaPch + 1);
            filaPch++;

            for (int indicePch = 0; indicePch < tablaPch.filas().size(); indicePch++) {
                List<String> datosPch = tablaPch.filas().get(indicePch);
                if (tablaPch.cphEsSeccion(indicePch)) {
                    Row franjaPch = hojaPch.createRow(filaPch);
                    for (int c = 0; c < columnasPch; c++) {
                        cphCelda(franjaPch, c, c == 0 ? CphTablaReporte.cphTextoDeSeccion(datosPch) : "", resumenTextoPch);
                    }
                    if (columnasPch > 1) {
                        hojaPch.addMergedRegion(new CellRangeAddress(filaPch, filaPch, 0, columnasPch - 1));
                    }
                    filaPch++;
                    continue;
                }
                boolean resumenPch = datosPch.contains("TOTAL") || datosPch.contains("PROMEDIO DEL CURSO");
                Row r = hojaPch.createRow(filaPch++);
                for (int c = 0; c < columnasPch; c++) {
                    String valorPch = c < datosPch.size() ? datosPch.get(c) : "";
                    boolean numericaPch = tablaPch.numericas().get(c);
                    Double parseadoPch = numericaPch ? cphComoNumero(valorPch) : null;
                    Cell celPch = r.createCell(c);
                    if (parseadoPch != null) {
                        celPch.setCellValue(parseadoPch);
                    } else {
                        celPch.setCellValue(valorPch);
                    }
                    celPch.setCellStyle(resumenPch ? (numericaPch ? resumenNumeroPch : resumenTextoPch) : (numericaPch ? numeroPch : textoPch));
                }
            }
            if (!tablaPch.pie().isEmpty()) {
                filaPch++;
                for (String notaPch : tablaPch.pie()) {
                    cphCelda(hojaPch.createRow(filaPch++), 0, notaPch, null);
                }
            }
            filaPch++;
            cphCelda(hojaPch.createRow(filaPch), 0, "Fecha de emision: " + LocalDate.now().format(FECHA_PCH), null);

            for (int c = 0; c < columnasPch; c++) {
                hojaPch.setColumnWidth(c, Math.min(255, Math.max(4, tablaPch.anchos().get(c) * 11 / 10 + 2)) * 256);
            }
            hojaPch.getPrintSetup().setLandscape(true);
            hojaPch.getPrintSetup().setFitWidth((short) 1);
            hojaPch.getPrintSetup().setFitHeight((short) 0);
            hojaPch.setFitToPage(true);
            libroPch.write(salidaPch);
        } catch (IOException e) {
            throw new RyeSgfaException("No se pudo escribir el archivo Excel.", e);
        }
    }

    private static Double cphComoNumero(String valorPch) {
        if (valorPch == null || !valorPch.matches("-?\\d+([.,]\\d+)?")) {
            return null;
        }
        return Double.parseDouble(valorPch.replace(',', '.'));
    }

    private static String cphNombreHoja(String tituloPch) {
        String limpioPch = tituloPch.replaceAll("[\\\\/?*\\[\\]:]", " ").strip();
        return limpioPch.length() > 31 ? limpioPch.substring(0, 31) : limpioPch;
    }

    private static void cphCelda(Row filaPch, int columnaPch, String textoPch, CellStyle estiloPch) {
        Cell c = filaPch.createCell(columnaPch);
        c.setCellValue(textoPch);
        if (estiloPch != null) {
            c.setCellStyle(estiloPch);
        }
    }

    private static CellStyle cphEstilo(Workbook libroPch, boolean negritaPch, boolean bordePch, IndexedColors fondoPch, HorizontalAlignment alineacionPch) {
        CellStyle e = libroPch.createCellStyle();
        Font f = libroPch.createFont();
        f.setBold(negritaPch);
        e.setFont(f);
        e.setAlignment(alineacionPch);
        e.setVerticalAlignment(VerticalAlignment.CENTER);
        e.setWrapText(true);
        if (bordePch) {
            e.setBorderBottom(BorderStyle.THIN);
            e.setBorderTop(BorderStyle.THIN);
            e.setBorderLeft(BorderStyle.THIN);
            e.setBorderRight(BorderStyle.THIN);
        }
        if (fondoPch != null) {
            e.setFillForegroundColor(fondoPch.getIndex());
            e.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        return e;
    }
}
