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

import pe.edu.sgfa.shell.RyeCerrarVistaEvent;
import pe.edu.sgfa.common.exception.RyeSgfaException;
import pe.edu.sgfa.ficha.domain.RacaEstadoFicha;
import pe.edu.sgfa.ficha.domain.RacaFichaGenerada;
import pe.edu.sgfa.ficha.service.RacaFichaService;
import pe.edu.sgfa.trayectoria.domain.YmclTrayectoria;
import pe.edu.sgfa.usuario.domain.RyeRolUsuario;
import pe.edu.sgfa.usuario.service.RyeSesionActual;

@Component
public class RacaHistorialFichasController {

    private final RacaFichaService fichaServiceCrr;
    private final RyeSesionActual sesionActualCrr;

    private YmclTrayectoria trayectoriaCrr;

    @FXML
    private Label tituloLabel;
    @FXML
    private TableView<RacaFichaGenerada> tabla;
    @FXML
    private TableColumn<RacaFichaGenerada, String> colCorrelativo;
    @FXML
    private TableColumn<RacaFichaGenerada, String> colFecha;
    @FXML
    private TableColumn<RacaFichaGenerada, String> colUsuario;
    @FXML
    private TableColumn<RacaFichaGenerada, String> colEstado;
    @FXML
    private TableColumn<RacaFichaGenerada, String> colObservacion;
    @FXML
    private Button botonAnular;

    public RacaHistorialFichasController(RacaFichaService fichaServiceCrr, RyeSesionActual sesionActualCrr) {
        this.fichaServiceCrr = fichaServiceCrr;
        this.sesionActualCrr = sesionActualCrr;
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

        boolean esAdminCrr = sesionActualCrr.ryeObtenerUsuarioActual()
                .map(u -> u.getRol() == RyeRolUsuario.ADMIN)
                .orElse(false);
        botonAnular.setVisible(esAdminCrr);
        botonAnular.setManaged(esAdminCrr);
    }

    public void setTrayectoria(YmclTrayectoria trayectoriaCrr) {
        this.trayectoriaCrr = trayectoriaCrr;
        YmclTrayectoria t = trayectoriaCrr;
        tituloLabel.setText("Historial de fichas - " + t.getEstudiante().getApellidoPaterno()
                + (t.getEstudiante().getApellidoMaterno() == null ? "" : " " + t.getEstudiante().getApellidoMaterno())
                + ", " + t.getEstudiante().getNombres());
        racaRecargar();
    }

    private void racaRecargar() {
        tabla.getItems().setAll(fichaServiceCrr.racaListarPorTrayectoria(trayectoriaCrr.getId()));
    }

    @FXML
    public void racaOnVer() {
        RacaFichaGenerada seleccionadaCrr = tabla.getSelectionModel().getSelectedItem();
        if (seleccionadaCrr == null) {
            racaMostrarAdvertencia("Seleccione una ficha.");
            return;
        }
        try {
            Desktop.getDesktop().open(new File(seleccionadaCrr.getArchivoPath()));
        } catch (IOException | UnsupportedOperationException e) {
            racaMostrarAdvertencia("No se pudo abrir el archivo: " + e.getMessage());
        }
    }

    @FXML
    public void racaOnAnular() {
        RacaFichaGenerada seleccionadaCrr = tabla.getSelectionModel().getSelectedItem();
        if (seleccionadaCrr == null) {
            racaMostrarAdvertencia("Seleccione una ficha.");
            return;
        }
        if (seleccionadaCrr.getEstado() == RacaEstadoFicha.ANULADA) {
            racaMostrarAdvertencia("Esa ficha ya esta anulada.");
            return;
        }

        TextInputDialog dialogoCrr = new TextInputDialog();
        dialogoCrr.setTitle("Anular ficha");
        dialogoCrr.setHeaderText("Anular la ficha " + seleccionadaCrr.getCorrelativo());
        dialogoCrr.setContentText("Motivo:");
        Optional<String> motivoCrr = dialogoCrr.showAndWait();
        if (motivoCrr.isEmpty()) {
            return;
        }

        try {
            fichaServiceCrr.racaAnular(seleccionadaCrr.getId(), motivoCrr.get());
            racaRecargar();
        } catch (RyeSgfaException e) {
            racaMostrarAdvertencia(e.getMessage());
        }
    }

    @FXML
    public void racaOnCerrar() {
        tabla.fireEvent(new RyeCerrarVistaEvent());
    }

    private void racaMostrarAdvertencia(String mensajeCrr) {
        new Alert(Alert.AlertType.WARNING, mensajeCrr, ButtonType.OK).showAndWait();
    }
}
