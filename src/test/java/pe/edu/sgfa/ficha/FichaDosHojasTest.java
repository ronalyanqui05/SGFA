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

import pe.edu.sgfa.ficha.service.FilaFichaCiclo;

class FichaDosHojasTest {

    private static final String[] ROMANOS = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    @Test
    void cadaEspecialidadCabeEnDosHojasComoMaximo() throws Exception {
        JasperReport plntll = JasperCompileManager.compileReport(
                getClass().getResourceAsStream("/reports/ficha-academica.jrxml"));
        Map<String, List<String[]>> plns = cargarPlanes();
        assertThat(plns).hasSize(6);

        for (var pln : plns.entrySet()) {
            JasperPrint prnt = llenar(plntll, pln.getValue());
            assertThat(prnt.getPages().size())
                    .as("hojas de la ficha de %s", pln.getKey())
                    .isBetween(1, 2);
        }
    }

    @Test
    void enMatematicaLaPrimeraHojaLlegaHastaElCicloV() throws Exception {
        JasperReport plntll = JasperCompileManager.compileReport(
                getClass().getResourceAsStream("/reports/ficha-academica.jrxml"));
        var mtmtc = cargarPlanes().values().iterator().next();

        JasperPrint prnt = llenar(plntll, mtmtc);

        var ttlsHj1 = prnt.getPages().get(0).getElements().stream()
                .filter(net.sf.jasperreports.engine.JRPrintText.class::isInstance)
                .map(e -> ((net.sf.jasperreports.engine.JRPrintText) e).getFullText())
                .filter(t -> t.matches("\\p{Lu}+ CICLO")).toList();
        assertThat(ttlsHj1).containsExactly("PRIMER CICLO", "SEGUNDO CICLO", "TERCER CICLO",
                "CUARTO CICLO", "QUINTO CICLO");
    }

    @Test
    void laSegundaHojaLlevaSuPropioEncabezadoYLaPrimeraNo() throws Exception {
        JasperReport plntll = JasperCompileManager.compileReport(
                getClass().getResourceAsStream("/reports/ficha-academica.jrxml"));

        JasperPrint prnt = llenar(plntll, cargarPlanes().values().iterator().next());

        String encbzd = "Escuela de Educacion Superior Pedagogica Publica Juliaca (E.E.S.P.P.J.)"
                + " - APAZA CONDORCALLO, Eynar - FICHA ACADEMICA";
        assertThat(textos(prnt, 0)).doesNotContain(encbzd)
                .contains("Programa de estudios: Educacion Secundaria - Especialidad Matematica");
        assertThat(textos(prnt, 1)).contains(encbzd);
    }

    private static java.util.List<String> textos(JasperPrint prnt, int pgn) {
        return prnt.getPages().get(pgn).getElements().stream()
                .filter(net.sf.jasperreports.engine.JRPrintText.class::isInstance)
                .map(e -> ((net.sf.jasperreports.engine.JRPrintText) e).getFullText()).toList();
    }

    @Test
    void laInstitucionVaCentradaYSinDireccion() throws Exception {
        JasperReport plntll = JasperCompileManager.compileReport(
                getClass().getResourceAsStream("/reports/ficha-academica.jrxml"));

        JasperPrint prnt = llenar(plntll, cargarPlanes().values().iterator().next());

        var insttcn = prnt.getPages().get(0).getElements().stream()
                .filter(net.sf.jasperreports.engine.JRPrintText.class::isInstance)
                .map(e -> (net.sf.jasperreports.engine.JRPrintText) e)
                .filter(t -> t.getFullText().startsWith("Escuela de Educacion Superior"))
                .findFirst().orElseThrow();
        assertThat(insttcn.getHorizontalTextAlign())
                .isEqualTo(net.sf.jasperreports.engine.type.HorizontalTextAlignEnum.CENTER);

        assertThat(insttcn.getX() * 2 + insttcn.getWidth()).isEqualTo(2 * 40 + 515);
        assertThat(textos(prnt, 0)).noneMatch(t -> t.contains("AV. Infancia"));
    }

