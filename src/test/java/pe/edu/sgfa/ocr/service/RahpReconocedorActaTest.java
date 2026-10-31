package pe.edu.sgfa.ocr.service;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RahpReconocedorActaTest {

    private List<RahpPalabraReconocida> fila(int y, String... tokensHnc) {
        List<RahpPalabraReconocida> palabrasHnc = new ArrayList<>();
        int x = 10;
        for (String tokenHnc : tokensHnc) {
            palabrasHnc.add(new RahpPalabraReconocida(tokenHnc, x, y, tokenHnc.length() * 10, 20));
            x += tokenHnc.length() * 10 + 15;
        }
        return palabrasHnc;
    }

    @Test
    void separaOrdenMatriculaApellidosYNombresDeUnaFilaReal() {

        List<RahpPalabraReconocida> palabrasHnc = fila(100,
                "1", "73821063", "CALCINA", "AGUILAR", "ALAIN", "UBALDO",
                "L", "15", "45", "D", "20", "60");

        var filasHnc = RahpReconocedorActa.rahpAgrupar(palabrasHnc, 2);

        assertThat(filasHnc).hasSize(1);
        var detectadaHnc = filasHnc.get(0);
        assertThat(detectadaHnc.ordenNro()).isEqualTo("1");
        assertThat(detectadaHnc.matricula()).isEqualTo("73821063");
        assertThat(detectadaHnc.apellidoPaterno()).isEqualTo("CALCINA");
        assertThat(detectadaHnc.apellidoMaterno()).isEqualTo("AGUILAR");
        assertThat(detectadaHnc.nombres()).isEqualTo("ALAIN UBALDO");
        assertThat(detectadaHnc.notasPorCurso()).hasSize(2);
        assertThat(detectadaHnc.notasPorCurso().get(0).situacionTexto()).isEqualTo("L");
        assertThat(detectadaHnc.notasPorCurso().get(0).notaTexto()).isEqualTo("15");
        assertThat(detectadaHnc.notasPorCurso().get(1).situacionTexto()).isEqualTo("D");
        assertThat(detectadaHnc.notasPorCurso().get(1).notaTexto()).isEqualTo("20");
    }

    @Test
    void separaDosFilasDistintasPorSuCoordenadaY() {
        List<RahpPalabraReconocida> todasHnc = new ArrayList<>();
        todasHnc.addAll(fila(100, "1", "73821063", "CALCINA", "AGUILAR", "ALAIN", "UBALDO", "L", "15"));
        todasHnc.addAll(fila(140, "2", "75978654", "CALSIN", "SUCASCA", "SOLEDAD", "YOVANA", "D", "20"));

        var filasHnc = RahpReconocedorActa.rahpAgrupar(todasHnc, 1);

        assertThat(filasHnc).hasSize(2);
        assertThat(filasHnc.get(0).matricula()).isEqualTo("73821063");
        assertThat(filasHnc.get(1).matricula()).isEqualTo("75978654");
    }

    @Test
    void noFallaConUnaFilaIncompleta() {
        List<RahpPalabraReconocida> palabrasHnc = fila(100, "LIPA", "CONDOR", "MACLODIA");

        var filasHnc = RahpReconocedorActa.rahpAgrupar(palabrasHnc, 3);

        assertThat(filasHnc).hasSize(1);
        var detectadaHnc = filasHnc.get(0);
        assertThat(detectadaHnc.ordenNro()).isNull();
        assertThat(detectadaHnc.matricula()).isNull();
        assertThat(detectadaHnc.apellidoPaterno()).isEqualTo("LIPA");
        assertThat(detectadaHnc.apellidoMaterno()).isEqualTo("CONDOR");
        assertThat(detectadaHnc.nombres()).isEqualTo("MACLODIA");
        assertThat(detectadaHnc.notasPorCurso()).hasSize(3);
        assertThat(detectadaHnc.notasPorCurso().get(0).situacionTexto()).isNull();
    }

    @Test
    void listaVaciaNoProduceFilas() {
        assertThat(RahpReconocedorActa.rahpAgrupar(List.of(), 7)).isEmpty();
    }
}
