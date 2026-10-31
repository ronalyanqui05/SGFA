package pe.edu.sgfa.ocr;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javafx.application.Platform;
import javafx.scene.Parent;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import pe.edu.sgfa.institucion.service.RacaInstitucionService;
import pe.edu.sgfa.shell.RyeSpringFxmlLoaderFactory;
import pe.edu.sgfa.usuario.service.RyeAdminBootstrapResult;
import pe.edu.sgfa.usuario.service.RyeAuthService;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RahpPantallasOcrCargaTest {

    @BeforeAll
    static void baseAislada() throws IOException {
        System.setProperty("sgfa.home-dir", Files.createTempDirectory("sgfa-pantallas-test-home-").toString());
    }

    @Autowired
    private RyeSpringFxmlLoaderFactory fxmlHnc;
    @Autowired
    private RyeAuthService authServiceHnc;
    @Autowired
    private RyeAdminBootstrapResult adminBootstrapResultHnc;
    @Autowired
    private RacaInstitucionService institucionServiceHnc;

    private static RyeAdminBootstrapResult.RyeCredenciales credencialesHnc;
    private static boolean javafxListoHnc;

    @BeforeEach
    void preparar() {
        if (credencialesHnc == null) {
            credencialesHnc = adminBootstrapResultHnc.ryeConsumir().orElseThrow();
        }
        authServiceHnc.ryeCerrarSesion();
        authServiceHnc.ryeAutenticar(credencialesHnc.username(), credencialesHnc.password());
        if (institucionServiceHnc.racaObtenerPrincipal().isEmpty()) {
            institucionServiceHnc.racaGuardar("IESPP Juliaca", "JULI", "Av. Principal 123", null, null);
        }
        if (!javafxListoHnc) {
            try {
                CompletableFuture<Void> arranqueHnc = new CompletableFuture<>();
                Platform.startup(() -> arranqueHnc.complete(null));
                arranqueHnc.get(20, TimeUnit.SECONDS);
            } catch (IllegalStateException yaIniciadoHnc) {

            } catch (Exception | Error e) {
                Assumptions.abort("JavaFX no se pudo iniciar en este entorno: " + e);
            }
            javafxListoHnc = true;
        }
    }

    private <T> T enHiloFx(Supplier<T> accionHnc) throws Exception {
        CompletableFuture<T> resultadoHnc = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                resultadoHnc.complete(accionHnc.get());
            } catch (Throwable t) {
                resultadoHnc.completeExceptionally(t);
            }
        });
        return resultadoHnc.get(30, TimeUnit.SECONDS);
    }

    private Parent cargar(String rutaHnc) throws Exception {
        return enHiloFx(() -> {
            try {
                return fxmlHnc.ryeCreate(rutaHnc).load();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    @Test
    void cargaLaPantallaDeImportacion() throws Exception {
        assertThat(cargar("/fxml/importacion/importacion.fxml")).isNotNull();
    }

    @Test
    void cargaLaPantallaDeUnReporte() throws Exception {
        assertThat(cargar("/fxml/ocr/importar-reporte.fxml")).isNotNull();
    }

    @Test
    void cargaLaPantallaDelLoteDePdf() throws Exception {
        assertThat(cargar("/fxml/ocr/importar-reportes-lote.fxml")).isNotNull();
    }

    @Test
    void cargaElCentroDeReportes() throws Exception {
        assertThat(cargar("/fxml/reporte/reportes.fxml")).isNotNull();
    }

    @Test
    void cargaElHistorialDeDocumentosEmitidos() throws Exception {
        assertThat(cargar("/fxml/reporte/documentos.fxml")).isNotNull();
    }

    @Test
    void cargaLaVistaPreviaDeLaFichaConElVisorNuevo() throws Exception {
        assertThat(cargar("/fxml/ficha/vista-previa-ficha.fxml")).isNotNull();
    }

    @Test
    void cargaLaVistaPreviaDeCualquierPdf() throws Exception {
        assertThat(cargar("/fxml/reporte/vista-previa-pdf.fxml")).isNotNull();
    }

    @Test
    void cargaElDashboardConElBotonDeReportes() throws Exception {
        assertThat(cargar("/fxml/dashboard/dashboard.fxml")).isNotNull();
    }

    @Test
    void cargaLaPantallaDelActaConsolidada() throws Exception {
        assertThat(cargar("/fxml/ocr/importar-acta.fxml")).isNotNull();
    }
}
