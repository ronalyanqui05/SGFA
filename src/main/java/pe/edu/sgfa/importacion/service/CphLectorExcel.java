package pe.edu.sgfa.importacion.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;

final class CphLectorExcel {

    private static final DataFormatter FORMATEADOR_PCH = new DataFormatter();

    private CphLectorExcel() {
    }

    static String cphTexto(Row filaPch, int columnaPch) {
        if (filaPch == null) {
            return null;
        }
        Cell celdaPch = filaPch.getCell(columnaPch);
        if (celdaPch == null) {
            return null;
        }
        String valorPch = FORMATEADOR_PCH.formatCellValue(celdaPch).trim();
        return valorPch.isEmpty() ? null : valorPch;
    }

    static Integer cphEntero(Row filaPch, int columnaPch) {
        String textoPch = cphTexto(filaPch, columnaPch);
        if (textoPch == null) {
            return null;
        }
        try {
            return (int) Double.parseDouble(textoPch.replace(",", "."));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("\"" + textoPch + "\" no es un numero valido.");
        }
    }

    static Double cphDecimal(Row filaPch, int columnaPch) {
        String textoPch = cphTexto(filaPch, columnaPch);
        if (textoPch == null) {
            return null;
        }
        try {
            return Double.parseDouble(textoPch.replace(",", "."));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("\"" + textoPch + "\" no es un numero valido.");
        }
    }

    static LocalDate fecha(Row filaPch, int columnaPch) {
        if (filaPch == null) {
            return null;
        }
        Cell celdaPch = filaPch.getCell(columnaPch);
        if (celdaPch == null) {
            return null;
        }
        if (celdaPch.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(celdaPch)) {
            return celdaPch.getLocalDateTimeCellValue().toLocalDate();
        }
        String textoPch = cphTexto(filaPch, columnaPch);
        if (textoPch == null) {
            return null;
        }
        try {
            return LocalDate.parse(textoPch);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("\"" + textoPch + "\" no es una fecha valida (use AAAA-MM-DD).");
        }
    }

    static String cphTextoObligatorio(Row filaPch, int columnaPch, String nombreCampoPch) {
        String valorPch = cphTexto(filaPch, columnaPch);
        if (valorPch == null) {
            throw new IllegalArgumentException("Falta el dato \"" + nombreCampoPch + "\".");
        }
        return valorPch;
    }

    static boolean cphFilaVacia(Row filaPch, int columnasPch) {
        if (filaPch == null) {
            return true;
        }
        for (int i = 0; i < columnasPch; i++) {
            if (cphTexto(filaPch, i) != null) {
                return false;
            }
        }
        return true;
    }

    static String cphNormalizarMayusculas(String textoPch) {
        return textoPch == null ? null : textoPch.trim().toUpperCase(Locale.ROOT);
    }
}
