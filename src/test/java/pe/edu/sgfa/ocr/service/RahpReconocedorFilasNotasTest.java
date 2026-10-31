package pe.edu.sgfa.ocr.service;

import java.util.List;

import org.junit.jupiter.api.Test;

import pe.edu.sgfa.programa.domain.RacaCiclo;
import pe.edu.sgfa.programa.domain.RacaCurso;
import pe.edu.sgfa.programa.domain.RacaPlanCurso;
import pe.edu.sgfa.programa.domain.RacaPlanEstudio;

import static org.assertj.core.api.Assertions.assertThat;

class RahpReconocedorFilasNotasTest {

    private static final Long SITUACION_APROBADO_HNC = 7L;

    private List<RacaPlanCurso> planDeEjemplo() {
        RacaPlanEstudio planHnc = new RacaPlanEstudio(null, "PLAN-1", "Plan de prueba", null, null);
        RacaCiclo cicloHnc = new RacaCiclo(planHnc, 1, "I", 1);
        RacaCurso matematicaHnc = new RacaCurso("MAT-101", "Matematica Basica", 4, 4, "OBLIGATORIO");
        RacaCurso comunicacionHnc = new RacaCurso("COM-101", "Comunicacion", 3, 3, "OBLIGATORIO");
        return List.of(
                new RacaPlanCurso(planHnc, cicloHnc, matematicaHnc, 1, true),
                new RacaPlanCurso(planHnc, cicloHnc, comunicacionHnc, 2, true));
    }

    @Test
    void reconoceFilasConCursoYNotaAlFinalDeLaLinea() {
        String textoHnc = """
                Matematica Basica 15
                Comunicacion 17.5
                """;

        var resultadoHnc = RahpReconocedorFilasNotas.rahpReconocer(textoHnc, planDeEjemplo(), SITUACION_APROBADO_HNC);

        assertThat(resultadoHnc.items()).hasSize(2);
        assertThat(resultadoHnc.advertencias()).isEmpty();
        assertThat(resultadoHnc.items().get(0).item().notaFinal()).isEqualTo(15.0);
        assertThat(resultadoHnc.items().get(0).item().situacionId()).isEqualTo(SITUACION_APROBADO_HNC);
        assertThat(resultadoHnc.items().get(1).item().notaFinal()).isEqualTo(17.5);
    }

    @Test
    void emparejaPorCodigoDeCursoIgnorandoMayusculasYTildes() {
        String textoHnc = "mat-101: 12";

        var resultadoHnc = RahpReconocedorFilasNotas.rahpReconocer(textoHnc, planDeEjemplo(), SITUACION_APROBADO_HNC);

        assertThat(resultadoHnc.items()).hasSize(1);
        assertThat(resultadoHnc.items().get(0).curso()).contains("Matematica Basica");
    }

    @Test
    void reportaAdvertenciaCuandoNoHayNotaValida() {
        String textoHnc = "Matematica Basica sin nota";

        var resultadoHnc = RahpReconocedorFilasNotas.rahpReconocer(textoHnc, planDeEjemplo(), SITUACION_APROBADO_HNC);

        assertThat(resultadoHnc.items()).isEmpty();
        assertThat(resultadoHnc.advertencias()).hasSize(1);
        assertThat(resultadoHnc.advertencias().get(0)).contains("no se encontro una nota");
    }

    @Test
    void reportaAdvertenciaCuandoLaNotaEstaFueraDeRango() {
        String textoHnc = "Matematica Basica 25";

        var resultadoHnc = RahpReconocedorFilasNotas.rahpReconocer(textoHnc, planDeEjemplo(), SITUACION_APROBADO_HNC);

        assertThat(resultadoHnc.items()).isEmpty();
        assertThat(resultadoHnc.advertencias().get(0)).contains("fuera del rango");
    }

    @Test
    void reportaAdvertenciaCuandoNoIdentificaNingunCurso() {
        String textoHnc = "Curso Inexistente 10";

        var resultadoHnc = RahpReconocedorFilasNotas.rahpReconocer(textoHnc, planDeEjemplo(), SITUACION_APROBADO_HNC);

        assertThat(resultadoHnc.items()).isEmpty();
        assertThat(resultadoHnc.advertencias().get(0)).contains("no se identifico ningun curso");
    }

    @Test
    void ignoraLineasVacias() {
        String textoHnc = "\n\n   \nMatematica Basica 14\n\n";

        var resultadoHnc = RahpReconocedorFilasNotas.rahpReconocer(textoHnc, planDeEjemplo(), SITUACION_APROBADO_HNC);

        assertThat(resultadoHnc.items()).hasSize(1);
    }
}
