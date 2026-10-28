package pe.edu.sgfa.reporte;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import pe.edu.sgfa.estudiante.service.EstudianteService;
import pe.edu.sgfa.institucion.service.InstitucionService;
import pe.edu.sgfa.nota.repository.SituacionAcademicaRepository;
import pe.edu.sgfa.nota.service.EscalaLiteral;
import pe.edu.sgfa.nota.service.ItemNota;
import pe.edu.sgfa.nota.service.RegistroAcademicoService;
import pe.edu.sgfa.periodo.domain.PeriodoAcademico;
import pe.edu.sgfa.periodo.service.PeriodoAcademicoService;
import pe.edu.sgfa.programa.domain.Ciclo;
import pe.edu.sgfa.programa.domain.PlanCurso;
import pe.edu.sgfa.programa.domain.PlanEstudio;
import pe.edu.sgfa.programa.domain.ProgramaEstudio;
import pe.edu.sgfa.programa.repository.PlanCursoRepository;
import pe.edu.sgfa.programa.service.CicloService;
import pe.edu.sgfa.programa.service.CursoService;
import pe.edu.sgfa.programa.service.PlanCursoService;
import pe.edu.sgfa.programa.service.PlanEstudioService;
import pe.edu.sgfa.programa.service.ProgramaEstudioService;
import pe.edu.sgfa.reporte.service.ReporteService;
import pe.edu.sgfa.reporte.service.TablaReporte;
import pe.edu.sgfa.trayectoria.domain.Trayectoria;
import pe.edu.sgfa.trayectoria.service.TrayectoriaService;
import pe.edu.sgfa.usuario.service.AdminBootstrapResult;
import pe.edu.sgfa.usuario.service.AuthService;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ReporteServiceTest {

    @BeforeAll
    static void baseAislada() throws IOException {
        System.setProperty("sgfa.home-dir", Files.createTempDirectory("sgfa-reporte-test-home-").toString());
    }

    @Autowired
    private ReporteService reporteService;
    @Autowired
    private pe.edu.sgfa.reporte.service.ExportadorPdf exportadorPdf;
    @Autowired
    private pe.edu.sgfa.reporte.service.ExportadorExcel exportadorExcel;
    @Autowired
    private InstitucionService institucionService;
    @Autowired
    private ProgramaEstudioService programaEstudioService;
    @Autowired
    private PlanEstudioService planEstudioService;
    @Autowired
    private CicloService cicloService;
    @Autowired
    private CursoService cursoService;
    @Autowired
    private PlanCursoService planCursoService;
    @Autowired
    private PlanCursoRepository planCursoRepository;
    @Autowired
    private PeriodoAcademicoService periodoAcademicoService;
    @Autowired
    private EstudianteService estudianteService;
    @Autowired
    private TrayectoriaService trayectoriaService;
    @Autowired
    private RegistroAcademicoService registroAcademicoService;
    @Autowired
    private SituacionAcademicaRepository situacionRepository;
    @Autowired
    private AuthService authService;
    @Autowired
    private AdminBootstrapResult adminBootstrapResult;

    private static AdminBootstrapResult.Credenciales credenciales;
    private static boolean armado;
    private static PlanEstudio plan;
    private static Ciclo ciclo1;
    private static PeriodoAcademico periodo;
    private static PeriodoAcademico periodo2;

    @BeforeEach
    void preparar() {
        if (credenciales == null) {
            credenciales = adminBootstrapResult.consumir().orElseThrow();
        }
        authService.cerrarSesion();
        authService.autenticar(credenciales.username(), credenciales.password());
        if (armado) {
            return;
        }
        institucionService.guardar("IESPP Juliaca", "JULI", null, null, null);
        ProgramaEstudio programa = programaEstudioService.crear("RPT", "Educacion Reportes", null, 10);
        plan = planEstudioService.crear(programa.getId(), "PR-1", "Plan de reportes", null, null);
        ciclo1 = cicloService.crear(plan.getId(), 1, "Ciclo I", 1);
        cicloService.crear(plan.getId(), 2, "Ciclo II", 2);
        int ordn = 1;
        for (String nmbr : List.of("ARTE", "BIOLOGIA", "CALCULO", "DIDACTICA")) {
            var crs = cursoService.crear("RPT-" + ordn, nmbr, 4, null, null);
            planCursoService.asignar(plan.getId(), ciclo1.getId(), crs.getId(), ordn++, true);
        }
        periodo = periodoAcademicoService.crear("2040-I", 2040, null, null, null);

        periodo2 = periodoAcademicoService.crear("2040-II", 2040, null, null, null);
        Trayectoria tryctrAlv = estudiante("80000001", "ALVA", "PEREZ", "Ana", 15.0, 16.0, 17.0, 19.0);

        registroAcademicoService.guardarMasivo(tryctrAlv.getId(), periodo2.getId(), List.of(new ItemNota(
                planCursoRepository.findByCicloIdOrderByOrdenAsc(ciclo1.getId()).get(0).getId(), 20.0,
                situacionRepository.findByCodigo(EscalaLiteral.DESTACADO).orElseThrow().getId(), null)));
        estudiante("80000002", "BENITES", "RAMOS", "Beto", 15.0, 15.0, 15.0, 8.0);
        estudiante("80000003", "CALLA", "SOTO", "Carla", 8.0, 9.0, 15.0, 15.0);
        estudiante("80000004", "DAVILA", "VEGA", "Dino", 15.0, 15.0, null, null);
        estudiante("80000005", "ESPINOZA", "LUNA", "Elsa", 15.0, 15.0, 15.0, null);
        trayectoriaService.iniciar(estudianteService.crear("80000006", "80000006", "Fabio", "FERRO", "DIAZ", null, null,
                null, null, null).getId(), programa.getId(), plan.getId(), null, "prueba");
        armado = true;
    }

    private Trayectoria estudiante(String dni, String ptrn, String mtrn, String nmbrs, Double... notas) {
        var e = estudianteService.crear(dni, dni, nmbrs, ptrn, mtrn, null, null, null, null, null);
        Trayectoria t = trayectoriaService.iniciar(e.getId(), plan.getPrograma().getId(), plan.getId(), null, "prueba");
        List<PlanCurso> crss = planCursoRepository.findByCicloIdOrderByOrdenAsc(ciclo1.getId());
        List<ItemNota> itms = new java.util.ArrayList<>();
        for (int i = 0; i < notas.length; i++) {
            if (notas[i] != null) {
                Long stcnId = situacionRepository.findByCodigo(EscalaLiteral.codigoPara(notas[i])).orElseThrow().getId();
                itms.add(new ItemNota(crss.get(i).getId(), notas[i], stcnId, null));
            }
        }
        registroAcademicoService.guardarMasivo(t.getId(), periodo.getId(), itms);
        return t;
    }

    private static List<String> fila(TablaReporte t, String cntn) {
        return t.filas().stream().filter(f -> f.stream().anyMatch(c -> c.contains(cntn))).findFirst().orElseThrow();
    }

    @Test
    void actaConsolidada_tieneUnGrupoPorCursoYLaNominaOrdenadaPorApellido() {
        TablaReporte act = reporteService.actaConsolidada(plan.getId(), ciclo1.getId(), periodo.getId());

        assertThat(act.titulo()).isEqualTo("ACTA CONSOLIDADA DE NOTAS");
        assertThat(act.firmas()).isTrue();

        assertThat(act.columnas()).hasSize(5 + 4 * 3 + 3);
        assertThat(act.grupos().stream().mapToInt(g -> g.columnas()).sum()).isEqualTo(act.columnas().size());
        assertThat(act.grupos()).extracting(g -> g.texto()).contains("ARTE (4 cred.)", "DIDACTICA (4 cred.)", "TOTAL");
        assertThat(act.columnas().subList(0, 5)).containsExactly("N°", "N° Matricula", "Apellido paterno",
                "Apellido materno", "Nombres");

        assertThat(act.filas().subList(0, 5)).extracting(f -> f.get(2))
                .containsExactly("ALVA", "BENITES", "CALLA", "DAVILA", "ESPINOZA");
        assertThat(act.filas().get(0).get(0)).isEqualTo("1");
    }

    @Test
    void actaConsolidada_llevaSituacionNotaPuntajeYPromedioDeCadaEstudiante() {
        TablaReporte act = reporteService.actaConsolidada(plan.getId(), ciclo1.getId(), periodo.getId());

        List<String> alv = fila(act, "ALVA");

        assertThat(alv.subList(5, 8)).containsExactly("L", "15", "60");

        assertThat(alv.subList(8, 11)).containsExactly("L", "16", "64");
        assertThat(alv.subList(14, 17)).containsExactly("D", "19", "76");

        assertThat(alv.subList(17, 20)).containsExactly("16", "268", "16.75");

        assertThat(fila(act, "BENITES").subList(14, 17)).containsExactly("I", "8", "32");

        List<String> dvl = fila(act, "DAVILA");
        assertThat(dvl.subList(11, 17)).containsOnly("");
        assertThat(dvl.subList(17, 20)).containsExactly("8", "120", "15.00");
    }

    @Test
    void actaConsolidada_terminaConElPromedioDeCadaCurso() {
        TablaReporte act = reporteService.actaConsolidada(plan.getId(), ciclo1.getId(), periodo.getId());

        List<String> ultm = act.filas().get(act.filas().size() - 1);
        assertThat(ultm.get(2)).isEqualTo("PROMEDIO DEL CURSO");

        assertThat(ultm.get(6)).isEqualTo("13.60");
    }

    @Test
    void riesgo_aplicaLaReglaDeLosSetentaYCincoPorCiento() {
        TablaReporte rsg = reporteService.riesgoYPromocion(plan.getId(), ciclo1.getId(), periodo.getId());

        List<String> alv = fila(rsg, "ALVA");
        assertThat(alv.subList(3, 10)).containsExactly("4/4", "16", "16", "100%", "16.75", "PROMUEVE", "BAJO");

        List<String> bnts = fila(rsg, "BENITES");
        assertThat(bnts.subList(5, 10)).containsExactly("12", "75%", "13.25", "PROMUEVE", "MEDIO");
        assertThat(bnts.get(10)).contains("Desaprobado en: DIDACTICA (8)");

        List<String> cll = fila(rsg, "CALLA");
        assertThat(cll.subList(8, 10)).containsExactly("NO PROMUEVE", "ALTO");
        assertThat(cll.get(10)).contains("no llega al 75%").contains("BIOLOGIA (9)");
    }

    @Test
    void riesgo_sinNotasCompletasNoConcluyeSiPuedeLlegarAlSetentaYCinco() {
        TablaReporte rsg = reporteService.riesgoYPromocion(plan.getId(), ciclo1.getId(), periodo.getId());

        List<String> dvl = fila(rsg, "DAVILA");
        assertThat(dvl.subList(8, 10)).containsExactly("POR DEFINIR", "INCOMPLETO");
        assertThat(dvl.get(10)).contains("Faltan notas: CALCULO, DIDACTICA");

        List<String> espnz = fila(rsg, "ESPINOZA");
        assertThat(espnz.subList(8, 10)).containsExactly("PROMUEVE", "BAJO");
        assertThat(espnz.get(10)).contains("Faltan notas: DIDACTICA");
    }

    @Test
    void riesgo_losDeMayorRiesgoVanPrimero() {
        TablaReporte rsg = reporteService.riesgoYPromocion(plan.getId(), ciclo1.getId(), periodo.getId());

        assertThat(rsg.filas()).extracting(f -> f.get(9))
                .containsExactly("ALTO", "INCOMPLETO", "MEDIO", "BAJO", "BAJO");
        assertThat(rsg.subtitulo()).anyMatch(s -> s.contains("Riesgo alto: 1") && s.contains("Incompletos: 1"));
    }

    @Test
    void resultadosPorCurso_cuentaSituacionesAprobadosYPromedios() {
        TablaReporte r = reporteService.resultadosPorCurso(plan.getId(), ciclo1.getId(), periodo.getId());

        List<String> art = fila(r, "ARTE");
        assertThat(art.subList(3, 15)).containsExactly("5", "0", "4", "0", "1", "0", "4", "1", "80%", "13.60", "8", "15");
        List<String> ddctc = fila(r, "DIDACTICA");

        assertThat(ddctc.subList(3, 12)).containsExactly("3", "1", "1", "0", "1", "0", "2", "1", "67%");
        List<String> ttl = r.filas().get(r.filas().size() - 1);
        assertThat(ttl.get(1)).isEqualTo("TOTAL");
        assertThat(ttl.get(3)).isEqualTo("17");
    }

    @Test
    void cobertura_comparaNotasCargadasConLasQueCorresponden() {
        TablaReporte c = reporteService.coberturaPorCiclo(plan.getId(), periodo.getId());

        List<String> prmr = c.filas().get(0);
        assertThat(prmr).containsExactly("PRIMER CICLO", "4", "5", "20", "17", "3", "85%", "Parcial");
        assertThat(c.filas().get(1)).containsExactly("SEGUNDO CICLO", "0", "0", "0", "0", "0", "-", "Sin notas");
    }

    @Test
    void pendientes_listaLosCursosQueFaltanYALosQueNoTienenNinguna() {
        TablaReporte p = reporteService.pendientes(plan.getId(), periodo.getId(), null);

        assertThat(p.filas()).hasSize(3);
        List<String> dvl = fila(p, "DAVILA");
        assertThat(dvl.get(3)).isEqualTo("PRIMER CICLO");
        assertThat(dvl.get(4)).isEqualTo("2/4");
        assertThat(dvl.get(5)).isEqualTo("CALCULO; DIDACTICA");
        assertThat(fila(p, "ESPINOZA").get(5)).isEqualTo("DIDACTICA");
        List<String> frr = fila(p, "FERRO");
        assertThat(frr.get(5)).isEqualTo("Sin ninguna nota en el periodo");
        assertThat(p.subtitulo()).anyMatch(s -> s.contains("incompletas: 2") && s.contains("sin ninguna nota: 1"));
    }

    @Test
    void pendientes_deUnCicloNoIncluyeALosSinNotas() {
        TablaReporte p = reporteService.pendientes(plan.getId(), periodo.getId(), ciclo1.getId());

        assertThat(p.filas()).hasSize(2);
        assertThat(p.filas()).noneMatch(f -> f.stream().anyMatch(c -> c.contains("FERRO")));
    }

    private TablaReporte merito(ReporteService.AmbitoMerito ambt, Integer lmt) {
        return reporteService.ordenMerito(plan.getId(), ambt, ciclo1.getId(), periodo.getId(), lmt);
    }

    @Test
    void merito_losEmpatadosComparteElPuestoYElSiguienteSalta() {
        TablaReporte m = merito(ReporteService.AmbitoMerito.CICLO_PERIODO, null);

        assertThat(m.filas()).extracting(f -> f.get(0)).containsExactly("1", "2", "2", "4", "5");
        assertThat(m.filas()).extracting(f -> f.get(2).split(",")[0].trim())
                .containsExactly("ALVA PEREZ", "DAVILA VEGA", "ESPINOZA LUNA", "BENITES RAMOS", "CALLA SOTO");
        assertThat(m.filas()).extracting(f -> f.get(4)).containsExactly("16.75", "15.00", "15.00", "13.25", "11.75");
    }

    @Test
    void merito_marcaTercioYQuintoSuperiorIncluyendoLosEmpatadosEnElCorte() {
        TablaReporte m = merito(ReporteService.AmbitoMerito.CICLO_PERIODO, null);

        assertThat(m.filas()).extracting(f -> f.get(5))
                .containsExactly("Quinto superior", "Tercio superior", "Tercio superior", "", "");
        assertThat(m.subtitulo()).anyMatch(s -> s.contains("Tercio superior: promedio de 15.00 o mas (3 estudiantes)")
                && s.contains("Quinto superior: 16.75 o mas (1)"));
    }

    @Test
    void merito_quienTieneCursosDesaprobadosONotasPorCargarIgualEntraConObservacion() {
        TablaReporte m = merito(ReporteService.AmbitoMerito.CICLO_PERIODO, null);

        assertThat(fila(m, "BENITES").get(6)).isEqualTo("1 curso desaprobado: DIDACTICA (8)");
        assertThat(fila(m, "CALLA").get(6)).isEqualTo("2 cursos desaprobados: ARTE (8), BIOLOGIA (9)");
        assertThat(fila(m, "DAVILA").get(6)).isEqualTo("Faltan notas de 2 cursos");
        assertThat(fila(m, "ESPINOZA").get(6)).isEqualTo("Faltan notas de 1 curso");
        assertThat(fila(m, "ALVA").get(6)).isEmpty();
    }

    @Test
    void merito_noIncluyeAQuienNoTieneNingunaNota() {
        TablaReporte m = merito(ReporteService.AmbitoMerito.CICLO_PERIODO, null);

        assertThat(m.filas()).noneMatch(f -> f.get(2).contains("FERRO"));
        assertThat(m.subtitulo()).anyMatch(s -> s.contains("Estudiantes en el orden: 5"));
    }

    @Test
    void merito_elLimiteDePuestosIncluyeALosEmpatadosEnElCorte() {
        TablaReporte m = merito(ReporteService.AmbitoMerito.CICLO_PERIODO, 2);

        assertThat(m.filas()).extracting(f -> f.get(0)).containsExactly("1", "2", "2");
        assertThat(merito(ReporteService.AmbitoMerito.CICLO_PERIODO, 1).filas()).hasSize(1);
        assertThat(m.subtitulo()).anyMatch(s -> s.contains("primeros 2 puestos (3 estudiantes)"));
    }

    @Test
    void merito_porCicloVigenteUsaLaNotaDeRecuperacionDeOtroPeriodo() {
        TablaReporte m = merito(ReporteService.AmbitoMerito.CICLO_VIGENTE, null);

        assertThat(fila(m, "ALVA").get(4)).isEqualTo("18.00");
        assertThat(m.filas().get(0).get(2)).startsWith("ALVA");
        assertThat(m.subtitulo()).anyMatch(s -> s.contains("notas vigentes de cualquier periodo"));
    }

    @Test
    void merito_acumuladoDelPlanTomaLasNotasVigentesDeTodosLosCiclos() {
        TablaReporte m = reporteService.ordenMerito(plan.getId(), ReporteService.AmbitoMerito.PLAN_ACUMULADO, null, null, null);

        assertThat(fila(m, "ALVA").get(4)).isEqualTo("18.00");
        assertThat(fila(m, "DAVILA").get(4)).isEqualTo("15.00");

        assertThat(fila(m, "DAVILA").get(6)).isEmpty();
        assertThat(m.subtitulo()).anyMatch(s -> s.startsWith("General"));
    }

    @Test
    void merito_enElSegundoPeriodoSoloEstaQuienTieneNotasAhi() {
        TablaReporte m = reporteService.ordenMerito(plan.getId(), ReporteService.AmbitoMerito.CICLO_PERIODO, ciclo1.getId(),
                periodo2.getId(), null);

        assertThat(m.filas()).hasSize(1);
        assertThat(m.filas().get(0).subList(2, 7).get(0)).startsWith("ALVA");
        assertThat(m.filas().get(0).get(4)).isEqualTo("20.00");
        assertThat(m.filas().get(0).get(6)).isEqualTo("Faltan notas de 3 cursos");
    }

    @Test
    void cadaReporteSeExportaAPdfYAExcel() throws Exception {
        java.nio.file.Path carpeta = Files.createTempDirectory("sgfa-export-");
        List<TablaReporte> rprts = List.of(
                reporteService.actaConsolidada(plan.getId(), ciclo1.getId(), periodo.getId()),
                reporteService.resultadosPorCurso(plan.getId(), ciclo1.getId(), periodo.getId()),
                reporteService.riesgoYPromocion(plan.getId(), ciclo1.getId(), periodo.getId()),
                reporteService.coberturaPorCiclo(plan.getId(), periodo.getId()),
                reporteService.pendientes(plan.getId(), periodo.getId(), null));
        String institucion = reporteService.nombreInstitucion();
        assertThat(institucion).isEqualTo("IESPP Juliaca");

        int n = 0;
        for (TablaReporte r : rprts) {
            java.nio.file.Path pdf = carpeta.resolve("r" + n + ".pdf");
            java.nio.file.Path xlsx = carpeta.resolve("r" + n++ + ".xlsx");
            exportadorPdf.escribir(r, institucion, pdf);
            exportadorExcel.escribir(r, institucion, xlsx);
            try (var doc = org.apache.pdfbox.pdmodel.PDDocument.load(pdf.toFile())) {
                assertThat(new org.apache.pdfbox.text.PDFTextStripper().getText(doc)).contains(r.titulo(), "IESPP Juliaca");
            }
            try (var lbr = new org.apache.poi.xssf.usermodel.XSSFWorkbook(Files.newInputStream(xlsx))) {
                assertThat(lbr.getSheetAt(0).getLastRowNum()).isGreaterThan(r.filas().size());
            }
        }
    }

    @Test
    void cuadroDeHonor_unaFranjaPorEspecialidadConSusTresPrimerosLugaresYEmpatesCompartidos() {

        programaEstudioService.crear("VAC", "Especialidad Vacia", null, 10);

        TablaReporte hnr = reporteService.cuadroDeHonor(periodo.getId(), 3);

        assertThat(hnr.titulo()).isEqualTo("CUADRO DE HONOR POR ESPECIALIDAD");
        assertThat(hnr.firmas()).isTrue();

        assertThat(hnr.secciones()).hasSize(2);
        List<String> frnjs = hnr.secciones().stream().sorted().map(i -> TablaReporte.textoDeSeccion(hnr.filas().get(i))).toList();
        assertThat(frnjs.get(0)).startsWith("EDUCACION REPORTES").contains("5 estudiantes con notas");
        assertThat(frnjs.get(1)).startsWith("ESPECIALIDAD VACIA").contains("0 estudiantes");

        List<List<String>> flsHnr = hnr.filas().stream().filter(f -> f.get(0).endsWith("lugar")).toList();
        assertThat(flsHnr).extracting(f -> f.get(0)).containsExactly("Primer lugar", "Segundo lugar", "Segundo lugar");
        assertThat(flsHnr).extracting(f -> f.get(2)).satisfiesExactly(
                n -> assertThat(n).startsWith("ALVA"), n -> assertThat(n).startsWith("DAVILA"), n -> assertThat(n).startsWith("ESPINOZA"));
        assertThat(flsHnr.get(0).get(5)).isEqualTo("16.75");
        assertThat(flsHnr.get(0).get(3)).isEqualTo("PRIMER CICLO");

        assertThat(flsHnr.get(1).get(6)).contains("Faltan notas de 2 cursos");

        assertThat(hnr.filas().stream().anyMatch(f -> f.get(2).equals("Sin notas cargadas en este periodo"))).isTrue();
        assertThat(hnr.filas().stream().map(f -> f.get(2)).anyMatch(n -> n.startsWith("BENITES"))).isFalse();
    }

    @Test
    void cuadroDeHonor_conMasLugaresIncluyeAlSiguienteYValidaElNumero() {
        TablaReporte hnr = reporteService.cuadroDeHonor(periodo.getId(), 4);
        assertThat(hnr.filas().stream().map(f -> f.get(2)).anyMatch(n -> n.startsWith("BENITES"))).isTrue();
        assertThat(hnr.filas().stream().map(f -> f.get(0))).contains("4° lugar");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> reporteService.cuadroDeHonor(periodo.getId(), 0))
                .hasMessageContaining("al menos 1");
    }

    @Test
    void cuadroDeHonor_sePuedeExportarAUnSoloPdfYExcelConLasFranjas() throws IOException {
        TablaReporte hnr = reporteService.cuadroDeHonor(periodo.getId(), 3);
        java.nio.file.Path carpeta = Files.createTempDirectory("sgfa-honor-");
        java.nio.file.Path pdf = carpeta.resolve("honor.pdf");
        java.nio.file.Path xlsx = carpeta.resolve("honor.xlsx");
        exportadorPdf.escribir(hnr, "IESPP Juliaca", pdf);
        exportadorExcel.escribir(hnr, "IESPP Juliaca", xlsx);
        try (var doc = org.apache.pdfbox.pdmodel.PDDocument.load(pdf.toFile())) {
            assertThat(doc.getNumberOfPages()).isEqualTo(1);
            assertThat(new org.apache.pdfbox.text.PDFTextStripper().getText(doc))
                    .contains("CUADRO DE HONOR POR ESPECIALIDAD", "EDUCACION REPORTES", "Primer lugar", "Segundo lugar", "ALVA");
        }
        try (var lbr = new org.apache.poi.xssf.usermodel.XSSFWorkbook(Files.newInputStream(xlsx))) {
            assertThat(lbr.getSheetAt(0).getNumMergedRegions()).isGreaterThanOrEqualTo(hnr.secciones().size());
        }
    }
}
