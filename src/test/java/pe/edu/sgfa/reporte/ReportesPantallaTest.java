package pe.edu.sgfa.reporte;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;

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
import pe.edu.sgfa.programa.repository.PlanCursoRepository;
import pe.edu.sgfa.programa.service.CicloService;
import pe.edu.sgfa.programa.service.CursoService;
import pe.edu.sgfa.programa.service.PlanCursoService;
import pe.edu.sgfa.programa.service.PlanEstudioService;
import pe.edu.sgfa.programa.service.ProgramaEstudioService;
import pe.edu.sgfa.reporte.ui.ReportesController;
import pe.edu.sgfa.shell.SpringFxmlLoaderFactory;
import pe.edu.sgfa.trayectoria.service.TrayectoriaService;
import pe.edu.sgfa.usuario.service.AdminBootstrapResult;
import pe.edu.sgfa.usuario.service.AuthService;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ReportesPantallaTest {

    @BeforeAll
    static void baseAislada() throws IOException {
        System.setProperty("sgfa.home-dir", Files.createTempDirectory("sgfa-pantalla-reportes-home-").toString());
    }

    @Autowired
    private SpringFxmlLoaderFactory fxml;
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
    private static boolean javafxListo;
    private static PlanEstudio plan;
    private static Ciclo ciclo1;
    private static PeriodoAcademico periodo;

    @BeforeEach
    void preparar() {
        if (credenciales == null) {
            credenciales = adminBootstrapResult.consumir().orElseThrow();
        }
        authService.cerrarSesion();
        authService.autenticar(credenciales.username(), credenciales.password());
        if (!javafxListo) {
            try {
                CompletableFuture<Void> arrnq = new CompletableFuture<>();
                Platform.startup(() -> arrnq.complete(null));
                arrnq.get(20, TimeUnit.SECONDS);
            } catch (IllegalStateException yaIncd) {

            } catch (Exception | Error e) {
                Assumptions.abort("JavaFX no se pudo iniciar en este entorno: " + e);
            }
            javafxListo = true;
        }
        if (armado) {
            return;
        }
        institucionService.guardar("IESPP Juliaca", "JULI", null, null, null);
        var programa = programaEstudioService.crear("PNT", "Educacion Pantalla", null, 10);
        plan = planEstudioService.crear(programa.getId(), "PN-1", "Plan pantalla", null, null);
        ciclo1 = cicloService.crear(plan.getId(), 1, "Ciclo I", 1);
        int ordn = 1;
        for (String nmbr : List.of("ARTE", "BIOLOGIA")) {
            var crs = cursoService.crear("PNT-" + ordn, nmbr, 4, null, null);
            planCursoService.asignar(plan.getId(), ciclo1.getId(), crs.getId(), ordn++, true);
        }
        periodo = periodoAcademicoService.crear("2050-I", 2050, null, null, null);
        estudiante("90000001", "ALVA", 16.0, 17.0);
        estudiante("90000002", "BENITES", 15.0, 8.0);
        estudiante("90000003", "CALLA", 15.0, 15.0);
        armado = true;
    }

    private void estudiante(String dni, String ptrn, double... notas) {
        var e = estudianteService.crear(dni, dni, "Nombre", ptrn, "X", null, null, null, null, null);
        var t = trayectoriaService.iniciar(e.getId(), plan.getPrograma().getId(), plan.getId(), null, "prueba");
        List<PlanCurso> crss = planCursoRepository.findByCicloIdOrderByOrdenAsc(ciclo1.getId());
        List<ItemNota> itms = new java.util.ArrayList<>();
        for (int i = 0; i < notas.length; i++) {
            Long stcn = situacionRepository.findByCodigo(EscalaLiteral.codigoPara(notas[i])).orElseThrow().getId();
            itms.add(new ItemNota(crss.get(i).getId(), notas[i], stcn, null));
        }
        registroAcademicoService.guardarMasivo(t.getId(), periodo.getId(), itms);
    }

    private <T> T enHiloFx(Supplier<T> accn) throws Exception {
        CompletableFuture<T> rsltd = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                rsltd.complete(accn.get());
            } catch (Throwable t) {
                rsltd.completeExceptionally(t);
            }
        });
        return rsltd.get(30, TimeUnit.SECONDS);
    }

    @SuppressWarnings("unchecked")
    private static <T> T campo(Object cntrldr, String nmbr) {
        return (T) ReflectionTestUtils.getField(cntrldr, nmbr);
    }

    private ReportesController abrir() throws Exception {
        return enHiloFx(() -> {
            try {
                FXMLLoader ldr = fxml.create("/fxml/reporte/reportes.fxml");
                ldr.load();
                return ldr.<ReportesController>getController();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    private void generar(ReportesController c, int reporte, String lmt) throws Exception {
        enHiloFx(() -> {
            ComboBox<Object> rprts = campo(c, "reporteCombo");
            rprts.getSelectionModel().select(reporte);
            ComboBox<Object> prgrms = campo(c, "programaCombo");
            prgrms.getSelectionModel().select(prgrms.getItems().stream()
                    .filter(p -> ((pe.edu.sgfa.programa.domain.ProgramaEstudio) p).getCodigo().equals("PNT"))
                    .findFirst().orElseThrow());
            ComboBox<Object> prds = campo(c, "periodoCombo");
            prds.getSelectionModel().select(prds.getItems().stream()
                    .filter(p -> ((PeriodoAcademico) p).getNombre().equals("2050-I")).findFirst().orElseThrow());
            TextField cmpLmt = campo(c, "limiteField");
            cmpLmt.setText(lmt);
            Button generar = campo(c, "generarButton");
            generar.fire();
            return null;
        });
        for (int i = 0; i < 100; i++) {
            Thread.sleep(100);
            Button generar = campo(c, "generarButton");
            boolean lbr = enHiloFx(() -> !generar.isDisabled());
            if (lbr) {
                return;
            }
        }
    }

    private int filas(ReportesController c) throws Exception {
        TableView<?> tbl = campo(c, "tabla");
        return enHiloFx(() -> tbl.getItems().size());
    }

    private boolean deshabilitado(ReportesController c, String campo) throws Exception {
        javafx.scene.Node nodo = campo(c, campo);
        return enHiloFx(nodo::isDisable);
    }

    @Test
    void elActaSeGeneraYSePuedeEmitirComoOficial() throws Exception {
        ReportesController c = abrir();

        generar(c, 0, "");

        assertThat(filas(c)).isEqualTo(4);
        assertThat(deshabilitado(c, "excelButton")).isFalse();
        assertThat(deshabilitado(c, "pdfButton")).isFalse();
        assertThat(deshabilitado(c, "oficialButton")).isFalse();
    }

    @Test
    void elOrdenDeMeritoMuestraSusOpcionesYSeGeneraConLimite() throws Exception {
        ReportesController c = abrir();
        enHiloFx(() -> {
            c.elegirOrdenMerito();
            return null;
        });
        javafx.scene.layout.HBox opciones = campo(c, "opcionesMeritoBox");
        assertThat(enHiloFx(opciones::isVisible)).isTrue();

        generar(c, 1, "");
        assertThat(filas(c)).isEqualTo(3);
        assertThat(deshabilitado(c, "oficialButton")).isFalse();

        generar(c, 1, "1");
        assertThat(filas(c)).isEqualTo(1);
    }

    @Test
    void elOrdenDeMeritoGeneralNoPideCicloNiPeriodo() throws Exception {
        ReportesController c = abrir();
        enHiloFx(() -> {
            c.elegirOrdenMerito();
            ComboBox<Object> ambt = campo(c, "ambitoCombo");
            ambt.getSelectionModel().select(2);
            return null;
        });

        assertThat(deshabilitado(c, "cicloCombo")).isTrue();
        assertThat(deshabilitado(c, "periodoCombo")).isTrue();
        generar(c, 1, "");
        assertThat(filas(c)).isEqualTo(3);
    }

    @Test
    void losReportesInternosNoSeEmitenComoOficiales() throws Exception {
        ReportesController c = abrir();

        generar(c, 2, "");

        assertThat(filas(c)).isEqualTo(3);
        assertThat(deshabilitado(c, "excelButton")).isFalse();
        assertThat(deshabilitado(c, "oficialButton")).isTrue();
    }

    @Test
    void enPendientesElCicloSePuedeDejarEnTodos() throws Exception {
        ReportesController c = abrir();
        enHiloFx(() -> {
            ComboBox<Object> rprts = campo(c, "reporteCombo");
            rprts.getSelectionModel().select(5);
            return null;
        });
        CheckBox tds = campo(c, "todosCiclosCheck");

        assertThat(enHiloFx(tds::isVisible)).isTrue();
        enHiloFx(() -> {
            tds.setSelected(true);
            return null;
        });
        assertThat(deshabilitado(c, "cicloCombo")).isTrue();
    }
}
