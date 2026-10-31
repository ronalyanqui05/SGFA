package pe.edu.sgfa.ocr.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

public final class RahpReconocedorActa {

    private static final Pattern MATRICULA_HNC = Pattern.compile("^\\d{7,8}$");
    private static final Pattern NUMERO_CUALQUIERA_HNC = Pattern.compile("^\\d+$");
    private static final Pattern SOLO_LETRAS_HNC = Pattern.compile("^[\\p{L}]+$");

    private RahpReconocedorActa() {
    }

    public static List<RahpFilaActaDetectada> rahpAgrupar(List<RahpPalabraReconocida> palabrasHnc, int cantidadCursosHnc) {
        List<List<RahpPalabraReconocida>> filasHnc = rahpAgruparEnFilas(palabrasHnc);
        List<RahpFilaActaDetectada> resultadoHnc = new ArrayList<>();
        for (List<RahpPalabraReconocida> filaHnc : filasHnc) {
            resultadoHnc.add(rahpInterpretarFila(filaHnc, cantidadCursosHnc));
        }
        return resultadoHnc;
    }

    private static List<List<RahpPalabraReconocida>> rahpAgruparEnFilas(List<RahpPalabraReconocida> palabrasHnc) {
        List<RahpPalabraReconocida> ordenadasHnc = palabrasHnc.stream()
                .sorted(Comparator.comparingInt(RahpPalabraReconocida::rahpCentroY))
                .toList();
        if (ordenadasHnc.isEmpty()) {
            return List.of();
        }

        int alturaTipicaHnc = (int) ordenadasHnc.stream().mapToInt(RahpPalabraReconocida::alto).average().orElse(20);
        int toleranciaHnc = Math.max(8, alturaTipicaHnc / 2);

        List<List<RahpPalabraReconocida>> filasHnc = new ArrayList<>();
        List<RahpPalabraReconocida> filaActualHnc = new ArrayList<>();
        int centroFilaActualHnc = Integer.MIN_VALUE;

        for (RahpPalabraReconocida palabraHnc : ordenadasHnc) {
            if (filaActualHnc.isEmpty() || Math.abs(palabraHnc.rahpCentroY() - centroFilaActualHnc) <= toleranciaHnc) {
                filaActualHnc.add(palabraHnc);
                centroFilaActualHnc = rahpPromedioCentro(filaActualHnc);
            } else {
                filasHnc.add(rahpOrdenarPorX(filaActualHnc));
                filaActualHnc = new ArrayList<>(List.of(palabraHnc));
                centroFilaActualHnc = palabraHnc.rahpCentroY();
            }
        }
        if (!filaActualHnc.isEmpty()) {
            filasHnc.add(rahpOrdenarPorX(filaActualHnc));
        }
        return filasHnc;
    }

    private static int rahpPromedioCentro(List<RahpPalabraReconocida> filaHnc) {
        return (int) filaHnc.stream().mapToInt(RahpPalabraReconocida::rahpCentroY).average().orElse(0);
    }

    private static List<RahpPalabraReconocida> rahpOrdenarPorX(List<RahpPalabraReconocida> filaHnc) {
        return filaHnc.stream().sorted(Comparator.comparingInt(RahpPalabraReconocida::x)).toList();
    }

    private static RahpFilaActaDetectada rahpInterpretarFila(List<RahpPalabraReconocida> filaHnc, int cantidadCursosHnc) {
        List<String> tokensHnc = filaHnc.stream().map(RahpPalabraReconocida::texto).toList();
        int i = 0;

        String ordenNroHnc = null;
        if (i < tokensHnc.size() && NUMERO_CUALQUIERA_HNC.matcher(tokensHnc.get(i)).matches() && tokensHnc.get(i).length() <= 3) {
            ordenNroHnc = tokensHnc.get(i);
            i++;
        }

        String matriculaHnc = null;
        if (i < tokensHnc.size() && MATRICULA_HNC.matcher(tokensHnc.get(i)).matches()) {
            matriculaHnc = tokensHnc.get(i);
            i++;
        }

        List<String> nombreCompletoHnc = new ArrayList<>();
        while (i < tokensHnc.size() && SOLO_LETRAS_HNC.matcher(tokensHnc.get(i)).matches() && tokensHnc.get(i).length() > 1) {
            nombreCompletoHnc.add(tokensHnc.get(i));
            i++;
        }

        String apellidoPaternoHnc = nombreCompletoHnc.size() > 0 ? nombreCompletoHnc.get(0) : null;
        String apellidoMaternoHnc = nombreCompletoHnc.size() > 1 ? nombreCompletoHnc.get(1) : null;
        String nombresHnc = nombreCompletoHnc.size() > 2
                ? String.join(" ", nombreCompletoHnc.subList(2, nombreCompletoHnc.size())) : null;

        List<RahpFilaActaDetectada.RahpNotaCurso> notasHnc = new ArrayList<>();
        List<String> restantesHnc = tokensHnc.subList(Math.min(i, tokensHnc.size()), tokensHnc.size());
        int j = 0;
        for (int cursoHnc = 0; cursoHnc < cantidadCursosHnc; cursoHnc++) {
            String situacionHnc = j < restantesHnc.size() ? restantesHnc.get(j) : null;
            String notaHnc = j + 1 < restantesHnc.size() ? restantesHnc.get(j + 1) : null;
            notasHnc.add(new RahpFilaActaDetectada.RahpNotaCurso(situacionHnc, notaHnc));

            int avanceHnc = 2;
            if (j + 2 < restantesHnc.size() && NUMERO_CUALQUIERA_HNC.matcher(restantesHnc.get(j + 2)).matches()
                    && notaHnc != null && NUMERO_CUALQUIERA_HNC.matcher(notaHnc).matches()) {
                avanceHnc = 3;
            }
            j += avanceHnc;
        }

        return new RahpFilaActaDetectada(ordenNroHnc, matriculaHnc, apellidoPaternoHnc, apellidoMaternoHnc, nombresHnc, notasHnc);
    }

    public record RahpFilaActaDetectada(String ordenNro, String matricula, String apellidoPaterno,
                                     String apellidoMaterno, String nombres, List<RahpNotaCurso> notasPorCurso) {

        public record RahpNotaCurso(String situacionTexto, String notaTexto) {
        }
    }
}
