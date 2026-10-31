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

import pe.edu.sgfa.institucion.service.RacaInstitucionService;
import pe.edu.sgfa.ocr.service.RahpLectorReportesPdf;
import pe.edu.sgfa.ocr.service.RahpLectorReportesPdf.RahpHojaLeida;
import pe.edu.sgfa.ocr.service.RahpResolutorReporte;
import pe.edu.sgfa.ocr.service.RahpResolutorReporte.RahpEstado;
import pe.edu.sgfa.ocr.service.RahpResolutorReporte.RahpPropuesta;
import pe.edu.sgfa.periodo.service.YmclPeriodoAcademicoService;
import pe.edu.sgfa.programa.domain.RacaCiclo;
import pe.edu.sgfa.programa.service.RacaCicloService;
import pe.edu.sgfa.programa.service.RacaCursoService;
import pe.edu.sgfa.programa.service.RacaPlanCursoService;
import pe.edu.sgfa.programa.service.RacaPlanEstudioService;
import pe.edu.sgfa.programa.service.RacaProgramaEstudioService;
import pe.edu.sgfa.usuario.service.RyeAdminBootstrapResult;
import pe.edu.sgfa.usuario.service.RyeAuthService;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RahpPdfRealMasivoResolucionTest {

    private static final File PDF_HNC = new File(System.getProperty("user.home"), "Downloads/pruebas ocr/pdf_masivo.pdf");

    @BeforeAll
    static void baseAislada() throws IOException {
        System.setProperty("sgfa.home-dir", Files.createTempDirectory("sgfa-real-test-home-").toString());
    }

    @Autowired
    private RacaInstitucionService institucionServiceHnc;
    @Autowired
    private RacaProgramaEstudioService programaEstudioServiceHnc;
    @Autowired
    private RacaPlanEstudioService planEstudioServiceHnc;
    @Autowired
    private RacaCicloService cicloServiceHnc;
    @Autowired
    private RacaCursoService cursoServiceHnc;
    @Autowired
    private RacaPlanCursoService planCursoServiceHnc;
    @Autowired
    private YmclPeriodoAcademicoService periodoAcademicoServiceHnc;
    @Autowired
    private RahpLectorReportesPdf lectorHnc;
    @Autowired
    private RahpResolutorReporte resolutorHnc;
    @Autowired
    private RyeAuthService authServiceHnc;
    @Autowired
    private RyeAdminBootstrapResult adminBootstrapResultHnc;

    private static RyeAdminBootstrapResult.RyeCredenciales credencialesHnc;
    private static RacaCiclo ciclo1Hnc;

    @BeforeEach
    void preparar() {
        Assumptions.assumeTrue(PDF_HNC.isFile(), "No esta " + PDF_HNC);
        if (credencialesHnc == null) {
            credencialesHnc = adminBootstrapResultHnc.ryeConsumir().orElseThrow();
        }
        authServiceHnc.ryeCerrarSesion();
        authServiceHnc.ryeAutenticar(credencialesHnc.username(), credencialesHnc.password());
        if (ciclo1Hnc == null) {
            institucionServiceHnc.racaGuardar("IESPP Juliaca", "JULI", null, null, null);

            programaEstudioServiceHnc.racaCrear("COM", "Educacion Secundaria - Especialidad Comunicacion", null, 10);
            programaEstudioServiceHnc.racaCrear("INI", "Educacion Inicial", null, 10);
            var matHnc = programaEstudioServiceHnc.racaCrear("MAT", "Educacion Secundaria - Especialidad Matematica", null, 10);
            var planHnc = planEstudioServiceHnc.racaCrear(matHnc.getId(), "076-2020", "RVM 076-2020 MINEDU", null, null);
            ciclo1Hnc = cicloServiceHnc.racaCrear(planHnc.getId(), 1, "Ciclo I", 1);
            int ordenHnc = 1;
            for (Object[] c : new Object[][] {
                    {"LECTURA Y ESCRITURA EN LA EDUCACIÓN SUPERIOR", 3}, {"RESOLUCIÓN DE PROBLEMAS MATEMÁTICOS I", 3},
                    {"HISTORIA, SOCIEDAD Y DIVERSIDAD", 3}, {"DESARROLLO PERSONAL I", 5},
                    {"PRÁCTICA E INVESTIGACIÓN I", 5}, {"EDUCACIÓN Y SOCIEDAD EN EL SIGLO XXI", 5}}) {
                var cursoHnc = cursoServiceHnc.racaCrear("MAT-I_CM0" + ordenHnc, (String) c[0], (Integer) c[1], null, null);
                planCursoServiceHnc.racaAsignar(planHnc.getId(), ciclo1Hnc.getId(), cursoHnc.getId(), ordenHnc++, true);
            }
            periodoAcademicoServiceHnc.ymclCrear("2026-I", 2026, null, null, null);
            periodoAcademicoServiceHnc.ymclCrear("2026-II", 2026, null, null, null);
        }
    }

    @Test
    void lasOchoHojasRealesQuedanListasParaGuardar() {
        List<RahpHojaLeida> hojasHnc = lectorHnc.rahpLeer(PDF_HNC, null);
        List<RahpPropuesta> propuestasHnc = resolutorHnc.rahpProponerTodos(hojasHnc.stream().map(RahpHojaLeida::reporte).toList());

        for (int i = 0; i < propuestasHnc.size(); i++) {
            RahpPropuesta p = propuestasHnc.get(i);
            System.out.println("RESOL>> H" + (i + 1) + " " + p.rahpEstado() + " " + p.matricula() + " " + p.apellidoPaterno()
                    + " | " + (p.ciclo() == null ? "?" : p.ciclo().getNombre()) + " " + (p.periodo() == null ? "?" : p.periodo().getNombre())
                    + " | cursos " + p.rahpCursosEmparejados() + "/" + p.cursos().size() + " prom " + p.promedioCalculado()
                    + "/" + p.promedioImpreso() + " | avisos=" + p.avisos() + " notas=" + p.notas());
        }

        assertThat(propuestasHnc).extracting(RahpPropuesta::rahpEstado).containsOnly(RahpEstado.LISTO);
    }
}
