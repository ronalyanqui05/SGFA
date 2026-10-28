package pe.edu.sgfa.ficha.service;

import java.util.List;

import pe.edu.sgfa.nota.service.ResultadoPromedio;

public record DatosCiclo(int numero, String nombre, List<FilaFichaCiclo> filas, ResultadoPromedio promedio) {
}
