package pe.edu.sgfa.nota.service;

public final class YmclEscalaLiteral {

    public static final String DESTACADO = "DESTACADO";
    public static final String LOGRADO = "LOGRADO";
    public static final String PROCESO = "PROCESO";
    public static final String INICIO = "INICIO";
    public static final String PREVIO_INICIO = "PREVIO_INICIO";

    private YmclEscalaLiteral() {
    }

    public static String ymclCodigoPara(Double notaChp) {
        if (notaChp == null || notaChp == 0) {
            return PREVIO_INICIO;
        }
        if (notaChp >= 19) {
            return DESTACADO;
        }
        if (notaChp >= 15) {
            return LOGRADO;
        }
        if (notaChp >= 11) {
            return PROCESO;
        }
        return INICIO;
    }
}
