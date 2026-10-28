package pe.edu.sgfa.ficha.service;

public final class TituloCiclo {

    private static final String[] ORDINALES = {"PRIMER", "SEGUNDO", "TERCER", "CUARTO", "QUINTO", "SEXTO",
            "SÉPTIMO", "OCTAVO", "NOVENO", "DÉCIMO"};

    private TituloCiclo() {
    }

    public static String de(int nmr) {
        if (nmr < 1 || nmr > ORDINALES.length) {
            return "CICLO " + nmr;
        }
        return ORDINALES[nmr - 1] + " CICLO";
    }
}
