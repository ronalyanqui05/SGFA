package pe.edu.sgfa.ocr.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LectorSituacionTest {

    @ParameterizedTest
    @CsvSource({
            "D,DESTACADO", "L,LOGRADO", "E,PROCESO", "I,INICIO", "P,PREVIO_INICIO",
            "Destacado,DESTACADO", "LOGRADO,LOGRADO", "En proceso,PROCESO", "EN PROCESO,PROCESO",
            "Inicio,INICIO", "Previo al inicio,PREVIO_INICIO", "PREVIO AL INICIO,PREVIO_INICIO",
            "tosraDO,LOGRADO", "DESTACAD0,DESTACADO", "PROCESO,PROCESO", "l,LOGRADO"})
    void reconoceLetrasYPalabras(String texto, String esperado) {
        assertThat(LectorSituacion.codigoDe(texto)).isEqualTo(esperado);
    }

    @Test
    void textoQueNoEsUnaSituacionDaNull() {
        assertThat(LectorSituacion.codigoDe("XYZ")).isNull();
        assertThat(LectorSituacion.codigoDe("")).isNull();
        assertThat(LectorSituacion.codigoDe((String) null)).isNull();
    }

    @Test
    void descartaElRuidoDeAlLado() {
        assertThat(LectorSituacion.codigoDe(List.of("\u00BFS", "LOGRADO"))).isEqualTo("LOGRADO");
        assertThat(LectorSituacion.codigoDe(List.of("EN", "PROCESO"))).isEqualTo("PROCESO");
    }
}
