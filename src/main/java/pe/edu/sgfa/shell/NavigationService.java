package pe.edu.sgfa.shell;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Consumer;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.collections.ListChangeListener;
import javafx.stage.PopupWindow;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.springframework.stereotype.Component;

@Component
public class NavigationService {

    private static final String STYLESHEET = "/css/sgfa.css";

    private final SpringFxmlLoaderFactory fxmlLoaderFactory;
    private final ThemeService themeService;
    private final IdentidadVisual identidadVisual;
    private Stage stage;
    private VistaHost host;

    public NavigationService(SpringFxmlLoaderFactory fxmlLoaderFactory, ThemeService themeService,
                              IdentidadVisual identidadVisual) {
        this.fxmlLoaderFactory = fxmlLoaderFactory;
        this.themeService = themeService;
        this.identidadVisual = identidadVisual;
    }

    public void inicializar(Stage stage) {
        this.stage = stage;

        identidadVisual.recargar();
        javafx.beans.binding.Bindings.bindContent(stage.getIcons(), identidadVisual.iconos());
        aplicarTemaATodasLasVentanas();
    }

    private void aplicarTemaATodasLasVentanas() {
        String css = getClass().getResource(STYLESHEET).toExternalForm();
        Window.getWindows().addListener((ListChangeListener<Window>) cambio -> {
            while (cambio.next()) {
                for (Window vntn : cambio.getAddedSubList()) {
                    if (vntn instanceof PopupWindow || vntn.getScene() == null) {
                        continue;
                    }

                    if (vntn instanceof Stage otra && otra != stage && otra.getIcons().isEmpty()) {
                        otra.getIcons().setAll(identidadVisual.iconos());
                    }
                    if (!vntn.getScene().getStylesheets().contains(css)) {
                        vntn.getScene().getStylesheets().add(css);
                    }
                    Parent rz = vntn.getScene().getRoot();
                    themeService.aplicarA(rz);
                    forzarRecalculoDeEstilos(rz);
                }
            }
        });
    }

    public void mostrarLogin() {
        host = null;
        mostrarEnVentanaPrincipal("/fxml/auth/login.fxml");
    }

    public void mostrarPrincipal() {
        mostrarEnVentanaPrincipal("/fxml/dashboard/dashboard.fxml");
    }

    public void abrirVista(String fxmlPth, String ttl) {
        abrirVista(fxmlPth, ttl, controlador -> { });
    }

    public void abrirVista(String fxmlPth, String ttl, Consumer<Object> inclzdr) {
        if (host == null) {
            throw new IllegalStateException("No hay un contenedor de vistas activo (dashboard no mostrado).");
        }
        try {
            FXMLLoader ldr = fxmlLoaderFactory.create(fxmlPth);
            Parent rt = ldr.load();
            inclzdr.accept(ldr.getController());
            host.mostrar(rt, ttl);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo cargar la vista " + fxmlPth, e);
        }
    }

    public void registrarHost(VistaHost host) {
        this.host = host;
    }

    private void mostrarEnVentanaPrincipal(String fxmlPth) {
        Parent rt = cargar(fxmlPth);
        themeService.aplicarA(rt);
        if (stage.getScene() == null) {
            Scene scene = new Scene(rt, 1100, 700);
            scene.getStylesheets().add(getClass().getResource(STYLESHEET).toExternalForm());
            stage.setScene(scene);
        } else {
            stage.getScene().setRoot(rt);
        }
        forzarRecalculoDeEstilos(rt);
    }

    private void forzarRecalculoDeEstilos(Parent rz) {
        rz.applyCss();
    }

    private Parent cargar(String fxmlPth) {
        try {
            return fxmlLoaderFactory.create(fxmlPth).load();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo cargar la vista " + fxmlPth, e);
        }
    }
}
