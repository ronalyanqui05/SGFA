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

import pe.edu.sgfa.ficha.service.RacaFilaFichaCiclo;

class RacaFichaPaginacionTest {

    @Test
    void cadaCicloQuedaCompletoEnUnaSolaHoja() throws Exception {
        var plantillaCrr = JasperCompileManager.compileReport(getClass().getResourceAsStream("/reports/ficha-academica.jrxml"));
        String[] romanosCrr = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        List<RacaFilaFichaCiclo> filasCrr = new ArrayList<>();
        for (int cicloCrr = 1; cicloCrr <= 10; cicloCrr++) {
            for (int cursoCrr = 1; cursoCrr <= 9; cursoCrr++) {
                filasCrr.add(new RacaFilaFichaCiclo(cicloCrr, "Ciclo " + romanosCrr[cicloCrr - 1], "C" + cursoCrr, "Curso " + cursoCrr, 3,
                        14.0, 42.0, "31/12/2026", null, 14.0, 27, 378.0));
            }
        }
        var parametrosCrr = new HashMap<String, Object>();
        for (String nombreCrr : new String[] {"institucionNombre", "estudianteNombreCompleto", "programaNombre",
                "planNombre", "fechaEmision"}) {
            parametrosCrr.put(nombreCrr, nombreCrr);
        }

        JasperPrint printCrr = JasperFillManager.fillReport(plantillaCrr, parametrosCrr, new JRBeanCollectionDataSource(filasCrr));

        assertThat(printCrr.getPages().size()).isGreaterThanOrEqualTo(3);
        int titulosCrr = 0;
        for (int paginaCrr = 0; paginaCrr < printCrr.getPages().size(); paginaCrr++) {
            long titulosDeLaHojaCrr = textos(printCrr, paginaCrr).stream().filter(t -> t.matches("\\p{Lu}+ CICLO")).count();
            long pieDeLaHojaCrr = textos(printCrr, paginaCrr).stream().filter(t -> t.startsWith("Promedio ponderado semestral")).count();

            assertThat(pieDeLaHojaCrr).as("hoja %d: ciclos completos", paginaCrr + 1).isEqualTo(titulosDeLaHojaCrr);
            titulosCrr += titulosDeLaHojaCrr;
        }
        assertThat(titulosCrr).isEqualTo(10);
    }

    private static List<String> textos(JasperPrint printCrr, int paginaCrr) {
        return printCrr.getPages().get(paginaCrr).getElements().stream()
                .filter(JRPrintText.class::isInstance)
                .map(e -> ((JRPrintText) e).getFullText())
                .toList();
    }
}
