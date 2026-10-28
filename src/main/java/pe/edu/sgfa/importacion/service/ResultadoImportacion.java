package pe.edu.sgfa.importacion.service;

import java.util.List;

public record ResultadoImportacion(int totalFilas, int exitosas, List<ErrorImportacion> errores) {

    public boolean tieneErrores() {
        return !errores.isEmpty();
    }
}
