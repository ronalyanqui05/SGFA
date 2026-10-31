package pe.edu.sgfa.reporte.domain;

public enum CphTipoDocumento {
    ACTA("AC", "Acta consolidada"),
    ORDEN_MERITO("OM", "Orden de merito");

    private final String prefijo;
    private final String nombre;

    CphTipoDocumento(String prefijoPch, String nombrePch) {
        this.prefijo = prefijoPch;
        this.nombre = nombrePch;
    }

    public String cphPrefijo() {
        return prefijo;
    }

    public String cphNombre() {
        return nombre;
    }
}
