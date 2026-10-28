package pe.edu.sgfa.ocr.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pe.edu.sgfa.nota.service.FilaRegistro;
import pe.edu.sgfa.nota.service.ItemNota;
import pe.edu.sgfa.programa.domain.PlanCurso;

public final class ReconocedorFilasNotas {

    private static final Pattern NOTA_AL_FINAL = Pattern.compile("(\\d{1,2}([.,]\\d{1,2})?)\\s*$");

    private ReconocedorFilasNotas() {
    }

    public static Resultado reconocer(String txt, List<PlanCurso> cnddts, Long stcnId) {
        List<ItemNotaReconocida> items = new ArrayList<>();
        List<String> advertencias = new ArrayList<>();
        if (txt == null) {
            return new Resultado(items, advertencias);
        }

        for (String lnOrgnl : txt.split("\\R")) {
            String ln = lnOrgnl.strip();
            if (ln.isEmpty()) {
                continue;
            }

            Matcher ntMtchr = NOTA_AL_FINAL.matcher(ln);
            if (!ntMtchr.find()) {
                advertencias.add("\"" + ln + "\": no se encontro una nota (0-20) al final de la linea.");
                continue;
            }
            double nota;
            try {
                nota = Double.parseDouble(ntMtchr.group(1).replace(',', '.'));
            } catch (NumberFormatException e) {
                advertencias.add("\"" + ln + "\": la nota no es un numero valido.");
                continue;
            }
            if (nota < 0 || nota > 20) {
                advertencias.add("\"" + ln + "\": la nota " + nota + " esta fuera del rango 0-20.");
                continue;
            }

            String txtCrs = ln.substring(0, ntMtchr.start()).strip();
            if (txtCrs.isEmpty()) {
                advertencias.add("\"" + ln + "\": no se encontro el nombre del curso antes de la nota.");
                continue;
            }

            List<PlanCurso> cncdncs = emparejarCurso(txtCrs, cnddts);
            if (cncdncs.isEmpty()) {
                advertencias.add("\"" + ln + "\": no se identifico ningun curso del plan para \"" + txtCrs + "\".");
                continue;
            }
            if (cncdncs.size() > 1) {
                advertencias.add("\"" + ln + "\": \"" + txtCrs + "\" coincide con varios cursos, no se pudo elegir uno solo.");
                continue;
            }

            PlanCurso curso = cncdncs.get(0);
            ItemNota item = new ItemNota(curso.getId(), nota, stcnId, null);
            items.add(new ItemNotaReconocida(curso.getCurso().getCodigo() + " - " + curso.getCurso().getNombre(), nota, item));
        }

        return new Resultado(items, advertencias);
    }

    private static List<PlanCurso> emparejarCurso(String txtCrs, List<PlanCurso> cnddts) {

        String nrmlzd = normalizar(txtCrs).replaceAll("[:\\-.,]+$", "").strip();

        List<PlanCurso> porCdg = cnddts.stream()
                .filter(pc -> normalizar(pc.getCurso().getCodigo()).equals(nrmlzd)
                        || nrmlzd.startsWith(normalizar(pc.getCurso().getCodigo()) + " "))
                .toList();
        if (!porCdg.isEmpty()) {
            return porCdg;
        }

        List<PlanCurso> porNmbr = cnddts.stream()
                .filter(pc -> {
                    String nmbr = normalizar(pc.getCurso().getNombre());
                    return nrmlzd.contains(nmbr) || nmbr.contains(nrmlzd);
                })
                .toList();
        return porNmbr;
    }

    private static String normalizar(String txt) {
        if (txt == null) {
            return "";
        }
        String sinTlds = java.text.Normalizer.normalize(txt, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinTlds.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    public static List<PlanCurso> cursosDe(List<FilaRegistro> cnddts) {
        return cnddts.stream().map(FilaRegistro::planCurso).toList();
    }

    public record ItemNotaReconocida(String curso, double nota, ItemNota item) {
    }

    public record Resultado(List<ItemNotaReconocida> items, List<String> advertencias) {
    }
}
