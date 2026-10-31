package pe.edu.sgfa.ficha;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.junit.jupiter.api.Test;

import pe.edu.sgfa.ficha.service.RacaFilaFichaCiclo;

class RacaFichaDosHojasTest {

    private static final String[] ROMANOS_CRR = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    @Test
    void cadaEspecialidadCabeEnDosHojasComoMaximo() throws Exception {
        JasperReport plantillaCrr = JasperCompileManager.compileReport(
                getClass().getResourceAsStream("/reports/ficha-academica.jrxml"));
        Map<String, List<String[]>> planesCrr = cargarPlanes();
        assertThat(planesCrr).hasSize(6);

        for (var planCrr : planesCrr.entrySet()) {
            JasperPrint printCrr = llenar(plantillaCrr, planCrr.getValue());
            assertThat(printCrr.getPages().size())
                    .as("hojas de la ficha de %s", planCrr.getKey())
                    .isBetween(1, 2);
        }
    }

    @Test
    void enMatematicaLaPrimeraHojaLlegaHastaElCicloV() throws Exception {
        JasperReport plantillaCrr = JasperCompileManager.compileReport(
                getClass().getResourceAsStream("/reports/ficha-academica.jrxml"));
        var matematicaCrr = cargarPlanes().values().iterator().next();

        JasperPrint printCrr = llenar(plantillaCrr, matematicaCrr);

        var titulosHoja1Crr = printCrr.getPages().get(0).getElements().stream()
                .filter(net.sf.jasperreports.engine.JRPrintText.class::isInstance)
                .map(e -> ((net.sf.jasperreports.engine.JRPrintText) e).getFullText())
                .filter(t -> t.matches("\\p{Lu}+ CICLO")).toList();
        assertThat(titulosHoja1Crr).containsExactly("PRIMER CICLO", "SEGUNDO CICLO", "TERCER CICLO",
                "CUARTO CICLO", "QUINTO CICLO");
    }

    @Test
    void laSegundaHojaLlevaSuPropioEncabezadoYLaPrimeraNo() throws Exception {
        JasperReport plantillaCrr = JasperCompileManager.compileReport(
                getClass().getResourceAsStream("/reports/ficha-academica.jrxml"));

        JasperPrint printCrr = llenar(plantillaCrr, cargarPlanes().values().iterator().next());

        String encabezadoCrr = "Escuela de Educacion Superior Pedagogica Publica Juliaca (E.E.S.P.P.J.)"
                + " - APAZA CONDORCALLO, Eynar - FICHA ACADEMICA";
        assertThat(textos(printCrr, 0)).doesNotContain(encabezadoCrr)
                .contains("Programa de estudios: Educacion Secundaria - Especialidad Matematica");
        assertThat(textos(printCrr, 1)).contains(encabezadoCrr);
    }

    private static java.util.List<String> textos(JasperPrint printCrr, int paginaCrr) {
        return printCrr.getPages().get(paginaCrr).getElements().stream()
                .filter(net.sf.jasperreports.engine.JRPrintText.class::isInstance)
                .map(e -> ((net.sf.jasperreports.engine.JRPrintText) e).getFullText()).toList();
    }

    @Test
    void laInstitucionVaCentradaYSinDireccion() throws Exception {
        JasperReport plantillaCrr = JasperCompileManager.compileReport(
                getClass().getResourceAsStream("/reports/ficha-academica.jrxml"));

        JasperPrint printCrr = llenar(plantillaCrr, cargarPlanes().values().iterator().next());

        var institucionCrr = printCrr.getPages().get(0).getElements().stream()
                .filter(net.sf.jasperreports.engine.JRPrintText.class::isInstance)
                .map(e -> (net.sf.jasperreports.engine.JRPrintText) e)
                .filter(t -> t.getFullText().startsWith("Escuela de Educacion Superior"))
                .findFirst().orElseThrow();
        assertThat(institucionCrr.getHorizontalTextAlign())
                .isEqualTo(net.sf.jasperreports.engine.type.HorizontalTextAlignEnum.CENTER);

        assertThat(institucionCrr.getX() * 2 + institucionCrr.getWidth()).isEqualTo(2 * 40 + 515);
        assertThat(textos(printCrr, 0)).noneMatch(t -> t.contains("AV. Infancia"));
    }

