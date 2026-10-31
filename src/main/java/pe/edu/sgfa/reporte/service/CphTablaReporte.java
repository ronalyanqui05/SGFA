package pe.edu.sgfa.reporte.service;

import java.util.List;
import java.util.Set;

public record CphTablaReporte(String titulo, List<String> subtitulo, List<CphGrupo> grupos, List<String> columnas,
                           List<Integer> anchos, List<Boolean> numericas, List<List<String>> filas, List<String> pie,
                           boolean firmas, Set<Integer> secciones) {

    public CphTablaReporte(String titulo, List<String> subtitulo, List<CphGrupo> grupos, List<String> columnas,
                        List<Integer> anchos, List<Boolean> numericas, List<List<String>> filas, List<String> pie,
                        boolean firmas) {
        this(titulo, subtitulo, grupos, columnas, anchos, numericas, filas, pie, firmas, Set.of());
    }

    public boolean cphEsSeccion(int indiceFilaPch) {
        return secciones != null && secciones.contains(indiceFilaPch);
    }

    public static String cphTextoDeSeccion(List<String> filaPch) {
        return filaPch.stream().filter(s -> s != null && !s.isBlank()).findFirst().orElse("");
    }

    public record CphGrupo(String texto, int columnas) {
    }

    public boolean cphTieneGrupos() {
        return grupos != null && !grupos.isEmpty();
    }
}
