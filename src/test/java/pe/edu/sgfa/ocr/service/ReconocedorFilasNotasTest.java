package pe.edu.sgfa.ocr.service;

import java.util.List;

import org.junit.jupiter.api.Test;

import pe.edu.sgfa.programa.domain.Ciclo;
import pe.edu.sgfa.programa.domain.Curso;
import pe.edu.sgfa.programa.domain.PlanCurso;
import pe.edu.sgfa.programa.domain.PlanEstudio;

import static org.assertj.core.api.Assertions.assertThat;

class ReconocedorFilasNotasTest {

    private static final Long SITUACION_APROBADO = 7L;

    private List<PlanCurso> planDeEjemplo() {
        PlanEstudio pln = new PlanEstudio(null, "PLAN-1", "Plan de prueba", null, null);
        Ciclo ccl = new Ciclo(pln, 1, "I", 1);
        Curso mtmtc = new Curso("MAT-101", "Matematica Basica", 4, 4, "OBLIGATORIO");
        Curso cmnccn = new Curso("COM-101", "Comunicacion", 3, 3, "OBLIGATORIO");
        return List.of(
                new PlanCurso(pln, ccl, mtmtc, 1, true),
                new PlanCurso(pln, ccl, cmnccn, 2, true));
    }

    @Test
    void reconoceFilasConCursoYNotaAlFinalDeLaLinea() {
        String txt = """
                Matematica Basica 15
                Comunicacion 17.5
                """;

        var rsltd = ReconocedorFilasNotas.reconocer(txt, planDeEjemplo(), SITUACION_APROBADO);

        assertThat(rsltd.items()).hasSize(2);
        assertThat(rsltd.advertencias()).isEmpty();
        assertThat(rsltd.items().get(0).item().notaFinal()).isEqualTo(15.0);
        assertThat(rsltd.items().get(0).item().situacionId()).isEqualTo(SITUACION_APROBADO);
        assertThat(rsltd.items().get(1).item().notaFinal()).isEqualTo(17.5);
    }

    @Test
    void emparejaPorCodigoDeCursoIgnorandoMayusculasYTildes() {
        String txt = "mat-101: 12";

        var rsltd = ReconocedorFilasNotas.reconocer(txt, planDeEjemplo(), SITUACION_APROBADO);

        assertThat(rsltd.items()).hasSize(1);
        assertThat(rsltd.items().get(0).curso()).contains("Matematica Basica");
    }

    @Test
    void reportaAdvertenciaCuandoNoHayNotaValida() {
        String txt = "Matematica Basica sin nota";

        var rsltd = ReconocedorFilasNotas.reconocer(txt, planDeEjemplo(), SITUACION_APROBADO);

        assertThat(rsltd.items()).isEmpty();
        assertThat(rsltd.advertencias()).hasSize(1);
        assertThat(rsltd.advertencias().get(0)).contains("no se encontro una nota");
    }

    @Test
    void reportaAdvertenciaCuandoLaNotaEstaFueraDeRango() {
        String txt = "Matematica Basica 25";

        var rsltd = ReconocedorFilasNotas.reconocer(txt, planDeEjemplo(), SITUACION_APROBADO);

        assertThat(rsltd.items()).isEmpty();
        assertThat(rsltd.advertencias().get(0)).contains("fuera del rango");
    }

    @Test
    void reportaAdvertenciaCuandoNoIdentificaNingunCurso() {
        String txt = "Curso Inexistente 10";

        var rsltd = ReconocedorFilasNotas.reconocer(txt, planDeEjemplo(), SITUACION_APROBADO);

        assertThat(rsltd.items()).isEmpty();
        assertThat(rsltd.advertencias().get(0)).contains("no se identifico ningun curso");
    }

    @Test
    void ignoraLineasVacias() {
        String txt = "\n\n   \nMatematica Basica 14\n\n";

        var rsltd = ReconocedorFilasNotas.reconocer(txt, planDeEjemplo(), SITUACION_APROBADO);

        assertThat(rsltd.items()).hasSize(1);
    }
}
