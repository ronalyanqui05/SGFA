package pe.edu.sgfa.importacion.service;

import java.util.List;

public record CphResultadoImportacion(int totalFilas, int exitosas, List<CphErrorImportacion> errores) {

    public boolean cphTieneErrores() {
        return !errores.isEmpty();
    }
}
