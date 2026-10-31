package pe.edu.sgfa.ficha.service;

public final class RacaTituloCiclo {

    private static final String[] ORDINALES_CRR = {"PRIMER", "SEGUNDO", "TERCER", "CUARTO", "QUINTO", "SEXTO",
            "SÉPTIMO", "OCTAVO", "NOVENO", "DÉCIMO"};

    private RacaTituloCiclo() {
    }

    public static String de(int numeroCrr) {
        if (numeroCrr < 1 || numeroCrr > ORDINALES_CRR.length) {
            return "CICLO " + numeroCrr;
        }
        return ORDINALES_CRR[numeroCrr - 1] + " CICLO";
    }
}
