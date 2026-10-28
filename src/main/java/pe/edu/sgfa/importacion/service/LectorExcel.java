package pe.edu.sgfa.importacion.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;

final class LectorExcel {

    private static final DataFormatter FORMATEADOR = new DataFormatter();

    private LectorExcel() {
    }

    static String texto(Row fl, int clmn) {
        if (fl == null) {
            return null;
        }
        Cell cld = fl.getCell(clmn);
        if (cld == null) {
            return null;
        }
        String vlr = FORMATEADOR.formatCellValue(cld).trim();
        return vlr.isEmpty() ? null : vlr;
    }

    static Integer entero(Row fl, int clmn) {
        String texto = texto(fl, clmn);
        if (texto == null) {
            return null;
        }
        try {
            return (int) Double.parseDouble(texto.replace(",", "."));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("\"" + texto + "\" no es un numero valido.");
        }
    }

    static Double decimal(Row fl, int clmn) {
        String texto = texto(fl, clmn);
        if (texto == null) {
            return null;
        }
        try {
            return Double.parseDouble(texto.replace(",", "."));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("\"" + texto + "\" no es un numero valido.");
        }
    }

    static LocalDate fecha(Row fl, int clmn) {
        if (fl == null) {
            return null;
        }
        Cell cld = fl.getCell(clmn);
        if (cld == null) {
            return null;
        }
        if (cld.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cld)) {
            return cld.getLocalDateTimeCellValue().toLocalDate();
        }
        String texto = texto(fl, clmn);
        if (texto == null) {
            return null;
        }
        try {
            return LocalDate.parse(texto);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("\"" + texto + "\" no es una fecha valida (use AAAA-MM-DD).");
        }
    }

    static String textoObligatorio(Row fl, int clmn, String nmbrCmp) {
        String vlr = texto(fl, clmn);
        if (vlr == null) {
            throw new IllegalArgumentException("Falta el dato \"" + nmbrCmp + "\".");
        }
        return vlr;
    }

    static boolean filaVacia(Row fl, int clmns) {
        if (fl == null) {
            return true;
        }
        for (int i = 0; i < clmns; i++) {
            if (texto(fl, i) != null) {
                return false;
            }
        }
        return true;
    }

    static String normalizarMayusculas(String texto) {
        return texto == null ? null : texto.trim().toUpperCase(Locale.ROOT);
    }
}
