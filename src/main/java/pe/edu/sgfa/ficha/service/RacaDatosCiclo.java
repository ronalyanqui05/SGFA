package pe.edu.sgfa.ficha.service;

import java.util.List;

import pe.edu.sgfa.nota.service.YmclResultadoPromedio;

public record RacaDatosCiclo(int numero, String nombre, List<RacaFilaFichaCiclo> filas, YmclResultadoPromedio promedio) {
}
