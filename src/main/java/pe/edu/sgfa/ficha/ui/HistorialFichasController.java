package pe.edu.sgfa.ficha.ui;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import org.springframework.stereotype.Component;

import pe.edu.sgfa.shell.CerrarVistaEvent;
import pe.edu.sgfa.common.exception.SgfaException;
import pe.edu.sgfa.ficha.domain.EstadoFicha;
import pe.edu.sgfa.ficha.domain.FichaGenerada;
import pe.edu.sgfa.ficha.service.FichaService;
import pe.edu.sgfa.trayectoria.domain.Trayectoria;
import pe.edu.sgfa.usuario.domain.RolUsuario;
import pe.edu.sgfa.usuario.service.SesionActual;

@Component
public class HistorialFichasController {

    private final FichaService fichaService;
    private final SesionActual sesionActual;

    private Trayectoria trayectoria;

    @FXML
    private Label tituloLabel;
    @FXML
    private TableView<FichaGenerada> tabla;
    @FXML
    private TableColumn<FichaGenerada, String> colCorrelativo;
    @FXML
    private TableColumn<FichaGenerada, String> colFecha;
    @FXML
    private TableColumn<FichaGenerada, String> colUsuario;
    @FXML
    private TableColumn<FichaGenerada, String> colEstado;
    @FXML
    private TableColumn<FichaGenerada, String> colObservacion;
    @FXML
    private Button botonAnular;

    public HistorialFichasController(FichaService fichaService, SesionActual sesionActual) {
        this.fichaService = fichaService;
        this.sesionActual = sesionActual;
    }

    @FXML
    public void initialize() {
        colCorrelativo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCorrelativo()));
        colFecha.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFechaGeneracion().toString()));
        colUsuario.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getUsuario() == null ? "-" : d.getValue().getUsuario().getUsername()));
        colEstado.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEstado().name()));
        colObservacion.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getObservacion() == null ? "" : d.getValue().getObservacion()));

        boolean esAdmn = sesionActual.obtenerUsuarioActual()
                .map(u -> u.getRol() == RolUsuario.ADMIN)
                .orElse(false);
        botonAnular.setVisible(esAdmn);
        botonAnular.setManaged(esAdmn);
    }

    public void setTrayectoria(Trayectoria trayectoria) {
        this.trayectoria = trayectoria;
        Trayectoria t = trayectoria;
        tituloLabel.setText("Historial de fichas - " + t.getEstudiante().getApellidoPaterno()
                + (t.getEstudiante().getApellidoMaterno() == null ? "" : " " + t.getEstudiante().getApellidoMaterno())
                + ", " + t.getEstudiante().getNombres());
        recargar();
    }

    private void recargar() {
        tabla.getItems().setAll(fichaService.listarPorTrayectoria(trayectoria.getId()));
    }

    @FXML
    public void onVer() {
        FichaGenerada slccnd = tabla.getSelectionModel().getSelectedItem();
        if (slccnd == null) {
            mostrarAdvertencia("Seleccione una ficha.");
            return;
        }
        try {
            Desktop.getDesktop().open(new File(slccnd.getArchivoPath()));
        } catch (IOException | UnsupportedOperationException e) {
            mostrarAdvertencia("No se pudo abrir el archivo: " + e.getMessage());
        }
    }

    @FXML
    public void onAnular() {
        FichaGenerada slccnd = tabla.getSelectionModel().getSelectedItem();
        if (slccnd == null) {
            mostrarAdvertencia("Seleccione una ficha.");
            return;
        }
        if (slccnd.getEstado() == EstadoFicha.ANULADA) {
            mostrarAdvertencia("Esa ficha ya esta anulada.");
            return;
        }

        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Anular ficha");
        dlg.setHeaderText("Anular la ficha " + slccnd.getCorrelativo());
        dlg.setContentText("Motivo:");
        Optional<String> mtv = dlg.showAndWait();
        if (mtv.isEmpty()) {
            return;
        }

        try {
            fichaService.anular(slccnd.getId(), mtv.get());
            recargar();
        } catch (SgfaException e) {
            mostrarAdvertencia(e.getMessage());
        }
    }

    @FXML
    public void onCerrar() {
        tabla.fireEvent(new CerrarVistaEvent());
    }

    private void mostrarAdvertencia(String mnsj) {
        new Alert(Alert.AlertType.WARNING, mnsj, ButtonType.OK).showAndWait();
    }
}
