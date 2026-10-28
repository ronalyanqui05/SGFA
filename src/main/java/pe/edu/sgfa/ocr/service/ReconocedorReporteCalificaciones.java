package pe.edu.sgfa.ocr.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ReconocedorReporteCalificaciones {

    private static final Pattern MATRICULA = Pattern.compile("^\\d{7,8}$");
    private static final Pattern NOTA = Pattern.compile("^\\d{1,2}([.,]\\d{1,2})?$");
    private static final Pattern PERIODO = Pattern.compile("(20\\d{2})\\s*[-–—]\\s*(IV|III|II|I|[1-4]|[|!l]{1,3})(?![\\p{L}\\d])");

    private static final Pattern CICLO = Pattern.compile("\\bCICLO\\s+([IVX]{1,4}|\\d{1,2}|[|!l]{1,3})(?![\\p{L}\\d])");
    private static final Pattern NOMBRE_COMPLETO = Pattern.compile(
            "((?:[A-ZÁÉÍÓÚÑ]{2,}\\s+)*[A-ZÁÉÍÓÚÑ]{2,})\\s*,\\s*"
                    + "([\\p{L}]+(?:\\s+[\\p{L}]+)*)");

    private ReconocedorReporteCalificaciones() {
    }

    public static ReporteDetectado reconocer(List<PalabraReconocida> plbrs) {
        List<String> advertencias = new ArrayList<>();
        List<List<PalabraReconocida>> lineas = agruparEnLineas(plbrs);
        String textoCompleto = String.join("\n", lineas.stream().map(ReconocedorReporteCalificaciones::textoDe).toList());

        String matricula = plbrs.stream().map(PalabraReconocida::texto)
                .filter(t -> MATRICULA.matcher(t).matches()).findFirst().orElse(null);
        String periodo = buscarPeriodoPorPalabras(plbrs);
        if (periodo == null) {
            periodo = buscarPeriodo(textoCompleto);
        }
        Integer ciclo = buscarCicloPorPalabras(plbrs);
        if (ciclo == null) {
            ciclo = buscarCiclo(textoCompleto);
        }
        String[] nombre = buscarNombrePorPalabras(plbrs);
        if (nombre == null) {
            nombre = buscarNombre(lineas);
        }

        List<CursoDetectado> cursos = leerTabla(plbrs, advertencias);
        Double prmd = buscarPromedio(plbrs);

        if (matricula == null) {
            advertencias.add("No se reconocio el N° de matricula.");
        }
        if (nombre == null) {
            advertencias.add("No se reconocio el nombre del estudiante.");
        }
        if (periodo == null) {
            advertencias.add("No se reconocio el periodo academico.");
        }
        if (ciclo == null) {
            advertencias.add("No se reconocio el ciclo.");
        }
        return new ReporteDetectado(matricula, nombre == null ? null : nombre[0], nombre == null ? null : nombre[1],
                nombre == null ? null : nombre[2], periodo, ciclo, textoCompleto, cursos, prmd, advertencias);
    }

    private static String buscarPeriodo(String texto) {
        Matcher m = PERIODO.matcher(texto);
        return m.find() ? m.group(1) + "-" + semestre(m.group(2)) : null;
    }

    private static Integer buscarCiclo(String texto) {
        Matcher m = CICLO.matcher(texto);
        return m.find() ? aNumero(comoRomano(m.group(1))) : null;
    }

    private static String semestre(String vlr) {
        return switch (vlr) {
            case "1", "4", "I", "IV", "|", "!", "l" -> "I";
            case "2", "3", "II", "III", "||", "!!", "ll" -> "II";
            default -> comoRomano(vlr).startsWith("II") ? "II" : "I";
        };
    }

    private static String buscarPeriodoPorPalabras(List<PalabraReconocida> plbrs) {
        List<PalabraReconocida> ans = plbrs.stream().filter(p -> p.texto().matches("20\\d{2}"))
                .sorted(Comparator.comparingInt(PalabraReconocida::y)).toList();
        for (PalabraReconocida an : ans) {
            for (PalabraReconocida p : vecinasALaDerecha(plbrs, an)) {
                if (p.texto().matches("[-\u2013\u2014]+")) {
                    continue;
                }
                if (esRuido(p.texto())) {
                    continue;
                }
                String sfj = p.texto().replaceFirst("^[-\u2013\u2014]+", "");
                if (sfj.matches("IV|III|II|I|[1-4]|[|!l]{1,3}")) {
                    return an.texto() + "-" + semestre(sfj);
                }
                break;
            }
        }
        return null;
    }

    private static Integer buscarCicloPorPalabras(List<PalabraReconocida> plbrs) {
        for (PalabraReconocida ciclo : plbrs.stream()
                .filter(p -> p.texto().equals(p.texto().toUpperCase(java.util.Locale.ROOT)) || p.texto().length() >= 5)
                .filter(p -> EmparejadorNombres.similitud(p.texto(), "CICLO") >= 0.7)
                .sorted(Comparator.comparingInt(PalabraReconocida::y)).toList()) {
            for (PalabraReconocida p : vecinasALaDerecha(plbrs, ciclo)) {
                if (esRuido(p.texto())) {
                    continue;
                }
                if (p.texto().matches("[IVX]{1,4}|\\d{1,2}|[|!l]{1,3}")) {
                    return aNumero(comoRomano(p.texto()));
                }
                break;
            }
        }
        return null;
    }

    private static boolean esRuido(String texto) {
        return texto.codePoints().noneMatch(Character::isLetterOrDigit) && !texto.matches("[|!]+");
    }

    private static List<PalabraReconocida> vecinasALaDerecha(List<PalabraReconocida> plbrs, PalabraReconocida ancl) {
        return plbrs.stream()
                .filter(p -> p != ancl && p.x() >= ancl.x() + (int) (ancl.ancho() * 0.6))
                .filter(p -> Math.abs(p.centroY() - ancl.centroY()) <= Math.max(10, ancl.alto()))
                .sorted(Comparator.comparingInt(PalabraReconocida::x)).toList();
    }

    private static String comoRomano(String vlr) {
        return vlr.matches("[|!l]+") ? "I".repeat(vlr.length()) : vlr;
    }

    static Integer aNumero(String vlr) {
        if (vlr.matches("\\d+")) {
            return Integer.parseInt(vlr);
        }
        int ttl = 0;
        int antrr = 0;
        for (int i = vlr.length() - 1; i >= 0; i--) {
            int actl = switch (vlr.charAt(i)) {
                case 'I' -> 1;
                case 'V' -> 5;
                case 'X' -> 10;
                default -> 0;
            };
            ttl += actl < antrr ? -actl : actl;
            antrr = Math.max(antrr, actl);
        }
        return ttl == 0 ? null : ttl;
    }

    private static final java.util.Set<String> CONECTORES = java.util.Set.of("DE", "DEL", "LA", "LAS", "LOS", "Y",
            "SAN", "SANTA", "VAN", "VON");

    private static String[] buscarNombrePorPalabras(List<PalabraReconocida> plbrs) {
        PalabraReconocida etqt = plbrs.stream()
                .filter(p -> EmparejadorNombres.similitud(p.texto(), "ESTUDIANTE") >= 0.7)
                .min(Comparator.comparingInt(PalabraReconocida::y)).orElse(null);
        if (etqt == null) {
            return null;
        }
        int alto = Math.max(10, etqt.alto());
        int xDrch = plbrs.stream()
                .filter(p -> EmparejadorNombres.similitud(p.texto(), "TURNO") >= 0.7)
                .filter(p -> Math.abs(p.centroY() - etqt.centroY()) <= 3 * alto && p.x() > etqt.x())
                .mapToInt(PalabraReconocida::x).min().orElse(Integer.MAX_VALUE);
        List<PalabraReconocida> cld = plbrs.stream()
                .filter(p -> p.x() >= etqt.x() + etqt.ancho() - 4 && p.x() + p.ancho() <= xDrch)
                .filter(p -> p.centroY() >= etqt.centroY() - alto * 1.3 && p.centroY() <= etqt.centroY() + alto * 2.6)
                .map(ReconocedorReporteCalificaciones::sinRuido)
                .filter(p -> p.texto().codePoints().noneMatch(Character::isDigit))
                .filter(p -> {
                    long ltrs = p.texto().codePoints().filter(Character::isLetter).count();
                    return ltrs >= 3 || (ltrs >= 1 && CONECTORES.contains(p.texto().toUpperCase(java.util.Locale.ROOT)));
                })
                .toList();
        if (cld.isEmpty()) {
            return null;
        }
        String texto = String.join(" ", agruparEnLineas(cld).stream().map(ReconocedorReporteCalificaciones::textoDe).toList());
        Matcher m = NOMBRE_COMPLETO.matcher(texto);
        if (!m.find()) {
            return null;
        }
        return partirNombre(m.group(1), m.group(2));
    }

    private static PalabraReconocida sinRuido(PalabraReconocida p) {
        String lmp = p.texto().replaceAll("[!|]", "I").replaceAll(",[.\u2022]+", ",")
                .replaceAll("^[^\\p{L}]+", "").replaceAll("[^\\p{L},]+$", "");
        return new PalabraReconocida(lmp, p.x(), p.y(), p.ancho(), p.alto());
    }

    private static String[] partirNombre(String aplldsTxt, String nombres) {
        String[] apllds = aplldsTxt.strip().split("\\s+");
        nombres = nombres.strip().replaceAll("\\s+Turno.*$", "").strip();
        if (apllds.length == 1) {
            return new String[] {apllds[0], null, nombres};
        }

        return new String[] {apllds[0], String.join(" ", java.util.Arrays.copyOfRange(apllds, 1, apllds.length)),
                nombres};
    }

    private static String[] buscarNombre(List<List<PalabraReconocida>> lineas) {

        List<List<PalabraReconocida>> cnddts = new ArrayList<>(lineas.stream()
                .filter(l -> l.stream().anyMatch(p -> EmparejadorNombres.normalizar(p.texto()).equals("ESTUDIANTE")))
                .toList());
        if (cnddts.isEmpty()) {
            cnddts.addAll(lineas);
        }
        for (List<PalabraReconocida> ln : cnddts) {
            Matcher m = NOMBRE_COMPLETO.matcher(textoDe(ln));
            if (!m.find()) {
                continue;
            }

            return partirNombre(m.group(1), m.group(2));
        }
        return null;
    }

    private static Double buscarPromedio(List<PalabraReconocida> plbrs) {
        List<PalabraReconocida> prmds = plbrs.stream()
                .filter(p -> EmparejadorNombres.similitud(p.texto(), "PROMEDIO") >= 0.75)
                .sorted(Comparator.comparingInt(PalabraReconocida::y)).toList();
        for (PalabraReconocida prmd : prmds) {
            PalabraReconocida mjr = null;
            for (PalabraReconocida p : plbrs) {
                String lmp = p.texto().replaceAll("[^0-9.,]", "");
                if (!lmp.matches("\\d{1,2}[.,]\\d{2}") || p.centroX() < prmd.x()
                        || Math.abs(p.centroY() - prmd.centroY()) > 2.5 * Math.max(10, prmd.alto())) {
                    continue;
                }
                if (Double.parseDouble(lmp.replace(',', '.')) > 20) {
                    continue;
                }
                if (mjr == null || Math.abs(p.centroY() - prmd.centroY()) < Math.abs(mjr.centroY() - prmd.centroY())) {
                    mjr = p;
                }
            }
            if (mjr != null) {
                return Double.parseDouble(mjr.texto().replaceAll("[^0-9.,]", "").replace(',', '.'));
            }
        }
        return null;
    }

    private record Geometria(int xNota, int tolNota, int izqCursos, int derCursos, int sitDesde, int sitHasta,
                             int yDesde, int yHasta, int alturaRef) {
    }

    private static boolean esCeldaCompetencia(PalabraReconocida p) {
        String t = p.texto();

        return !t.equals(t.toUpperCase(java.util.Locale.ROOT)) && EmparejadorNombres.similitud(t, "COMPETENCIA") >= 0.72;
    }

    private static int mediana(List<Integer> vlrs) {
        List<Integer> ordn = vlrs.stream().sorted().toList();
        return ordn.get(ordn.size() / 2);
    }

    private static Geometria geometriaPorDatos(List<PalabraReconocida> plbrs) {
        List<PalabraReconocida> clds = plbrs.stream().filter(ReconocedorReporteCalificaciones::esCeldaCompetencia).toList();
        if (clds.size() < 4) {
            return null;
        }
        int xCmp = mediana(clds.stream().map(PalabraReconocida::x).toList());
        int ancho = mediana(clds.stream().map(PalabraReconocida::ancho).toList());
        clds = clds.stream().filter(p -> Math.abs(p.x() - xCmp) <= ancho * 0.6).toList();
        if (clds.size() < 4) {
            return null;
        }
        int alto = Math.max(10, mediana(clds.stream().map(PalabraReconocida::alto).toList()));
        int yMinr = clds.stream().mapToInt(PalabraReconocida::centroY).min().orElse(0);
        int yMaxr = clds.stream().mapToInt(PalabraReconocida::centroY).max().orElse(0);

        List<PalabraReconocida> cnddts = plbrs.stream()
                .filter(p -> p.texto().matches("\\d{1,2}") && Integer.parseInt(p.texto()) <= 20)
                .filter(p -> centroX(p) > xCmp + 1.6 * ancho)
                .filter(p -> p.centroY() >= yMinr - alto && p.centroY() <= yMaxr + alto)
                .toList();
        if (cnddts.size() < 3) {
            return null;
        }
        int xNota = mediana(cnddts.stream().map(ReconocedorReporteCalificaciones::centroX).toList());
        long enClmn = cnddts.stream().filter(p -> Math.abs(centroX(p) - xNota) <= 0.8 * ancho).count();
        if (enClmn < 3) {
            return null;
        }

        int d = Math.max(1, (int) ((xNota - xCmp) / 0.78));
        Integer yPrmd = plbrs.stream()
                .filter(p -> EmparejadorNombres.similitud(p.texto(), "PROMEDIO") >= 0.75)
                .map(PalabraReconocida::centroY).filter(y -> y > yMaxr).min(Integer::compare).orElse(null);
        int yHasta = Math.min(yPrmd == null ? Integer.MAX_VALUE : yPrmd - alto, yMaxr + 3 * alto);
        return new Geometria(xNota, (int) (d * 0.12), xNota - d - (int) (d * 0.23), xCmp - Math.max(4, (int) (d * 0.02)),
                xNota - (int) (d * 0.30), xNota - (int) (d * 0.09), yMinr - alto / 2, yHasta, alto);
    }

    private static Geometria geometriaPorEncabezados(List<PalabraReconocida> plbrs) {
        PalabraReconocida cmptnc = plbrs.stream()
                .filter(p -> EmparejadorNombres.normalizar(p.texto()).equals("COMPETENCIA"))
                .min(Comparator.comparingInt(PalabraReconocida::y)).orElse(null);
        PalabraReconocida sstm = plbrs.stream()
                .filter(p -> EmparejadorNombres.normalizar(p.texto()).equals("SISTEMA"))
                .findFirst().orElse(null);
        if (cmptnc == null || sstm == null) {
            return null;
        }

        PalabraReconocida cursos = plbrs.stream()
                .filter(p -> EmparejadorNombres.normalizar(p.texto()).equals("CURSOS"))
                .min(Comparator.comparingInt(p -> Math.abs(p.centroY() - cmptnc.centroY()))).orElse(null);
        if (cursos == null) {
            return null;
        }
        int xNota = centroX(sstm);
        int dstnc = Math.max(1, xNota - centroX(cursos));
        int yDesde = cursos.centroY() + (int) (1.5 * cursos.alto());
        Integer yPrmd = plbrs.stream()
                .filter(p -> EmparejadorNombres.normalizar(p.texto()).equals("PROMEDIO"))
                .map(PalabraReconocida::centroY).filter(y -> y > yDesde).min(Integer::compare).orElse(null);
        return new Geometria(xNota, (int) (dstnc * 0.12), centroX(cursos) - (int) (dstnc * 0.23),
                cmptnc.x() - Math.max(4, (int) (dstnc * 0.02)), xNota - (int) (dstnc * 0.30),
                xNota - (int) (dstnc * 0.09), yDesde, yPrmd == null ? Integer.MAX_VALUE : yPrmd - 5,
                cursos.alto());
    }

    private static List<CursoDetectado> leerTabla(List<PalabraReconocida> plbrs, List<String> advertencias) {
        Geometria geo = geometriaPorDatos(plbrs);
        if (geo == null) {
            geo = geometriaPorEncabezados(plbrs);
        }
        if (geo == null) {
            advertencias.add("No se encontro la tabla de cursos (ni las celdas de competencias ni los encabezados). "
                    + "Pruebe girar la imagen o agregue los cursos a mano.");
            return List.of();
        }
        int xNota = geo.xNota();
        int tlrncNt = geo.tolNota();
        int izqrdCrss = geo.izqCursos();
        int drchCrss = geo.derCursos();
        int yDesde = geo.yDesde();
        int yHasta = geo.yHasta();
        int alturaRef = geo.alturaRef();
        int stcnDsd = geo.sitDesde();
        int stcnHst = geo.sitHasta();

        List<PalabraReconocida> nts = plbrs.stream()
                .filter(p -> NOTA.matcher(p.texto()).matches())
                .filter(p -> Math.abs(centroX(p) - xNota) <= tlrncNt)
                .filter(p -> p.centroY() > yDesde && p.centroY() < yHasta)
                .filter(p -> Double.parseDouble(p.texto().replace(',', '.')) <= 20)
                .sorted(Comparator.comparingInt(PalabraReconocida::centroY)).toList();

        List<PalabraReconocida> txtCrss = plbrs.stream()
                .filter(p -> centroX(p) >= izqrdCrss && p.x() + p.ancho() <= drchCrss + 6)
                .filter(p -> p.centroY() > yDesde && p.centroY() < yHasta)

                .filter(p -> !(p.texto().matches("\\d{1,2}")))
                .toList();
        List<List<PalabraReconocida>> lnsCrs = agruparEnLineas(txtCrss);

        int sprcn = separacionTipica(nts, alturaRef);
        List<GrupoCurso> grps = new ArrayList<>();
        for (PalabraReconocida nota : nts) {
            grps.add(new GrupoCurso(nota));
        }
        List<GrupoCurso> sinNt = new ArrayList<>();
        for (List<PalabraReconocida> ln : lnsCrs) {
            int yLn = (int) ln.stream().mapToInt(PalabraReconocida::centroY).average().orElse(0);
            GrupoCurso crcn = grps.stream()
                    .min(Comparator.comparingInt(g -> Math.abs(g.nota.centroY() - yLn))).orElse(null);
            if (crcn != null && Math.abs(crcn.nota.centroY() - yLn) <= sprcn * 0.6) {
                crcn.lineas.add(ln);
                continue;
            }

            GrupoCurso ultm = sinNt.isEmpty() ? null : sinNt.get(sinNt.size() - 1);
            if (ultm != null && yLn - ultm.yUltimaLinea() <= alturaRef * 1.8) {
                ultm.lineas.add(ln);
            } else {
                GrupoCurso nv = new GrupoCurso(null);
                nv.lineas.add(ln);
                sinNt.add(nv);
            }
        }
        grps.addAll(sinNt);
        grps.sort(Comparator.comparingInt(GrupoCurso::yReferencia));

        List<PalabraReconocida> txtStcn = plbrs.stream()
                .filter(p -> centroX(p) >= stcnDsd && centroX(p) <= stcnHst)
                .filter(p -> p.centroY() > yDesde && p.centroY() < yHasta)
                .filter(p -> p.texto().codePoints().filter(Character::isLetter).count() >= 2
                        || p.texto().matches("[DLEIP]"))
                .toList();

        List<CursoDetectado> rsltd = new ArrayList<>();
        for (GrupoCurso g : grps) {
            String nombre = limpiarNombreCurso(
                    g.lineas.stream().flatMap(List::stream).map(PalabraReconocida::texto).toList());
            if (nombre.replaceAll("[^\\p{L}]", "").length() < 3) {
                continue;
            }
            Double vlr = g.nota == null ? null : Double.parseDouble(g.nota.texto().replace(',', '.'));
            String situacion = g.nota == null ? null : LectorSituacion.codigoDe(txtStcn.stream()
                    .filter(p -> Math.abs(p.centroY() - g.nota.centroY()) <= sprcn * 0.6)
                    .sorted(Comparator.comparingInt(PalabraReconocida::centroY).thenComparingInt(PalabraReconocida::x))
                    .map(PalabraReconocida::texto).toList());
            rsltd.add(new CursoDetectado(nombre, vlr, situacion));
        }
        return rsltd;
    }

    private static String limpiarNombreCurso(List<String> tkns) {
        List<String> lmps = new ArrayList<>();
        boolean brrFnl = false;
        for (String tkn : tkns) {
            if (tkn.matches("[|!l]+")) {
                brrFnl = true;
                continue;
            }
            boolean tnLtr = tkn.codePoints().anyMatch(Character::isLetter);
            boolean ltrSlt = tkn.length() == 1 && !"YEAOUIVX".contains(tkn);
            if (!tnLtr || ltrSlt) {
                continue;
            }
            lmps.add(tkn);
            brrFnl = false;
        }
        if (brrFnl) {
            lmps.add("I");
        }
        return String.join(" ", lmps);
    }

    private static int separacionTipica(List<PalabraReconocida> nts, int altrPlbr) {
        if (nts.size() < 2) {
            return Math.max(40, altrPlbr * 4);
        }
        List<Integer> slts = new ArrayList<>();
        for (int i = 1; i < nts.size(); i++) {
            slts.add(nts.get(i).centroY() - nts.get(i - 1).centroY());
        }
        slts.sort(Integer::compare);
        return slts.get(slts.size() / 2);
    }

    private static final class GrupoCurso {
        final PalabraReconocida nota;
        final List<List<PalabraReconocida>> lineas = new ArrayList<>();

        GrupoCurso(PalabraReconocida nota) {
            this.nota = nota;
        }

        int yUltimaLinea() {
            return (int) lineas.get(lineas.size() - 1).stream().mapToInt(PalabraReconocida::centroY).average().orElse(0);
        }

        int yReferencia() {
            return nota != null ? nota.centroY() : yUltimaLinea();
        }
    }

    private static int centroX(PalabraReconocida p) {
        return p.x() + p.ancho() / 2;
    }

    private static String textoDe(List<PalabraReconocida> ln) {
        return String.join(" ", ln.stream().map(PalabraReconocida::texto).toList());
    }

    private static List<List<PalabraReconocida>> agruparEnLineas(List<PalabraReconocida> plbrs) {
        List<PalabraReconocida> ordnds = plbrs.stream()
                .sorted(Comparator.comparingInt(PalabraReconocida::centroY)).toList();
        if (ordnds.isEmpty()) {
            return List.of();
        }
        int altrTpc = (int) ordnds.stream().mapToInt(PalabraReconocida::alto).average().orElse(20);
        int tlrnc = Math.max(8, altrTpc / 2);

        List<List<PalabraReconocida>> lineas = new ArrayList<>();
        List<PalabraReconocida> actl = new ArrayList<>();
        int cntr = 0;
        for (PalabraReconocida p : ordnds) {
            if (actl.isEmpty() || Math.abs(p.centroY() - cntr) <= tlrnc) {
                actl.add(p);
                cntr = (int) actl.stream().mapToInt(PalabraReconocida::centroY).average().orElse(0);
            } else {
                lineas.add(ordenarPorX(actl));
                actl = new ArrayList<>(List.of(p));
                cntr = p.centroY();
            }
        }
        lineas.add(ordenarPorX(actl));
        return lineas;
    }

    private static List<PalabraReconocida> ordenarPorX(List<PalabraReconocida> ln) {
        return ln.stream().sorted(Comparator.comparingInt(PalabraReconocida::x)).toList();
    }

    public record CursoDetectado(String nombre, Double nota, String situacion) {
    }

    public record ReporteDetectado(String matricula, String apellidoPaterno, String apellidoMaterno, String nombres,
                                    String periodo, Integer ciclo, String textoCompleto, List<CursoDetectado> cursos,
                                    Double promedioReporte, List<String> advertencias) {
    }
}
