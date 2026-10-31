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

import pe.edu.sgfa.estudiante.domain.YmclEstudiante;
import pe.edu.sgfa.estudiante.repository.YmclEstudianteRepository;
import pe.edu.sgfa.estudiante.service.YmclEstudianteService;
import pe.edu.sgfa.importacion.service.CphImportacionNotasService;
import pe.edu.sgfa.importacion.service.CphResultadoImportacion;
import pe.edu.sgfa.institucion.service.RacaInstitucionService;
import pe.edu.sgfa.nota.domain.YmclRegistroAcademico;
import pe.edu.sgfa.nota.repository.YmclRegistroAcademicoRepository;
import pe.edu.sgfa.periodo.domain.YmclPeriodoAcademico;
import pe.edu.sgfa.periodo.service.YmclPeriodoAcademicoService;
import pe.edu.sgfa.programa.domain.RacaCiclo;
import pe.edu.sgfa.programa.domain.RacaCurso;
import pe.edu.sgfa.programa.domain.RacaPlanCurso;
import pe.edu.sgfa.programa.domain.RacaPlanEstudio;
import pe.edu.sgfa.programa.domain.RacaProgramaEstudio;
import pe.edu.sgfa.programa.service.RacaCicloService;
import pe.edu.sgfa.programa.service.RacaCursoService;
import pe.edu.sgfa.programa.service.RacaPlanCursoService;
import pe.edu.sgfa.programa.service.RacaPlanEstudioService;
import pe.edu.sgfa.programa.service.RacaProgramaEstudioService;
import pe.edu.sgfa.trayectoria.domain.YmclTrayectoria;
import pe.edu.sgfa.trayectoria.service.YmclTrayectoriaService;
import pe.edu.sgfa.usuario.service.RyeAdminBootstrapResult;
import pe.edu.sgfa.usuario.service.RyeAuthService;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CphImportacionNotasServiceTest {

    @BeforeAll
    static void useIsolatedHomeDir() throws IOException {
        Path tempHomePch = Files.createTempDirectory("sgfa-importacion-notas-test-home-");
        System.setProperty("sgfa.home-dir", tempHomePch.toString());
    }

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
    private YmclPeriodoAcademicoService periodoAcademicoServicePch;
    @Autowired
    private YmclEstudianteService estudianteServicePch;
    @Autowired
    private YmclEstudianteRepository estudianteRepositoryPch;
    @Autowired
    private YmclTrayectoriaService trayectoriaServicePch;
    @Autowired
    private YmclRegistroAcademicoRepository registroAcademicoRepositoryPch;
    @Autowired
    private CphImportacionNotasService importacionNotasServicePch;
    @Autowired
    private RyeAuthService authServicePch;
    @Autowired
    private RyeAdminBootstrapResult adminBootstrapResultPch;

    private static RyeAdminBootstrapResult.RyeCredenciales credencialesAdminPch;

    private RacaProgramaEstudio programa;
    private RacaPlanEstudio planPch;
    private RacaCiclo ciclo1Pch;
    private RacaCurso curso1Pch;
    private RacaPlanCurso planCurso1Pch;
    private YmclPeriodoAcademico periodoPch;
    private YmclEstudiante estudianteExistentePch;
    private YmclTrayectoria trayectoriaExistentePch;

    @BeforeEach
    void prepararDatosBase() {
        if (credencialesAdminPch == null) {
            credencialesAdminPch = adminBootstrapResultPch.ryeConsumir()
                    .orElseThrow(() -> new IllegalStateException("El admin inicial no se genero"));
        }
        authServicePch.ryeCerrarSesion();
        authServicePch.ryeAutenticar(credencialesAdminPch.username(), credencialesAdminPch.password());

        if (institucionServicePch.racaObtenerPrincipal().isEmpty()) {
            institucionServicePch.racaGuardar("IESPP Juliaca", "JULI", "Av. Principal 123", null, null);
        }

        long marcaPch = System.nanoTime();
        programa = programaEstudioServicePch.racaCrear("PROG" + marcaPch, "Programa import", null, 10);
        planPch = planEstudioServicePch.racaCrear(programa.getId(), "PLAN" + marcaPch, "Plan import", null, null);
        ciclo1Pch = cicloServicePch.racaCrear(planPch.getId(), 1, "Primer ciclo", 1);
        curso1Pch = cursoServicePch.racaCrear("CUR" + marcaPch, "Curso importado", 4, null, null);
        planCurso1Pch = planCursoServicePch.racaAsignar(planPch.getId(), ciclo1Pch.getId(), curso1Pch.getId(), 1, true);
        periodoPch = periodoAcademicoServicePch.ymclCrear("PER" + marcaPch, 2026, "I", null, null);

        estudianteExistentePch = estudianteServicePch.ymclCrear("EXIST" + marcaPch, null, "Maria", "Condori", null, null, null,
                null, null, null);
        trayectoriaExistentePch = trayectoriaServicePch.ymclIniciar(estudianteExistentePch.getId(), programa.getId(), planPch.getId(),
                null, null);
    }

    @Test
    void importaNotasDeExistenteYCreaEstudianteConTrayectoriaParaHistoricoCompleto() throws IOException {
        long marcaPch = System.nanoTime();
        String codigoNuevoPch = "NUEVO" + marcaPch;
        String cursoInexistentePch = "NOEXISTE" + marcaPch;

        String[] cabeceraPch = {"Codigo Estudiante", "DNI", "Apellido Paterno", "Apellido Materno", "Nombres",
                "Programa", "Plan", "Fecha Inicio Trayectoria", "Ciclo", "Curso", "Periodo", "Nota", "Situacion",
                "Observacion"};
        Object[][] filasPch = {

                {estudianteExistentePch.getCodigo(), null, null, null, null, null, null, null, "1", curso1Pch.getCodigo(),
                        periodoPch.getNombre(), "16", "APROBADO", null},

                {codigoNuevoPch, null, "Torres", "Apaza", "Luis", programa.getCodigo(), planPch.getCodigo(), "2024-03-01",
                        "1", curso1Pch.getCodigo(), periodoPch.getNombre(), "14", "APROBADO", "importado"},

                {estudianteExistentePch.getCodigo(), null, null, null, null, null, null, null, "1", cursoInexistentePch,
                        periodoPch.getNombre(), "10", "APROBADO", null},

                {estudianteExistentePch.getCodigo(), null, null, null, null, null, null, null, "1", curso1Pch.getCodigo(),
                        periodoPch.getNombre(), "25", "APROBADO", null},
        };
        Path archivoPch = escribirWorkbook(cabeceraPch, filasPch);

        CphResultadoImportacion resultadoPch = importacionNotasServicePch.cphImportar(archivoPch);

        assertThat(resultadoPch.totalFilas()).isEqualTo(4);
        assertThat(resultadoPch.exitosas()).isEqualTo(2);
        assertThat(resultadoPch.errores()).hasSize(2);
        assertThat(resultadoPch.errores()).anySatisfy(e -> assertThat(e.mensaje()).contains(cursoInexistentePch));
        assertThat(resultadoPch.errores()).anySatisfy(e -> assertThat(e.mensaje()).contains("entre 0 y 20"));

        YmclRegistroAcademico registroExistentePch = registroAcademicoRepositoryPch
                .findByTrayectoriaIdAndPlanCursoIdAndPeriodoId(trayectoriaExistentePch.getId(), planCurso1Pch.getId(),
                        periodoPch.getId())
                .orElseThrow();
        assertThat(registroExistentePch.getNotaFinal()).isEqualTo(16.0);

        YmclEstudiante nuevoPch = estudianteRepositoryPch.findByCodigo(codigoNuevoPch).orElseThrow();
        YmclTrayectoria trayectoriaNuevaPch = trayectoriaServicePch.ymclObtenerActiva(nuevoPch.getId()).orElseThrow();
        assertThat(trayectoriaNuevaPch.getFechaInicio()).isEqualTo(java.time.LocalDate.of(2024, 3, 1));
        YmclRegistroAcademico registroNuevoPch = registroAcademicoRepositoryPch
                .findByTrayectoriaIdAndPlanCursoIdAndPeriodoId(trayectoriaNuevaPch.getId(), planCurso1Pch.getId(),
                        periodoPch.getId())
                .orElseThrow();
        assertThat(registroNuevoPch.getNotaFinal()).isEqualTo(14.0);
        assertThat(registroNuevoPch.getObservacion()).isEqualTo("importado");
    }

    @Test
    void mismoEstudianteConOtroCicloSoloAgregaNotaSinDuplicarNadaEnLaMismaTrayectoria() throws IOException {

        RacaCiclo ciclo2Pch = cicloServicePch.racaCrear(planPch.getId(), 2, "Segundo ciclo", 2);
        RacaCurso curso2Pch = cursoServicePch.racaCrear("CUR2" + System.nanoTime(), "Curso del ciclo 2", 3, null, null);
        RacaPlanCurso planCurso2Pch = planCursoServicePch.racaAsignar(planPch.getId(), ciclo2Pch.getId(), curso2Pch.getId(), 1, true);

        String[] cabeceraPch = {"Codigo Estudiante", "DNI", "Apellido Paterno", "Apellido Materno", "Nombres",
                "Programa", "Plan", "Fecha Inicio Trayectoria", "Ciclo", "Curso", "Periodo", "Nota", "Situacion",
                "Observacion"};
        Object[][] filasPch = {
                {estudianteExistentePch.getCodigo(), null, null, null, null, null, null, null, "2", curso2Pch.getCodigo(),
                        periodoPch.getNombre(), "17", "APROBADO", null},
        };
        Path archivoPch = escribirWorkbook(cabeceraPch, filasPch);

        CphResultadoImportacion resultadoPch = importacionNotasServicePch.cphImportar(archivoPch);

        assertThat(resultadoPch.exitosas()).isEqualTo(1);
        assertThat(resultadoPch.errores()).isEmpty();

        assertThat(estudianteRepositoryPch.findByCodigo(estudianteExistentePch.getCodigo()).orElseThrow().getId())
                .isEqualTo(estudianteExistentePch.getId());
        YmclTrayectoria trayectoriaTrasImportarPch = trayectoriaServicePch.ymclObtenerActiva(estudianteExistentePch.getId()).orElseThrow();
        assertThat(trayectoriaTrasImportarPch.getId()).isEqualTo(trayectoriaExistentePch.getId());

        YmclRegistroAcademico notaCiclo2Pch = registroAcademicoRepositoryPch
                .findByTrayectoriaIdAndPlanCursoIdAndPeriodoId(trayectoriaExistentePch.getId(), planCurso2Pch.getId(),
                        periodoPch.getId())
                .orElseThrow();
        assertThat(notaCiclo2Pch.getNotaFinal()).isEqualTo(17.0);
    }

    @Test
    void trayectoriaActivaEnOtroPlanQuedaComoError() throws IOException {
        long marcaPch = System.nanoTime();
        RacaProgramaEstudio otroProgramaPch = programaEstudioServicePch.racaCrear("PROG2" + marcaPch, "Otro programa", null, 10);
        RacaPlanEstudio otroPlanPch = planEstudioServicePch.racaCrear(otroProgramaPch.getId(), "PLAN2" + marcaPch, "Otro plan", null, null);

        String[] cabeceraPch = {"Codigo Estudiante", "DNI", "Apellido Paterno", "Apellido Materno", "Nombres",
                "Programa", "Plan", "Fecha Inicio Trayectoria", "Ciclo", "Curso", "Periodo", "Nota", "Situacion",
                "Observacion"};
        Object[][] filasPch = {
                {estudianteExistentePch.getCodigo(), null, null, null, null, otroProgramaPch.getCodigo(),
                        otroPlanPch.getCodigo(), null, "1", curso1Pch.getCodigo(), periodoPch.getNombre(), "10", "APROBADO",
                        null},
        };
        Path archivoPch = escribirWorkbook(cabeceraPch, filasPch);

        CphResultadoImportacion resultadoPch = importacionNotasServicePch.cphImportar(archivoPch);

        assertThat(resultadoPch.exitosas()).isZero();
        assertThat(resultadoPch.errores()).hasSize(1);
        assertThat(resultadoPch.errores().get(0).mensaje()).contains("ya tiene una trayectoria activa");
    }

    private Path escribirWorkbook(String[] cabeceraPch, Object[][] filasPch) throws IOException {
        Path archivoPch = Files.createTempFile("sgfa-import-notas-", ".xlsx");
        try (XSSFWorkbook libroPch = new XSSFWorkbook()) {
            Sheet hojaPch = libroPch.createSheet("Notas");
            Row filaCabeceraPch = hojaPch.createRow(0);
            for (int i = 0; i < cabeceraPch.length; i++) {
                filaCabeceraPch.createCell(i).setCellValue(cabeceraPch[i]);
            }
            for (int f = 0; f < filasPch.length; f++) {
                Row filaPch = hojaPch.createRow(f + 1);
                Object[] datosPch = filasPch[f];
                for (int c = 0; c < datosPch.length; c++) {
                    if (datosPch[c] != null) {
                        filaPch.createCell(c).setCellValue(datosPch[c].toString());
                    }
                }
            }
            try (OutputStream salidaPch = Files.newOutputStream(archivoPch)) {
                libroPch.write(salidaPch);
            }
        }
        return archivoPch;
    }
}
