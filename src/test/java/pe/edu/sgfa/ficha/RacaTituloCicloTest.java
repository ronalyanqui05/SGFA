package pe.edu.sgfa.ficha;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import pe.edu.sgfa.ficha.service.RacaTituloCiclo;

class RacaTituloCicloTest {

    @Test
    void losDiezCiclosVanSoloEnLetras() {
        assertThat(RacaTituloCiclo.de(1)).isEqualTo("PRIMER CICLO");
        assertThat(RacaTituloCiclo.de(2)).isEqualTo("SEGUNDO CICLO");
        assertThat(RacaTituloCiclo.de(3)).isEqualTo("TERCER CICLO");
        assertThat(RacaTituloCiclo.de(4)).isEqualTo("CUARTO CICLO");
        assertThat(RacaTituloCiclo.de(5)).isEqualTo("QUINTO CICLO");
        assertThat(RacaTituloCiclo.de(6)).isEqualTo("SEXTO CICLO");
        assertThat(RacaTituloCiclo.de(7)).isEqualTo("SÉPTIMO CICLO");
        assertThat(RacaTituloCiclo.de(8)).isEqualTo("OCTAVO CICLO");
        assertThat(RacaTituloCiclo.de(9)).isEqualTo("NOVENO CICLO");
        assertThat(RacaTituloCiclo.de(10)).isEqualTo("DÉCIMO CICLO");
    }

    @Test
    void fueraDeRangoNoFalla() {
        assertThat(RacaTituloCiclo.de(11)).isEqualTo("CICLO 11");
    }
}
