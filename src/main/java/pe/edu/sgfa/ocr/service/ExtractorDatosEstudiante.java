package pe.edu.sgfa.ocr.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ExtractorDatosEstudiante {

    private static final Pattern DNI = Pattern.compile("\\b(\\d{8})\\b");
    private static final Pattern NOMBRES = Pattern.compile(
            "(?im)^\\s*NOMBRES?\\s*[:\\-]?\\s*(.+)$");
    private static final Pattern APELLIDO_PATERNO = Pattern.compile(
            "(?im)^\\s*APELLIDO\\s*PATERNO\\s*[:\\-]?\\s*(.+)$");
    private static final Pattern APELLIDO_MATERNO = Pattern.compile(
            "(?im)^\\s*APELLIDO\\s*MATERNO\\s*[:\\-]?\\s*(.+)$");
    private static final Pattern APELLIDOS_JUNTOS = Pattern.compile(
            "(?im)^\\s*APELLIDOS\\s*[:\\-]?\\s*(.+)$");
    private static final Pattern FECHA_NACIMIENTO = Pattern.compile(
            "(?im)FECHA\\s*(DE\\s*)?NACIMIENTO\\s*[:\\-]?\\s*(\\d{1,2}[/\\-]\\d{1,2}[/\\-]\\d{2,4})");
    private static final DateTimeFormatter[] FORMATOS_FECHA = {
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("d-M-yyyy"),
    };

    private ExtractorDatosEstudiante() {
    }

    public static DatosDetectados extraer(String txtOcr) {
        if (txtOcr == null || txtOcr.isBlank()) {
            return new DatosDetectados(null, null, null, null, null);
        }
        String dni = buscar(DNI, txtOcr);
        String nombres = buscar(NOMBRES, txtOcr);
        String apellidoPaterno = buscar(APELLIDO_PATERNO, txtOcr);
        String apellidoMaterno = buscar(APELLIDO_MATERNO, txtOcr);
        if (apellidoPaterno == null) {

            String aplldsJnts = buscar(APELLIDOS_JUNTOS, txtOcr);
            if (aplldsJnts != null) {
                String[] prts = aplldsJnts.trim().split("\\s+", 2);
                apellidoPaterno = prts[0];
                apellidoMaterno = prts.length > 1 ? prts[1] : null;
            }
        }
        LocalDate fechaNacimiento = buscarFecha(txtOcr);
        return new DatosDetectados(dni, nombres, apellidoPaterno, apellidoMaterno, fechaNacimiento);
    }

    private static String buscar(Pattern ptrn, String txt) {
        Matcher m = ptrn.matcher(txt);
        if (!m.find()) {
            return null;
        }
        String vlr = m.group(m.groupCount()).strip();
        return vlr.isEmpty() ? null : vlr;
    }

    private static LocalDate buscarFecha(String txt) {
        Matcher m = FECHA_NACIMIENTO.matcher(txt);
        if (!m.find()) {
            return null;
        }
        String vlr = m.group(2);
        for (DateTimeFormatter frmt : FORMATOS_FECHA) {
            try {
                return LocalDate.parse(vlr, frmt);
            } catch (DateTimeParseException ignrd) {

            }
        }
        return null;
    }

    public record DatosDetectados(String dni, String nombres, String apellidoPaterno, String apellidoMaterno,
                                   LocalDate fechaNacimiento) {
    }
}
