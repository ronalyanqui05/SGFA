package pe.edu.sgfa.ocr.service;

import java.util.List;
import java.util.Map;

import pe.edu.sgfa.nota.service.YmclEscalaLiteral;

public final class RahpLectorSituacion {

    private static final Map<String, String> POR_LETRA_HNC = Map.of(
            "D", YmclEscalaLiteral.DESTACADO,
            "L", YmclEscalaLiteral.LOGRADO,
            "E", YmclEscalaLiteral.PROCESO,
            "I", YmclEscalaLiteral.INICIO,
            "P", YmclEscalaLiteral.PREVIO_INICIO);

    private static final List<Map.Entry<String, String>> POR_PALABRA_HNC = List.of(
            Map.entry("DESTACADO", YmclEscalaLiteral.DESTACADO),
            Map.entry("LOGRADO", YmclEscalaLiteral.LOGRADO),
            Map.entry("EN PROCESO", YmclEscalaLiteral.PROCESO),
            Map.entry("PREVIO AL INICIO", YmclEscalaLiteral.PREVIO_INICIO),
            Map.entry("INICIO", YmclEscalaLiteral.INICIO));

    private static final double UMBRAL_HNC = 0.7;

    private RahpLectorSituacion() {
    }

    public static String rahpCodigoDe(String textoHnc) {
        String normalizadoHnc = RahpEmparejadorNombres.rahpNormalizar(textoHnc);
        if (normalizadoHnc.isEmpty()) {
            return null;
        }
        if (normalizadoHnc.length() == 1) {
            return POR_LETRA_HNC.get(normalizadoHnc);
        }

        if (normalizadoHnc.equals("EN") || normalizadoHnc.startsWith("EN ")) {
            return YmclEscalaLiteral.PROCESO;
        }
        if (normalizadoHnc.startsWith("PREVIO")) {
            return YmclEscalaLiteral.PREVIO_INICIO;
        }
        String mejorHnc = null;
        double mejorPuntajeHnc = 0;
        for (Map.Entry<String, String> candidatoHnc : POR_PALABRA_HNC) {
            double puntajeHnc = RahpEmparejadorNombres.rahpSimilitud(normalizadoHnc, candidatoHnc.getKey());
            if (puntajeHnc > mejorPuntajeHnc) {
                mejorPuntajeHnc = puntajeHnc;
                mejorHnc = candidatoHnc.getValue();
            }
        }
        return mejorPuntajeHnc >= UMBRAL_HNC ? mejorHnc : null;
    }

    public static String rahpCodigoDe(List<String> palabrasHnc) {
        String juntoHnc = rahpCodigoDe(String.join(" ", palabrasHnc));
        if (juntoHnc != null) {
            return juntoHnc;
        }
        for (String palabraHnc : palabrasHnc) {
            String codigoHnc = rahpCodigoDe(palabraHnc);
            if (codigoHnc != null) {
                return codigoHnc;
            }
        }
        return null;
    }
}
