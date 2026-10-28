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

import pe.edu.sgfa.institucion.service.InstitucionService;
import pe.edu.sgfa.shell.SpringFxmlLoaderFactory;
import pe.edu.sgfa.usuario.service.AdminBootstrapResult;
import pe.edu.sgfa.usuario.service.AuthService;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PantallasOcrCargaTest {

    @BeforeAll
    static void baseAislada() throws IOException {
        System.setProperty("sgfa.home-dir", Files.createTempDirectory("sgfa-pantallas-test-home-").toString());
    }

    @Autowired
    private SpringFxmlLoaderFactory fxml;
    @Autowired
    private AuthService authService;
    @Autowired
    private AdminBootstrapResult adminBootstrapResult;
    @Autowired
    private InstitucionService institucionService;

    private static AdminBootstrapResult.Credenciales credenciales;
    private static boolean javafxListo;

    @BeforeEach
    void preparar() {
        if (credenciales == null) {
            credenciales = adminBootstrapResult.consumir().orElseThrow();
        }
        authService.cerrarSesion();
        authService.autenticar(credenciales.username(), credenciales.password());
        if (institucionService.obtenerPrincipal().isEmpty()) {
            institucionService.guardar("IESPP Juliaca", "JULI", "Av. Principal 123", null, null);
        }
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

    private Parent cargar(String rt) throws Exception {
        return enHiloFx(() -> {
            try {
                return fxml.create(rt).load();
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
