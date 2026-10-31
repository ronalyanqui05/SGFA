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

import pe.edu.sgfa.estudiante.service.YmclEstudianteService;
import pe.edu.sgfa.institucion.service.RacaInstitucionService;
import pe.edu.sgfa.nota.repository.YmclSituacionAcademicaRepository;
import pe.edu.sgfa.nota.service.YmclEscalaLiteral;
import pe.edu.sgfa.nota.service.YmclItemNota;
import pe.edu.sgfa.nota.service.YmclRegistroAcademicoService;
import pe.edu.sgfa.periodo.domain.YmclPeriodoAcademico;
import pe.edu.sgfa.periodo.service.YmclPeriodoAcademicoService;
import pe.edu.sgfa.programa.domain.RacaCiclo;
import pe.edu.sgfa.programa.domain.RacaPlanCurso;
import pe.edu.sgfa.programa.domain.RacaPlanEstudio;
import pe.edu.sgfa.programa.domain.RacaProgramaEstudio;
import pe.edu.sgfa.programa.repository.RacaPlanCursoRepository;
import pe.edu.sgfa.programa.service.RacaCicloService;
import pe.edu.sgfa.programa.service.RacaCursoService;
import pe.edu.sgfa.programa.service.RacaPlanCursoService;
import pe.edu.sgfa.programa.service.RacaPlanEstudioService;
import pe.edu.sgfa.programa.service.RacaProgramaEstudioService;
import pe.edu.sgfa.reporte.service.CphReporteService;
import pe.edu.sgfa.reporte.service.CphTablaReporte;
import pe.edu.sgfa.trayectoria.domain.YmclTrayectoria;
import pe.edu.sgfa.trayectoria.service.YmclTrayectoriaService;
import pe.edu.sgfa.usuario.service.RyeAdminBootstrapResult;
import pe.edu.sgfa.usuario.service.RyeAuthService;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CphReporteServiceTest {

    @BeforeAll
    static void baseAislada() throws IOException {
        System.setProperty("sgfa.home-dir", Files.createTempDirectory("sgfa-reporte-test-home-").toString());
    }

    @Autowired
    private CphReporteService reporteServicePch;
    @Autowired
    private pe.edu.sgfa.reporte.service.CphExportadorPdf exportadorPdfPch;
    @Autowired
    private pe.edu.sgfa.reporte.service.CphExportadorExcel exportadorExcelPch;
    @Autowired
    private RacaInstitucionService institucionServicePch;
    @Autowired
    private RacaProgramaEstudioService programaEstudioServicePch;
    @Autowired
    private RacaPlanEstudioService planEstudioServicePch;
    @Autowired
    private RacaCicloService cicloServicePch;
    @Autowired
    private RacaCursoService cursoServicePch;
    @Autowired
    private RacaPlanCursoService planCursoServicePch;
    @Autowired
    private RacaPlanCursoRepository planCursoRepositoryPch;
    @Autowired
    private YmclPeriodoAcademicoService periodoAcademicoServicePch;
    @Autowired
    private YmclEstudianteService estudianteServicePch;
    @Autowired
    private YmclTrayectoriaService trayectoriaServicePch;
    @Autowired
    private YmclRegistroAcademicoService registroAcademicoServicePch;
    @Autowired
    private YmclSituacionAcademicaRepository situacionRepositoryPch;
    @Autowired
    private RyeAuthService authServicePch;
    @Autowired
    private RyeAdminBootstrapResult adminBootstrapResultPch;

    private static RyeAdminBootstrapResult.RyeCredenciales credencialesPch;
    private static boolean armadoPch;
    private static RacaPlanEstudio planPch;
    private static RacaCiclo ciclo1Pch;
    private static YmclPeriodoAcademico periodoPch;
    private static YmclPeriodoAcademico periodo2Pch;

    @BeforeEach
    void preparar() {
        if (credencialesPch == null) {
            credencialesPch = adminBootstrapResultPch.ryeConsumir().orElseThrow();
        }
        authServicePch.ryeCerrarSesion();
        authServicePch.ryeAutenticar(credencialesPch.username(), credencialesPch.password());
        if (armadoPch) {
            return;
        }
        institucionServicePch.racaGuardar("IESPP Juliaca", "JULI", null, null, null);
        RacaProgramaEstudio programaPch = programaEstudioServicePch.racaCrear("RPT", "Educacion Reportes", null, 10);
        planPch = planEstudioServicePch.racaCrear(programaPch.getId(), "PR-1", "Plan de reportes", null, null);
        ciclo1Pch = cicloServicePch.racaCrear(planPch.getId(), 1, "Ciclo I", 1);
        cicloServicePch.racaCrear(planPch.getId(), 2, "Ciclo II", 2);
        int ordenPch = 1;
        for (String nombrePch : List.of("ARTE", "BIOLOGIA", "CALCULO", "DIDACTICA")) {
            var cursoPch = cursoServicePch.racaCrear("RPT-" + ordenPch, nombrePch, 4, null, null);
            planCursoServicePch.racaAsignar(planPch.getId(), ciclo1Pch.getId(), cursoPch.getId(), ordenPch++, true);
        }
        periodoPch = periodoAcademicoServicePch.ymclCrear("2040-I", 2040, null, null, null);

        periodo2Pch = periodoAcademicoServicePch.ymclCrear("2040-II", 2040, null, null, null);
        YmclTrayectoria trayectoriaAlvaPch = estudiante("80000001", "ALVA", "PEREZ", "Ana", 15.0, 16.0, 17.0, 19.0);

        registroAcademicoServicePch.ymclGuardarMasivo(trayectoriaAlvaPch.getId(), periodo2Pch.getId(), List.of(new YmclItemNota(
                planCursoRepositoryPch.findByCicloIdOrderByOrdenAsc(ciclo1Pch.getId()).get(0).getId(), 20.0,
                situacionRepositoryPch.findByCodigo(YmclEscalaLiteral.DESTACADO).orElseThrow().getId(), null)));
        estudiante("80000002", "BENITES", "RAMOS", "Beto", 15.0, 15.0, 15.0, 8.0);
        estudiante("80000003", "CALLA", "SOTO", "Carla", 8.0, 9.0, 15.0, 15.0);
        estudiante("80000004", "DAVILA", "VEGA", "Dino", 15.0, 15.0, null, null);
        estudiante("80000005", "ESPINOZA", "LUNA", "Elsa", 15.0, 15.0, 15.0, null);
        trayectoriaServicePch.ymclIniciar(estudianteServicePch.ymclCrear("80000006", "80000006", "Fabio", "FERRO", "DIAZ", null, null,
                null, null, null).getId(), programaPch.getId(), planPch.getId(), null, "prueba");
        armadoPch = true;
    }

    private YmclTrayectoria estudiante(String dniPch, String paternoPch, String maternoPch, String nombresPch, Double... notasPch) {
        var e = estudianteServicePch.ymclCrear(dniPch, dniPch, nombresPch, paternoPch, maternoPch, null, null, null, null, null);
        YmclTrayectoria t = trayectoriaServicePch.ymclIniciar(e.getId(), planPch.getPrograma().getId(), planPch.getId(), null, "prueba");
        List<RacaPlanCurso> cursosPch = planCursoRepositoryPch.findByCicloIdOrderByOrdenAsc(ciclo1Pch.getId());
        List<YmclItemNota> itemsPch = new java.util.ArrayList<>();
        for (int i = 0; i < notasPch.length; i++) {
            if (notasPch[i] != null) {
                Long situacionIdPch = situacionRepositoryPch.findByCodigo(YmclEscalaLiteral.ymclCodigoPara(notasPch[i])).orElseThrow().getId();
                itemsPch.add(new YmclItemNota(cursosPch.get(i).getId(), notasPch[i], situacionIdPch, null));
            }
        }
        registroAcademicoServicePch.ymclGuardarMasivo(t.getId(), periodoPch.getId(), itemsPch);
        return t;
    }

    private static List<String> fila(CphTablaReporte t, String contienePch) {
        return t.filas().stream().filter(f -> f.stream().anyMatch(c -> c.contains(contienePch))).findFirst().orElseThrow();
    }

    @Test
    void actaConsolidadaTieneUnGrupoPorCursoYLaNominaOrdenadaPorApellido() {
        CphTablaReporte actaPch = reporteServicePch.cphActaConsolidada(planPch.getId(), ciclo1Pch.getId(), periodoPch.getId());

        assertThat(actaPch.titulo()).isEqualTo("ACTA CONSOLIDADA DE NOTAS");
        assertThat(actaPch.firmas()).isTrue();

        assertThat(actaPch.columnas()).hasSize(5 + 4 * 3 + 3);
        assertThat(actaPch.grupos().stream().mapToInt(g -> g.columnas()).sum()).isEqualTo(actaPch.columnas().size());
        assertThat(actaPch.grupos()).extracting(g -> g.texto()).contains("ARTE (4 cred.)", "DIDACTICA (4 cred.)", "TOTAL");
        assertThat(actaPch.columnas().subList(0, 5)).containsExactly("N°", "N° Matricula", "Apellido paterno",
                "Apellido materno", "Nombres");

        assertThat(actaPch.filas().subList(0, 5)).extracting(f -> f.get(2))
                .containsExactly("ALVA", "BENITES", "CALLA", "DAVILA", "ESPINOZA");
        assertThat(actaPch.filas().get(0).get(0)).isEqualTo("1");
    }

    @Test
    void actaConsolidadaLlevaSituacionNotaPuntajeYPromedioDeCadaEstudiante() {
        CphTablaReporte actaPch = reporteServicePch.cphActaConsolidada(planPch.getId(), ciclo1Pch.getId(), periodoPch.getId());

        List<String> alvaPch = fila(actaPch, "ALVA");

        assertThat(alvaPch.subList(5, 8)).containsExactly("L", "15", "60");

        assertThat(alvaPch.subList(8, 11)).containsExactly("L", "16", "64");
        assertThat(alvaPch.subList(14, 17)).containsExactly("D", "19", "76");

        assertThat(alvaPch.subList(17, 20)).containsExactly("16", "268", "16.75");

        assertThat(fila(actaPch, "BENITES").subList(14, 17)).containsExactly("I", "8", "32");

        List<String> davilaPch = fila(actaPch, "DAVILA");
        assertThat(davilaPch.subList(11, 17)).containsOnly("");
        assertThat(davilaPch.subList(17, 20)).containsExactly("8", "120", "15.00");
    }

    @Test
    void actaConsolidadaTerminaConElPromedioDeCadaCurso() {
        CphTablaReporte actaPch = reporteServicePch.cphActaConsolidada(planPch.getId(), ciclo1Pch.getId(), periodoPch.getId());

        List<String> ultimaPch = actaPch.filas().get(actaPch.filas().size() - 1);
        assertThat(ultimaPch.get(2)).isEqualTo("PROMEDIO DEL CURSO");

        assertThat(ultimaPch.get(6)).isEqualTo("13.60");
    }

    @Test
    void riesgoAplicaLaReglaDeLosSetentaYCincoPorCiento() {
        CphTablaReporte riesgoPch = reporteServicePch.cphRiesgoYPromocion(planPch.getId(), ciclo1Pch.getId(), periodoPch.getId());

        List<String> alvaPch = fila(riesgoPch, "ALVA");
        assertThat(alvaPch.subList(3, 10)).containsExactly("4/4", "16", "16", "100%", "16.75", "PROMUEVE", "BAJO");

        List<String> benitesPch = fila(riesgoPch, "BENITES");
        assertThat(benitesPch.subList(5, 10)).containsExactly("12", "75%", "13.25", "PROMUEVE", "MEDIO");
        assertThat(benitesPch.get(10)).contains("Desaprobado en: DIDACTICA (8)");

        List<String> callaPch = fila(riesgoPch, "CALLA");
        assertThat(callaPch.subList(8, 10)).containsExactly("NO PROMUEVE", "ALTO");
        assertThat(callaPch.get(10)).contains("no llega al 75%").contains("BIOLOGIA (9)");
    }

    @Test
    void riesgoSinNotasCompletasNoConcluyeSiPuedeLlegarAlSetentaYCinco() {
        CphTablaReporte riesgoPch = reporteServicePch.cphRiesgoYPromocion(planPch.getId(), ciclo1Pch.getId(), periodoPch.getId());

        List<String> davilaPch = fila(riesgoPch, "DAVILA");
        assertThat(davilaPch.subList(8, 10)).containsExactly("POR DEFINIR", "INCOMPLETO");
        assertThat(davilaPch.get(10)).contains("Faltan notas: CALCULO, DIDACTICA");

        List<String> espinozaPch = fila(riesgoPch, "ESPINOZA");
        assertThat(espinozaPch.subList(8, 10)).containsExactly("PROMUEVE", "BAJO");
        assertThat(espinozaPch.get(10)).contains("Faltan notas: DIDACTICA");
    }

    @Test
    void riesgoLosDeMayorRiesgoVanPrimero() {
        CphTablaReporte riesgoPch = reporteServicePch.cphRiesgoYPromocion(planPch.getId(), ciclo1Pch.getId(), periodoPch.getId());

        assertThat(riesgoPch.filas()).extracting(f -> f.get(9))
                .containsExactly("ALTO", "INCOMPLETO", "MEDIO", "BAJO", "BAJO");
        assertThat(riesgoPch.subtitulo()).anyMatch(s -> s.contains("Riesgo alto: 1") && s.contains("Incompletos: 1"));
    }

    @Test
    void resultadosPorCursoCuentaSituacionesAprobadosYPromedios() {
        CphTablaReporte r = reporteServicePch.cphResultadosPorCurso(planPch.getId(), ciclo1Pch.getId(), periodoPch.getId());

        List<String> artePch = fila(r, "ARTE");
        assertThat(artePch.subList(3, 15)).containsExactly("5", "0", "4", "0", "1", "0", "4", "1", "80%", "13.60", "8", "15");
        List<String> didacticaPch = fila(r, "DIDACTICA");

        assertThat(didacticaPch.subList(3, 12)).containsExactly("3", "1", "1", "0", "1", "0", "2", "1", "67%");
        List<String> totalPch = r.filas().get(r.filas().size() - 1);
        assertThat(totalPch.get(1)).isEqualTo("TOTAL");
        assertThat(totalPch.get(3)).isEqualTo("17");
    }

    @Test
    void coberturaComparaNotasCargadasConLasQueCorresponden() {
        CphTablaReporte c = reporteServicePch.cphCoberturaPorCiclo(planPch.getId(), periodoPch.getId());

        List<String> primeroPch = c.filas().get(0);
        assertThat(primeroPch).containsExactly("PRIMER CICLO", "4", "5", "20", "17", "3", "85%", "Parcial");
        assertThat(c.filas().get(1)).containsExactly("SEGUNDO CICLO", "0", "0", "0", "0", "0", "-", "Sin notas");
    }

    @Test
    void pendientesListaLosCursosQueFaltanYALosQueNoTienenNinguna() {
        CphTablaReporte p = reporteServicePch.cphPendientes(planPch.getId(), periodoPch.getId(), null);

        assertThat(p.filas()).hasSize(3);
        List<String> davilaPch = fila(p, "DAVILA");
        assertThat(davilaPch.get(3)).isEqualTo("PRIMER CICLO");
        assertThat(davilaPch.get(4)).isEqualTo("2/4");
        assertThat(davilaPch.get(5)).isEqualTo("CALCULO; DIDACTICA");
        assertThat(fila(p, "ESPINOZA").get(5)).isEqualTo("DIDACTICA");
        List<String> ferroPch = fila(p, "FERRO");
        assertThat(ferroPch.get(5)).isEqualTo("Sin ninguna nota en el periodo");
        assertThat(p.subtitulo()).anyMatch(s -> s.contains("incompletas: 2") && s.contains("sin ninguna nota: 1"));
    }

    @Test
    void pendientesDeUnCicloNoIncluyeALosSinNotas() {
        CphTablaReporte p = reporteServicePch.cphPendientes(planPch.getId(), periodoPch.getId(), ciclo1Pch.getId());

        assertThat(p.filas()).hasSize(2);
        assertThat(p.filas()).noneMatch(f -> f.stream().anyMatch(c -> c.contains("FERRO")));
    }

    private CphTablaReporte merito(CphReporteService.CphAmbitoMerito ambitoPch, Integer limitePch) {
        return reporteServicePch.cphOrdenMerito(planPch.getId(), ambitoPch, ciclo1Pch.getId(), periodoPch.getId(), limitePch);
    }

    @Test
    void meritoLosEmpatadosComparteElPuestoYElSiguienteSalta() {
        CphTablaReporte m = merito(CphReporteService.CphAmbitoMerito.CICLO_PERIODO, null);

        assertThat(m.filas()).extracting(f -> f.get(0)).containsExactly("1", "2", "2", "4", "5");
        assertThat(m.filas()).extracting(f -> f.get(2).split(",")[0].trim())
                .containsExactly("ALVA PEREZ", "DAVILA VEGA", "ESPINOZA LUNA", "BENITES RAMOS", "CALLA SOTO");
        assertThat(m.filas()).extracting(f -> f.get(4)).containsExactly("16.75", "15.00", "15.00", "13.25", "11.75");
    }

    @Test
    void meritoMarcaTercioYQuintoSuperiorIncluyendoLosEmpatadosEnElCorte() {
        CphTablaReporte m = merito(CphReporteService.CphAmbitoMerito.CICLO_PERIODO, null);

        assertThat(m.filas()).extracting(f -> f.get(5))
                .containsExactly("Quinto superior", "Tercio superior", "Tercio superior", "", "");
        assertThat(m.subtitulo()).anyMatch(s -> s.contains("Tercio superior: promedio de 15.00 o mas (3 estudiantes)")
                && s.contains("Quinto superior: 16.75 o mas (1)"));
    }

    @Test
    void meritoQuienTieneCursosDesaprobadosONotasPorCargarIgualEntraConObservacion() {
        CphTablaReporte m = merito(CphReporteService.CphAmbitoMerito.CICLO_PERIODO, null);

        assertThat(fila(m, "BENITES").get(6)).isEqualTo("1 curso desaprobado: DIDACTICA (8)");
        assertThat(fila(m, "CALLA").get(6)).isEqualTo("2 cursos desaprobados: ARTE (8), BIOLOGIA (9)");
        assertThat(fila(m, "DAVILA").get(6)).isEqualTo("Faltan notas de 2 cursos");
        assertThat(fila(m, "ESPINOZA").get(6)).isEqualTo("Faltan notas de 1 curso");
        assertThat(fila(m, "ALVA").get(6)).isEmpty();
    }

    @Test
    void meritoNoIncluyeAQuienNoTieneNingunaNota() {
        CphTablaReporte m = merito(CphReporteService.CphAmbitoMerito.CICLO_PERIODO, null);

        assertThat(m.filas()).noneMatch(f -> f.get(2).contains("FERRO"));
        assertThat(m.subtitulo()).anyMatch(s -> s.contains("Estudiantes en el orden: 5"));
    }

    @Test
    void meritoElLimiteDePuestosIncluyeALosEmpatadosEnElCorte() {
        CphTablaReporte m = merito(CphReporteService.CphAmbitoMerito.CICLO_PERIODO, 2);

        assertThat(m.filas()).extracting(f -> f.get(0)).containsExactly("1", "2", "2");
        assertThat(merito(CphReporteService.CphAmbitoMerito.CICLO_PERIODO, 1).filas()).hasSize(1);
        assertThat(m.subtitulo()).anyMatch(s -> s.contains("primeros 2 puestos (3 estudiantes)"));
    }

    @Test
    void meritoPorCicloVigenteUsaLaNotaDeRecuperacionDeOtroPeriodo() {
        CphTablaReporte m = merito(CphReporteService.CphAmbitoMerito.CICLO_VIGENTE, null);

        assertThat(fila(m, "ALVA").get(4)).isEqualTo("18.00");
        assertThat(m.filas().get(0).get(2)).startsWith("ALVA");
        assertThat(m.subtitulo()).anyMatch(s -> s.contains("notas vigentes de cualquier periodo"));
    }

    @Test
    void meritoAcumuladoDelPlanTomaLasNotasVigentesDeTodosLosCiclos() {
        CphTablaReporte m = reporteServicePch.cphOrdenMerito(planPch.getId(), CphReporteService.CphAmbitoMerito.PLAN_ACUMULADO, null, null, null);

        assertThat(fila(m, "ALVA").get(4)).isEqualTo("18.00");
        assertThat(fila(m, "DAVILA").get(4)).isEqualTo("15.00");

        assertThat(fila(m, "DAVILA").get(6)).isEmpty();
        assertThat(m.subtitulo()).anyMatch(s -> s.startsWith("General"));
    }

    @Test
    void meritoEnElSegundoPeriodoSoloEstaQuienTieneNotasAhi() {
        CphTablaReporte m = reporteServicePch.cphOrdenMerito(planPch.getId(), CphReporteService.CphAmbitoMerito.CICLO_PERIODO, ciclo1Pch.getId(),
                periodo2Pch.getId(), null);

        assertThat(m.filas()).hasSize(1);
        assertThat(m.filas().get(0).subList(2, 7).get(0)).startsWith("ALVA");
        assertThat(m.filas().get(0).get(4)).isEqualTo("20.00");
        assertThat(m.filas().get(0).get(6)).isEqualTo("Faltan notas de 3 cursos");
    }

    @Test
    void cadaReporteSeExportaAPdfYAExcel() throws Exception {
        java.nio.file.Path carpetaPch = Files.createTempDirectory("sgfa-export-");
        List<CphTablaReporte> reportesPch = List.of(
                reporteServicePch.cphActaConsolidada(planPch.getId(), ciclo1Pch.getId(), periodoPch.getId()),
                reporteServicePch.cphResultadosPorCurso(planPch.getId(), ciclo1Pch.getId(), periodoPch.getId()),
                reporteServicePch.cphRiesgoYPromocion(planPch.getId(), ciclo1Pch.getId(), periodoPch.getId()),
                reporteServicePch.cphCoberturaPorCiclo(planPch.getId(), periodoPch.getId()),
                reporteServicePch.cphPendientes(planPch.getId(), periodoPch.getId(), null));
        String institucionPch = reporteServicePch.cphNombreInstitucion();
        assertThat(institucionPch).isEqualTo("IESPP Juliaca");

        int n = 0;
        for (CphTablaReporte r : reportesPch) {
            java.nio.file.Path pdfPch = carpetaPch.resolve("r" + n + ".pdf");
            java.nio.file.Path xlsxPch = carpetaPch.resolve("r" + n++ + ".xlsx");
            exportadorPdfPch.cphEscribir(r, institucionPch, pdfPch);
            exportadorExcelPch.cphEscribir(r, institucionPch, xlsxPch);
            try (var docPch = org.apache.pdfbox.pdmodel.PDDocument.load(pdfPch.toFile())) {
                assertThat(new org.apache.pdfbox.text.PDFTextStripper().getText(docPch)).contains(r.titulo(), "IESPP Juliaca");
            }
            try (var libroPch = new org.apache.poi.xssf.usermodel.XSSFWorkbook(Files.newInputStream(xlsxPch))) {
                assertThat(libroPch.getSheetAt(0).getLastRowNum()).isGreaterThan(r.filas().size());
            }
        }
    }

    @Test
    void cuadroDeHonorUnaFranjaPorEspecialidadConSusTresPrimerosLugaresYEmpatesCompartidos() {

        programaEstudioServicePch.racaCrear("VAC", "Especialidad Vacia", null, 10);

        CphTablaReporte honorPch = reporteServicePch.cphCuadroDeHonor(periodoPch.getId(), 3);

        assertThat(honorPch.titulo()).isEqualTo("CUADRO DE HONOR POR ESPECIALIDAD");
        assertThat(honorPch.firmas()).isTrue();

        assertThat(honorPch.secciones()).hasSize(2);
        List<String> franjasPch = honorPch.secciones().stream().sorted().map(i -> CphTablaReporte.cphTextoDeSeccion(honorPch.filas().get(i))).toList();
        assertThat(franjasPch.get(0)).startsWith("EDUCACION REPORTES").contains("5 estudiantes con notas");
        assertThat(franjasPch.get(1)).startsWith("ESPECIALIDAD VACIA").contains("0 estudiantes");

        List<List<String>> filasHonorPch = honorPch.filas().stream().filter(f -> f.get(0).endsWith("lugar")).toList();
        assertThat(filasHonorPch).extracting(f -> f.get(0)).containsExactly("Primer lugar", "Segundo lugar", "Segundo lugar");
        assertThat(filasHonorPch).extracting(f -> f.get(2)).satisfiesExactly(
                n -> assertThat(n).startsWith("ALVA"), n -> assertThat(n).startsWith("DAVILA"), n -> assertThat(n).startsWith("ESPINOZA"));
        assertThat(filasHonorPch.get(0).get(5)).isEqualTo("16.75");
        assertThat(filasHonorPch.get(0).get(3)).isEqualTo("PRIMER CICLO");

        assertThat(filasHonorPch.get(1).get(6)).contains("Faltan notas de 2 cursos");

        assertThat(honorPch.filas().stream().anyMatch(f -> f.get(2).equals("Sin notas cargadas en este periodo"))).isTrue();
        assertThat(honorPch.filas().stream().map(f -> f.get(2)).anyMatch(n -> n.startsWith("BENITES"))).isFalse();
    }

    @Test
    void cuadroDeHonorConMasLugaresIncluyeAlSiguienteYValidaElNumero() {
        CphTablaReporte honorPch = reporteServicePch.cphCuadroDeHonor(periodoPch.getId(), 4);
        assertThat(honorPch.filas().stream().map(f -> f.get(2)).anyMatch(n -> n.startsWith("BENITES"))).isTrue();
        assertThat(honorPch.filas().stream().map(f -> f.get(0))).contains("4° lugar");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> reporteServicePch.cphCuadroDeHonor(periodoPch.getId(), 0))
                .hasMessageContaining("al menos 1");
    }

    @Test
    void cuadroDeHonorSePuedeExportarAUnSoloPdfYExcelConLasFranjas() throws IOException {
        CphTablaReporte honorPch = reporteServicePch.cphCuadroDeHonor(periodoPch.getId(), 3);
        java.nio.file.Path carpetaPch = Files.createTempDirectory("sgfa-honor-");
        java.nio.file.Path pdfPch = carpetaPch.resolve("honor.pdf");
        java.nio.file.Path xlsxPch = carpetaPch.resolve("honor.xlsx");
        exportadorPdfPch.cphEscribir(honorPch, "IESPP Juliaca", pdfPch);
        exportadorExcelPch.cphEscribir(honorPch, "IESPP Juliaca", xlsxPch);
        try (var docPch = org.apache.pdfbox.pdmodel.PDDocument.load(pdfPch.toFile())) {
            assertThat(docPch.getNumberOfPages()).isEqualTo(1);
            assertThat(new org.apache.pdfbox.text.PDFTextStripper().getText(docPch))
                    .contains("CUADRO DE HONOR POR ESPECIALIDAD", "EDUCACION REPORTES", "Primer lugar", "Segundo lugar", "ALVA");
        }
        try (var libroPch = new org.apache.poi.xssf.usermodel.XSSFWorkbook(Files.newInputStream(xlsxPch))) {
            assertThat(libroPch.getSheetAt(0).getNumMergedRegions()).isGreaterThanOrEqualTo(honorPch.secciones().size());
        }
    }
}
