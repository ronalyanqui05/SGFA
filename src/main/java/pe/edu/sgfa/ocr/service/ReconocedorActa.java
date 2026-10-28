package pe.edu.sgfa.ocr.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

public final class ReconocedorActa {

    private static final Pattern MATRICULA = Pattern.compile("^\\d{7,8}$");
    private static final Pattern NUMERO_CUALQUIERA = Pattern.compile("^\\d+$");
    private static final Pattern SOLO_LETRAS = Pattern.compile("^[\\p{L}]+$");

    private ReconocedorActa() {
    }

    public static List<FilaActaDetectada> agrupar(List<PalabraReconocida> plbrs, int cntddCrss) {
        List<List<PalabraReconocida>> fls = agruparEnFilas(plbrs);
        List<FilaActaDetectada> rsltd = new ArrayList<>();
        for (List<PalabraReconocida> fl : fls) {
            rsltd.add(interpretarFila(fl, cntddCrss));
        }
        return rsltd;
    }

    private static List<List<PalabraReconocida>> agruparEnFilas(List<PalabraReconocida> plbrs) {
        List<PalabraReconocida> ordnds = plbrs.stream()
                .sorted(Comparator.comparingInt(PalabraReconocida::centroY))
                .toList();
        if (ordnds.isEmpty()) {
            return List.of();
        }

        int altrTpc = (int) ordnds.stream().mapToInt(PalabraReconocida::alto).average().orElse(20);
        int tlrnc = Math.max(8, altrTpc / 2);

        List<List<PalabraReconocida>> fls = new ArrayList<>();
        List<PalabraReconocida> flActl = new ArrayList<>();
        int cntrFlActl = Integer.MIN_VALUE;

        for (PalabraReconocida plbr : ordnds) {
            if (flActl.isEmpty() || Math.abs(plbr.centroY() - cntrFlActl) <= tlrnc) {
                flActl.add(plbr);
                cntrFlActl = promedioCentro(flActl);
            } else {
                fls.add(ordenarPorX(flActl));
                flActl = new ArrayList<>(List.of(plbr));
                cntrFlActl = plbr.centroY();
            }
        }
        if (!flActl.isEmpty()) {
            fls.add(ordenarPorX(flActl));
        }
        return fls;
    }

    private static int promedioCentro(List<PalabraReconocida> fl) {
        return (int) fl.stream().mapToInt(PalabraReconocida::centroY).average().orElse(0);
    }

    private static List<PalabraReconocida> ordenarPorX(List<PalabraReconocida> fl) {
        return fl.stream().sorted(Comparator.comparingInt(PalabraReconocida::x)).toList();
    }

    private static FilaActaDetectada interpretarFila(List<PalabraReconocida> fl, int cntddCrss) {
        List<String> tkns = fl.stream().map(PalabraReconocida::texto).toList();
        int i = 0;

        String ordenNro = null;
        if (i < tkns.size() && NUMERO_CUALQUIERA.matcher(tkns.get(i)).matches() && tkns.get(i).length() <= 3) {
            ordenNro = tkns.get(i);
            i++;
        }

        String matricula = null;
        if (i < tkns.size() && MATRICULA.matcher(tkns.get(i)).matches()) {
            matricula = tkns.get(i);
            i++;
        }

        List<String> nmbrCmplt = new ArrayList<>();
        while (i < tkns.size() && SOLO_LETRAS.matcher(tkns.get(i)).matches() && tkns.get(i).length() > 1) {
            nmbrCmplt.add(tkns.get(i));
            i++;
        }

        String apellidoPaterno = nmbrCmplt.size() > 0 ? nmbrCmplt.get(0) : null;
        String apellidoMaterno = nmbrCmplt.size() > 1 ? nmbrCmplt.get(1) : null;
        String nombres = nmbrCmplt.size() > 2
                ? String.join(" ", nmbrCmplt.subList(2, nmbrCmplt.size())) : null;

        List<FilaActaDetectada.NotaCurso> nts = new ArrayList<>();
        List<String> rstnts = tkns.subList(Math.min(i, tkns.size()), tkns.size());
        int j = 0;
        for (int crs = 0; crs < cntddCrss; crs++) {
            String stcn = j < rstnts.size() ? rstnts.get(j) : null;
            String nt = j + 1 < rstnts.size() ? rstnts.get(j + 1) : null;
            nts.add(new FilaActaDetectada.NotaCurso(stcn, nt));

            int avnc = 2;
            if (j + 2 < rstnts.size() && NUMERO_CUALQUIERA.matcher(rstnts.get(j + 2)).matches()
                    && nt != null && NUMERO_CUALQUIERA.matcher(nt).matches()) {
                avnc = 3;
            }
            j += avnc;
        }

        return new FilaActaDetectada(ordenNro, matricula, apellidoPaterno, apellidoMaterno, nombres, nts);
    }

    public record FilaActaDetectada(String ordenNro, String matricula, String apellidoPaterno,
                                     String apellidoMaterno, String nombres, List<NotaCurso> notasPorCurso) {

        public record NotaCurso(String situacionTexto, String notaTexto) {
        }
    }
}
