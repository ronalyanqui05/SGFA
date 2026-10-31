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
import pe.edu.sgfa.programa.repository.RacaPlanCursoRepository;
import pe.edu.sgfa.programa.service.RacaCicloService;
import pe.edu.sgfa.programa.service.RacaCursoService;
import pe.edu.sgfa.programa.service.RacaPlanCursoService;
import pe.edu.sgfa.programa.service.RacaPlanEstudioService;
import pe.edu.sgfa.programa.service.RacaProgramaEstudioService;
import pe.edu.sgfa.reporte.ui.CphReportesController;
import pe.edu.sgfa.shell.RyeSpringFxmlLoaderFactory;
import pe.edu.sgfa.trayectoria.service.YmclTrayectoriaService;
import pe.edu.sgfa.usuario.service.RyeAdminBootstrapResult;
import pe.edu.sgfa.usuario.service.RyeAuthService;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CphReportesPantallaTest {

    @BeforeAll
    static void baseAislada() throws IOException {
        System.setProperty("sgfa.home-dir", Files.createTempDirectory("sgfa-pantalla-reportes-home-").toString());
    }

    @Autowired
    private RyeSpringFxmlLoaderFactory fxmlPch;
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
    private static boolean javafxListoPch;
    private static RacaPlanEstudio planPch;
    private static RacaCiclo ciclo1Pch;
    private static YmclPeriodoAcademico periodoPch;

    @BeforeEach
    void preparar() {
        if (credencialesPch == null) {
            credencialesPch = adminBootstrapResultPch.ryeConsumir().orElseThrow();
        }
        authServicePch.ryeCerrarSesion();
        authServicePch.ryeAutenticar(credencialesPch.username(), credencialesPch.password());
        if (!javafxListoPch) {
            try {
                CompletableFuture<Void> arranquePch = new CompletableFuture<>();
                Platform.startup(() -> arranquePch.complete(null));
                arranquePch.get(20, TimeUnit.SECONDS);
            } catch (IllegalStateException yaIniciadoPch) {

            } catch (Exception | Error e) {
                Assumptions.abort("JavaFX no se pudo iniciar en este entorno: " + e);
            }
            javafxListoPch = true;
        }
        if (armadoPch) {
            return;
        }
        institucionServicePch.racaGuardar("IESPP Juliaca", "JULI", null, null, null);
        var programaPch = programaEstudioServicePch.racaCrear("PNT", "Educacion Pantalla", null, 10);
        planPch = planEstudioServicePch.racaCrear(programaPch.getId(), "PN-1", "Plan pantalla", null, null);
        ciclo1Pch = cicloServicePch.racaCrear(planPch.getId(), 1, "Ciclo I", 1);
        int ordenPch = 1;
        for (String nombrePch : List.of("ARTE", "BIOLOGIA")) {
            var cursoPch = cursoServicePch.racaCrear("PNT-" + ordenPch, nombrePch, 4, null, null);
            planCursoServicePch.racaAsignar(planPch.getId(), ciclo1Pch.getId(), cursoPch.getId(), ordenPch++, true);
        }
        periodoPch = periodoAcademicoServicePch.ymclCrear("2050-I", 2050, null, null, null);
        estudiante("90000001", "ALVA", 16.0, 17.0);
        estudiante("90000002", "BENITES", 15.0, 8.0);
        estudiante("90000003", "CALLA", 15.0, 15.0);
        armadoPch = true;
    }

    private void estudiante(String dniPch, String paternoPch, double... notasPch) {
        var e = estudianteServicePch.ymclCrear(dniPch, dniPch, "Nombre", paternoPch, "X", null, null, null, null, null);
        var t = trayectoriaServicePch.ymclIniciar(e.getId(), planPch.getPrograma().getId(), planPch.getId(), null, "prueba");
        List<RacaPlanCurso> cursosPch = planCursoRepositoryPch.findByCicloIdOrderByOrdenAsc(ciclo1Pch.getId());
        List<YmclItemNota> itemsPch = new java.util.ArrayList<>();
        for (int i = 0; i < notasPch.length; i++) {
            Long situacionPch = situacionRepositoryPch.findByCodigo(YmclEscalaLiteral.ymclCodigoPara(notasPch[i])).orElseThrow().getId();
            itemsPch.add(new YmclItemNota(cursosPch.get(i).getId(), notasPch[i], situacionPch, null));
        }
        registroAcademicoServicePch.ymclGuardarMasivo(t.getId(), periodoPch.getId(), itemsPch);
    }

    private <T> T enHiloFx(Supplier<T> accionPch) throws Exception {
        CompletableFuture<T> resultadoPch = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                resultadoPch.complete(accionPch.get());
            } catch (Throwable t) {
                resultadoPch.completeExceptionally(t);
            }
        });
        return resultadoPch.get(30, TimeUnit.SECONDS);
    }

    @SuppressWarnings("unchecked")
    private static <T> T campo(Object controladorPch, String nombrePch) {
        return (T) ReflectionTestUtils.getField(controladorPch, nombrePch);
    }

    private CphReportesController abrir() throws Exception {
        return enHiloFx(() -> {
            try {
                FXMLLoader loaderPch = fxmlPch.ryeCreate("/fxml/reporte/reportes.fxml");
                loaderPch.load();
                return loaderPch.<CphReportesController>getController();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    private void generar(CphReportesController c, int reportePch, String limitePch) throws Exception {
        enHiloFx(() -> {
            ComboBox<Object> reportesPch = campo(c, "reporteCombo");
            reportesPch.getSelectionModel().select(reportePch);
            ComboBox<Object> programasPch = campo(c, "programaCombo");
            programasPch.getSelectionModel().select(programasPch.getItems().stream()
                    .filter(p -> ((pe.edu.sgfa.programa.domain.RacaProgramaEstudio) p).getCodigo().equals("PNT"))
                    .findFirst().orElseThrow());
            ComboBox<Object> periodosPch = campo(c, "periodoCombo");
            periodosPch.getSelectionModel().select(periodosPch.getItems().stream()
                    .filter(p -> ((YmclPeriodoAcademico) p).getNombre().equals("2050-I")).findFirst().orElseThrow());
            TextField campoLimitePch = campo(c, "limiteField");
            campoLimitePch.setText(limitePch);
            Button generarPch = campo(c, "generarButton");
            generarPch.fire();
            return null;
        });
        for (int i = 0; i < 100; i++) {
            Thread.sleep(100);
            Button generarPch = campo(c, "generarButton");
            boolean librePch = enHiloFx(() -> !generarPch.isDisabled());
            if (librePch) {
                return;
            }
        }
    }

    private int filas(CphReportesController c) throws Exception {
        TableView<?> tablaPch = campo(c, "tabla");
        return enHiloFx(() -> tablaPch.getItems().size());
    }

    private boolean deshabilitado(CphReportesController c, String campoPch) throws Exception {
        javafx.scene.Node nodoPch = campo(c, campoPch);
        return enHiloFx(nodoPch::isDisable);
    }

    @Test
    void elActaSeGeneraYSePuedeEmitirComoOficial() throws Exception {
        CphReportesController c = abrir();

        generar(c, 0, "");

        assertThat(filas(c)).isEqualTo(4);
        assertThat(deshabilitado(c, "excelButton")).isFalse();
        assertThat(deshabilitado(c, "pdfButton")).isFalse();
        assertThat(deshabilitado(c, "oficialButton")).isFalse();
    }

    @Test
    void elOrdenDeMeritoMuestraSusOpcionesYSeGeneraConLimite() throws Exception {
        CphReportesController c = abrir();
        enHiloFx(() -> {
            c.cphElegirOrdenMerito();
            return null;
        });
        javafx.scene.layout.HBox opcionesPch = campo(c, "opcionesMeritoBox");
        assertThat(enHiloFx(opcionesPch::isVisible)).isTrue();

        generar(c, 1, "");
        assertThat(filas(c)).isEqualTo(3);
        assertThat(deshabilitado(c, "oficialButton")).isFalse();

        generar(c, 1, "1");
        assertThat(filas(c)).isEqualTo(1);
    }

    @Test
    void elOrdenDeMeritoGeneralNoPideCicloNiPeriodo() throws Exception {
        CphReportesController c = abrir();
        enHiloFx(() -> {
            c.cphElegirOrdenMerito();
            ComboBox<Object> ambitoPch = campo(c, "ambitoCombo");
            ambitoPch.getSelectionModel().select(2);
            return null;
        });

        assertThat(deshabilitado(c, "cicloCombo")).isTrue();
        assertThat(deshabilitado(c, "periodoCombo")).isTrue();
        generar(c, 1, "");
        assertThat(filas(c)).isEqualTo(3);
    }

    @Test
    void losReportesInternosNoSeEmitenComoOficiales() throws Exception {
        CphReportesController c = abrir();

        generar(c, 2, "");

        assertThat(filas(c)).isEqualTo(3);
        assertThat(deshabilitado(c, "excelButton")).isFalse();
        assertThat(deshabilitado(c, "oficialButton")).isTrue();
    }

    @Test
    void enPendientesElCicloSePuedeDejarEnTodos() throws Exception {
        CphReportesController c = abrir();
        enHiloFx(() -> {
            ComboBox<Object> reportesPch = campo(c, "reporteCombo");
            reportesPch.getSelectionModel().select(5);
            return null;
        });
        CheckBox todosPch = campo(c, "todosCiclosCheck");

        assertThat(enHiloFx(todosPch::isVisible)).isTrue();
        enHiloFx(() -> {
            todosPch.setSelected(true);
            return null;
        });
        assertThat(deshabilitado(c, "cicloCombo")).isTrue();
    }
}
