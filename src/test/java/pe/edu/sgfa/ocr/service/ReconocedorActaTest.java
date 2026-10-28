package pe.edu.sgfa.ocr.service;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReconocedorActaTest {

    private List<PalabraReconocida> fila(int y, String... tokens) {
        List<PalabraReconocida> plbrs = new ArrayList<>();
        int x = 10;
        for (String tkn : tokens) {
            plbrs.add(new PalabraReconocida(tkn, x, y, tkn.length() * 10, 20));
            x += tkn.length() * 10 + 15;
        }
        return plbrs;
    }

    @Test
    void separaOrdenMatriculaApellidosYNombresDeUnaFilaReal() {

        List<PalabraReconocida> plbrs = fila(100,
                "1", "73821063", "CALCINA", "AGUILAR", "ALAIN", "UBALDO",
                "L", "15", "45", "D", "20", "60");

        var fls = ReconocedorActa.agrupar(plbrs, 2);

        assertThat(fls).hasSize(1);
        var dtctd = fls.get(0);
        assertThat(dtctd.ordenNro()).isEqualTo("1");
        assertThat(dtctd.matricula()).isEqualTo("73821063");
        assertThat(dtctd.apellidoPaterno()).isEqualTo("CALCINA");
        assertThat(dtctd.apellidoMaterno()).isEqualTo("AGUILAR");
        assertThat(dtctd.nombres()).isEqualTo("ALAIN UBALDO");
        assertThat(dtctd.notasPorCurso()).hasSize(2);
        assertThat(dtctd.notasPorCurso().get(0).situacionTexto()).isEqualTo("L");
        assertThat(dtctd.notasPorCurso().get(0).notaTexto()).isEqualTo("15");
        assertThat(dtctd.notasPorCurso().get(1).situacionTexto()).isEqualTo("D");
        assertThat(dtctd.notasPorCurso().get(1).notaTexto()).isEqualTo("20");
    }

    @Test
    void separaDosFilasDistintasPorSuCoordenadaY() {
        List<PalabraReconocida> tds = new ArrayList<>();
        tds.addAll(fila(100, "1", "73821063", "CALCINA", "AGUILAR", "ALAIN", "UBALDO", "L", "15"));
        tds.addAll(fila(140, "2", "75978654", "CALSIN", "SUCASCA", "SOLEDAD", "YOVANA", "D", "20"));

        var fls = ReconocedorActa.agrupar(tds, 1);

        assertThat(fls).hasSize(2);
        assertThat(fls.get(0).matricula()).isEqualTo("73821063");
        assertThat(fls.get(1).matricula()).isEqualTo("75978654");
    }

    @Test
    void noFallaConUnaFilaIncompleta() {
        List<PalabraReconocida> plbrs = fila(100, "LIPA", "CONDOR", "MACLODIA");

        var fls = ReconocedorActa.agrupar(plbrs, 3);

        assertThat(fls).hasSize(1);
        var dtctd = fls.get(0);
        assertThat(dtctd.ordenNro()).isNull();
        assertThat(dtctd.matricula()).isNull();
        assertThat(dtctd.apellidoPaterno()).isEqualTo("LIPA");
        assertThat(dtctd.apellidoMaterno()).isEqualTo("CONDOR");
        assertThat(dtctd.nombres()).isEqualTo("MACLODIA");
        assertThat(dtctd.notasPorCurso()).hasSize(3);
        assertThat(dtctd.notasPorCurso().get(0).situacionTexto()).isNull();
    }

    @Test
    void listaVaciaNoProduceFilas() {
        assertThat(ReconocedorActa.agrupar(List.of(), 7)).isEmpty();
    }
}
