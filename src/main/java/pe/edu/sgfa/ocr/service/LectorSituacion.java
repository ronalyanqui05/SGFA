package pe.edu.sgfa.ocr.service;

import java.util.List;
import java.util.Map;

import pe.edu.sgfa.nota.service.EscalaLiteral;

public final class LectorSituacion {

    private static final Map<String, String> POR_LETRA = Map.of(
            "D", EscalaLiteral.DESTACADO,
            "L", EscalaLiteral.LOGRADO,
            "E", EscalaLiteral.PROCESO,
            "I", EscalaLiteral.INICIO,
            "P", EscalaLiteral.PREVIO_INICIO);

    private static final List<Map.Entry<String, String>> POR_PALABRA = List.of(
            Map.entry("DESTACADO", EscalaLiteral.DESTACADO),
            Map.entry("LOGRADO", EscalaLiteral.LOGRADO),
            Map.entry("EN PROCESO", EscalaLiteral.PROCESO),
            Map.entry("PREVIO AL INICIO", EscalaLiteral.PREVIO_INICIO),
            Map.entry("INICIO", EscalaLiteral.INICIO));

    private static final double UMBRAL = 0.7;

    private LectorSituacion() {
    }

    public static String codigoDe(String txt) {
        String nrmlzd = EmparejadorNombres.normalizar(txt);
        if (nrmlzd.isEmpty()) {
            return null;
        }
        if (nrmlzd.length() == 1) {
            return POR_LETRA.get(nrmlzd);
        }

        if (nrmlzd.equals("EN") || nrmlzd.startsWith("EN ")) {
            return EscalaLiteral.PROCESO;
        }
        if (nrmlzd.startsWith("PREVIO")) {
            return EscalaLiteral.PREVIO_INICIO;
        }
        String mjr = null;
        double mjrPntj = 0;
        for (Map.Entry<String, String> cnddt : POR_PALABRA) {
            double pntj = EmparejadorNombres.similitud(nrmlzd, cnddt.getKey());
            if (pntj > mjrPntj) {
                mjrPntj = pntj;
                mjr = cnddt.getValue();
            }
        }
        return mjrPntj >= UMBRAL ? mjr : null;
    }

    public static String codigoDe(List<String> plbrs) {
        String jnt = codigoDe(String.join(" ", plbrs));
        if (jnt != null) {
            return jnt;
        }
        for (String plbr : plbrs) {
            String cdg = codigoDe(plbr);
            if (cdg != null) {
                return cdg;
            }
        }
        return null;
    }
}
