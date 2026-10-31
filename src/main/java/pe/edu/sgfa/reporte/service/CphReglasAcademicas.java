package pe.edu.sgfa.reporte.service;

public final class CphReglasAcademicas {

    public static final double NOTA_MINIMA_APROBATORIA = 11;
    public static final double PORCENTAJE_PROMOCION = 0.75;

    private CphReglasAcademicas() {
    }

    public static boolean cphAprobada(Double notaPch) {
        return notaPch != null && notaPch >= NOTA_MINIMA_APROBATORIA;
    }
}
