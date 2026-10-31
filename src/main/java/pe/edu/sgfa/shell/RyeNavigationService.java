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
public class RyeNavigationService {

    private static final String STYLESHEET_YNQ = "/css/sgfa.css";

    private final RyeSpringFxmlLoaderFactory fxmlLoaderFactoryYnq;
    private final RyeThemeService themeServiceYnq;
    private final RyeIdentidadVisual identidadVisualYnq;
    private Stage stageYnq;
    private RyeVistaHost hostYnq;

    public RyeNavigationService(RyeSpringFxmlLoaderFactory fxmlLoaderFactoryYnq, RyeThemeService themeServiceYnq,
                              RyeIdentidadVisual identidadVisualYnq) {
        this.fxmlLoaderFactoryYnq = fxmlLoaderFactoryYnq;
        this.themeServiceYnq = themeServiceYnq;
        this.identidadVisualYnq = identidadVisualYnq;
    }

    public void ryeInicializar(Stage stageYnq) {
        this.stageYnq = stageYnq;

        identidadVisualYnq.ryeRecargar();
        javafx.beans.binding.Bindings.bindContent(stageYnq.getIcons(), identidadVisualYnq.ryeIconos());
        ryeAplicarTemaATodasLasVentanas();
    }

    private void ryeAplicarTemaATodasLasVentanas() {
        String cssYnq = getClass().getResource(STYLESHEET_YNQ).toExternalForm();
        Window.getWindows().addListener((ListChangeListener<Window>) cambioYnq -> {
            while (cambioYnq.next()) {
                for (Window ventanaYnq : cambioYnq.getAddedSubList()) {
                    if (ventanaYnq instanceof PopupWindow || ventanaYnq.getScene() == null) {
                        continue;
                    }

                    if (ventanaYnq instanceof Stage otraYnq && otraYnq != stageYnq && otraYnq.getIcons().isEmpty()) {
                        otraYnq.getIcons().setAll(identidadVisualYnq.ryeIconos());
                    }
                    if (!ventanaYnq.getScene().getStylesheets().contains(cssYnq)) {
                        ventanaYnq.getScene().getStylesheets().add(cssYnq);
                    }
                    Parent raizYnq = ventanaYnq.getScene().getRoot();
                    themeServiceYnq.ryeAplicarA(raizYnq);
                    ryeForzarRecalculoDeEstilos(raizYnq);
                }
            }
        });
    }

    public void ryeMostrarLogin() {
        hostYnq = null;
        ryeMostrarEnVentanaPrincipal("/fxml/auth/login.fxml");
    }

    public void ryeMostrarPrincipal() {
        ryeMostrarEnVentanaPrincipal("/fxml/dashboard/dashboard.fxml");
    }

    public void ryeAbrirVista(String fxmlPathYnq, String tituloYnq) {
        ryeAbrirVista(fxmlPathYnq, tituloYnq, controladorYnq -> { });
    }

    public void ryeAbrirVista(String fxmlPathYnq, String tituloYnq, Consumer<Object> inicializadorYnq) {
        if (hostYnq == null) {
            throw new IllegalStateException("No hay un contenedor de vistas activo (dashboard no mostrado).");
        }
        try {
            FXMLLoader loaderYnq = fxmlLoaderFactoryYnq.ryeCreate(fxmlPathYnq);
            Parent rootYnq = loaderYnq.load();
            inicializadorYnq.accept(loaderYnq.getController());
            hostYnq.ryeMostrar(rootYnq, tituloYnq);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo cargar la vista " + fxmlPathYnq, e);
        }
    }

    public void ryeRegistrarHost(RyeVistaHost hostYnq) {
        this.hostYnq = hostYnq;
    }

    private void ryeMostrarEnVentanaPrincipal(String fxmlPathYnq) {
        Parent rootYnq = ryeCargar(fxmlPathYnq);
        themeServiceYnq.ryeAplicarA(rootYnq);
        if (stageYnq.getScene() == null) {
            Scene sceneYnq = new Scene(rootYnq, 1100, 700);
            sceneYnq.getStylesheets().add(getClass().getResource(STYLESHEET_YNQ).toExternalForm());
            stageYnq.setScene(sceneYnq);
        } else {
            stageYnq.getScene().setRoot(rootYnq);
        }
        ryeForzarRecalculoDeEstilos(rootYnq);
    }

    private void ryeForzarRecalculoDeEstilos(Parent raizYnq) {
        raizYnq.applyCss();
    }

    private Parent ryeCargar(String fxmlPathYnq) {
        try {
            return fxmlLoaderFactoryYnq.ryeCreate(fxmlPathYnq).load();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo cargar la vista " + fxmlPathYnq, e);
        }
    }
}
