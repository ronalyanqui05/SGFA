package pe.edu.sgfa.ocr.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import pe.edu.sgfa.ocr.service.ReconocedorReporteCalificaciones.ReporteDetectado;

class ReconocedorReporteCalificacionesTest {

    private final List<PalabraReconocida> palabras = new ArrayList<>();

    private void linea(int x, int y, String txt) {
        int crsr = x;
        for (String t : txt.split(" ")) {
            int anch = 10 * t.length();
            palabras.add(new PalabraReconocida(t, crsr, y - 8, anch, 17));
            crsr += anch + 8;
        }
    }

    private ReporteDetectado reporteEjemplo() {
        linea(258, 186, "REPORTE DE CALIFICACIONES DE CURSOS Y MÓDULOS");
        linea(70, 243, "Nombre de la Institución:");
        linea(870, 235, "Periodo");
        linea(980, 244, "2026 - I");
        linea(450, 293, "EDUCACIÓN SECUNDARIA, ESPECIALIDAD");
        linea(585, 319, "MATEMÁTICA");
        linea(870, 295, "Ciclo -");
        linea(980, 304, "CICLO I");
        linea(72, 362, "Apellidos y Nombres del Estudiante:");
        linea(510, 368, "APAZA CONDORCALLO, Eynar");
        linea(870, 366, "Turno:");
        linea(980, 366, "Mañana");
        linea(72, 415, "Nro. de Matricula:");
        linea(610, 422, "76916587");

        linea(82, 483, "N°");
        linea(250, 486, "CURSOS");
        linea(457, 488, "COMPETENCIA");
        linea(1010, 470, "CALIFICACIÓN PARA EL");
        linea(1000, 487, "SISTEMA DE EDUCACIÓN");
        linea(1050, 503, "SUPERIOR");

        curso(1, 563, new String[] {"LECTURA Y ESCRITURA EN LA", "EDUCACIÓN SUPERIOR"}, "16");
        curso(2, 655, new String[] {"RESOLUCIÓN DE PROBLEMAS", "MATEMÁTICOS I"}, "14");
        curso(3, 748, new String[] {"HISTORIA, SOCIEDAD Y DIVERSIDAD"}, "16");
        curso(4, 840, new String[] {"DESARROLLO PERSONAL I"}, "16");
        curso(5, 932, new String[] {"PRÁCTICA E INVESTIGACIÓN I"}, "16");
        curso(6, 1027, new String[] {"EDUCACIÓN Y SOCIEDAD EN EL SIGLO", "XXI"}, "17");
        linea(90, 1130, "7");
        linea(450, 1090, "Competencia 1");
        linea(275, 1195, "PROMEDIO PONDERADO DEL PERIODO ACADÉMICO");
        linea(1000, 1197, "15.96");
        return ReconocedorReporteCalificaciones.reconocer(palabras);
    }

    private void curso(int n, int yNt, String[] lnsNmbr, String nota) {
        linea(90, yNt, String.valueOf(n));
        int y = yNt - (lnsNmbr.length - 1) * 9;
        for (String l : lnsNmbr) {
            linea(115, y - 6, l);
            y += 18;
        }
        for (int c = -1; c <= 1; c++) {
            linea(445, yNt + c * 31, "Competencia " + (3 + c));
        }
        linea(885, yNt, nota.equals("14") ? "EN PROCESO" : "LOGRADO");
        linea(1060, yNt, nota);
    }

    @Test
    void leeEncabezado() {
        ReporteDetectado r = reporteEjemplo();
        assertThat(r.matricula()).isEqualTo("76916587");
        assertThat(r.apellidoPaterno()).isEqualTo("APAZA");
        assertThat(r.apellidoMaterno()).isEqualTo("CONDORCALLO");
        assertThat(r.nombres()).isEqualTo("Eynar");
        assertThat(r.periodo()).isEqualTo("2026-I");
        assertThat(r.ciclo()).isEqualTo(1);
        assertThat(r.promedioReporte()).isEqualTo(15.96);
        assertThat(r.textoCompleto()).contains("MATEMÁTICA");
    }

