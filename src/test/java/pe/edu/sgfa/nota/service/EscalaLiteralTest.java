package pe.edu.sgfa.nota.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EscalaLiteralTest {

    @Test
    void sinNotaEsPrevioAlInicio() {
        assertThat(EscalaLiteral.codigoPara(null)).isEqualTo(EscalaLiteral.PREVIO_INICIO);
    }

    @Test
    void ceroEsPrevioAlInicio() {
        assertThat(EscalaLiteral.codigoPara(0.0)).isEqualTo(EscalaLiteral.PREVIO_INICIO);
    }

    @Test
    void unoAdiezEsInicio() {
        assertThat(EscalaLiteral.codigoPara(1.0)).isEqualTo(EscalaLiteral.INICIO);
        assertThat(EscalaLiteral.codigoPara(10.0)).isEqualTo(EscalaLiteral.INICIO);
    }

    @Test
    void onceACatorceEsProceso() {
        assertThat(EscalaLiteral.codigoPara(11.0)).isEqualTo(EscalaLiteral.PROCESO);
        assertThat(EscalaLiteral.codigoPara(13.0)).isEqualTo(EscalaLiteral.PROCESO);
        assertThat(EscalaLiteral.codigoPara(14.0)).isEqualTo(EscalaLiteral.PROCESO);
    }

    @Test
    void quinceADieciochoEsLogrado() {
        assertThat(EscalaLiteral.codigoPara(15.0)).isEqualTo(EscalaLiteral.LOGRADO);
        assertThat(EscalaLiteral.codigoPara(18.0)).isEqualTo(EscalaLiteral.LOGRADO);
    }

    @Test
    void diecinueveAVeinteEsDestacado() {
        assertThat(EscalaLiteral.codigoPara(19.0)).isEqualTo(EscalaLiteral.DESTACADO);
        assertThat(EscalaLiteral.codigoPara(20.0)).isEqualTo(EscalaLiteral.DESTACADO);
    }
}
