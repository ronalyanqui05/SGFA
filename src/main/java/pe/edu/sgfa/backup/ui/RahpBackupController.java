package pe.edu.sgfa.backup.ui;

import java.io.File;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import org.springframework.stereotype.Component;

import pe.edu.sgfa.shell.RyeCerrarVistaEvent;
import pe.edu.sgfa.backup.service.RahpBackupService;
import pe.edu.sgfa.backup.service.RahpInfoBackup;
import pe.edu.sgfa.common.exception.RyeSgfaException;

@Component
public class RahpBackupController {

    private static final DateTimeFormatter FORMATO_FECHA_HNC = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final RahpBackupService backupServiceHnc;

    @FXML
    private TableView<RahpInfoBackup> tabla;
    @FXML
    private TableColumn<RahpInfoBackup, String> colArchivo;
    @FXML
    private TableColumn<RahpInfoBackup, String> colFecha;
    @FXML
    private TableColumn<RahpInfoBackup, String> colTamano;

    public RahpBackupController(RahpBackupService backupServiceHnc) {
        this.backupServiceHnc = backupServiceHnc;
    }

    @FXML
    public void initialize() {
        colArchivo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().rahpNombreArchivo()));
        colFecha.setCellValueFactory(d -> new SimpleStringProperty(FORMATO_FECHA_HNC.format(d.getValue().fecha())));
        colTamano.setCellValueFactory(d -> new SimpleStringProperty(rahpFormatearTamano(d.getValue().tamanoBytes())));
        rahpRecargar();
    }

    private void rahpRecargar() {
        tabla.getItems().setAll(backupServiceHnc.rahpListar());
    }

    @FXML
    public void rahpOnRespaldar() {
        try {
            var archivoHnc = backupServiceHnc.rahpRespaldar();
            rahpRecargar();
            rahpMostrarInfo("Respaldo creado:\n" + archivoHnc);
        } catch (RyeSgfaException e) {
            rahpMostrarAdvertencia(e.getMessage());
        }
    }

    @FXML
    public void rahpOnRestaurarSeleccionado() {
        RahpInfoBackup seleccionadoHnc = tabla.getSelectionModel().getSelectedItem();
        if (seleccionadoHnc == null) {
            rahpMostrarAdvertencia("Seleccione un respaldo de la lista.");
            return;
        }
        rahpConfirmarYRestaurar(seleccionadoHnc.archivo());
    }

    @FXML
    public void rahpOnRestaurarDesdeArchivo() {
        FileChooser chooserHnc = new FileChooser();
        chooserHnc.setTitle("Seleccionar archivo de respaldo");
        chooserHnc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Base de datos SGFA", "*.db"));
        File archivoHnc = chooserHnc.showOpenDialog(tabla.getScene().getWindow());
        if (archivoHnc != null) {
            rahpConfirmarYRestaurar(archivoHnc.toPath());
        }
    }

    private void rahpConfirmarYRestaurar(java.nio.file.Path archivoHnc) {
        Alert confirmacionHnc = new Alert(Alert.AlertType.CONFIRMATION,
                "Esto reemplazara TODOS los datos actuales por el contenido de:\n" + archivoHnc
                        + "\n\nSe creara un respaldo de seguridad del estado actual antes de continuar, "
                        + "pero la aplicacion se cerrara al terminar y debera volver a abrirla.\n\n"
                        + "¿Desea continuar?",
                ButtonType.YES, ButtonType.NO);
        confirmacionHnc.setHeaderText("Restaurar respaldo");
        Optional<ButtonType> respuestaHnc = confirmacionHnc.showAndWait();
        if (respuestaHnc.isEmpty() || respuestaHnc.get() != ButtonType.YES) {
            return;
        }

        try {
            backupServiceHnc.rahpRestaurar(archivoHnc);
        } catch (RyeSgfaException e) {
            rahpMostrarAdvertencia(e.getMessage());
            return;
        }

        Alert listoHnc = new Alert(Alert.AlertType.INFORMATION,
                "Restauracion completada. SGFA se cerrara ahora; vuelva a abrirlo para continuar.",
                ButtonType.OK);
        listoHnc.setHeaderText(null);
        listoHnc.showAndWait();
        Platform.exit();
    }

    @FXML
    public void rahpOnCerrar() {
        tabla.fireEvent(new RyeCerrarVistaEvent());
    }

    private String rahpFormatearTamano(long bytesHnc) {
        if (bytesHnc < 1024) {
            return bytesHnc + " B";
        }
        double kb = bytesHnc / 1024.0;
        if (kb < 1024) {
            return String.format(Locale.ROOT, "%.1f KB", kb);
        }
        return String.format(Locale.ROOT, "%.1f MB", kb / 1024.0);
    }

    private void rahpMostrarInfo(String mensajeHnc) {
        Alert alertaHnc = new Alert(Alert.AlertType.INFORMATION, mensajeHnc, ButtonType.OK);
        alertaHnc.setHeaderText(null);
        alertaHnc.showAndWait();
    }

    private void rahpMostrarAdvertencia(String mensajeHnc) {
        new Alert(Alert.AlertType.WARNING, mensajeHnc, ButtonType.OK).showAndWait();
    }
}
