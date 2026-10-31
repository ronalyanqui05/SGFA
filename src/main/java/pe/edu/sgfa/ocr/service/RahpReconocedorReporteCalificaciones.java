package pe.edu.sgfa.ocr.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RahpReconocedorReporteCalificaciones {

    private static final Pattern MATRICULA_HNC = Pattern.compile("^\\d{7,8}$");
    private static final Pattern NOTA_HNC = Pattern.compile("^\\d{1,2}([.,]\\d{1,2})?$");
    private static final Pattern PERIODO_HNC = Pattern.compile("(20\\d{2})\\s*[-–—]\\s*(IV|III|II|I|[1-4]|[|!l]{1,3})(?![\\p{L}\\d])");

    private static final Pattern CICLO_HNC = Pattern.compile("\\bCICLO\\s+([IVX]{1,4}|\\d{1,2}|[|!l]{1,3})(?![\\p{L}\\d])");
    private static final Pattern NOMBRE_COMPLETO_HNC = Pattern.compile(
            "((?:[A-ZÁÉÍÓÚÑ]{2,}\\s+)*[A-ZÁÉÍÓÚÑ]{2,})\\s*,\\s*"
                    + "([\\p{L}]+(?:\\s+[\\p{L}]+)*)");

    private RahpReconocedorReporteCalificaciones() {
    }

    public static RahpReporteDetectado rahpReconocer(List<RahpPalabraReconocida> palabrasHnc) {
        List<String> advertenciasHnc = new ArrayList<>();
        List<List<RahpPalabraReconocida>> lineasHnc = rahpAgruparEnLineas(palabrasHnc);
        String textoCompletoHnc = String.join("\n", lineasHnc.stream().map(RahpReconocedorReporteCalificaciones::rahpTextoDe).toList());

        String matriculaHnc = palabrasHnc.stream().map(RahpPalabraReconocida::texto)
                .filter(t -> MATRICULA_HNC.matcher(t).matches()).findFirst().orElse(null);
        String periodoHnc = rahpBuscarPeriodoPorPalabras(palabrasHnc);
        if (periodoHnc == null) {
            periodoHnc = rahpBuscarPeriodo(textoCompletoHnc);
        }
        Integer cicloHnc = rahpBuscarCicloPorPalabras(palabrasHnc);
        if (cicloHnc == null) {
            cicloHnc = rahpBuscarCiclo(textoCompletoHnc);
        }
        String[] nombreHnc = rahpBuscarNombrePorPalabras(palabrasHnc);
        if (nombreHnc == null) {
            nombreHnc = rahpBuscarNombre(lineasHnc);
        }

        List<RahpCursoDetectado> cursosHnc = rahpLeerTabla(palabrasHnc, advertenciasHnc);
        Double promedioHnc = rahpBuscarPromedio(palabrasHnc);

        if (matriculaHnc == null) {
            advertenciasHnc.add("No se reconocio el N° de matricula.");
        }
        if (nombreHnc == null) {
            advertenciasHnc.add("No se reconocio el nombre del estudiante.");
        }
        if (periodoHnc == null) {
            advertenciasHnc.add("No se reconocio el periodo academico.");
        }
        if (cicloHnc == null) {
            advertenciasHnc.add("No se reconocio el ciclo.");
        }
        return new RahpReporteDetectado(matriculaHnc, nombreHnc == null ? null : nombreHnc[0], nombreHnc == null ? null : nombreHnc[1],
                nombreHnc == null ? null : nombreHnc[2], periodoHnc, cicloHnc, textoCompletoHnc, cursosHnc, promedioHnc, advertenciasHnc);
    }

    private static String rahpBuscarPeriodo(String textoHnc) {
        Matcher m = PERIODO_HNC.matcher(textoHnc);
        return m.find() ? m.group(1) + "-" + rahpSemestre(m.group(2)) : null;
    }

    private static Integer rahpBuscarCiclo(String textoHnc) {
        Matcher m = CICLO_HNC.matcher(textoHnc);
        return m.find() ? rahpANumero(rahpComoRomano(m.group(1))) : null;
    }

    private static String rahpSemestre(String valorHnc) {
        return switch (valorHnc) {
            case "1", "4", "I", "IV", "|", "!", "l" -> "I";
            case "2", "3", "II", "III", "||", "!!", "ll" -> "II";
            default -> rahpComoRomano(valorHnc).startsWith("II") ? "II" : "I";
        };
    }

    private static String rahpBuscarPeriodoPorPalabras(List<RahpPalabraReconocida> palabrasHnc) {
        List<RahpPalabraReconocida> aniosHnc = palabrasHnc.stream().filter(p -> p.texto().matches("20\\d{2}"))
                .sorted(Comparator.comparingInt(RahpPalabraReconocida::y)).toList();
        for (RahpPalabraReconocida anioHnc : aniosHnc) {
            for (RahpPalabraReconocida p : rahpVecinasALaDerecha(palabrasHnc, anioHnc)) {
                if (p.texto().matches("[-\u2013\u2014]+")) {
                    continue;
                }
                if (rahpEsRuido(p.texto())) {
                    continue;
                }
                String sufijoHnc = p.texto().replaceFirst("^[-\u2013\u2014]+", "");
                if (sufijoHnc.matches("IV|III|II|I|[1-4]|[|!l]{1,3}")) {
                    return anioHnc.texto() + "-" + rahpSemestre(sufijoHnc);
                }
                break;
            }
        }
        return null;
    }

    private static Integer rahpBuscarCicloPorPalabras(List<RahpPalabraReconocida> palabrasHnc) {
        for (RahpPalabraReconocida cicloHnc : palabrasHnc.stream()
                .filter(p -> p.texto().equals(p.texto().toUpperCase(java.util.Locale.ROOT)) || p.texto().length() >= 5)
                .filter(p -> RahpEmparejadorNombres.rahpSimilitud(p.texto(), "CICLO") >= 0.7)
                .sorted(Comparator.comparingInt(RahpPalabraReconocida::y)).toList()) {
            for (RahpPalabraReconocida p : rahpVecinasALaDerecha(palabrasHnc, cicloHnc)) {
                if (rahpEsRuido(p.texto())) {
                    continue;
                }
                if (p.texto().matches("[IVX]{1,4}|\\d{1,2}|[|!l]{1,3}")) {
                    return rahpANumero(rahpComoRomano(p.texto()));
                }
                break;
            }
        }
        return null;
    }

    private static boolean rahpEsRuido(String textoHnc) {
        return textoHnc.codePoints().noneMatch(Character::isLetterOrDigit) && !textoHnc.matches("[|!]+");
    }

    private static List<RahpPalabraReconocida> rahpVecinasALaDerecha(List<RahpPalabraReconocida> palabrasHnc, RahpPalabraReconocida anclaHnc) {
        return palabrasHnc.stream()
                .filter(p -> p != anclaHnc && p.x() >= anclaHnc.x() + (int) (anclaHnc.ancho() * 0.6))
                .filter(p -> Math.abs(p.rahpCentroY() - anclaHnc.rahpCentroY()) <= Math.max(10, anclaHnc.alto()))
                .sorted(Comparator.comparingInt(RahpPalabraReconocida::x)).toList();
    }

    private static String rahpComoRomano(String valorHnc) {
        return valorHnc.matches("[|!l]+") ? "I".repeat(valorHnc.length()) : valorHnc;
    }

    static Integer rahpANumero(String valorHnc) {
        if (valorHnc.matches("\\d+")) {
            return Integer.parseInt(valorHnc);
        }
        int totalHnc = 0;
        int anteriorHnc = 0;
        for (int i = valorHnc.length() - 1; i >= 0; i--) {
            int actualHnc = switch (valorHnc.charAt(i)) {
                case 'I' -> 1;
                case 'V' -> 5;
                case 'X' -> 10;
                default -> 0;
            };
            totalHnc += actualHnc < anteriorHnc ? -actualHnc : actualHnc;
            anteriorHnc = Math.max(anteriorHnc, actualHnc);
        }
        return totalHnc == 0 ? null : totalHnc;
    }

    private static final java.util.Set<String> CONECTORES_HNC = java.util.Set.of("DE", "DEL", "LA", "LAS", "LOS", "Y",
            "SAN", "SANTA", "VAN", "VON");

    private static String[] rahpBuscarNombrePorPalabras(List<RahpPalabraReconocida> palabrasHnc) {
        RahpPalabraReconocida etiquetaHnc = palabrasHnc.stream()
                .filter(p -> RahpEmparejadorNombres.rahpSimilitud(p.texto(), "ESTUDIANTE") >= 0.7)
                .min(Comparator.comparingInt(RahpPalabraReconocida::y)).orElse(null);
        if (etiquetaHnc == null) {
            return null;
        }
        int altoHnc = Math.max(10, etiquetaHnc.alto());
        int xDerechaHnc = palabrasHnc.stream()
                .filter(p -> RahpEmparejadorNombres.rahpSimilitud(p.texto(), "TURNO") >= 0.7)
                .filter(p -> Math.abs(p.rahpCentroY() - etiquetaHnc.rahpCentroY()) <= 3 * altoHnc && p.x() > etiquetaHnc.x())
                .mapToInt(RahpPalabraReconocida::x).min().orElse(Integer.MAX_VALUE);
        List<RahpPalabraReconocida> celdaHnc = palabrasHnc.stream()
                .filter(p -> p.x() >= etiquetaHnc.x() + etiquetaHnc.ancho() - 4 && p.x() + p.ancho() <= xDerechaHnc)
                .filter(p -> p.rahpCentroY() >= etiquetaHnc.rahpCentroY() - altoHnc * 1.3 && p.rahpCentroY() <= etiquetaHnc.rahpCentroY() + altoHnc * 2.6)
                .map(RahpReconocedorReporteCalificaciones::rahpSinRuido)
                .filter(p -> p.texto().codePoints().noneMatch(Character::isDigit))
                .filter(p -> {
                    long letrasHnc = p.texto().codePoints().filter(Character::isLetter).count();
                    return letrasHnc >= 3 || (letrasHnc >= 1 && CONECTORES_HNC.contains(p.texto().toUpperCase(java.util.Locale.ROOT)));
                })
                .toList();
        if (celdaHnc.isEmpty()) {
            return null;
        }
        String textoHnc = String.join(" ", rahpAgruparEnLineas(celdaHnc).stream().map(RahpReconocedorReporteCalificaciones::rahpTextoDe).toList());
        Matcher m = NOMBRE_COMPLETO_HNC.matcher(textoHnc);
        if (!m.find()) {
            return null;
        }
        return rahpPartirNombre(m.group(1), m.group(2));
    }

    private static RahpPalabraReconocida rahpSinRuido(RahpPalabraReconocida p) {
        String limpioHnc = p.texto().replaceAll("[!|]", "I").replaceAll(",[.\u2022]+", ",")
                .replaceAll("^[^\\p{L}]+", "").replaceAll("[^\\p{L},]+$", "");
        return new RahpPalabraReconocida(limpioHnc, p.x(), p.y(), p.ancho(), p.alto());
    }

    private static String[] rahpPartirNombre(String apellidosTextoHnc, String nombresHnc) {
        String[] apellidosHnc = apellidosTextoHnc.strip().split("\\s+");
        nombresHnc = nombresHnc.strip().replaceAll("\\s+Turno.*$", "").strip();
        if (apellidosHnc.length == 1) {
            return new String[] {apellidosHnc[0], null, nombresHnc};
        }

        return new String[] {apellidosHnc[0], String.join(" ", java.util.Arrays.copyOfRange(apellidosHnc, 1, apellidosHnc.length)),
                nombresHnc};
    }

    private static String[] rahpBuscarNombre(List<List<RahpPalabraReconocida>> lineasHnc) {

        List<List<RahpPalabraReconocida>> candidatasHnc = new ArrayList<>(lineasHnc.stream()
                .filter(l -> l.stream().anyMatch(p -> RahpEmparejadorNombres.rahpNormalizar(p.texto()).equals("ESTUDIANTE")))
                .toList());
        if (candidatasHnc.isEmpty()) {
            candidatasHnc.addAll(lineasHnc);
        }
        for (List<RahpPalabraReconocida> lineaHnc : candidatasHnc) {
            Matcher m = NOMBRE_COMPLETO_HNC.matcher(rahpTextoDe(lineaHnc));
            if (!m.find()) {
                continue;
            }

            return rahpPartirNombre(m.group(1), m.group(2));
        }
        return null;
    }

    private static Double rahpBuscarPromedio(List<RahpPalabraReconocida> palabrasHnc) {
        List<RahpPalabraReconocida> promediosHnc = palabrasHnc.stream()
                .filter(p -> RahpEmparejadorNombres.rahpSimilitud(p.texto(), "PROMEDIO") >= 0.75)
                .sorted(Comparator.comparingInt(RahpPalabraReconocida::y)).toList();
        for (RahpPalabraReconocida promedioHnc : promediosHnc) {
            RahpPalabraReconocida mejorHnc = null;
            for (RahpPalabraReconocida p : palabrasHnc) {
                String limpioHnc = p.texto().replaceAll("[^0-9.,]", "");
                if (!limpioHnc.matches("\\d{1,2}[.,]\\d{2}") || p.rahpCentroX() < promedioHnc.x()
                        || Math.abs(p.rahpCentroY() - promedioHnc.rahpCentroY()) > 2.5 * Math.max(10, promedioHnc.alto())) {
                    continue;
                }
                if (Double.parseDouble(limpioHnc.replace(',', '.')) > 20) {
                    continue;
                }
                if (mejorHnc == null || Math.abs(p.rahpCentroY() - promedioHnc.rahpCentroY()) < Math.abs(mejorHnc.rahpCentroY() - promedioHnc.rahpCentroY())) {
                    mejorHnc = p;
                }
            }
            if (mejorHnc != null) {
                return Double.parseDouble(mejorHnc.texto().replaceAll("[^0-9.,]", "").replace(',', '.'));
            }
        }
        return null;
    }

    private record RahpGeometria(int xNota, int tolNota, int izqCursos, int derCursos, int sitDesde, int sitHasta,
                             int yDesde, int yHasta, int alturaRef) {
    }

    private static boolean rahpEsCeldaCompetencia(RahpPalabraReconocida p) {
        String t = p.texto();

        return !t.equals(t.toUpperCase(java.util.Locale.ROOT)) && RahpEmparejadorNombres.rahpSimilitud(t, "COMPETENCIA") >= 0.72;
    }

    private static int rahpMediana(List<Integer> valoresHnc) {
        List<Integer> ordenHnc = valoresHnc.stream().sorted().toList();
        return ordenHnc.get(ordenHnc.size() / 2);
    }

    private static RahpGeometria rahpGeometriaPorDatos(List<RahpPalabraReconocida> palabrasHnc) {
        List<RahpPalabraReconocida> celdasHnc = palabrasHnc.stream().filter(RahpReconocedorReporteCalificaciones::rahpEsCeldaCompetencia).toList();
        if (celdasHnc.size() < 4) {
            return null;
        }
        int xCompHnc = rahpMediana(celdasHnc.stream().map(RahpPalabraReconocida::x).toList());
        int anchoHnc = rahpMediana(celdasHnc.stream().map(RahpPalabraReconocida::ancho).toList());
        celdasHnc = celdasHnc.stream().filter(p -> Math.abs(p.x() - xCompHnc) <= anchoHnc * 0.6).toList();
        if (celdasHnc.size() < 4) {
            return null;
        }
        int altoHnc = Math.max(10, rahpMediana(celdasHnc.stream().map(RahpPalabraReconocida::alto).toList()));
        int yMinHnc = celdasHnc.stream().mapToInt(RahpPalabraReconocida::rahpCentroY).min().orElse(0);
        int yMaxHnc = celdasHnc.stream().mapToInt(RahpPalabraReconocida::rahpCentroY).max().orElse(0);

        List<RahpPalabraReconocida> candidatasHnc = palabrasHnc.stream()
                .filter(p -> p.texto().matches("\\d{1,2}") && Integer.parseInt(p.texto()) <= 20)
                .filter(p -> rahpCentroX(p) > xCompHnc + 1.6 * anchoHnc)
                .filter(p -> p.rahpCentroY() >= yMinHnc - altoHnc && p.rahpCentroY() <= yMaxHnc + altoHnc)
                .toList();
        if (candidatasHnc.size() < 3) {
            return null;
        }
        int xNotaHnc = rahpMediana(candidatasHnc.stream().map(RahpReconocedorReporteCalificaciones::rahpCentroX).toList());
        long enColumnaHnc = candidatasHnc.stream().filter(p -> Math.abs(rahpCentroX(p) - xNotaHnc) <= 0.8 * anchoHnc).count();
        if (enColumnaHnc < 3) {
            return null;
        }

        int d = Math.max(1, (int) ((xNotaHnc - xCompHnc) / 0.78));
        Integer yPromedioHnc = palabrasHnc.stream()
                .filter(p -> RahpEmparejadorNombres.rahpSimilitud(p.texto(), "PROMEDIO") >= 0.75)
                .map(RahpPalabraReconocida::rahpCentroY).filter(y -> y > yMaxHnc).min(Integer::compare).orElse(null);
        int yHastaHnc = Math.min(yPromedioHnc == null ? Integer.MAX_VALUE : yPromedioHnc - altoHnc, yMaxHnc + 3 * altoHnc);
        return new RahpGeometria(xNotaHnc, (int) (d * 0.12), xNotaHnc - d - (int) (d * 0.23), xCompHnc - Math.max(4, (int) (d * 0.02)),
                xNotaHnc - (int) (d * 0.30), xNotaHnc - (int) (d * 0.09), yMinHnc - altoHnc / 2, yHastaHnc, altoHnc);
    }

    private static RahpGeometria rahpGeometriaPorEncabezados(List<RahpPalabraReconocida> palabrasHnc) {
        RahpPalabraReconocida competenciaHnc = palabrasHnc.stream()
                .filter(p -> RahpEmparejadorNombres.rahpNormalizar(p.texto()).equals("COMPETENCIA"))
                .min(Comparator.comparingInt(RahpPalabraReconocida::y)).orElse(null);
        RahpPalabraReconocida sistemaHnc = palabrasHnc.stream()
                .filter(p -> RahpEmparejadorNombres.rahpNormalizar(p.texto()).equals("SISTEMA"))
                .findFirst().orElse(null);
        if (competenciaHnc == null || sistemaHnc == null) {
            return null;
        }

        RahpPalabraReconocida cursosHnc = palabrasHnc.stream()
                .filter(p -> RahpEmparejadorNombres.rahpNormalizar(p.texto()).equals("CURSOS"))
                .min(Comparator.comparingInt(p -> Math.abs(p.rahpCentroY() - competenciaHnc.rahpCentroY()))).orElse(null);
        if (cursosHnc == null) {
            return null;
        }
        int xNotaHnc = rahpCentroX(sistemaHnc);
        int distanciaHnc = Math.max(1, xNotaHnc - rahpCentroX(cursosHnc));
        int yDesdeHnc = cursosHnc.rahpCentroY() + (int) (1.5 * cursosHnc.alto());
        Integer yPromedioHnc = palabrasHnc.stream()
                .filter(p -> RahpEmparejadorNombres.rahpNormalizar(p.texto()).equals("PROMEDIO"))
                .map(RahpPalabraReconocida::rahpCentroY).filter(y -> y > yDesdeHnc).min(Integer::compare).orElse(null);
        return new RahpGeometria(xNotaHnc, (int) (distanciaHnc * 0.12), rahpCentroX(cursosHnc) - (int) (distanciaHnc * 0.23),
                competenciaHnc.x() - Math.max(4, (int) (distanciaHnc * 0.02)), xNotaHnc - (int) (distanciaHnc * 0.30),
                xNotaHnc - (int) (distanciaHnc * 0.09), yDesdeHnc, yPromedioHnc == null ? Integer.MAX_VALUE : yPromedioHnc - 5,
                cursosHnc.alto());
    }

    private static List<RahpCursoDetectado> rahpLeerTabla(List<RahpPalabraReconocida> palabrasHnc, List<String> advertenciasHnc) {
        RahpGeometria geoHnc = rahpGeometriaPorDatos(palabrasHnc);
        if (geoHnc == null) {
            geoHnc = rahpGeometriaPorEncabezados(palabrasHnc);
        }
        if (geoHnc == null) {
            advertenciasHnc.add("No se encontro la tabla de cursos (ni las celdas de competencias ni los encabezados). "
                    + "Pruebe girar la imagen o agregue los cursos a mano.");
            return List.of();
        }
        int xNotaHnc = geoHnc.xNota();
        int toleranciaNotaHnc = geoHnc.tolNota();
        int izquierdaCursosHnc = geoHnc.izqCursos();
        int derechaCursosHnc = geoHnc.derCursos();
        int yDesdeHnc = geoHnc.yDesde();
        int yHastaHnc = geoHnc.yHasta();
        int alturaRefHnc = geoHnc.alturaRef();
        int situacionDesdeHnc = geoHnc.sitDesde();
        int situacionHastaHnc = geoHnc.sitHasta();

        List<RahpPalabraReconocida> notasHnc = palabrasHnc.stream()
                .filter(p -> NOTA_HNC.matcher(p.texto()).matches())
                .filter(p -> Math.abs(rahpCentroX(p) - xNotaHnc) <= toleranciaNotaHnc)
                .filter(p -> p.rahpCentroY() > yDesdeHnc && p.rahpCentroY() < yHastaHnc)
                .filter(p -> Double.parseDouble(p.texto().replace(',', '.')) <= 20)
                .sorted(Comparator.comparingInt(RahpPalabraReconocida::rahpCentroY)).toList();

        List<RahpPalabraReconocida> textoCursosHnc = palabrasHnc.stream()
                .filter(p -> rahpCentroX(p) >= izquierdaCursosHnc && p.x() + p.ancho() <= derechaCursosHnc + 6)
                .filter(p -> p.rahpCentroY() > yDesdeHnc && p.rahpCentroY() < yHastaHnc)

                .filter(p -> !(p.texto().matches("\\d{1,2}")))
                .toList();
        List<List<RahpPalabraReconocida>> lineasCursoHnc = rahpAgruparEnLineas(textoCursosHnc);

        int separacionHnc = rahpSeparacionTipica(notasHnc, alturaRefHnc);
        List<RahpGrupoCurso> gruposHnc = new ArrayList<>();
        for (RahpPalabraReconocida notaHnc : notasHnc) {
            gruposHnc.add(new RahpGrupoCurso(notaHnc));
        }
        List<RahpGrupoCurso> sinNotaHnc = new ArrayList<>();
        for (List<RahpPalabraReconocida> lineaHnc : lineasCursoHnc) {
            int yLineaHnc = (int) lineaHnc.stream().mapToInt(RahpPalabraReconocida::rahpCentroY).average().orElse(0);
            RahpGrupoCurso cercanoHnc = gruposHnc.stream()
                    .min(Comparator.comparingInt(g -> Math.abs(g.nota.rahpCentroY() - yLineaHnc))).orElse(null);
            if (cercanoHnc != null && Math.abs(cercanoHnc.nota.rahpCentroY() - yLineaHnc) <= separacionHnc * 0.6) {
                cercanoHnc.lineas.add(lineaHnc);
                continue;
            }

            RahpGrupoCurso ultimoHnc = sinNotaHnc.isEmpty() ? null : sinNotaHnc.get(sinNotaHnc.size() - 1);
            if (ultimoHnc != null && yLineaHnc - ultimoHnc.rahpYUltimaLinea() <= alturaRefHnc * 1.8) {
                ultimoHnc.lineas.add(lineaHnc);
            } else {
                RahpGrupoCurso nuevoHnc = new RahpGrupoCurso(null);
                nuevoHnc.lineas.add(lineaHnc);
                sinNotaHnc.add(nuevoHnc);
            }
        }
        gruposHnc.addAll(sinNotaHnc);
        gruposHnc.sort(Comparator.comparingInt(RahpGrupoCurso::rahpYReferencia));

        List<RahpPalabraReconocida> textoSituacionHnc = palabrasHnc.stream()
                .filter(p -> rahpCentroX(p) >= situacionDesdeHnc && rahpCentroX(p) <= situacionHastaHnc)
                .filter(p -> p.rahpCentroY() > yDesdeHnc && p.rahpCentroY() < yHastaHnc)
                .filter(p -> p.texto().codePoints().filter(Character::isLetter).count() >= 2
                        || p.texto().matches("[DLEIP]"))
                .toList();

        List<RahpCursoDetectado> resultadoHnc = new ArrayList<>();
        for (RahpGrupoCurso g : gruposHnc) {
            String nombreHnc = rahpLimpiarNombreCurso(
                    g.lineas.stream().flatMap(List::stream).map(RahpPalabraReconocida::texto).toList());
            if (nombreHnc.replaceAll("[^\\p{L}]", "").length() < 3) {
                continue;
            }
            Double valorHnc = g.nota == null ? null : Double.parseDouble(g.nota.texto().replace(',', '.'));
            String situacionHnc = g.nota == null ? null : RahpLectorSituacion.rahpCodigoDe(textoSituacionHnc.stream()
                    .filter(p -> Math.abs(p.rahpCentroY() - g.nota.rahpCentroY()) <= separacionHnc * 0.6)
                    .sorted(Comparator.comparingInt(RahpPalabraReconocida::rahpCentroY).thenComparingInt(RahpPalabraReconocida::x))
                    .map(RahpPalabraReconocida::texto).toList());
            resultadoHnc.add(new RahpCursoDetectado(nombreHnc, valorHnc, situacionHnc));
        }
        return resultadoHnc;
    }

    private static String rahpLimpiarNombreCurso(List<String> tokensHnc) {
        List<String> limpiosHnc = new ArrayList<>();
        boolean barraFinalHnc = false;
        for (String tokenHnc : tokensHnc) {
            if (tokenHnc.matches("[|!l]+")) {
                barraFinalHnc = true;
                continue;
            }
            boolean tieneLetraHnc = tokenHnc.codePoints().anyMatch(Character::isLetter);
            boolean letraSueltaHnc = tokenHnc.length() == 1 && !"YEAOUIVX".contains(tokenHnc);
            if (!tieneLetraHnc || letraSueltaHnc) {
                continue;
            }
            limpiosHnc.add(tokenHnc);
            barraFinalHnc = false;
        }
        if (barraFinalHnc) {
            limpiosHnc.add("I");
        }
        return String.join(" ", limpiosHnc);
    }

    private static int rahpSeparacionTipica(List<RahpPalabraReconocida> notasHnc, int alturaPalabraHnc) {
        if (notasHnc.size() < 2) {
            return Math.max(40, alturaPalabraHnc * 4);
        }
        List<Integer> saltosHnc = new ArrayList<>();
        for (int i = 1; i < notasHnc.size(); i++) {
            saltosHnc.add(notasHnc.get(i).rahpCentroY() - notasHnc.get(i - 1).rahpCentroY());
        }
        saltosHnc.sort(Integer::compare);
        return saltosHnc.get(saltosHnc.size() / 2);
    }

    private static final class RahpGrupoCurso {
        final RahpPalabraReconocida nota;
        final List<List<RahpPalabraReconocida>> lineas = new ArrayList<>();

        RahpGrupoCurso(RahpPalabraReconocida notaHnc) {
            this.nota = notaHnc;
        }

        int rahpYUltimaLinea() {
            return (int) lineas.get(lineas.size() - 1).stream().mapToInt(RahpPalabraReconocida::rahpCentroY).average().orElse(0);
        }

        int rahpYReferencia() {
            return nota != null ? nota.rahpCentroY() : rahpYUltimaLinea();
        }
    }

    private static int rahpCentroX(RahpPalabraReconocida p) {
        return p.x() + p.ancho() / 2;
    }

    private static String rahpTextoDe(List<RahpPalabraReconocida> lineaHnc) {
        return String.join(" ", lineaHnc.stream().map(RahpPalabraReconocida::texto).toList());
    }

    private static List<List<RahpPalabraReconocida>> rahpAgruparEnLineas(List<RahpPalabraReconocida> palabrasHnc) {
        List<RahpPalabraReconocida> ordenadasHnc = palabrasHnc.stream()
                .sorted(Comparator.comparingInt(RahpPalabraReconocida::rahpCentroY)).toList();
        if (ordenadasHnc.isEmpty()) {
            return List.of();
        }
        int alturaTipicaHnc = (int) ordenadasHnc.stream().mapToInt(RahpPalabraReconocida::alto).average().orElse(20);
        int toleranciaHnc = Math.max(8, alturaTipicaHnc / 2);

        List<List<RahpPalabraReconocida>> lineasHnc = new ArrayList<>();
        List<RahpPalabraReconocida> actualHnc = new ArrayList<>();
        int centroHnc = 0;
        for (RahpPalabraReconocida p : ordenadasHnc) {
            if (actualHnc.isEmpty() || Math.abs(p.rahpCentroY() - centroHnc) <= toleranciaHnc) {
                actualHnc.add(p);
                centroHnc = (int) actualHnc.stream().mapToInt(RahpPalabraReconocida::rahpCentroY).average().orElse(0);
            } else {
                lineasHnc.add(rahpOrdenarPorX(actualHnc));
                actualHnc = new ArrayList<>(List.of(p));
                centroHnc = p.rahpCentroY();
            }
        }
        lineasHnc.add(rahpOrdenarPorX(actualHnc));
        return lineasHnc;
    }

    private static List<RahpPalabraReconocida> rahpOrdenarPorX(List<RahpPalabraReconocida> lineaHnc) {
        return lineaHnc.stream().sorted(Comparator.comparingInt(RahpPalabraReconocida::x)).toList();
    }

    public record RahpCursoDetectado(String nombre, Double nota, String situacion) {
    }

    public record RahpReporteDetectado(String matricula, String apellidoPaterno, String apellidoMaterno, String nombres,
                                    String periodo, Integer ciclo, String textoCompleto, List<RahpCursoDetectado> cursos,
                                    Double promedioReporte, List<String> advertencias) {
    }
}
