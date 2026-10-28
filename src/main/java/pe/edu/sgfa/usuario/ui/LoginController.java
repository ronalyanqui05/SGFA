package pe.edu.sgfa.usuario.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;

import pe.edu.sgfa.shell.NavigationService;
import pe.edu.sgfa.shell.ThemeService;
import pe.edu.sgfa.usuario.service.AuthService;
import pe.edu.sgfa.usuario.service.CredencialesInvalidasException;
import pe.edu.sgfa.usuario.service.UsuarioInactivoException;

@Component
public class LoginController {

    private final AuthService authService;
    private final NavigationService navigationService;
    private final ThemeService themeService;

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

    private final pe.edu.sgfa.shell.IdentidadVisual identidadVisual;

    public LoginController(AuthService authService, NavigationService navigationService, ThemeService themeService,
                            pe.edu.sgfa.shell.IdentidadVisual identidadVisual) {
        this.identidadVisual = identidadVisual;
        this.authService = authService;
        this.navigationService = navigationService;
        this.themeService = themeService;
    }

    @FXML
    public void initialize() {
        javafx.scene.image.Image logo = identidadVisual.logoProperty().get();
        logoLogin.setImage(logo);
        logoLogin.setVisible(logo != null);
        logoLogin.setManaged(logo != null);
        aplicarFondo();

        javafx.beans.value.ChangeListener<String> alCambiar = (obs, antes, ahora) -> aplicarFondo();
        panelMarca.getProperties().put("fondoListener", alCambiar);
        identidadVisual.fondoLoginUrlProperty().addListener(new javafx.beans.value.WeakChangeListener<>(alCambiar));
        actualizarTextoTema();
        themeService.oscuroProperty().addListener((obs, antes, ahora) -> actualizarTextoTema());
    }

    private void aplicarFondo() {
        String url = identidadVisual.fondoLoginUrlProperty().get();
        panelMarca.getStyleClass().removeAll("con-fondo", "fondo-claro");
        if (url == null) {
            panelMarca.setStyle(null);
            return;
        }

        panelMarca.setStyle("-fx-background-image: url('" + url + "'); -fx-background-size: cover; "
                + "-fx-background-position: center center; -fx-background-repeat: no-repeat;");
        panelMarca.getStyleClass().add("con-fondo");
        if (identidadVisual.fondoLoginClaroProperty().get()) {
            panelMarca.getStyleClass().add("fondo-claro");
        }
    }

    @FXML
    public void onAlternarTema() {
        themeService.alternar();
    }

    private void actualizarTextoTema() {
        temaButton.setText(themeService.isOscuro() ? "☀ Modo claro" : "🌙 Modo oscuro");
    }

    @FXML
    public void onLogin() {
        errorLabel.setText("");
        String usrnm = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String psswrd = passwordField.getText() == null ? "" : passwordField.getText();

        if (usrnm.isEmpty() || psswrd.isEmpty()) {
            errorLabel.setText("Ingrese usuario y contrasena.");
            return;
        }

        try {
            authService.autenticar(usrnm, psswrd);
            passwordField.clear();
            navigationService.mostrarPrincipal();
        } catch (CredencialesInvalidasException | UsuarioInactivoException e) {
            errorLabel.setText(e.getMessage());
            passwordField.clear();
        }
    }
}
