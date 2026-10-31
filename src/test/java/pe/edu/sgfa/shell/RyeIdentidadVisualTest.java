package pe.edu.sgfa.shell;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Label;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javax.imageio.ImageIO;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;

import pe.edu.sgfa.institucion.service.RacaInstitucionService;
import pe.edu.sgfa.usuario.service.RyeAdminBootstrapResult;
import pe.edu.sgfa.usuario.service.RyeAuthService;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RyeIdentidadVisualTest {

    @BeforeAll
    static void baseAislada() throws IOException {
        System.setProperty("sgfa.home-dir", Files.createTempDirectory("sgfa-identidad-test-home-").toString());
    }

    @Autowired
    private RyeIdentidadVisual identidadYnq;
    @Autowired
    private RyeNavigationService navigationServiceYnq;
    @Autowired
    private RyeSpringFxmlLoaderFactory fxmlYnq;
    @Autowired
    private RacaInstitucionService institucionServiceYnq;
    @Autowired
    private RyeAuthService authServiceYnq;
    @Autowired
    private RyeAdminBootstrapResult adminBootstrapResultYnq;

    private static RyeAdminBootstrapResult.RyeCredenciales credencialesYnq;
    private static boolean javafxListoYnq;

    @BeforeEach
    void preparar() {
        if (credencialesYnq == null) {
            credencialesYnq = adminBootstrapResultYnq.ryeConsumir().orElseThrow();
        }
        authServiceYnq.ryeCerrarSesion();
        authServiceYnq.ryeAutenticar(credencialesYnq.username(), credencialesYnq.password());
        if (institucionServiceYnq.racaObtenerPrincipal().isEmpty()) {
            institucionServiceYnq.racaGuardar("IESPP Juliaca", "JULI", null, null, null);
        }
        if (!javafxListoYnq) {
            try {
                CompletableFuture<Void> arranqueYnq = new CompletableFuture<>();
                Platform.startup(() -> arranqueYnq.complete(null));
                arranqueYnq.get(20, TimeUnit.SECONDS);
            } catch (IllegalStateException yaIniciadoYnq) {

            } catch (Exception | Error e) {
                Assumptions.abort("JavaFX no se pudo iniciar en este entorno: " + e);
            }
            javafxListoYnq = true;
        }
    }

    private static <T> T enHiloFx(Supplier<T> accionYnq) throws Exception {
        CompletableFuture<T> resultadoYnq = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                resultadoYnq.complete(accionYnq.get());
            } catch (Throwable t) {
                resultadoYnq.completeExceptionally(t);
            }
        });
        return resultadoYnq.get(30, TimeUnit.SECONDS);
    }

    private static Path fondo(Color colorYnq, int anchoYnq, int altoYnq) throws IOException {
        BufferedImage imgYnq = new BufferedImage(anchoYnq, altoYnq, BufferedImage.TYPE_INT_RGB);
        var g = imgYnq.createGraphics();
        g.setColor(colorYnq);
        g.fillRect(0, 0, anchoYnq, altoYnq);
        g.dispose();
        Path archivoYnq = Files.createTempFile("fondo-prueba-", ".png");
        ImageIO.write(imgYnq, "png", archivoYnq.toFile());
        return archivoYnq;
    }

    @Test
    void fondoClaroPideLetrasOscurasYFondoOscuroLetrasBlancas() throws Exception {
        institucionServiceYnq.racaActualizarFondoLogin(fondo(new Color(245, 245, 235), 1280, 720));
        enHiloFx(() -> { identidadYnq.ryeRecargar(); return null; });
        assertThat(identidadYnq.fondoLoginProperty().get()).isNotNull();
        assertThat(identidadYnq.fondoLoginClaroProperty().get()).as("foto clara -> letras oscuras").isTrue();

        institucionServiceYnq.racaActualizarFondoLogin(fondo(new Color(20, 30, 60), 1280, 720));
        enHiloFx(() -> { identidadYnq.ryeRecargar(); return null; });
        assertThat(identidadYnq.fondoLoginClaroProperty().get()).as("foto oscura -> letras blancas").isFalse();

        var medioYnq = fondoCalculado(new Color(140, 140, 140));
        assertThat(RyeIdentidadVisual.ryeEsClara(medioYnq)).isTrue();
        var conVeloYnq = RyeIdentidadVisual.ryeConVelo(medioYnq, true);
        assertThat(RyeIdentidadVisual.ryeLuminanciaMedia(conVeloYnq)).isGreaterThan(0.5);
    }

    private static BufferedImage fondoCalculado(Color colorYnq) {
        BufferedImage imgYnq = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        var g = imgYnq.createGraphics();
        g.setColor(colorYnq);
        g.fillRect(0, 0, 100, 100);
        g.dispose();
        return imgYnq;
    }

    @Test
    void sinFondoSeUsaElPorDefectoYSeValidaLaImagen() throws Exception {
        institucionServiceYnq.racaActualizarFondoLogin(fondo(Color.DARK_GRAY, 800, 600));
        institucionServiceYnq.racaQuitarFondoLogin();
        enHiloFx(() -> { identidadYnq.ryeRecargar(); return null; });
        assertThat(identidadYnq.fondoLoginProperty().get()).isNull();
        assertThat(identidadYnq.fondoLoginClaroProperty().get()).isFalse();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> institucionServiceYnq.racaActualizarFondoLogin(fondo(Color.RED, 200, 100)))
                .hasMessageContaining("muy chica");
        Path noImagenYnq = Files.createTempFile("falso-", ".png");
        Files.writeString(noImagenYnq, "esto no es una imagen");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> institucionServiceYnq.racaActualizarFondoLogin(noImagenYnq))
                .hasMessageContaining("no es una imagen");
    }

    @Test
    void elLoginMuestraElFondoYCambiaElColorDeLasLetras() throws Exception {
        institucionServiceYnq.racaActualizarFondoLogin(fondo(new Color(250, 250, 250), 1280, 720));
        enHiloFx(() -> { identidadYnq.ryeRecargar(); return null; });
        javafx.scene.layout.VBox panelYnq = enHiloFx(() -> {
            try {
                javafx.fxml.FXMLLoader loaderYnq = fxmlYnq.ryeCreate("/fxml/auth/login.fxml");
                javafx.scene.Parent raizYnq = loaderYnq.load();
                return (javafx.scene.layout.VBox) raizYnq.lookup(".login-brand-panel");
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
        assertThat(panelYnq.getStyle()).contains("-fx-background-image").contains("sgfa-fondo-login-");
        assertThat(panelYnq.getStyleClass()).contains("con-fondo", "fondo-claro");

        institucionServiceYnq.racaQuitarFondoLogin();
        enHiloFx(() -> { identidadYnq.ryeRecargar(); return null; });
        assertThat(panelYnq.getStyle()).as("al quitarlo vuelve al color por defecto").isEmpty();
        assertThat(panelYnq.getStyleClass()).doesNotContain("con-fondo", "fondo-claro");
    }

    private static Path logo(Color colorYnq) throws IOException {
        BufferedImage imgYnq = new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB);
        var g = imgYnq.createGraphics();
        g.setColor(colorYnq);
        g.fillRect(0, 0, 200, 100);
        g.dispose();
        Path archivoYnq = Files.createTempFile("logo-prueba-", ".png");
        ImageIO.write(imgYnq, "png", archivoYnq.toFile());
        return archivoYnq;
    }

    private static int rgb(Image imagenYnq, int x, int y) {
        return imagenYnq.getPixelReader().getArgb(x, y);
    }

    @Test
    void sinLogoLaAppTieneUnIconoPorDefectoCuadrado() throws Exception {
        enHiloFx(() -> {
            identidadYnq.ryeRecargar();
            return null;
        });

        assertThat(enHiloFx(() -> identidadYnq.logoProperty().get())).isNull();
        var iconosYnq = enHiloFx(() -> RyeList.copyOf(identidadYnq.ryeIconos()));
        assertThat(iconosYnq).hasSize(7);
        assertThat(iconosYnq).extracting(i -> (int) i.getWidth()).containsExactly(16, 24, 32, 48, 64, 128, 256);
        assertThat(iconosYnq).allSatisfy(i -> assertThat(i.getHeight()).isEqualTo(i.getWidth()));

        Image grandeYnq = iconosYnq.get(6);
        assertThat((rgb(grandeYnq, 128, 20) >>> 24) & 0xFF).isEqualTo(255);
    }

    private static class RyeList {
        static <T> java.util.List<T> copyOf(java.util.Collection<T> c) {
            return java.util.List.copyOf(c);
        }
    }

    @Test
    void conLogoLosIconosSonCuadradosYNoDeformanElLogo() throws Exception {
        institucionServiceYnq.racaActualizarLogo(logo(Color.RED));
        enHiloFx(() -> {
            identidadYnq.ryeRecargar();
            return null;
        });

        Image logoYnq = enHiloFx(() -> identidadYnq.logoProperty().get());
        assertThat(logoYnq).isNotNull();
        assertThat((int) logoYnq.getWidth()).isEqualTo(200);
        Image grandeYnq = enHiloFx(() -> identidadYnq.ryeIconos().get(6));
        assertThat((int) grandeYnq.getWidth()).isEqualTo(256);
        assertThat((int) grandeYnq.getHeight()).isEqualTo(256);

        assertThat(rgb(grandeYnq, 128, 128) & 0x00FFFFFF).isEqualTo(0xFF0000);
        assertThat((rgb(grandeYnq, 128, 10) >>> 24) & 0xFF).isZero();
    }

    @Test
    void laVentanaPrincipalSigueAlLogoYSeActualizaAlCambiarlo() throws Exception {
        institucionServiceYnq.racaActualizarLogo(logo(Color.RED));
        Stage ventanaYnq = enHiloFx(() -> {
            Stage s = new Stage();
            navigationServiceYnq.ryeInicializar(s);
            return s;
        });
        assertThat(enHiloFx(() -> ventanaYnq.getIcons().size())).isEqualTo(7);
        int antesYnq = enHiloFx(() -> rgb(ventanaYnq.getIcons().get(6), 128, 128) & 0x00FFFFFF);
        assertThat(antesYnq).isEqualTo(0xFF0000);

        institucionServiceYnq.racaActualizarLogo(logo(Color.BLUE));
        enHiloFx(() -> {
            identidadYnq.ryeRecargar();
            return null;
        });

        assertThat(enHiloFx(() -> ventanaYnq.getIcons().size())).isEqualTo(7);
        assertThat(enHiloFx(() -> rgb(ventanaYnq.getIcons().get(6), 128, 128) & 0x00FFFFFF)).isEqualTo(0x0000FF);
    }

    private interface RyeUser32 extends com.sun.jna.Library {
        RyeUser32 INSTANCE = Native.load("user32", RyeUser32.class);

        Pointer FindWindowW(WString claseYnq, WString tituloYnq);

        long SendMessageW(Pointer ventanaYnq, int mensajeYnq, long wParamYnq, long lParamYnq);
    }

    private boolean windowsLeAsignoIcono(String tituloYnq) throws Exception {
        for (int intentoYnq = 0; intentoYnq < 20; intentoYnq++) {
            Pointer ventanaYnq = RyeUser32.INSTANCE.FindWindowW(null, new WString(tituloYnq));
            if (ventanaYnq != null) {
                long pequenoYnq = RyeUser32.INSTANCE.SendMessageW(ventanaYnq, 0x007F, 0, 0);
                long grandeYnq = RyeUser32.INSTANCE.SendMessageW(ventanaYnq, 0x007F, 1, 0);
                return pequenoYnq != 0 && grandeYnq != 0;
            }
            Thread.sleep(250);
        }
        throw new AssertionError("Windows no encontro la ventana \"" + tituloYnq + "\"");
    }

    @Test
    void windowsRealmenteMuestraElLogoComoIconoDeLaVentana() throws Exception {
        Assumptions.assumeTrue(System.getProperty("os.name").toLowerCase().contains("win"), "Solo en Windows");
        institucionServiceYnq.racaActualizarLogo(logo(Color.RED));
        enHiloFx(() -> {
            Platform.setImplicitExit(false);
            identidadYnq.ryeRecargar();
            return null;
        });
        String tituloYnq = "SGFA-ICONO-" + System.nanoTime();
        Stage ventanaYnq = enHiloFx(() -> {
            Stage s = new Stage();
            navigationServiceYnq.ryeInicializar(s);
            s.setTitle(tituloYnq);
            s.setScene(new Scene(new StackPane(), 300, 100));
            s.show();
            return s;
        });
        try {
            assertThat(windowsLeAsignoIcono(tituloYnq)).isTrue();
        } finally {
            enHiloFx(() -> {
                ventanaYnq.close();
                return null;
            });
        }
    }

    @Test
    void tambienElIconoPorDefectoSinLogoLoAsignaWindows() throws Exception {
        Assumptions.assumeTrue(System.getProperty("os.name").toLowerCase().contains("win"), "Solo en Windows");
        String tituloYnq = "SGFA-ICONO-DEFECTO-" + System.nanoTime();
        Stage ventanaYnq = enHiloFx(() -> {
            Platform.setImplicitExit(false);
            Stage s = new Stage();
            s.getIcons().setAll(RyeIdentidadVisual.ryeIcono(null, 16), RyeIdentidadVisual.ryeIcono(null, 32), RyeIdentidadVisual.ryeIcono(null, 256));
            s.setTitle(tituloYnq);
            s.setScene(new Scene(new StackPane(), 300, 100));
            s.show();
            return s;
        });
        try {
            assertThat(windowsLeAsignoIcono(tituloYnq)).isTrue();
        } finally {
            enHiloFx(() -> {
                ventanaYnq.close();
                return null;
            });
        }
    }

    private Object cargar(String rutaYnq) throws Exception {
        return enHiloFx(() -> {
            try {
                FXMLLoader loaderYnq = fxmlYnq.ryeCreate(rutaYnq);
                loaderYnq.load();
                return loaderYnq.getController();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    @Test
    void elMenuLateralMuestraElLogoDeLaInstitucion() throws Exception {
        institucionServiceYnq.racaActualizarLogo(logo(Color.RED));
        enHiloFx(() -> {
            identidadYnq.ryeRecargar();
            return null;
        });

        Object dashboardYnq = cargar("/fxml/dashboard/dashboard.fxml");

        ImageView imagenYnq = (ImageView) ReflectionTestUtils.getField(dashboardYnq, "marcaImagen");
        Label letraYnq = (Label) ReflectionTestUtils.getField(dashboardYnq, "marcaLetra");
        assertThat(enHiloFx(imagenYnq::isVisible)).isTrue();
        assertThat(enHiloFx(() -> imagenYnq.getImage() != null)).isTrue();
        assertThat(enHiloFx(letraYnq::isVisible)).isFalse();

        institucionServiceYnq.racaActualizarLogo(logo(Color.BLUE));
        enHiloFx(() -> {
            identidadYnq.ryeRecargar();
            return null;
        });
        assertThat(enHiloFx(() -> rgb(imagenYnq.getImage(), 100, 50) & 0x00FFFFFF)).isEqualTo(0x0000FF);
    }

    @Test
    void elLoginMuestraElLogoDeLaInstitucion() throws Exception {
        institucionServiceYnq.racaActualizarLogo(logo(Color.RED));
        enHiloFx(() -> {
            identidadYnq.ryeRecargar();
            return null;
        });

        Object loginYnq = cargar("/fxml/auth/login.fxml");

        ImageView imagenYnq = (ImageView) ReflectionTestUtils.getField(loginYnq, "logoLogin");
        assertThat(enHiloFx(imagenYnq::isVisible)).isTrue();
        assertThat(enHiloFx(() -> imagenYnq.getImage() != null)).isTrue();
    }
}
