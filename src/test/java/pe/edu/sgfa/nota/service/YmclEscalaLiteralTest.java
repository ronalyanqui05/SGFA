package pe.edu.sgfa.nota.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class YmclEscalaLiteralTest {

    @Test
    void sinNotaEsPrevioAlInicio() {
        assertThat(YmclEscalaLiteral.ymclCodigoPara(null)).isEqualTo(YmclEscalaLiteral.PREVIO_INICIO);
    }

    @Test
    void ceroEsPrevioAlInicio() {
        assertThat(YmclEscalaLiteral.ymclCodigoPara(0.0)).isEqualTo(YmclEscalaLiteral.PREVIO_INICIO);
    }

    @Test
    void unoAdiezEsInicio() {
        assertThat(YmclEscalaLiteral.ymclCodigoPara(1.0)).isEqualTo(YmclEscalaLiteral.INICIO);
        assertThat(YmclEscalaLiteral.ymclCodigoPara(10.0)).isEqualTo(YmclEscalaLiteral.INICIO);
    }

    @Test
    void onceACatorceEsProceso() {
        assertThat(YmclEscalaLiteral.ymclCodigoPara(11.0)).isEqualTo(YmclEscalaLiteral.PROCESO);
        assertThat(YmclEscalaLiteral.ymclCodigoPara(13.0)).isEqualTo(YmclEscalaLiteral.PROCESO);
        assertThat(YmclEscalaLiteral.ymclCodigoPara(14.0)).isEqualTo(YmclEscalaLiteral.PROCESO);
    }

    @Test
    void quinceADieciochoEsLogrado() {
        assertThat(YmclEscalaLiteral.ymclCodigoPara(15.0)).isEqualTo(YmclEscalaLiteral.LOGRADO);
        assertThat(YmclEscalaLiteral.ymclCodigoPara(18.0)).isEqualTo(YmclEscalaLiteral.LOGRADO);
    }

    @Test
    void diecinueveAVeinteEsDestacado() {
        assertThat(YmclEscalaLiteral.ymclCodigoPara(19.0)).isEqualTo(YmclEscalaLiteral.DESTACADO);
        assertThat(YmclEscalaLiteral.ymclCodigoPara(20.0)).isEqualTo(YmclEscalaLiteral.DESTACADO);
    }
}
