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
import pe.edu.sgfa.importacion.service.CphImportacionEstudiantesService;
import pe.edu.sgfa.importacion.service.CphResultadoImportacion;
import pe.edu.sgfa.institucion.service.RacaInstitucionService;
import pe.edu.sgfa.programa.domain.RacaPlanEstudio;
import pe.edu.sgfa.programa.domain.RacaProgramaEstudio;
import pe.edu.sgfa.programa.service.RacaPlanEstudioService;
import pe.edu.sgfa.programa.service.RacaProgramaEstudioService;
import pe.edu.sgfa.trayectoria.domain.YmclTrayectoria;
import pe.edu.sgfa.trayectoria.service.YmclTrayectoriaService;
import pe.edu.sgfa.usuario.service.RyeAdminBootstrapResult;
import pe.edu.sgfa.usuario.service.RyeAuthService;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CphImportacionEstudiantesServiceTest {

    @BeforeAll
    static void useIsolatedHomeDir() throws IOException {
        Path tempHomePch = Files.createTempDirectory("sgfa-importacion-estudiantes-test-home-");
        System.setProperty("sgfa.home-dir", tempHomePch.toString());
    }

    @Autowired
    private RacaInstitucionService institucionServicePch;
    @Autowired
    private CphImportacionEstudiantesService importacionEstudiantesServicePch;
    @Autowired
    private YmclEstudianteRepository estudianteRepositoryPch;
    @Autowired
    private RacaProgramaEstudioService programaEstudioServicePch;
    @Autowired
    private RacaPlanEstudioService planEstudioServicePch;
    @Autowired
    private YmclTrayectoriaService trayectoriaServicePch;
    @Autowired
    private RyeAuthService authServicePch;
    @Autowired
    private RyeAdminBootstrapResult adminBootstrapResultPch;

    private static RyeAdminBootstrapResult.RyeCredenciales credencialesAdminPch;

    @BeforeEach
    void autenticarComoAdmin() {
        if (credencialesAdminPch == null) {
            credencialesAdminPch = adminBootstrapResultPch.ryeConsumir()
                    .orElseThrow(() -> new IllegalStateException("El admin inicial no se genero"));
        }
        authServicePch.ryeCerrarSesion();
        authServicePch.ryeAutenticar(credencialesAdminPch.username(), credencialesAdminPch.password());

        if (institucionServicePch.racaObtenerPrincipal().isEmpty()) {
            institucionServicePch.racaGuardar("IESPP Juliaca", "JULI", "Av. Principal 123", null, null);
        }
    }

    @Test
    void importaFilasValidasYReportaErroresPorFila() throws IOException {
        long marcaPch = System.nanoTime();
        String[] cabeceraPch = {"Codigo", "DNI", "Apellido Paterno", "Apellido Materno", "Nombres", "Sexo",
                "Fecha Nacimiento", "Direccion", "Telefono", "Correo"};
        Object[][] filasPch = {
                {"IMP" + marcaPch + "A", null, "Quispe", "Mamani", "Ana", "F", "2001-05-14", null, null, null},
                {"IMP" + marcaPch + "B", null, "Torres", "Apaza", "Luis", "M", null, null, null, null},
                {null, null, null, "Materno sin obligatorios", "", null, null, null, null, null},
                {"IMP" + marcaPch + "A", null, "Quispe", "Mamani", "Duplicado", "F", null, null, null, null},
        };
        Path archivoPch = escribirWorkbook(cabeceraPch, filasPch);

        CphResultadoImportacion resultadoPch = importacionEstudiantesServicePch.cphImportar(archivoPch);

        assertThat(resultadoPch.totalFilas()).isEqualTo(4);
        assertThat(resultadoPch.exitosas()).isEqualTo(2);
        assertThat(resultadoPch.errores()).hasSize(2);
        assertThat(resultadoPch.errores().get(0).fila()).isEqualTo(4);
        assertThat(resultadoPch.errores().get(1).fila()).isEqualTo(5);

        assertThat(estudianteRepositoryPch.findByCodigo("IMP" + marcaPch + "A")).isPresent();
        assertThat(estudianteRepositoryPch.findByCodigo("IMP" + marcaPch + "B")).isPresent();
    }

    @Test
    void siLaFilaTraeProgramaYPlanElEstudianteQuedaMatriculadoDeUnaVez() throws IOException {
        long marcaPch = System.nanoTime();
        RacaProgramaEstudio programaPch = programaEstudioServicePch.racaCrear("PROG" + marcaPch, "Programa import", null, 10);
        RacaPlanEstudio planPch = planEstudioServicePch.racaCrear(programaPch.getId(), "PLAN" + marcaPch, "Plan import", null, null);

        String codigoConPlanPch = "IMP" + marcaPch + "CONPLAN";
        String codigoSinPlanPch = "IMP" + marcaPch + "SINPLAN";
        String[] cabeceraPch = {"Codigo", "DNI", "Apellido Paterno", "Apellido Materno", "Nombres", "Sexo",
                "Fecha Nacimiento", "Direccion", "Telefono", "Correo", "Programa", "Plan", "Fecha Inicio Trayectoria"};
        Object[][] filasPch = {

                {codigoConPlanPch, null, "Quispe", "Mamani", "Ana", "F", null, null, null, null,
                        programaPch.getCodigo(), planPch.getCodigo(), "2024-03-01"},

                {codigoSinPlanPch, null, "Torres", "Apaza", "Luis", "M", null, null, null, null, null, null, null},
        };
        Path archivoPch = escribirWorkbook(cabeceraPch, filasPch);

        CphResultadoImportacion resultadoPch = importacionEstudiantesServicePch.cphImportar(archivoPch);

        assertThat(resultadoPch.exitosas()).isEqualTo(2);
        assertThat(resultadoPch.errores()).isEmpty();

        YmclEstudiante conPlanPch = estudianteRepositoryPch.findByCodigo(codigoConPlanPch).orElseThrow();
        YmclTrayectoria trayectoriaPch = trayectoriaServicePch.ymclObtenerActiva(conPlanPch.getId()).orElseThrow();
        assertThat(trayectoriaPch.getPlanEstudio().getId()).isEqualTo(planPch.getId());
        assertThat(trayectoriaPch.getFechaInicio()).isEqualTo(java.time.LocalDate.of(2024, 3, 1));

        YmclEstudiante sinPlanPch = estudianteRepositoryPch.findByCodigo(codigoSinPlanPch).orElseThrow();
        assertThat(trayectoriaServicePch.ymclObtenerActiva(sinPlanPch.getId())).isEmpty();
    }

    private Path escribirWorkbook(String[] cabeceraPch, Object[][] filasPch) throws IOException {
        Path archivoPch = Files.createTempFile("sgfa-import-estudiantes-", ".xlsx");
        try (XSSFWorkbook libroPch = new XSSFWorkbook()) {
            Sheet hojaPch = libroPch.createSheet("Estudiantes");
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
