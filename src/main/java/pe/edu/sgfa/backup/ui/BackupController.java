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

import pe.edu.sgfa.shell.CerrarVistaEvent;
import pe.edu.sgfa.backup.service.BackupService;
import pe.edu.sgfa.backup.service.InfoBackup;
import pe.edu.sgfa.common.exception.SgfaException;

@Component
public class BackupController {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final BackupService backupService;

    @FXML
    private TableView<InfoBackup> tabla;
    @FXML
    private TableColumn<InfoBackup, String> colArchivo;
    @FXML
    private TableColumn<InfoBackup, String> colFecha;
    @FXML
    private TableColumn<InfoBackup, String> colTamano;

    public BackupController(BackupService backupService) {
        this.backupService = backupService;
    }

    @FXML
    public void initialize() {
        colArchivo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().nombreArchivo()));
        colFecha.setCellValueFactory(d -> new SimpleStringProperty(FORMATO_FECHA.format(d.getValue().fecha())));
        colTamano.setCellValueFactory(d -> new SimpleStringProperty(formatearTamano(d.getValue().tamanoBytes())));
        recargar();
    }

    private void recargar() {
        tabla.getItems().setAll(backupService.listar());
    }

    @FXML
    public void onRespaldar() {
        try {
            var archivo = backupService.respaldar();
            recargar();
            mostrarInfo("Respaldo creado:\n" + archivo);
        } catch (SgfaException e) {
            mostrarAdvertencia(e.getMessage());
        }
    }

    @FXML
    public void onRestaurarSeleccionado() {
        InfoBackup slccnd = tabla.getSelectionModel().getSelectedItem();
        if (slccnd == null) {
            mostrarAdvertencia("Seleccione un respaldo de la lista.");
            return;
        }
        confirmarYRestaurar(slccnd.archivo());
    }

    @FXML
    public void onRestaurarDesdeArchivo() {
        FileChooser chsr = new FileChooser();
        chsr.setTitle("Seleccionar archivo de respaldo");
        chsr.getExtensionFilters().add(new FileChooser.ExtensionFilter("Base de datos SGFA", "*.db"));
        File archivo = chsr.showOpenDialog(tabla.getScene().getWindow());
        if (archivo != null) {
            confirmarYRestaurar(archivo.toPath());
        }
    }

    private void confirmarYRestaurar(java.nio.file.Path archivo) {
        Alert cnfrmcn = new Alert(Alert.AlertType.CONFIRMATION,
                "Esto reemplazara TODOS los datos actuales por el contenido de:\n" + archivo
                        + "\n\nSe creara un respaldo de seguridad del estado actual antes de continuar, "
                        + "pero la aplicacion se cerrara al terminar y debera volver a abrirla.\n\n"
                        + "¿Desea continuar?",
                ButtonType.YES, ButtonType.NO);
        cnfrmcn.setHeaderText("Restaurar respaldo");
        Optional<ButtonType> rspst = cnfrmcn.showAndWait();
        if (rspst.isEmpty() || rspst.get() != ButtonType.YES) {
            return;
        }

        try {
            backupService.restaurar(archivo);
        } catch (SgfaException e) {
            mostrarAdvertencia(e.getMessage());
            return;
        }

        Alert lst = new Alert(Alert.AlertType.INFORMATION,
                "Restauracion completada. SGFA se cerrara ahora; vuelva a abrirlo para continuar.",
                ButtonType.OK);
        lst.setHeaderText(null);
        lst.showAndWait();
        Platform.exit();
    }

    @FXML
    public void onCerrar() {
        tabla.fireEvent(new CerrarVistaEvent());
    }

    private String formatearTamano(long byts) {
        if (byts < 1024) {
            return byts + " B";
        }
        double kb = byts / 1024.0;
        if (kb < 1024) {
            return String.format(Locale.ROOT, "%.1f KB", kb);
        }
        return String.format(Locale.ROOT, "%.1f MB", kb / 1024.0);
    }

    private void mostrarInfo(String mnsj) {
        Alert alrt = new Alert(Alert.AlertType.INFORMATION, mnsj, ButtonType.OK);
        alrt.setHeaderText(null);
        alrt.showAndWait();
    }

    private void mostrarAdvertencia(String mnsj) {
        new Alert(Alert.AlertType.WARNING, mnsj, ButtonType.OK).showAndWait();
    }
}
