package pe.edu.sgfa.reporte.service;

import java.util.List;
import java.util.Set;

public record TablaReporte(String titulo, List<String> subtitulo, List<Grupo> grupos, List<String> columnas,
                           List<Integer> anchos, List<Boolean> numericas, List<List<String>> filas, List<String> pie,
                           boolean firmas, Set<Integer> secciones) {

    public TablaReporte(String titulo, List<String> subtitulo, List<Grupo> grupos, List<String> columnas,
                        List<Integer> anchos, List<Boolean> numericas, List<List<String>> filas, List<String> pie,
                        boolean firmas) {
        this(titulo, subtitulo, grupos, columnas, anchos, numericas, filas, pie, firmas, Set.of());
    }

    public boolean esSeccion(int indcFl) {
        return secciones != null && secciones.contains(indcFl);
    }

    public static String textoDeSeccion(List<String> fl) {
        return fl.stream().filter(s -> s != null && !s.isBlank()).findFirst().orElse("");
    }

    public record Grupo(String texto, int columnas) {
    }

    public boolean tieneGrupos() {
        return grupos != null && !grupos.isEmpty();
    }
}
