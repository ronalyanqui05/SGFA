package pe.edu.sgfa.ocr.service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

public final class RahpEmparejadorNombres {

    public static final double UMBRAL = 0.78;

    private static final double MARGEN_HNC = 0.03;

    private RahpEmparejadorNombres() {
    }

    public static String rahpNormalizar(String textoHnc) {
        if (textoHnc == null) {
            return "";
        }
        String sinTildesHnc = Normalizer.normalize(textoHnc, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildesHnc.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9 ]", " ").replaceAll("\\s+", " ").strip();
    }

    public static double rahpSimilitud(String a, String b) {
        String x = rahpNormalizar(a);
        String y = rahpNormalizar(b);
        if (x.isEmpty() || y.isEmpty()) {
            return 0;
        }
        if (x.equals(y)) {
            return 1;
        }

        if (Math.min(x.length(), y.length()) >= 8 && Math.abs(x.length() - y.length()) >= 4
                && (x.contains(y) || y.contains(x))) {
            return 0.95;
        }
        int distanciaHnc = rahpDistanciaLevenshtein(x, y);
        double porLetrasHnc = 1.0 - (double) distanciaHnc / Math.max(x.length(), y.length());

        return Math.max(porLetrasHnc, 0.97 * rahpSimilitudPorPalabras(x, y));
    }

    private static double rahpSimilitudPorPalabras(String x, String y) {
        String[] a = x.split(" ");
        String[] b = y.split(" ");
        double totalHnc = java.util.Arrays.stream(a).mapToInt(String::length).sum()
                + java.util.Arrays.stream(b).mapToInt(String::length).sum();
        return (rahpMejoresParejas(a, b) + rahpMejoresParejas(b, a)) / totalHnc;
    }

    private static double rahpMejoresParejas(String[] de, String[] en) {
        double sumaHnc = 0;
        for (String palabraHnc : de) {
            double mejorHnc = 0;
            for (String otraHnc : en) {
                int d = rahpDistanciaLevenshtein(palabraHnc, otraHnc);
                mejorHnc = Math.max(mejorHnc, 1.0 - (double) d / Math.max(palabraHnc.length(), otraHnc.length()));
            }
            sumaHnc += mejorHnc >= 0.7 ? mejorHnc * palabraHnc.length() : 0;
        }
        return sumaHnc;
    }

    public static <T> Optional<T> rahpMejorCoincidencia(String textoHnc, List<T> candidatosHnc, Function<T, String> nombreDeHnc) {
        T mejorHnc = null;
        double puntajeMejorHnc = 0;
        double puntajeSegundoHnc = 0;
        for (T candidatoHnc : candidatosHnc) {
            double puntajeHnc = rahpSimilitud(textoHnc, nombreDeHnc.apply(candidatoHnc));
            if (puntajeHnc > puntajeMejorHnc) {
                puntajeSegundoHnc = puntajeMejorHnc;
                puntajeMejorHnc = puntajeHnc;
                mejorHnc = candidatoHnc;
            } else if (puntajeHnc > puntajeSegundoHnc) {
                puntajeSegundoHnc = puntajeHnc;
            }
        }
        boolean exactoHnc = puntajeMejorHnc >= 1.0;
        if (mejorHnc == null || puntajeMejorHnc < UMBRAL || (!exactoHnc && puntajeMejorHnc - puntajeSegundoHnc < MARGEN_HNC)) {
            return Optional.empty();
        }
        return Optional.of(mejorHnc);
    }

    public static double rahpCobertura(String nombreHnc, String textoHnc) {
        List<String> palabrasNombreHnc = java.util.Arrays.stream(rahpNormalizar(nombreHnc).split(" "))
                .filter(p -> p.length() >= 4).toList();
        if (palabrasNombreHnc.isEmpty()) {
            return 0;
        }
        String[] palabrasTextoHnc = rahpNormalizar(textoHnc).split(" ");
        long encontradasHnc = palabrasNombreHnc.stream()
                .filter(p -> java.util.Arrays.stream(palabrasTextoHnc).anyMatch(t -> rahpSimilitud(p, t) >= 0.8))
                .count();
        return (double) encontradasHnc / palabrasNombreHnc.size();
    }

    private static int rahpDistanciaLevenshtein(String a, String b) {
        int[] anteriorHnc = new int[b.length() + 1];
        int[] actualHnc = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            anteriorHnc[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            actualHnc[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int costoHnc = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                actualHnc[j] = Math.min(Math.min(actualHnc[j - 1] + 1, anteriorHnc[j] + 1), anteriorHnc[j - 1] + costoHnc);
            }
            int[] auxHnc = anteriorHnc;
            anteriorHnc = actualHnc;
            actualHnc = auxHnc;
        }
        return anteriorHnc[b.length()];
    }
}
