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

import pe.edu.sgfa.common.exception.SgfaException;
import pe.edu.sgfa.reporte.service.TablaReporte.Grupo;

@Component
public class ExportadorExcel {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public void escribir(TablaReporte tbl, String insttcn, Path dstn) {
        try (Workbook lbr = new XSSFWorkbook(); OutputStream sld = Files.newOutputStream(dstn)) {
            Sheet hj = lbr.createSheet(nombreHoja(tbl.titulo()));
            int columnas = tbl.columnas().size();
            CellStyle ngrt = estilo(lbr, true, false, null, HorizontalAlignment.LEFT);
            CellStyle encbzd = estilo(lbr, true, true, IndexedColors.GREY_25_PERCENT, HorizontalAlignment.CENTER);
            CellStyle texto = estilo(lbr, false, true, null, HorizontalAlignment.LEFT);
            CellStyle nmr = estilo(lbr, false, true, null, HorizontalAlignment.RIGHT);
            CellStyle rsmnTxt = estilo(lbr, true, true, IndexedColors.LIGHT_CORNFLOWER_BLUE, HorizontalAlignment.LEFT);
            CellStyle rsmnNmr = estilo(lbr, true, true, IndexedColors.LIGHT_CORNFLOWER_BLUE, HorizontalAlignment.RIGHT);

            int fl = 0;
            if (insttcn != null && !insttcn.isBlank()) {
                celda(hj.createRow(fl++), 0, insttcn, ngrt);
            }
            celda(hj.createRow(fl++), 0, tbl.titulo(), ngrt);
            for (String ln : tbl.subtitulo()) {
                celda(hj.createRow(fl++), 0, ln, null);
            }
            fl++;

            if (tbl.tieneGrupos()) {
                Row flGrps = hj.createRow(fl);
                int col = 0;
                for (Grupo g : tbl.grupos()) {
                    for (int k = 0; k < g.columnas(); k++) {
                        celda(flGrps, col + k, k == 0 ? g.texto() : "", encbzd);
                    }
                    if (g.columnas() > 1) {
                        hj.addMergedRegion(new CellRangeAddress(fl, fl, col, col + g.columnas() - 1));
                    }
                    col += g.columnas();
                }
                fl++;
            }
            Row flClmns = hj.createRow(fl);
            for (int c = 0; c < columnas; c++) {
                celda(flClmns, c, tbl.columnas().get(c), encbzd);
            }
            hj.createFreezePane(0, fl + 1);
            fl++;

            for (int indc = 0; indc < tbl.filas().size(); indc++) {
                List<String> dts = tbl.filas().get(indc);
                if (tbl.esSeccion(indc)) {
                    Row frnj = hj.createRow(fl);
                    for (int c = 0; c < columnas; c++) {
                        celda(frnj, c, c == 0 ? TablaReporte.textoDeSeccion(dts) : "", rsmnTxt);
                    }
                    if (columnas > 1) {
                        hj.addMergedRegion(new CellRangeAddress(fl, fl, 0, columnas - 1));
                    }
                    fl++;
                    continue;
                }
                boolean rsmn = dts.contains("TOTAL") || dts.contains("PROMEDIO DEL CURSO");
                Row r = hj.createRow(fl++);
                for (int c = 0; c < columnas; c++) {
                    String vlr = c < dts.size() ? dts.get(c) : "";
                    boolean nmrc = tbl.numericas().get(c);
                    Double prsd = nmrc ? comoNumero(vlr) : null;
                    Cell cel = r.createCell(c);
                    if (prsd != null) {
                        cel.setCellValue(prsd);
                    } else {
                        cel.setCellValue(vlr);
                    }
                    cel.setCellStyle(rsmn ? (nmrc ? rsmnNmr : rsmnTxt) : (nmrc ? nmr : texto));
                }
            }
            if (!tbl.pie().isEmpty()) {
                fl++;
                for (String nt : tbl.pie()) {
                    celda(hj.createRow(fl++), 0, nt, null);
                }
            }
            fl++;
            celda(hj.createRow(fl), 0, "Fecha de emision: " + LocalDate.now().format(FECHA), null);

            for (int c = 0; c < columnas; c++) {
                hj.setColumnWidth(c, Math.min(255, Math.max(4, tbl.anchos().get(c) * 11 / 10 + 2)) * 256);
            }
            hj.getPrintSetup().setLandscape(true);
            hj.getPrintSetup().setFitWidth((short) 1);
            hj.getPrintSetup().setFitHeight((short) 0);
            hj.setFitToPage(true);
            lbr.write(sld);
        } catch (IOException e) {
            throw new SgfaException("No se pudo escribir el archivo Excel.", e);
        }
    }

    private static Double comoNumero(String vlr) {
        if (vlr == null || !vlr.matches("-?\\d+([.,]\\d+)?")) {
            return null;
        }
        return Double.parseDouble(vlr.replace(',', '.'));
    }

    private static String nombreHoja(String titulo) {
        String lmp = titulo.replaceAll("[\\\\/?*\\[\\]:]", " ").strip();
        return lmp.length() > 31 ? lmp.substring(0, 31) : lmp;
    }

    private static void celda(Row fl, int clmn, String texto, CellStyle estilo) {
        Cell c = fl.createCell(clmn);
        c.setCellValue(texto);
        if (estilo != null) {
            c.setCellStyle(estilo);
        }
    }

    private static CellStyle estilo(Workbook lbr, boolean ngrt, boolean brd, IndexedColors fnd, HorizontalAlignment alncn) {
        CellStyle e = lbr.createCellStyle();
        Font f = lbr.createFont();
        f.setBold(ngrt);
        e.setFont(f);
        e.setAlignment(alncn);
        e.setVerticalAlignment(VerticalAlignment.CENTER);
        e.setWrapText(true);
        if (brd) {
            e.setBorderBottom(BorderStyle.THIN);
            e.setBorderTop(BorderStyle.THIN);
            e.setBorderLeft(BorderStyle.THIN);
            e.setBorderRight(BorderStyle.THIN);
        }
        if (fnd != null) {
            e.setFillForegroundColor(fnd.getIndex());
            e.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        return e;
    }
}
