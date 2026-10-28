package pe.edu.sgfa.ocr;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import pe.edu.sgfa.institucion.service.InstitucionService;
import pe.edu.sgfa.ocr.service.LectorReportesPdf;
import pe.edu.sgfa.ocr.service.LectorReportesPdf.HojaLeida;
import pe.edu.sgfa.ocr.service.ResolutorReporte;
import pe.edu.sgfa.ocr.service.ResolutorReporte.Estado;
import pe.edu.sgfa.ocr.service.ResolutorReporte.Propuesta;
import pe.edu.sgfa.periodo.service.PeriodoAcademicoService;
import pe.edu.sgfa.programa.domain.Ciclo;
import pe.edu.sgfa.programa.service.CicloService;
import pe.edu.sgfa.programa.service.CursoService;
import pe.edu.sgfa.programa.service.PlanCursoService;
import pe.edu.sgfa.programa.service.PlanEstudioService;
import pe.edu.sgfa.programa.service.ProgramaEstudioService;
import pe.edu.sgfa.usuario.service.AdminBootstrapResult;
import pe.edu.sgfa.usuario.service.AuthService;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PdfRealMasivoResolucionTest {

    private static final File PDF = new File(System.getProperty("user.home"), "Downloads/pruebas ocr/pdf_masivo.pdf");

    @BeforeAll
    static void baseAislada() throws IOException {
        System.setProperty("sgfa.home-dir", Files.createTempDirectory("sgfa-real-test-home-").toString());
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
    private LectorReportesPdf lector;
    @Autowired
    private ResolutorReporte resolutor;
    @Autowired
    private AuthService authService;
    @Autowired
    private AdminBootstrapResult adminBootstrapResult;

    private static AdminBootstrapResult.Credenciales credenciales;
    private static Ciclo ciclo1;

    @BeforeEach
    void preparar() {
        Assumptions.assumeTrue(PDF.isFile(), "No esta " + PDF);
        if (credenciales == null) {
            credenciales = adminBootstrapResult.consumir().orElseThrow();
        }
        authService.cerrarSesion();
        authService.autenticar(credenciales.username(), credenciales.password());
        if (ciclo1 == null) {
            institucionService.guardar("IESPP Juliaca", "JULI", null, null, null);

            programaEstudioService.crear("COM", "Educacion Secundaria - Especialidad Comunicacion", null, 10);
            programaEstudioService.crear("INI", "Educacion Inicial", null, 10);
            var mat = programaEstudioService.crear("MAT", "Educacion Secundaria - Especialidad Matematica", null, 10);
            var pln = planEstudioService.crear(mat.getId(), "076-2020", "RVM 076-2020 MINEDU", null, null);
            ciclo1 = cicloService.crear(pln.getId(), 1, "Ciclo I", 1);
            int ordn = 1;
            for (Object[] c : new Object[][] {
                    {"LECTURA Y ESCRITURA EN LA EDUCACIÓN SUPERIOR", 3}, {"RESOLUCIÓN DE PROBLEMAS MATEMÁTICOS I", 3},
                    {"HISTORIA, SOCIEDAD Y DIVERSIDAD", 3}, {"DESARROLLO PERSONAL I", 5},
                    {"PRÁCTICA E INVESTIGACIÓN I", 5}, {"EDUCACIÓN Y SOCIEDAD EN EL SIGLO XXI", 5}}) {
                var crs = cursoService.crear("MAT-I_CM0" + ordn, (String) c[0], (Integer) c[1], null, null);
                planCursoService.asignar(pln.getId(), ciclo1.getId(), crs.getId(), ordn++, true);
            }
            periodoAcademicoService.crear("2026-I", 2026, null, null, null);
            periodoAcademicoService.crear("2026-II", 2026, null, null, null);
        }
    }

    @Test
    void lasOchoHojasRealesQuedanListasParaGuardar() {
        List<HojaLeida> hjs = lector.leer(PDF, null);
        List<Propuesta> prpsts = resolutor.proponerTodos(hjs.stream().map(HojaLeida::reporte).toList());

        for (int i = 0; i < prpsts.size(); i++) {
            Propuesta p = prpsts.get(i);
            System.out.println("RESOL>> H" + (i + 1) + " " + p.estado() + " " + p.matricula() + " " + p.apellidoPaterno()
                    + " | " + (p.ciclo() == null ? "?" : p.ciclo().getNombre()) + " " + (p.periodo() == null ? "?" : p.periodo().getNombre())
                    + " | cursos " + p.cursosEmparejados() + "/" + p.cursos().size() + " prom " + p.promedioCalculado()
                    + "/" + p.promedioImpreso() + " | avisos=" + p.avisos() + " notas=" + p.notas());
        }

        assertThat(prpsts).extracting(Propuesta::estado).containsOnly(Estado.LISTO);
    }
}
