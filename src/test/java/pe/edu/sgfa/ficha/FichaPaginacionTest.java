package pe.edu.sgfa.ficha;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.sf.jasperreports.engine.JRPrintText;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.junit.jupiter.api.Test;

import pe.edu.sgfa.ficha.service.FilaFichaCiclo;

class FichaPaginacionTest {

    @Test
    void cadaCicloQuedaCompletoEnUnaSolaHoja() throws Exception {
        var plntll = JasperCompileManager.compileReport(getClass().getResourceAsStream("/reports/ficha-academica.jrxml"));
        String[] rmns = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        List<FilaFichaCiclo> fls = new ArrayList<>();
        for (int ccl = 1; ccl <= 10; ccl++) {
            for (int crs = 1; crs <= 9; crs++) {
                fls.add(new FilaFichaCiclo(ccl, "Ciclo " + rmns[ccl - 1], "C" + crs, "Curso " + crs, 3,
                        14.0, 42.0, "31/12/2026", null, 14.0, 27, 378.0));
            }
        }
        var prmtrs = new HashMap<String, Object>();
        for (String nmbr : new String[] {"institucionNombre", "estudianteNombreCompleto", "programaNombre",
                "planNombre", "fechaEmision"}) {
            prmtrs.put(nmbr, nmbr);
        }

        JasperPrint prnt = JasperFillManager.fillReport(plntll, prmtrs, new JRBeanCollectionDataSource(fls));

        assertThat(prnt.getPages().size()).isGreaterThanOrEqualTo(3);
        int ttls = 0;
        for (int pgn = 0; pgn < prnt.getPages().size(); pgn++) {
            long ttlsDeLaHj = textos(prnt, pgn).stream().filter(t -> t.matches("\\p{Lu}+ CICLO")).count();
            long pieDeLaHj = textos(prnt, pgn).stream().filter(t -> t.startsWith("Promedio ponderado semestral")).count();

            assertThat(pieDeLaHj).as("hoja %d: ciclos completos", pgn + 1).isEqualTo(ttlsDeLaHj);
            ttls += ttlsDeLaHj;
        }
        assertThat(ttls).isEqualTo(10);
    }

    private static List<String> textos(JasperPrint prnt, int pgn) {
        return prnt.getPages().get(pgn).getElements().stream()
                .filter(JRPrintText.class::isInstance)
                .map(e -> ((JRPrintText) e).getFullText())
                .toList();
    }
}
