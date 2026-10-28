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

import pe.edu.sgfa.institucion.service.InstitucionService;
import pe.edu.sgfa.usuario.service.AdminBootstrapResult;
import pe.edu.sgfa.usuario.service.AuthService;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IdentidadVisualTest {

    @BeforeAll
    static void baseAislada() throws IOException {
        System.setProperty("sgfa.home-dir", Files.createTempDirectory("sgfa-identidad-test-home-").toString());
    }

    @Autowired
    private IdentidadVisual identidad;
    @Autowired
    private NavigationService navigationService;
    @Autowired
    private SpringFxmlLoaderFactory fxml;
    @Autowired
    private InstitucionService institucionService;
    @Autowired
    private AuthService authService;
    @Autowired
    private AdminBootstrapResult adminBootstrapResult;

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
            institucionService.guardar("IESPP Juliaca", "JULI", null, null, null);
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

    private static <T> T enHiloFx(Supplier<T> accn) throws Exception {
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

    private static Path fondo(Color clr, int anch, int alt) throws IOException {
        BufferedImage img = new BufferedImage(anch, alt, BufferedImage.TYPE_INT_RGB);
        var g = img.createGraphics();
        g.setColor(clr);
        g.fillRect(0, 0, anch, alt);
        g.dispose();
        Path archv = Files.createTempFile("fondo-prueba-", ".png");
        ImageIO.write(img, "png", archv.toFile());
        return archv;
    }

    @Test
    void fondoClaroPideLetrasOscurasYFondoOscuroLetrasBlancas() throws Exception {
        institucionService.actualizarFondoLogin(fondo(new Color(245, 245, 235), 1280, 720));
        enHiloFx(() -> { identidad.recargar(); return null; });
        assertThat(identidad.fondoLoginProperty().get()).isNotNull();
        assertThat(identidad.fondoLoginClaroProperty().get()).as("foto clara -> letras oscuras").isTrue();

        institucionService.actualizarFondoLogin(fondo(new Color(20, 30, 60), 1280, 720));
        enHiloFx(() -> { identidad.recargar(); return null; });
        assertThat(identidad.fondoLoginClaroProperty().get()).as("foto oscura -> letras blancas").isFalse();

        var md = fondoCalculado(new Color(140, 140, 140));
        assertThat(IdentidadVisual.esClara(md)).isTrue();
        var conVelo = IdentidadVisual.conVelo(md, true);
        assertThat(IdentidadVisual.luminanciaMedia(conVelo)).isGreaterThan(0.5);
    }

    private static BufferedImage fondoCalculado(Color clr) {
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        var g = img.createGraphics();
        g.setColor(clr);
        g.fillRect(0, 0, 100, 100);
        g.dispose();
        return img;
    }

    @Test
    void sinFondoSeUsaElPorDefectoYSeValidaLaImagen() throws Exception {
        institucionService.actualizarFondoLogin(fondo(Color.DARK_GRAY, 800, 600));
        institucionService.quitarFondoLogin();
        enHiloFx(() -> { identidad.recargar(); return null; });
        assertThat(identidad.fondoLoginProperty().get()).isNull();
        assertThat(identidad.fondoLoginClaroProperty().get()).isFalse();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> institucionService.actualizarFondoLogin(fondo(Color.RED, 200, 100)))
                .hasMessageContaining("muy chica");
        Path noImgn = Files.createTempFile("falso-", ".png");
        Files.writeString(noImgn, "esto no es una imagen");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> institucionService.actualizarFondoLogin(noImgn))
                .hasMessageContaining("no es una imagen");
    }

    @Test
    void elLoginMuestraElFondoYCambiaElColorDeLasLetras() throws Exception {
        institucionService.actualizarFondoLogin(fondo(new Color(250, 250, 250), 1280, 720));
        enHiloFx(() -> { identidad.recargar(); return null; });
        javafx.scene.layout.VBox panel = enHiloFx(() -> {
            try {
                javafx.fxml.FXMLLoader ldr = fxml.create("/fxml/auth/login.fxml");
                javafx.scene.Parent raiz = ldr.load();
                return (javafx.scene.layout.VBox) raiz.lookup(".login-brand-panel");
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
        assertThat(panel.getStyle()).contains("-fx-background-image").contains("sgfa-fondo-login-");
        assertThat(panel.getStyleClass()).contains("con-fondo", "fondo-claro");

        institucionService.quitarFondoLogin();
        enHiloFx(() -> { identidad.recargar(); return null; });
        assertThat(panel.getStyle()).as("al quitarlo vuelve al color por defecto").isEmpty();
        assertThat(panel.getStyleClass()).doesNotContain("con-fondo", "fondo-claro");
    }

    private static Path logo(Color clr) throws IOException {
        BufferedImage img = new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB);
        var g = img.createGraphics();
        g.setColor(clr);
        g.fillRect(0, 0, 200, 100);
        g.dispose();
        Path archv = Files.createTempFile("logo-prueba-", ".png");
        ImageIO.write(img, "png", archv.toFile());
        return archv;
    }

    private static int rgb(Image imgn, int x, int y) {
        return imgn.getPixelReader().getArgb(x, y);
    }

    @Test
    void sinLogoLaAppTieneUnIconoPorDefectoCuadrado() throws Exception {
        enHiloFx(() -> {
            identidad.recargar();
            return null;
        });

        assertThat(enHiloFx(() -> identidad.logoProperty().get())).isNull();
        var iconos = enHiloFx(() -> List.copyOf(identidad.iconos()));
        assertThat(iconos).hasSize(7);
        assertThat(iconos).extracting(i -> (int) i.getWidth()).containsExactly(16, 24, 32, 48, 64, 128, 256);
        assertThat(iconos).allSatisfy(i -> assertThat(i.getHeight()).isEqualTo(i.getWidth()));

        Image grnd = iconos.get(6);
        assertThat((rgb(grnd, 128, 20) >>> 24) & 0xFF).isEqualTo(255);
    }

    private static class List {
        static <T> java.util.List<T> copyOf(java.util.Collection<T> c) {
            return java.util.List.copyOf(c);
        }
    }

    @Test
    void conLogoLosIconosSonCuadradosYNoDeformanElLogo() throws Exception {
        institucionService.actualizarLogo(logo(Color.RED));
        enHiloFx(() -> {
            identidad.recargar();
            return null;
        });

        Image logo = enHiloFx(() -> identidad.logoProperty().get());
        assertThat(logo).isNotNull();
        assertThat((int) logo.getWidth()).isEqualTo(200);
        Image grnd = enHiloFx(() -> identidad.iconos().get(6));
        assertThat((int) grnd.getWidth()).isEqualTo(256);
        assertThat((int) grnd.getHeight()).isEqualTo(256);

        assertThat(rgb(grnd, 128, 128) & 0x00FFFFFF).isEqualTo(0xFF0000);
        assertThat((rgb(grnd, 128, 10) >>> 24) & 0xFF).isZero();
    }

    @Test
    void laVentanaPrincipalSigueAlLogoYSeActualizaAlCambiarlo() throws Exception {
        institucionService.actualizarLogo(logo(Color.RED));
        Stage vntn = enHiloFx(() -> {
            Stage s = new Stage();
            navigationService.inicializar(s);
            return s;
        });
        assertThat(enHiloFx(() -> vntn.getIcons().size())).isEqualTo(7);
        int ants = enHiloFx(() -> rgb(vntn.getIcons().get(6), 128, 128) & 0x00FFFFFF);
        assertThat(ants).isEqualTo(0xFF0000);

        institucionService.actualizarLogo(logo(Color.BLUE));
        enHiloFx(() -> {
            identidad.recargar();
            return null;
        });

        assertThat(enHiloFx(() -> vntn.getIcons().size())).isEqualTo(7);
        assertThat(enHiloFx(() -> rgb(vntn.getIcons().get(6), 128, 128) & 0x00FFFFFF)).isEqualTo(0x0000FF);
    }

    private interface User32 extends com.sun.jna.Library {
        User32 INSTANCE = Native.load("user32", User32.class);

        Pointer FindWindowW(WString cls, WString ttl);

        long SendMessageW(Pointer vntn, int mnsj, long wPrm, long lPrm);
    }

    private boolean windowsLeAsignoIcono(String ttl) throws Exception {
        for (int intnt = 0; intnt < 20; intnt++) {
            Pointer vntn = User32.INSTANCE.FindWindowW(null, new WString(ttl));
            if (vntn != null) {
                long pqn = User32.INSTANCE.SendMessageW(vntn, 0x007F, 0, 0);
                long grnd = User32.INSTANCE.SendMessageW(vntn, 0x007F, 1, 0);
                return pqn != 0 && grnd != 0;
            }
            Thread.sleep(250);
        }
        throw new AssertionError("Windows no encontro la ventana \"" + ttl + "\"");
    }

    @Test
    void windowsRealmenteMuestraElLogoComoIconoDeLaVentana() throws Exception {
        Assumptions.assumeTrue(System.getProperty("os.name").toLowerCase().contains("win"), "Solo en Windows");
        institucionService.actualizarLogo(logo(Color.RED));
        enHiloFx(() -> {
            Platform.setImplicitExit(false);
            identidad.recargar();
            return null;
        });
        String ttl = "SGFA-ICONO-" + System.nanoTime();
        Stage vntn = enHiloFx(() -> {
            Stage s = new Stage();
            navigationService.inicializar(s);
            s.setTitle(ttl);
            s.setScene(new Scene(new StackPane(), 300, 100));
            s.show();
            return s;
        });
        try {
            assertThat(windowsLeAsignoIcono(ttl)).isTrue();
        } finally {
            enHiloFx(() -> {
                vntn.close();
                return null;
            });
        }
    }

    @Test
    void tambienElIconoPorDefectoSinLogoLoAsignaWindows() throws Exception {
        Assumptions.assumeTrue(System.getProperty("os.name").toLowerCase().contains("win"), "Solo en Windows");
        String ttl = "SGFA-ICONO-DEFECTO-" + System.nanoTime();
        Stage vntn = enHiloFx(() -> {
            Platform.setImplicitExit(false);
            Stage s = new Stage();
            s.getIcons().setAll(IdentidadVisual.icono(null, 16), IdentidadVisual.icono(null, 32), IdentidadVisual.icono(null, 256));
            s.setTitle(ttl);
            s.setScene(new Scene(new StackPane(), 300, 100));
            s.show();
            return s;
        });
        try {
            assertThat(windowsLeAsignoIcono(ttl)).isTrue();
        } finally {
            enHiloFx(() -> {
                vntn.close();
                return null;
            });
        }
    }

    private Object cargar(String rt) throws Exception {
        return enHiloFx(() -> {
            try {
                FXMLLoader ldr = fxml.create(rt);
                ldr.load();
                return ldr.getController();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    @Test
    void elMenuLateralMuestraElLogoDeLaInstitucion() throws Exception {
        institucionService.actualizarLogo(logo(Color.RED));
        enHiloFx(() -> {
            identidad.recargar();
            return null;
        });

        Object dshbrd = cargar("/fxml/dashboard/dashboard.fxml");

        ImageView imgn = (ImageView) ReflectionTestUtils.getField(dshbrd, "marcaImagen");
        Label ltr = (Label) ReflectionTestUtils.getField(dshbrd, "marcaLetra");
        assertThat(enHiloFx(imgn::isVisible)).isTrue();
        assertThat(enHiloFx(() -> imgn.getImage() != null)).isTrue();
        assertThat(enHiloFx(ltr::isVisible)).isFalse();

        institucionService.actualizarLogo(logo(Color.BLUE));
        enHiloFx(() -> {
            identidad.recargar();
            return null;
        });
        assertThat(enHiloFx(() -> rgb(imgn.getImage(), 100, 50) & 0x00FFFFFF)).isEqualTo(0x0000FF);
    }

    @Test
    void elLoginMuestraElLogoDeLaInstitucion() throws Exception {
        institucionService.actualizarLogo(logo(Color.RED));
        enHiloFx(() -> {
            identidad.recargar();
            return null;
        });

        Object lgn = cargar("/fxml/auth/login.fxml");

        ImageView imgn = (ImageView) ReflectionTestUtils.getField(lgn, "logoLogin");
        assertThat(enHiloFx(imgn::isVisible)).isTrue();
        assertThat(enHiloFx(() -> imgn.getImage() != null)).isTrue();
    }
}