    @Test
    void ningunNombreDeCursoQuedaCortado() throws Exception {
        JasperReport plntll = JasperCompileManager.compileReport(
                getClass().getResourceAsStream("/reports/ficha-academica.jrxml"));
        var pln = cargarPlanes().values().iterator().next();

        JasperPrint prnt = llenar(plntll, pln);

        String imprs = new StringBuilder().append(textos(prnt)).toString();
        for (String[] crs : pln) {

            for (String plbr : crs[2].split(" ")) {
                assertThat(imprs).as("palabra '%s' de '%s'", plbr, crs[2]).contains(plbr);
            }
        }
    }

    @Test
    void cadaFilaImprimeNotaCreditosPuntajeYFecha() throws Exception {
        JasperReport plntll = JasperCompileManager.compileReport(
                getClass().getResourceAsStream("/reports/ficha-academica.jrxml"));
        var pln = cargarPlanes().values().iterator().next();

        String imprs = textos(llenar(plntll, pln));

        assertThat(imprs.lines().filter(l -> l.equals("15.0")).count()).isEqualTo(pln.size());
        assertThat(imprs.lines().filter(l -> l.equals("31/12/2026")).count()).isEqualTo(pln.size());
    }

    private static String textos(JasperPrint prnt) {
        StringBuilder txt = new StringBuilder();
        for (var pgn : prnt.getPages()) {
            for (var elmnt : pgn.getElements()) {
                if (elmnt instanceof net.sf.jasperreports.engine.JRPrintText t) {
                    txt.append(t.getFullText()).append('\n');
                }
            }
        }
        return txt.toString();
    }

    private static JasperPrint llenar(JasperReport plntll, List<String[]> crss) throws JRException {
        Map<Integer, List<String[]>> porCcl = new LinkedHashMap<>();
        for (String[] c : crss) {
            porCcl.computeIfAbsent(Integer.parseInt(c[1]), k -> new ArrayList<>()).add(c);
        }
        List<FilaFichaCiclo> fls = new ArrayList<>();
        for (var ccl : porCcl.entrySet()) {
            int crdts = ccl.getValue().stream().mapToInt(c -> Integer.parseInt(c[3])).sum();
            for (String[] c : ccl.getValue()) {
                int crd = Integer.parseInt(c[3]);
                fls.add(new FilaFichaCiclo(ccl.getKey(), "Ciclo " + ROMANOS[ccl.getKey() - 1], "X", c[2], crd,
                        15.0, 15.0 * crd, "31/12/2026", null, 15.0, crdts, 15.0 * crdts));
            }
        }
        Map<String, Object> prmtrs = new HashMap<>();
        prmtrs.put("correlativo", "000001-2026");
        prmtrs.put("institucionNombre", "Escuela de Educacion Superior Pedagogica Publica Juliaca (E.E.S.P.P.J.)");
        prmtrs.put("institucionDireccion", "AV. Infancia");
        prmtrs.put("estudianteNombreCompleto", "APAZA CONDORCALLO, Eynar");
        prmtrs.put("estudianteDni", "76916587");
        prmtrs.put("estudianteCodigo", "76916587");
        prmtrs.put("programaNombre", "Educacion Secundaria - Especialidad Matematica");
        prmtrs.put("planNombre", "RVM 076-2020 MINEDU");
        prmtrs.put("fechaEmision", "15/10/2026");
        return JasperFillManager.fillReport(plntll, prmtrs, new JRBeanCollectionDataSource(fls));
    }

    private Map<String, List<String[]>> cargarPlanes() throws IOException {
        Map<String, List<String[]>> plns = new LinkedHashMap<>();
        try (var in = getClass().getResourceAsStream("/ficha/plan-estudios-ejemplo.txt")) {
            for (String ln : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
                if (ln.isBlank()) {
                    continue;
                }
                String[] prts = ln.split("\\|");
                plns.computeIfAbsent(prts[0], k -> new ArrayList<>()).add(prts);
            }
        }
        return plns;
    }
}