    @Test
    void ningunNombreDeCursoQuedaCortado() throws Exception {
        JasperReport plantillaCrr = JasperCompileManager.compileReport(
                getClass().getResourceAsStream("/reports/ficha-academica.jrxml"));
        var planCrr = cargarPlanes().values().iterator().next();

        JasperPrint printCrr = llenar(plantillaCrr, planCrr);

        String impresoCrr = new StringBuilder().append(textos(printCrr)).toString();
        for (String[] cursoCrr : planCrr) {

            for (String palabraCrr : cursoCrr[2].split(" ")) {
                assertThat(impresoCrr).as("palabra '%s' de '%s'", palabraCrr, cursoCrr[2]).contains(palabraCrr);
            }
        }
    }

    @Test
    void cadaFilaImprimeNotaCreditosPuntajeYFecha() throws Exception {
        JasperReport plantillaCrr = JasperCompileManager.compileReport(
                getClass().getResourceAsStream("/reports/ficha-academica.jrxml"));
        var planCrr = cargarPlanes().values().iterator().next();

        String impresoCrr = textos(llenar(plantillaCrr, planCrr));

        assertThat(impresoCrr.lines().filter(l -> l.equals("15.0")).count()).isEqualTo(planCrr.size());
        assertThat(impresoCrr.lines().filter(l -> l.equals("31/12/2026")).count()).isEqualTo(planCrr.size());
    }

    private static String textos(JasperPrint printCrr) {
        StringBuilder textoCrr = new StringBuilder();
        for (var paginaCrr : printCrr.getPages()) {
            for (var elementoCrr : paginaCrr.getElements()) {
                if (elementoCrr instanceof net.sf.jasperreports.engine.JRPrintText t) {
                    textoCrr.append(t.getFullText()).append('\n');
                }
            }
        }
        return textoCrr.toString();
    }

    private static JasperPrint llenar(JasperReport plantillaCrr, List<String[]> cursosCrr) throws JRException {
        Map<Integer, List<String[]>> porCicloCrr = new LinkedHashMap<>();
        for (String[] c : cursosCrr) {
            porCicloCrr.computeIfAbsent(Integer.parseInt(c[1]), k -> new ArrayList<>()).add(c);
        }
        List<RacaFilaFichaCiclo> filasCrr = new ArrayList<>();
        for (var cicloCrr : porCicloCrr.entrySet()) {
            int creditosCrr = cicloCrr.getValue().stream().mapToInt(c -> Integer.parseInt(c[3])).sum();
            for (String[] c : cicloCrr.getValue()) {
                int credCrr = Integer.parseInt(c[3]);
                filasCrr.add(new RacaFilaFichaCiclo(cicloCrr.getKey(), "Ciclo " + ROMANOS_CRR[cicloCrr.getKey() - 1], "X", c[2], credCrr,
                        15.0, 15.0 * credCrr, "31/12/2026", null, 15.0, creditosCrr, 15.0 * creditosCrr));
            }
        }
        Map<String, Object> parametrosCrr = new HashMap<>();
        parametrosCrr.put("correlativo", "000001-2026");
        parametrosCrr.put("institucionNombre", "Escuela de Educacion Superior Pedagogica Publica Juliaca (E.E.S.P.P.J.)");
        parametrosCrr.put("institucionDireccion", "AV. Infancia");
        parametrosCrr.put("estudianteNombreCompleto", "APAZA CONDORCALLO, Eynar");
        parametrosCrr.put("estudianteDni", "76916587");
        parametrosCrr.put("estudianteCodigo", "76916587");
        parametrosCrr.put("programaNombre", "Educacion Secundaria - Especialidad Matematica");
        parametrosCrr.put("planNombre", "RVM 076-2020 MINEDU");
        parametrosCrr.put("fechaEmision", "15/10/2026");
        return JasperFillManager.fillReport(plantillaCrr, parametrosCrr, new JRBeanCollectionDataSource(filasCrr));
    }

    private Map<String, List<String[]>> cargarPlanes() throws IOException {
        Map<String, List<String[]>> planesCrr = new LinkedHashMap<>();
        try (var in = getClass().getResourceAsStream("/ficha/plan-estudios-ejemplo.txt")) {
            for (String lineaCrr : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
                if (lineaCrr.isBlank()) {
                    continue;
                }
                String[] partesCrr = lineaCrr.split("\\|");
                planesCrr.computeIfAbsent(partesCrr[0], k -> new ArrayList<>()).add(partesCrr);
            }
        }
        return planesCrr;
    }
}
