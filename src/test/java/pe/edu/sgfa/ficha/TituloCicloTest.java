package pe.edu.sgfa.ficha;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import pe.edu.sgfa.ficha.service.TituloCiclo;

class TituloCicloTest {

    @Test
    void losDiezCiclosVanSoloEnLetras() {
        assertThat(TituloCiclo.de(1)).isEqualTo("PRIMER CICLO");
        assertThat(TituloCiclo.de(2)).isEqualTo("SEGUNDO CICLO");
        assertThat(TituloCiclo.de(3)).isEqualTo("TERCER CICLO");
        assertThat(TituloCiclo.de(4)).isEqualTo("CUARTO CICLO");
        assertThat(TituloCiclo.de(5)).isEqualTo("QUINTO CICLO");
        assertThat(TituloCiclo.de(6)).isEqualTo("SEXTO CICLO");
        assertThat(TituloCiclo.de(7)).isEqualTo("SÉPTIMO CICLO");
        assertThat(TituloCiclo.de(8)).isEqualTo("OCTAVO CICLO");
        assertThat(TituloCiclo.de(9)).isEqualTo("NOVENO CICLO");
        assertThat(TituloCiclo.de(10)).isEqualTo("DÉCIMO CICLO");
    }

    @Test
    void fueraDeRangoNoFalla() {
        assertThat(TituloCiclo.de(11)).isEqualTo("CICLO 11");
    }
}
