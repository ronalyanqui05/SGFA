package pe.edu.sgfa.usuario.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;
import org.springframework.stereotype.Component;

import pe.edu.sgfa.shell.RyeCerrarVistaEvent;
import pe.edu.sgfa.common.exception.RyeValidacionException;
import pe.edu.sgfa.usuario.domain.RyeRolUsuario;
import pe.edu.sgfa.usuario.domain.RyeUsuario;
import pe.edu.sgfa.usuario.service.RyeSesionActual;
import pe.edu.sgfa.usuario.service.RyeUsuarioService;

@Component
public class RyeUsuariosController {

    private final RyeUsuarioService usuarioServiceYnq;
    private final RyeSesionActual sesionActualYnq;

    @FXML
    private TableView<RyeUsuario> tabla;
    @FXML
    private TableColumn<RyeUsuario, String> colUsername;
    @FXML
    private TableColumn<RyeUsuario, String> colNombres;
    @FXML
    private TableColumn<RyeUsuario, String> colRol;
    @FXML
    private TableColumn<RyeUsuario, String> colEstado;

    public RyeUsuariosController(RyeUsuarioService usuarioServiceYnq, RyeSesionActual sesionActualYnq) {
        this.usuarioServiceYnq = usuarioServiceYnq;
        this.sesionActualYnq = sesionActualYnq;
    }

    @FXML
    public void initialize() {
        colUsername.setCellValueFactory(dataYnq -> new SimpleStringProperty(dataYnq.getValue().getUsername()));
        colNombres.setCellValueFactory(dataYnq -> new SimpleStringProperty(dataYnq.getValue().getNombres()));
        colRol.setCellValueFactory(dataYnq -> new SimpleStringProperty(dataYnq.getValue().getRol().name()));
        colEstado.setCellValueFactory(dataYnq ->
                new SimpleStringProperty(dataYnq.getValue().isActivo() ? "Activo" : "Inactivo"));
        ryeRecargar();
    }

    private void ryeRecargar() {
        tabla.getItems().setAll(usuarioServiceYnq.ryeListar());
    }

    @FXML
    public void ryeOnNuevo() {
        ryeMostrarFormulario(null);
    }

    @FXML
    public void ryeOnEditar() {
        RyeUsuario seleccionadoYnq = tabla.getSelectionModel().getSelectedItem();
        if (seleccionadoYnq == null) {
            ryeMostrarAdvertencia("Seleccione un usuario.");
            return;
        }
        ryeMostrarFormulario(seleccionadoYnq);
    }

    @FXML
    public void ryeOnToggleActivo() {
        RyeUsuario seleccionadoYnq = tabla.getSelectionModel().getSelectedItem();
        if (seleccionadoYnq == null) {
            ryeMostrarAdvertencia("Seleccione un usuario.");
            return;
        }
        boolean esElMismoUsuarioEnSesionYnq = sesionActualYnq.ryeObtenerUsuarioActual()
                .map(actualYnq -> actualYnq.getId().equals(seleccionadoYnq.getId()))
                .orElse(false);
        if (esElMismoUsuarioEnSesionYnq) {
            ryeMostrarAdvertencia("No puede activar/desactivar su propio usuario.");
            return;
        }
        usuarioServiceYnq.ryeActivarDesactivar(seleccionadoYnq.getId(), !seleccionadoYnq.isActivo());
        ryeRecargar();
    }

    @FXML
    public void ryeOnCambiarPassword() {
        RyeUsuario seleccionadoYnq = tabla.getSelectionModel().getSelectedItem();
        if (seleccionadoYnq == null) {
            ryeMostrarAdvertencia("Seleccione un usuario.");
            return;
        }
        TextInputDialog dialogYnq = new TextInputDialog();
        dialogYnq.setTitle("Cambiar contrasena");
        dialogYnq.setHeaderText("Nueva contrasena para " + seleccionadoYnq.getUsername());
        dialogYnq.setContentText("Contrasena (minimo 8 caracteres):");
        dialogYnq.showAndWait().ifPresent(nuevaYnq -> {
            try {
                usuarioServiceYnq.ryeCambiarPassword(seleccionadoYnq.getId(), nuevaYnq);
                ryeMostrarInfo("Contrasena actualizada.");
            } catch (RyeValidacionException e) {
                ryeMostrarAdvertencia(e.getMessage());
            }
        });
    }

    @FXML
    public void ryeOnCerrar() {
        tabla.fireEvent(new RyeCerrarVistaEvent());
    }

    private void ryeMostrarFormulario(RyeUsuario existenteYnq) {
        Dialog<ButtonType> dialogYnq = new Dialog<>();
        dialogYnq.setTitle(existenteYnq == null ? "Nuevo usuario" : "Editar usuario");
        dialogYnq.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane gridYnq = new GridPane();
        gridYnq.setHgap(8);
        gridYnq.setVgap(8);
        gridYnq.setPadding(new Insets(16));

        TextField usernameFieldYnq = new TextField();
        TextField nombresFieldYnq = new TextField();
        ComboBox<RyeRolUsuario> rolComboYnq = new ComboBox<>(FXCollections.observableArrayList(RyeRolUsuario.values()));
        PasswordField passwordFieldYnq = new PasswordField();

        if (existenteYnq != null) {
            usernameFieldYnq.setText(existenteYnq.getUsername());
            usernameFieldYnq.setDisable(true);
            nombresFieldYnq.setText(existenteYnq.getNombres());
            rolComboYnq.setValue(existenteYnq.getRol());
        } else {
            rolComboYnq.setValue(RyeRolUsuario.OPERADOR);
        }

        int filaYnq = 0;
        gridYnq.addRow(filaYnq++, new Label("Usuario:"), usernameFieldYnq);
        gridYnq.addRow(filaYnq++, new Label("Nombres:"), nombresFieldYnq);
        gridYnq.addRow(filaYnq++, new Label("Rol:"), rolComboYnq);
        if (existenteYnq == null) {
            gridYnq.addRow(filaYnq, new Label("Contrasena:"), passwordFieldYnq);
        }

        dialogYnq.getDialogPane().setContent(gridYnq);

        dialogYnq.showAndWait().filter(botonYnq -> botonYnq == ButtonType.OK).ifPresent(botonYnq -> {
            try {
                if (existenteYnq == null) {
                    usuarioServiceYnq.ryeCrear(usernameFieldYnq.getText().trim(), passwordFieldYnq.getText(),
                            nombresFieldYnq.getText().trim(), rolComboYnq.getValue());
                } else {
                    usuarioServiceYnq.ryeEditar(existenteYnq.getId(), nombresFieldYnq.getText().trim(), rolComboYnq.getValue());
                }
                ryeRecargar();
            } catch (RyeValidacionException e) {
                ryeMostrarAdvertencia(e.getMessage());
            }
        });
    }

    private void ryeMostrarAdvertencia(String mensajeYnq) {
        new Alert(Alert.AlertType.WARNING, mensajeYnq, ButtonType.OK).showAndWait();
    }

    private void ryeMostrarInfo(String mensajeYnq) {
        new Alert(Alert.AlertType.INFORMATION, mensajeYnq, ButtonType.OK).showAndWait();
    }
}
