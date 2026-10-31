package pe.edu.sgfa.usuario.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;

import pe.edu.sgfa.shell.RyeNavigationService;
import pe.edu.sgfa.shell.RyeThemeService;
import pe.edu.sgfa.usuario.service.RyeAuthService;
import pe.edu.sgfa.usuario.service.RyeCredencialesInvalidasException;
import pe.edu.sgfa.usuario.service.RyeUsuarioInactivoException;

@Component
public class RyeLoginController {

    private final RyeAuthService authServiceYnq;
    private final RyeNavigationService navigationServiceYnq;
    private final RyeThemeService themeServiceYnq;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    private javafx.scene.control.Button temaButton;

    @FXML
    private javafx.scene.image.ImageView logoLogin;

    @FXML
    private javafx.scene.layout.VBox panelMarca;

    private final pe.edu.sgfa.shell.RyeIdentidadVisual identidadVisualYnq;

    public RyeLoginController(RyeAuthService authServiceYnq, RyeNavigationService navigationServiceYnq, RyeThemeService themeServiceYnq,
                            pe.edu.sgfa.shell.RyeIdentidadVisual identidadVisualYnq) {
        this.identidadVisualYnq = identidadVisualYnq;
        this.authServiceYnq = authServiceYnq;
        this.navigationServiceYnq = navigationServiceYnq;
        this.themeServiceYnq = themeServiceYnq;
    }

    @FXML
    public void initialize() {
        javafx.scene.image.Image logoYnq = identidadVisualYnq.logoProperty().get();
        logoLogin.setImage(logoYnq);
        logoLogin.setVisible(logoYnq != null);
        logoLogin.setManaged(logoYnq != null);
        ryeAplicarFondo();

        javafx.beans.value.ChangeListener<String> alCambiarYnq = (obsYnq, antesYnq, ahoraYnq) -> ryeAplicarFondo();
        panelMarca.getProperties().put("fondoListener", alCambiarYnq);
        identidadVisualYnq.fondoLoginUrlProperty().addListener(new javafx.beans.value.WeakChangeListener<>(alCambiarYnq));
        ryeActualizarTextoTema();
        themeServiceYnq.oscuroProperty().addListener((obsYnq, antesYnq, ahoraYnq) -> ryeActualizarTextoTema());
    }

    private void ryeAplicarFondo() {
        String urlYnq = identidadVisualYnq.fondoLoginUrlProperty().get();
        panelMarca.getStyleClass().removeAll("con-fondo", "fondo-claro");
        if (urlYnq == null) {
            panelMarca.setStyle(null);
            return;
        }

        panelMarca.setStyle("-fx-background-image: url('" + urlYnq + "'); -fx-background-size: cover; "
                + "-fx-background-position: center center; -fx-background-repeat: no-repeat;");
        panelMarca.getStyleClass().add("con-fondo");
        if (identidadVisualYnq.fondoLoginClaroProperty().get()) {
            panelMarca.getStyleClass().add("fondo-claro");
        }
    }

    @FXML
    public void ryeOnAlternarTema() {
        themeServiceYnq.ryeAlternar();
    }

    private void ryeActualizarTextoTema() {
        temaButton.setText(themeServiceYnq.isOscuro() ? "☀ Modo claro" : "🌙 Modo oscuro");
    }

    @FXML
    public void ryeOnLogin() {
        errorLabel.setText("");
        String usernameYnq = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String passwordYnq = passwordField.getText() == null ? "" : passwordField.getText();

        if (usernameYnq.isEmpty() || passwordYnq.isEmpty()) {
            errorLabel.setText("Ingrese usuario y contrasena.");
            return;
        }

        try {
            authServiceYnq.ryeAutenticar(usernameYnq, passwordYnq);
            passwordField.clear();
            navigationServiceYnq.ryeMostrarPrincipal();
        } catch (RyeCredencialesInvalidasException | RyeUsuarioInactivoException e) {
            errorLabel.setText(e.getMessage());
            passwordField.clear();
        }
    }
}
