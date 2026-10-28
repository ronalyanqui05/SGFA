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

import pe.edu.sgfa.shell.CerrarVistaEvent;
import pe.edu.sgfa.common.exception.ValidacionException;
import pe.edu.sgfa.usuario.domain.RolUsuario;
import pe.edu.sgfa.usuario.domain.Usuario;
import pe.edu.sgfa.usuario.service.SesionActual;
import pe.edu.sgfa.usuario.service.UsuarioService;

@Component
public class UsuariosController {

    private final UsuarioService usuarioService;
    private final SesionActual sesionActual;

    @FXML
    private TableView<Usuario> tabla;
    @FXML
    private TableColumn<Usuario, String> colUsername;
    @FXML
    private TableColumn<Usuario, String> colNombres;
    @FXML
    private TableColumn<Usuario, String> colRol;
    @FXML
    private TableColumn<Usuario, String> colEstado;

    public UsuariosController(UsuarioService usuarioService, SesionActual sesionActual) {
        this.usuarioService = usuarioService;
        this.sesionActual = sesionActual;
    }

    @FXML
    public void initialize() {
        colUsername.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUsername()));
        colNombres.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNombres()));
        colRol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRol().name()));
        colEstado.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().isActivo() ? "Activo" : "Inactivo"));
        recargar();
    }

    private void recargar() {
        tabla.getItems().setAll(usuarioService.listar());
    }

    @FXML
    public void onNuevo() {
        mostrarFormulario(null);
    }

    @FXML
    public void onEditar() {
        Usuario slccnd = tabla.getSelectionModel().getSelectedItem();
        if (slccnd == null) {
            mostrarAdvertencia("Seleccione un usuario.");
            return;
        }
        mostrarFormulario(slccnd);
    }

    @FXML
    public void onToggleActivo() {
        Usuario slccnd = tabla.getSelectionModel().getSelectedItem();
        if (slccnd == null) {
            mostrarAdvertencia("Seleccione un usuario.");
            return;
        }
        boolean esElMsmUsrEnSsn = sesionActual.obtenerUsuarioActual()
                .map(actual -> actual.getId().equals(slccnd.getId()))
                .orElse(false);
        if (esElMsmUsrEnSsn) {
            mostrarAdvertencia("No puede activar/desactivar su propio usuario.");
            return;
        }
        usuarioService.activarDesactivar(slccnd.getId(), !slccnd.isActivo());
        recargar();
    }

    @FXML
    public void onCambiarPassword() {
        Usuario slccnd = tabla.getSelectionModel().getSelectedItem();
        if (slccnd == null) {
            mostrarAdvertencia("Seleccione un usuario.");
            return;
        }
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Cambiar contrasena");
        dlg.setHeaderText("Nueva contrasena para " + slccnd.getUsername());
        dlg.setContentText("Contrasena (minimo 8 caracteres):");
        dlg.showAndWait().ifPresent(nueva -> {
            try {
                usuarioService.cambiarPassword(slccnd.getId(), nueva);
                mostrarInfo("Contrasena actualizada.");
            } catch (ValidacionException e) {
                mostrarAdvertencia(e.getMessage());
            }
        });
    }

    @FXML
    public void onCerrar() {
        tabla.fireEvent(new CerrarVistaEvent());
    }

    private void mostrarFormulario(Usuario exstnt) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle(exstnt == null ? "Nuevo usuario" : "Editar usuario");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grd = new GridPane();
        grd.setHgap(8);
        grd.setVgap(8);
        grd.setPadding(new Insets(16));

        TextField usrnmFld = new TextField();
        TextField nmbrsFld = new TextField();
        ComboBox<RolUsuario> rolCmb = new ComboBox<>(FXCollections.observableArrayList(RolUsuario.values()));
        PasswordField psswrdFld = new PasswordField();

        if (exstnt != null) {
            usrnmFld.setText(exstnt.getUsername());
            usrnmFld.setDisable(true);
            nmbrsFld.setText(exstnt.getNombres());
            rolCmb.setValue(exstnt.getRol());
        } else {
            rolCmb.setValue(RolUsuario.OPERADOR);
        }

        int fl = 0;
        grd.addRow(fl++, new Label("Usuario:"), usrnmFld);
        grd.addRow(fl++, new Label("Nombres:"), nmbrsFld);
        grd.addRow(fl++, new Label("Rol:"), rolCmb);
        if (exstnt == null) {
            grd.addRow(fl, new Label("Contrasena:"), psswrdFld);
        }

        dlg.getDialogPane().setContent(grd);

        dlg.showAndWait().filter(boton -> boton == ButtonType.OK).ifPresent(boton -> {
            try {
                if (exstnt == null) {
                    usuarioService.crear(usrnmFld.getText().trim(), psswrdFld.getText(),
                            nmbrsFld.getText().trim(), rolCmb.getValue());
                } else {
                    usuarioService.editar(exstnt.getId(), nmbrsFld.getText().trim(), rolCmb.getValue());
                }
                recargar();
            } catch (ValidacionException e) {
                mostrarAdvertencia(e.getMessage());
            }
        });
    }

    private void mostrarAdvertencia(String mnsj) {
        new Alert(Alert.AlertType.WARNING, mnsj, ButtonType.OK).showAndWait();
    }

    private void mostrarInfo(String mnsj) {
        new Alert(Alert.AlertType.INFORMATION, mnsj, ButtonType.OK).showAndWait();
    }
}
