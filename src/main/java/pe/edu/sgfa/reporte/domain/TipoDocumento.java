package pe.edu.sgfa.reporte.domain;

public enum TipoDocumento {
    ACTA("AC", "Acta consolidada"),
    ORDEN_MERITO("OM", "Orden de merito");

    private final String prefijo;
    private final String nombre;

    TipoDocumento(String prefijo, String nombre) {
        this.prefijo = prefijo;
        this.nombre = nombre;
    }

    public String prefijo() {
        return prefijo;
    }

    public String nombre() {
        return nombre;
    }
}
