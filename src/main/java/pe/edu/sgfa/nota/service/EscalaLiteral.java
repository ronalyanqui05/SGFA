package pe.edu.sgfa.nota.service;

public final class EscalaLiteral {

    public static final String DESTACADO = "DESTACADO";
    public static final String LOGRADO = "LOGRADO";
    public static final String PROCESO = "PROCESO";
    public static final String INICIO = "INICIO";
    public static final String PREVIO_INICIO = "PREVIO_INICIO";

    private EscalaLiteral() {
    }

    public static String codigoPara(Double nota) {
        if (nota == null || nota == 0) {
            return PREVIO_INICIO;
        }
        if (nota >= 19) {
            return DESTACADO;
        }
        if (nota >= 15) {
            return LOGRADO;
        }
        if (nota >= 11) {
            return PROCESO;
        }
        return INICIO;
    }
}
