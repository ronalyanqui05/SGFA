package pe.edu.sgfa.ocr.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RahpExtractorDatosEstudiante {

    private static final Pattern DNI_HNC = Pattern.compile("\\b(\\d{8})\\b");
    private static final Pattern NOMBRES_HNC = Pattern.compile(
            "(?im)^\\s*NOMBRES?\\s*[:\\-]?\\s*(.+)$");
    private static final Pattern APELLIDO_PATERNO_HNC = Pattern.compile(
            "(?im)^\\s*APELLIDO\\s*PATERNO\\s*[:\\-]?\\s*(.+)$");
    private static final Pattern APELLIDO_MATERNO_HNC = Pattern.compile(
            "(?im)^\\s*APELLIDO\\s*MATERNO\\s*[:\\-]?\\s*(.+)$");
    private static final Pattern APELLIDOS_JUNTOS_HNC = Pattern.compile(
            "(?im)^\\s*APELLIDOS\\s*[:\\-]?\\s*(.+)$");
    private static final Pattern FECHA_NACIMIENTO_HNC = Pattern.compile(
            "(?im)FECHA\\s*(DE\\s*)?NACIMIENTO\\s*[:\\-]?\\s*(\\d{1,2}[/\\-]\\d{1,2}[/\\-]\\d{2,4})");
    private static final DateTimeFormatter[] FORMATOS_FECHA_HNC = {
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("d-M-yyyy"),
    };

    private RahpExtractorDatosEstudiante() {
    }

    public static RahpDatosDetectados rahpExtraer(String textoOcrHnc) {
        if (textoOcrHnc == null || textoOcrHnc.isBlank()) {
            return new RahpDatosDetectados(null, null, null, null, null);
        }
        String dniHnc = rahpBuscar(DNI_HNC, textoOcrHnc);
        String nombresHnc = rahpBuscar(NOMBRES_HNC, textoOcrHnc);
        String apellidoPaternoHnc = rahpBuscar(APELLIDO_PATERNO_HNC, textoOcrHnc);
        String apellidoMaternoHnc = rahpBuscar(APELLIDO_MATERNO_HNC, textoOcrHnc);
        if (apellidoPaternoHnc == null) {

            String apellidosJuntosHnc = rahpBuscar(APELLIDOS_JUNTOS_HNC, textoOcrHnc);
            if (apellidosJuntosHnc != null) {
                String[] partesHnc = apellidosJuntosHnc.trim().split("\\s+", 2);
                apellidoPaternoHnc = partesHnc[0];
                apellidoMaternoHnc = partesHnc.length > 1 ? partesHnc[1] : null;
            }
        }
        LocalDate fechaNacimientoHnc = rahpBuscarFecha(textoOcrHnc);
        return new RahpDatosDetectados(dniHnc, nombresHnc, apellidoPaternoHnc, apellidoMaternoHnc, fechaNacimientoHnc);
    }

    private static String rahpBuscar(Pattern patronHnc, String textoHnc) {
        Matcher m = patronHnc.matcher(textoHnc);
        if (!m.find()) {
            return null;
        }
        String valorHnc = m.group(m.groupCount()).strip();
        return valorHnc.isEmpty() ? null : valorHnc;
    }

    private static LocalDate rahpBuscarFecha(String textoHnc) {
        Matcher m = FECHA_NACIMIENTO_HNC.matcher(textoHnc);
        if (!m.find()) {
            return null;
        }
        String valorHnc = m.group(2);
        for (DateTimeFormatter formatoHnc : FORMATOS_FECHA_HNC) {
            try {
                return LocalDate.parse(valorHnc, formatoHnc);
            } catch (DateTimeParseException ignoradaHnc) {

            }
        }
        return null;
    }

    public record RahpDatosDetectados(String dni, String nombres, String apellidoPaterno, String apellidoMaterno,
                                   LocalDate fechaNacimiento) {
    }
}
