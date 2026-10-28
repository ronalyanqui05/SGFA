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
import pe.edu.sgfa.importacion.service.ImportacionEstudiantesService;
import pe.edu.sgfa.importacion.service.ResultadoImportacion;
import pe.edu.sgfa.institucion.service.InstitucionService;
import pe.edu.sgfa.programa.domain.PlanEstudio;
import pe.edu.sgfa.programa.domain.ProgramaEstudio;
import pe.edu.sgfa.programa.service.PlanEstudioService;
import pe.edu.sgfa.programa.service.ProgramaEstudioService;
import pe.edu.sgfa.trayectoria.domain.Trayectoria;
import pe.edu.sgfa.trayectoria.service.TrayectoriaService;
import pe.edu.sgfa.usuario.service.AdminBootstrapResult;
import pe.edu.sgfa.usuario.service.AuthService;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ImportacionEstudiantesServiceTest {

    @BeforeAll
    static void useIsolatedHomeDir() throws IOException {
        Path tmpHm = Files.createTempDirectory("sgfa-importacion-estudiantes-test-home-");
        System.setProperty("sgfa.home-dir", tmpHm.toString());
    }

    @Autowired
    private InstitucionService institucionService;
    @Autowired
    private ImportacionEstudiantesService importacionEstudiantesService;
    @Autowired
    private EstudianteRepository estudianteRepository;
    @Autowired
    private ProgramaEstudioService programaEstudioService;
    @Autowired
    private PlanEstudioService planEstudioService;
    @Autowired
    private TrayectoriaService trayectoriaService;
    @Autowired
    private AuthService authService;
    @Autowired
    private AdminBootstrapResult adminBootstrapResult;

    private static AdminBootstrapResult.Credenciales credencialesAdmin;

    @BeforeEach
    void autenticarComoAdmin() {
        if (credencialesAdmin == null) {
            credencialesAdmin = adminBootstrapResult.consumir()
                    .orElseThrow(() -> new IllegalStateException("El admin inicial no se genero"));
        }
        authService.cerrarSesion();
        authService.autenticar(credencialesAdmin.username(), credencialesAdmin.password());

        if (institucionService.obtenerPrincipal().isEmpty()) {
            institucionService.guardar("IESPP Juliaca", "JULI", "Av. Principal 123", null, null);
        }
    }

    @Test
    void importaFilasValidasYReportaErroresPorFila() throws IOException {
        long mrc = System.nanoTime();
        String[] cbcr = {"Codigo", "DNI", "Apellido Paterno", "Apellido Materno", "Nombres", "Sexo",
                "Fecha Nacimiento", "Direccion", "Telefono", "Correo"};
        Object[][] fls = {
                {"IMP" + mrc + "A", null, "Quispe", "Mamani", "Ana", "F", "2001-05-14", null, null, null},
                {"IMP" + mrc + "B", null, "Torres", "Apaza", "Luis", "M", null, null, null, null},
                {null, null, null, "Materno sin obligatorios", "", null, null, null, null, null},
                {"IMP" + mrc + "A", null, "Quispe", "Mamani", "Duplicado", "F", null, null, null, null},
        };
        Path archv = escribirWorkbook(cbcr, fls);

        ResultadoImportacion rsltd = importacionEstudiantesService.importar(archv);

        assertThat(rsltd.totalFilas()).isEqualTo(4);
        assertThat(rsltd.exitosas()).isEqualTo(2);
        assertThat(rsltd.errores()).hasSize(2);
        assertThat(rsltd.errores().get(0).fila()).isEqualTo(4);
        assertThat(rsltd.errores().get(1).fila()).isEqualTo(5);

        assertThat(estudianteRepository.findByCodigo("IMP" + mrc + "A")).isPresent();
        assertThat(estudianteRepository.findByCodigo("IMP" + mrc + "B")).isPresent();
    }

    @Test
    void siLaFilaTraeProgramaYPlanElEstudianteQuedaMatriculadoDeUnaVez() throws IOException {
        long mrc = System.nanoTime();
        ProgramaEstudio programa = programaEstudioService.crear("PROG" + mrc, "Programa import", null, 10);
        PlanEstudio pln = planEstudioService.crear(programa.getId(), "PLAN" + mrc, "Plan import", null, null);

        String cdgConPln = "IMP" + mrc + "CONPLAN";
        String cdgSinPln = "IMP" + mrc + "SINPLAN";
        String[] cbcr = {"Codigo", "DNI", "Apellido Paterno", "Apellido Materno", "Nombres", "Sexo",
                "Fecha Nacimiento", "Direccion", "Telefono", "Correo", "Programa", "Plan", "Fecha Inicio Trayectoria"};
        Object[][] fls = {

                {cdgConPln, null, "Quispe", "Mamani", "Ana", "F", null, null, null, null,
                        programa.getCodigo(), pln.getCodigo(), "2024-03-01"},

                {cdgSinPln, null, "Torres", "Apaza", "Luis", "M", null, null, null, null, null, null, null},
        };
        Path archv = escribirWorkbook(cbcr, fls);

        ResultadoImportacion rsltd = importacionEstudiantesService.importar(archv);

        assertThat(rsltd.exitosas()).isEqualTo(2);
        assertThat(rsltd.errores()).isEmpty();

        Estudiante conPln = estudianteRepository.findByCodigo(cdgConPln).orElseThrow();
        Trayectoria trayectoria = trayectoriaService.obtenerActiva(conPln.getId()).orElseThrow();
        assertThat(trayectoria.getPlanEstudio().getId()).isEqualTo(pln.getId());
        assertThat(trayectoria.getFechaInicio()).isEqualTo(java.time.LocalDate.of(2024, 3, 1));

        Estudiante sinPln = estudianteRepository.findByCodigo(cdgSinPln).orElseThrow();
        assertThat(trayectoriaService.obtenerActiva(sinPln.getId())).isEmpty();
    }

    private Path escribirWorkbook(String[] cbcr, Object[][] fls) throws IOException {
        Path archv = Files.createTempFile("sgfa-import-estudiantes-", ".xlsx");
        try (XSSFWorkbook lbr = new XSSFWorkbook()) {
            Sheet hj = lbr.createSheet("Estudiantes");
            Row flCbcr = hj.createRow(0);
            for (int i = 0; i < cbcr.length; i++) {
                flCbcr.createCell(i).setCellValue(cbcr[i]);
            }
            for (int f = 0; f < fls.length; f++) {
                Row fila = hj.createRow(f + 1);
                Object[] dts = fls[f];
                for (int c = 0; c < dts.length; c++) {
                    if (dts[c] != null) {
                        fila.createCell(c).setCellValue(dts[c].toString());
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