    @Test
    void leeLosSeisCursosPorNombreConSuNota() {
        ReporteDetectado r = reporteEjemplo();
        assertThat(r.cursos()).extracting(ReconocedorReporteCalificaciones.CursoDetectado::nombre).containsExactly(
                "LECTURA Y ESCRITURA EN LA EDUCACIÓN SUPERIOR",
                "RESOLUCIÓN DE PROBLEMAS MATEMÁTICOS I",
                "HISTORIA, SOCIEDAD Y DIVERSIDAD",
                "DESARROLLO PERSONAL I",
                "PRÁCTICA E INVESTIGACIÓN I",
                "EDUCACIÓN Y SOCIEDAD EN EL SIGLO XXI");
        assertThat(r.cursos()).extracting(ReconocedorReporteCalificaciones.CursoDetectado::nota)
                .containsExactly(16.0, 14.0, 16.0, 16.0, 16.0, 17.0);
        assertThat(r.cursos()).extracting(ReconocedorReporteCalificaciones.CursoDetectado::situacion)
                .containsExactly("LOGRADO", "PROCESO", "LOGRADO", "LOGRADO", "LOGRADO", "LOGRADO");
    }

    @Test
    void leeLaSituacionTambienComoLetra() {
        linea(250, 486, "CURSOS");
        linea(457, 488, "COMPETENCIA");
        linea(1000, 487, "SISTEMA");
        linea(115, 557, "LENGUA I");
        linea(900, 563, "D");
        linea(1060, 563, "19");
        linea(115, 650, "ARTE I");
        linea(900, 655, "P");
        linea(1060, 655, "0");
        ReporteDetectado r = ReconocedorReporteCalificaciones.reconocer(palabras);
        assertThat(r.cursos()).extracting(ReconocedorReporteCalificaciones.CursoDetectado::situacion)
                .containsExactly("DESTACADO", "PREVIO_INICIO");
    }

    @Test
    void sinTablaAvisaYNoFalla() {
        linea(10, 10, "texto cualquiera");
        ReporteDetectado r = ReconocedorReporteCalificaciones.reconocer(palabras);
        assertThat(r.cursos()).isEmpty();
        assertThat(r.advertencias()).isNotEmpty();
    }

    @Test
    void emparejaCursosConErroresDeOcr() {
        List<String> ctlg = List.of("Lectura y escritura en la educación superior", "Resolución de problemas matemáticos I",
                "Resolución de problemas matemáticos II", "Desarrollo personal I");
        assertThat(EmparejadorNombres.mejorCoincidencia("LECTURA Y ESCRITURA EN LA EDUCACION SUPERI0R", ctlg, s -> s))
                .contains(ctlg.get(0));
        assertThat(EmparejadorNombres.mejorCoincidencia("RESOLUCIÓN DE PROBLEMAS MATEMÁTICOS I", ctlg, s -> s))
                .contains(ctlg.get(1));
        assertThat(EmparejadorNombres.mejorCoincidencia("CURSO INEXISTENTE DE PRUEBA", ctlg, s -> s)).isEmpty();
    }

    @Test
    void toleraLaBarraQueElOcrLeeComoIRomana() {
        linea(870, 295, "Periodo 2026 - |");
        linea(980, 304, "CICLO |");
        linea(250, 486, "CURSOS");
        linea(457, 488, "COMPETENCIA");
        linea(1000, 487, "SISTEMA");
        linea(115, 557, "DESARROLLO PERSONAL | j");
        linea(1060, 563, "16");
        ReporteDetectado r = ReconocedorReporteCalificaciones.reconocer(palabras);
        assertThat(r.periodo()).isEqualTo("2026-I");
        assertThat(r.ciclo()).isEqualTo(1);
        assertThat(r.cursos()).extracting(ReconocedorReporteCalificaciones.CursoDetectado::nombre)
                .containsExactly("DESARROLLO PERSONAL I");
    }

    @Test
    void unoYDosNoSeConfunden() {
        List<String> ctlg = List.of("Desarrollo personal I", "Desarrollo personal II");
        assertThat(EmparejadorNombres.mejorCoincidencia("DESARROLLO PERSONAL I", ctlg, s -> s))
                .contains("Desarrollo personal I");
        assertThat(EmparejadorNombres.mejorCoincidencia("DESARROLLO PERSONAL II", ctlg, s -> s))
                .contains("Desarrollo personal II");
        assertThat(EmparejadorNombres.mejorCoincidencia("DESARROLLO PERSONAL", ctlg, s -> s)).isEmpty();
    }

    @Test
    void emparejaAunConPalabrasDesordenadas() {
        List<String> ctlg = List.of("Educación y sociedad en el siglo XXI", "Historia, sociedad y diversidad");
        assertThat(EmparejadorNombres.mejorCoincidencia("Y SOCIEDAD EN ET SIGLO EDUCACION", ctlg, s -> s))
                .contains(ctlg.get(0));
    }
}
