package pe.edu.sgfa.importacion;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import pe.edu.sgfa.estudiante.domain.Estudiante;
import pe.edu.sgfa.estudiante.repository.EstudianteRepository;
import pe.edu.sgfa.estudiante.service.EstudianteService;
import pe.edu.sgfa.importacion.service.ImportacionNotasService;
import pe.edu.sgfa.importacion.service.ResultadoImportacion;
import pe.edu.sgfa.institucion.service.InstitucionService;
import pe.edu.sgfa.nota.domain.RegistroAcademico;
import pe.edu.sgfa.nota.repository.RegistroAcademicoRepository;
import pe.edu.sgfa.periodo.domain.PeriodoAcademico;
import pe.edu.sgfa.periodo.service.PeriodoAcademicoService;
import pe.edu.sgfa.programa.domain.Ciclo;
import pe.edu.sgfa.programa.domain.Curso;
import pe.edu.sgfa.programa.domain.PlanCurso;
import pe.edu.sgfa.programa.domain.PlanEstudio;
import pe.edu.sgfa.programa.domain.ProgramaEstudio;
import pe.edu.sgfa.programa.service.CicloService;
import pe.edu.sgfa.programa.service.CursoService;
import pe.edu.sgfa.programa.service.PlanCursoService;
import pe.edu.sgfa.programa.service.PlanEstudioService;
import pe.edu.sgfa.programa.service.ProgramaEstudioService;
import pe.edu.sgfa.trayectoria.domain.Trayectoria;
import pe.edu.sgfa.trayectoria.service.TrayectoriaService;
import pe.edu.sgfa.usuario.service.AdminBootstrapResult;
import pe.edu.sgfa.usuario.service.AuthService;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ImportacionNotasServiceTest {

    @BeforeAll
    static void useIsolatedHomeDir() throws IOException {
        Path tmpHm = Files.createTempDirectory("sgfa-importacion-notas-test-home-");
        System.setProperty("sgfa.home-dir", tmpHm.toString());
    }

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
    private PeriodoAcademicoService periodoAcademicoService;
    @Autowired
    private EstudianteService estudianteService;
    @Autowired
    private EstudianteRepository estudianteRepository;
    @Autowired
    private TrayectoriaService trayectoriaService;
    @Autowired
    private RegistroAcademicoRepository registroAcademicoRepository;
    @Autowired
    private ImportacionNotasService importacionNotasService;
    @Autowired
    private AuthService authService;
    @Autowired
    private AdminBootstrapResult adminBootstrapResult;

    private static AdminBootstrapResult.Credenciales credencialesAdmin;

    private ProgramaEstudio programa;
    private PlanEstudio plan;
    private Ciclo ciclo1;
    private Curso curso1;
    private PlanCurso planCurso1;
    private PeriodoAcademico periodo;
    private Estudiante estudianteExistente;
    private Trayectoria trayectoriaExistente;

    @BeforeEach
    void prepararDatosBase() {
        if (credencialesAdmin == null) {
            credencialesAdmin = adminBootstrapResult.consumir()
                    .orElseThrow(() -> new IllegalStateException("El admin inicial no se genero"));
        }
        authService.cerrarSesion();
        authService.autenticar(credencialesAdmin.username(), credencialesAdmin.password());

        if (institucionService.obtenerPrincipal().isEmpty()) {
            institucionService.guardar("IESPP Juliaca", "JULI", "Av. Principal 123", null, null);
        }

        long mrc = System.nanoTime();
        programa = programaEstudioService.crear("PROG" + mrc, "Programa import", null, 10);
        plan = planEstudioService.crear(programa.getId(), "PLAN" + mrc, "Plan import", null, null);
        ciclo1 = cicloService.crear(plan.getId(), 1, "Primer ciclo", 1);
        curso1 = cursoService.crear("CUR" + mrc, "Curso importado", 4, null, null);
        planCurso1 = planCursoService.asignar(plan.getId(), ciclo1.getId(), curso1.getId(), 1, true);
        periodo = periodoAcademicoService.crear("PER" + mrc, 2026, "I", null, null);

        estudianteExistente = estudianteService.crear("EXIST" + mrc, null, "Maria", "Condori", null, null, null,
                null, null, null);
        trayectoriaExistente = trayectoriaService.iniciar(estudianteExistente.getId(), programa.getId(), plan.getId(),
                null, null);
    }

    @Test
    void importaNotasDeExistenteYCreaEstudianteConTrayectoriaParaHistoricoCompleto() throws IOException {
        long mrc = System.nanoTime();
        String cdgNv = "NUEVO" + mrc;
        String crsInxstnt = "NOEXISTE" + mrc;

        String[] cbcr = {"Codigo Estudiante", "DNI", "Apellido Paterno", "Apellido Materno", "Nombres",
                "Programa", "Plan", "Fecha Inicio Trayectoria", "Ciclo", "Curso", "Periodo", "Nota", "Situacion",
                "Observacion"};
        Object[][] fls = {

                {estudianteExistente.getCodigo(), null, null, null, null, null, null, null, "1", curso1.getCodigo(),
                        periodo.getNombre(), "16", "APROBADO", null},

                {cdgNv, null, "Torres", "Apaza", "Luis", programa.getCodigo(), plan.getCodigo(), "2024-03-01",
                        "1", curso1.getCodigo(), periodo.getNombre(), "14", "APROBADO", "importado"},

                {estudianteExistente.getCodigo(), null, null, null, null, null, null, null, "1", crsInxstnt,
                        periodo.getNombre(), "10", "APROBADO", null},

                {estudianteExistente.getCodigo(), null, null, null, null, null, null, null, "1", curso1.getCodigo(),
                        periodo.getNombre(), "25", "APROBADO", null},
        };
        Path archv = escribirWorkbook(cbcr, fls);

        ResultadoImportacion rsltd = importacionNotasService.importar(archv);

        assertThat(rsltd.totalFilas()).isEqualTo(4);
        assertThat(rsltd.exitosas()).isEqualTo(2);
        assertThat(rsltd.errores()).hasSize(2);
        assertThat(rsltd.errores()).anySatisfy(e -> assertThat(e.mensaje()).contains(crsInxstnt));
        assertThat(rsltd.errores()).anySatisfy(e -> assertThat(e.mensaje()).contains("entre 0 y 20"));

        RegistroAcademico rgstrExstnt = registroAcademicoRepository
                .findByTrayectoriaIdAndPlanCursoIdAndPeriodoId(trayectoriaExistente.getId(), planCurso1.getId(),
                        periodo.getId())
                .orElseThrow();
        assertThat(rgstrExstnt.getNotaFinal()).isEqualTo(16.0);

        Estudiante nv = estudianteRepository.findByCodigo(cdgNv).orElseThrow();
        Trayectoria tryctrNv = trayectoriaService.obtenerActiva(nv.getId()).orElseThrow();
        assertThat(tryctrNv.getFechaInicio()).isEqualTo(java.time.LocalDate.of(2024, 3, 1));
        RegistroAcademico rgstrNv = registroAcademicoRepository
                .findByTrayectoriaIdAndPlanCursoIdAndPeriodoId(tryctrNv.getId(), planCurso1.getId(),
                        periodo.getId())
                .orElseThrow();
        assertThat(rgstrNv.getNotaFinal()).isEqualTo(14.0);
        assertThat(rgstrNv.getObservacion()).isEqualTo("importado");
    }

    @Test
    void mismoEstudianteConOtroCicloSoloAgregaNotaSinDuplicarNadaEnLaMismaTrayectoria() throws IOException {

        Ciclo ccl2 = cicloService.crear(plan.getId(), 2, "Segundo ciclo", 2);
        Curso crs2 = cursoService.crear("CUR2" + System.nanoTime(), "Curso del ciclo 2", 3, null, null);
        PlanCurso plnCrs2 = planCursoService.asignar(plan.getId(), ccl2.getId(), crs2.getId(), 1, true);

        String[] cbcr = {"Codigo Estudiante", "DNI", "Apellido Paterno", "Apellido Materno", "Nombres",
                "Programa", "Plan", "Fecha Inicio Trayectoria", "Ciclo", "Curso", "Periodo", "Nota", "Situacion",
                "Observacion"};
        Object[][] fls = {
                {estudianteExistente.getCodigo(), null, null, null, null, null, null, null, "2", crs2.getCodigo(),
                        periodo.getNombre(), "17", "APROBADO", null},
        };
        Path archv = escribirWorkbook(cbcr, fls);

        ResultadoImportacion rsltd = importacionNotasService.importar(archv);

        assertThat(rsltd.exitosas()).isEqualTo(1);
        assertThat(rsltd.errores()).isEmpty();

        assertThat(estudianteRepository.findByCodigo(estudianteExistente.getCodigo()).orElseThrow().getId())
                .isEqualTo(estudianteExistente.getId());
        Trayectoria tryctrTrsImprtr = trayectoriaService.obtenerActiva(estudianteExistente.getId()).orElseThrow();
        assertThat(tryctrTrsImprtr.getId()).isEqualTo(trayectoriaExistente.getId());

        RegistroAcademico ntCcl2 = registroAcademicoRepository
                .findByTrayectoriaIdAndPlanCursoIdAndPeriodoId(trayectoriaExistente.getId(), plnCrs2.getId(),
                        periodo.getId())
                .orElseThrow();
        assertThat(ntCcl2.getNotaFinal()).isEqualTo(17.0);
    }

    @Test
    void trayectoriaActivaEnOtroPlanQuedaComoError() throws IOException {
        long mrc = System.nanoTime();
        ProgramaEstudio otrPrgrm = programaEstudioService.crear("PROG2" + mrc, "Otro programa", null, 10);
        PlanEstudio otrPln = planEstudioService.crear(otrPrgrm.getId(), "PLAN2" + mrc, "Otro plan", null, null);

        String[] cbcr = {"Codigo Estudiante", "DNI", "Apellido Paterno", "Apellido Materno", "Nombres",
                "Programa", "Plan", "Fecha Inicio Trayectoria", "Ciclo", "Curso", "Periodo", "Nota", "Situacion",
                "Observacion"};
        Object[][] fls = {
                {estudianteExistente.getCodigo(), null, null, null, null, otrPrgrm.getCodigo(),
                        otrPln.getCodigo(), null, "1", curso1.getCodigo(), periodo.getNombre(), "10", "APROBADO",
                        null},
        };
        Path archv = escribirWorkbook(cbcr, fls);

        ResultadoImportacion rsltd = importacionNotasService.importar(archv);

        assertThat(rsltd.exitosas()).isZero();
        assertThat(rsltd.errores()).hasSize(1);
        assertThat(rsltd.errores().get(0).mensaje()).contains("ya tiene una trayectoria activa");
    }

    private Path escribirWorkbook(String[] cbcr, Object[][] fls) throws IOException {
        Path archv = Files.createTempFile("sgfa-import-notas-", ".xlsx");
        try (XSSFWorkbook lbr = new XSSFWorkbook()) {
            Sheet hj = lbr.createSheet("Notas");
            Row flCbcr = hj.createRow(0);
            for (int i = 0; i < cbcr.length; i++) {
                flCbcr.createCell(i).setCellValue(cbcr[i]);
            }
            for (int f = 0; f < fls.length; f++) {
                Row fl = hj.createRow(f + 1);
                Object[] dts = fls[f];
                for (int c = 0; c < dts.length; c++) {
                    if (dts[c] != null) {
                        fl.createCell(c).setCellValue(dts[c].toString());
                    }
                }
            }
            try (OutputStream sld = Files.newOutputStream(archv)) {
                lbr.write(sld);
            }
        }
        return archv;
    }
}
