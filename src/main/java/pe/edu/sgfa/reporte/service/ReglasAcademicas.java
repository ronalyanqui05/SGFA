package pe.edu.sgfa.reporte.service;

public final class ReglasAcademicas {

    public static final double NOTA_MINIMA_APROBATORIA = 11;
    public static final double PORCENTAJE_PROMOCION = 0.75;

    private ReglasAcademicas() {
    }

    public static boolean aprobada(Double nt) {
        return nt != null && nt >= NOTA_MINIMA_APROBATORIA;
    }
}
