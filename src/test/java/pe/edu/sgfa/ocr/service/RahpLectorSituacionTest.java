package pe.edu.sgfa.ocr.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RahpLectorSituacionTest {

    @ParameterizedTest
    @CsvSource({
            "D,DESTACADO", "L,LOGRADO", "E,PROCESO", "I,INICIO", "P,PREVIO_INICIO",
            "Destacado,DESTACADO", "LOGRADO,LOGRADO", "En proceso,PROCESO", "EN PROCESO,PROCESO",
            "Inicio,INICIO", "Previo al inicio,PREVIO_INICIO", "PREVIO AL INICIO,PREVIO_INICIO",
            "tosraDO,LOGRADO", "DESTACAD0,DESTACADO", "PROCESO,PROCESO", "l,LOGRADO"})
    void reconoceLetrasYPalabras(String textoHnc, String esperadoHnc) {
        assertThat(RahpLectorSituacion.rahpCodigoDe(textoHnc)).isEqualTo(esperadoHnc);
    }

    @Test
    void textoQueNoEsUnaSituacionDaNull() {
        assertThat(RahpLectorSituacion.rahpCodigoDe("XYZ")).isNull();
        assertThat(RahpLectorSituacion.rahpCodigoDe("")).isNull();
        assertThat(RahpLectorSituacion.rahpCodigoDe((String) null)).isNull();
    }

    @Test
    void descartaElRuidoDeAlLado() {
        assertThat(RahpLectorSituacion.rahpCodigoDe(List.of("\u00BFS", "LOGRADO"))).isEqualTo("LOGRADO");
        assertThat(RahpLectorSituacion.rahpCodigoDe(List.of("EN", "PROCESO"))).isEqualTo("PROCESO");
    }
}
