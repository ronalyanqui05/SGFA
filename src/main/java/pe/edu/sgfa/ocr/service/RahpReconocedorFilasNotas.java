package pe.edu.sgfa.ocr.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pe.edu.sgfa.nota.service.YmclFilaRegistro;
import pe.edu.sgfa.nota.service.YmclItemNota;
import pe.edu.sgfa.programa.domain.RacaPlanCurso;

public final class RahpReconocedorFilasNotas {

    private static final Pattern NOTA_AL_FINAL_HNC = Pattern.compile("(\\d{1,2}([.,]\\d{1,2})?)\\s*$");

    private RahpReconocedorFilasNotas() {
    }

    public static RahpResultado rahpReconocer(String textoHnc, List<RacaPlanCurso> candidatosHnc, Long situacionIdHnc) {
        List<RahpItemNotaReconocida> itemsHnc = new ArrayList<>();
        List<String> advertenciasHnc = new ArrayList<>();
        if (textoHnc == null) {
            return new RahpResultado(itemsHnc, advertenciasHnc);
        }

        for (String lineaOriginalHnc : textoHnc.split("\\R")) {
            String lineaHnc = lineaOriginalHnc.strip();
            if (lineaHnc.isEmpty()) {
                continue;
            }

            Matcher notaMatcherHnc = NOTA_AL_FINAL_HNC.matcher(lineaHnc);
            if (!notaMatcherHnc.find()) {
                advertenciasHnc.add("\"" + lineaHnc + "\": no se encontro una nota (0-20) al final de la linea.");
                continue;
            }
            double notaHnc;
            try {
                notaHnc = Double.parseDouble(notaMatcherHnc.group(1).replace(',', '.'));
            } catch (NumberFormatException e) {
                advertenciasHnc.add("\"" + lineaHnc + "\": la nota no es un numero valido.");
                continue;
            }
            if (notaHnc < 0 || notaHnc > 20) {
                advertenciasHnc.add("\"" + lineaHnc + "\": la nota " + notaHnc + " esta fuera del rango 0-20.");
                continue;
            }

            String textoCursoHnc = lineaHnc.substring(0, notaMatcherHnc.start()).strip();
            if (textoCursoHnc.isEmpty()) {
                advertenciasHnc.add("\"" + lineaHnc + "\": no se encontro el nombre del curso antes de la nota.");
                continue;
            }

            List<RacaPlanCurso> coincidenciasHnc = rahpEmparejarCurso(textoCursoHnc, candidatosHnc);
            if (coincidenciasHnc.isEmpty()) {
                advertenciasHnc.add("\"" + lineaHnc + "\": no se identifico ningun curso del plan para \"" + textoCursoHnc + "\".");
                continue;
            }
            if (coincidenciasHnc.size() > 1) {
                advertenciasHnc.add("\"" + lineaHnc + "\": \"" + textoCursoHnc + "\" coincide con varios cursos, no se pudo elegir uno solo.");
                continue;
            }

            RacaPlanCurso cursoHnc = coincidenciasHnc.get(0);
            YmclItemNota itemHnc = new YmclItemNota(cursoHnc.getId(), notaHnc, situacionIdHnc, null);
            itemsHnc.add(new RahpItemNotaReconocida(cursoHnc.getCurso().getCodigo() + " - " + cursoHnc.getCurso().getNombre(), notaHnc, itemHnc));
        }

        return new RahpResultado(itemsHnc, advertenciasHnc);
    }

    private static List<RacaPlanCurso> rahpEmparejarCurso(String textoCursoHnc, List<RacaPlanCurso> candidatosHnc) {

        String normalizadoHnc = rahpNormalizar(textoCursoHnc).replaceAll("[:\\-.,]+$", "").strip();

        List<RacaPlanCurso> porCodigoHnc = candidatosHnc.stream()
                .filter(pc -> rahpNormalizar(pc.getCurso().getCodigo()).equals(normalizadoHnc)
                        || normalizadoHnc.startsWith(rahpNormalizar(pc.getCurso().getCodigo()) + " "))
                .toList();
        if (!porCodigoHnc.isEmpty()) {
            return porCodigoHnc;
        }

        List<RacaPlanCurso> porNombreHnc = candidatosHnc.stream()
                .filter(pc -> {
                    String nombreHnc = rahpNormalizar(pc.getCurso().getNombre());
                    return normalizadoHnc.contains(nombreHnc) || nombreHnc.contains(normalizadoHnc);
                })
                .toList();
        return porNombreHnc;
    }

    private static String rahpNormalizar(String textoHnc) {
        if (textoHnc == null) {
            return "";
        }
        String sinTildesHnc = java.text.Normalizer.normalize(textoHnc, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinTildesHnc.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    public static List<RacaPlanCurso> rahpCursosDe(List<YmclFilaRegistro> candidatosHnc) {
        return candidatosHnc.stream().map(YmclFilaRegistro::planCurso).toList();
    }

    public record RahpItemNotaReconocida(String curso, double nota, YmclItemNota item) {
    }

    public record RahpResultado(List<RahpItemNotaReconocida> items, List<String> advertencias) {
    }
}
