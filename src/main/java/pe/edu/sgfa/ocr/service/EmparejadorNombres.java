package pe.edu.sgfa.ocr.service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

public final class EmparejadorNombres {

    public static final double UMBRAL = 0.78;

    private static final double MARGEN = 0.03;

    private EmparejadorNombres() {
    }

    public static String normalizar(String txt) {
        if (txt == null) {
            return "";
        }
        String sinTlds = Normalizer.normalize(txt, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTlds.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9 ]", " ").replaceAll("\\s+", " ").strip();
    }

    public static double similitud(String a, String b) {
        String x = normalizar(a);
        String y = normalizar(b);
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
        int dstnc = distanciaLevenshtein(x, y);
        double porLtrs = 1.0 - (double) dstnc / Math.max(x.length(), y.length());

        return Math.max(porLtrs, 0.97 * similitudPorPalabras(x, y));
    }

    private static double similitudPorPalabras(String x, String y) {
        String[] a = x.split(" ");
        String[] b = y.split(" ");
        double ttl = java.util.Arrays.stream(a).mapToInt(String::length).sum()
                + java.util.Arrays.stream(b).mapToInt(String::length).sum();
        return (mejoresParejas(a, b) + mejoresParejas(b, a)) / ttl;
    }

    private static double mejoresParejas(String[] de, String[] en) {
        double sm = 0;
        for (String plbr : de) {
            double mjr = 0;
            for (String otr : en) {
                int d = distanciaLevenshtein(plbr, otr);
                mjr = Math.max(mjr, 1.0 - (double) d / Math.max(plbr.length(), otr.length()));
            }
            sm += mjr >= 0.7 ? mjr * plbr.length() : 0;
        }
        return sm;
    }

    public static <T> Optional<T> mejorCoincidencia(String txt, List<T> cnddts, Function<T, String> nmbrDe) {
        T mjr = null;
        double pntjMjr = 0;
        double pntjSgnd = 0;
        for (T cnddt : cnddts) {
            double pntj = similitud(txt, nmbrDe.apply(cnddt));
            if (pntj > pntjMjr) {
                pntjSgnd = pntjMjr;
                pntjMjr = pntj;
                mjr = cnddt;
            } else if (pntj > pntjSgnd) {
                pntjSgnd = pntj;
            }
        }
        boolean exct = pntjMjr >= 1.0;
        if (mjr == null || pntjMjr < UMBRAL || (!exct && pntjMjr - pntjSgnd < MARGEN)) {
            return Optional.empty();
        }
        return Optional.of(mjr);
    }

    public static double cobertura(String nmbr, String txt) {
        List<String> plbrsNmbr = java.util.Arrays.stream(normalizar(nmbr).split(" "))
                .filter(p -> p.length() >= 4).toList();
        if (plbrsNmbr.isEmpty()) {
            return 0;
        }
        String[] plbrsTxt = normalizar(txt).split(" ");
        long encntrds = plbrsNmbr.stream()
                .filter(p -> java.util.Arrays.stream(plbrsTxt).anyMatch(t -> similitud(p, t) >= 0.8))
                .count();
        return (double) encntrds / plbrsNmbr.size();
    }

    private static int distanciaLevenshtein(String a, String b) {
        int[] antrr = new int[b.length() + 1];
        int[] actl = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            antrr[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            actl[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cst = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                actl[j] = Math.min(Math.min(actl[j - 1] + 1, antrr[j] + 1), antrr[j - 1] + cst);
            }
            int[] aux = antrr;
            antrr = actl;
            actl = aux;
        }
        return antrr[b.length()];
    }
}
