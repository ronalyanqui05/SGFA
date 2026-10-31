package pe.edu.sgfa.institucion.ui;

import java.io.File;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import org.springframework.stereotype.Component;

import pe.edu.sgfa.shell.RyeCerrarVistaEvent;
import pe.edu.sgfa.common.exception.RyeValidacionException;
import pe.edu.sgfa.institucion.domain.RacaInstitucion;
import pe.edu.sgfa.institucion.service.RacaInstitucionService;

@Component
public class RacaInstitucionController {

    private final RacaInstitucionService institucionServiceCrr;

    @FXML
    private ImageView logoPreview;
    @FXML
    private ImageView fondoPreview;
    @FXML
    private TextField nombreField;
    @FXML
    private TextField siglasField;
    @FXML
    private TextField direccionField;
    @FXML
    private TextField telefonoField;
    @FXML
    private TextField correoField;

    private final pe.edu.sgfa.shell.RyeIdentidadVisual identidadVisualCrr;

    public RacaInstitucionController(RacaInstitucionService institucionServiceCrr, pe.edu.sgfa.shell.RyeIdentidadVisual identidadVisualCrr) {
        this.institucionServiceCrr = institucionServiceCrr;
        this.identidadVisualCrr = identidadVisualCrr;
    }

    @FXML
    public void initialize() {
        institucionServiceCrr.racaObtenerPrincipal().ifPresent(this::racaCargarEnFormulario);
    }

    private void racaCargarEnFormulario(RacaInstitucion institucionCrr) {
        nombreField.setText(institucionCrr.getNombre());
        siglasField.setText(institucionCrr.getSiglas());
        direccionField.setText(institucionCrr.getDireccion());
        telefonoField.setText(institucionCrr.getTelefono());
        correoField.setText(institucionCrr.getCorreo());
        racaMostrarLogo(institucionCrr.getLogoPath());
        racaMostrarFondo(institucionCrr.getFondoLoginPath());
    }

    private void racaMostrarLogo(String logoPathCrr) {
        if (logoPathCrr == null || logoPathCrr.isBlank()) {
            logoPreview.setImage(null);
            return;
        }
        logoPreview.setImage(new Image(new File(logoPathCrr).toURI().toString(), true));
    }

    @FXML
    public void racaOnGuardar() {
        try {
            institucionServiceCrr.racaGuardar(
                    racaTextoDe(nombreField), racaTextoDe(siglasField), racaTextoDe(direccionField),
                    racaTextoDe(telefonoField), racaTextoDe(correoField));
            racaMostrarInfo("Datos de la institucion guardados.");
        } catch (RyeValidacionException e) {
            racaMostrarAdvertencia(e.getMessage());
        }
    }

    @FXML
    public void racaOnCambiarLogo() {
        FileChooser chooserCrr = new FileChooser();
        chooserCrr.setTitle("Seleccionar logo");
        chooserCrr.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imagenes", "*.png", "*.jpg", "*.jpeg"));

        File archivoCrr = chooserCrr.showOpenDialog(logoPreview.getScene().getWindow());
        if (archivoCrr == null) {
            return;
        }

        try {
            RacaInstitucion institucionCrr = institucionServiceCrr.racaActualizarLogo(archivoCrr.toPath());
            racaMostrarLogo(institucionCrr.getLogoPath());
            identidadVisualCrr.ryeRecargar();
        } catch (RyeValidacionException e) {
            racaMostrarAdvertencia(e.getMessage());
        }
    }

    @FXML
    public void racaOnCambiarFondo() {
        FileChooser chooserCrr = new FileChooser();
        chooserCrr.setTitle("Seleccionar imagen de fondo");
        chooserCrr.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imagenes", "*.png", "*.jpg", "*.jpeg"));
        File archivoCrr = chooserCrr.showOpenDialog(fondoPreview.getScene().getWindow());
        if (archivoCrr == null) {
            return;
        }
        try {
            RacaInstitucion institucionCrr = institucionServiceCrr.racaActualizarFondoLogin(archivoCrr.toPath());
            racaMostrarFondo(institucionCrr.getFondoLoginPath());
            identidadVisualCrr.ryeRecargar();
        } catch (RyeValidacionException e) {
            racaMostrarAdvertencia(e.getMessage());
        }
    }

    @FXML
    public void racaOnQuitarFondo() {
        try {
            institucionServiceCrr.racaQuitarFondoLogin();
            racaMostrarFondo(null);
            identidadVisualCrr.ryeRecargar();
        } catch (RyeValidacionException e) {
            racaMostrarAdvertencia(e.getMessage());
        }
    }

    private void racaMostrarFondo(String rutaCrr) {
        File f = rutaCrr == null || rutaCrr.isBlank() ? null : new File(rutaCrr);
        fondoPreview.setImage(f != null && f.isFile() ? new Image(f.toURI().toString(), 192, 128, true, true) : null);
    }

    @FXML
    public void racaOnCerrar() {
        nombreField.fireEvent(new RyeCerrarVistaEvent());
    }

    private String racaTextoDe(TextField fieldCrr) {
        String textoCrr = fieldCrr.getText();
        return textoCrr == null || textoCrr.isBlank() ? null : textoCrr.trim();
    }

    private void racaMostrarAdvertencia(String mensajeCrr) {
        new Alert(Alert.AlertType.WARNING, mensajeCrr, ButtonType.OK).showAndWait();
    }

    private void racaMostrarInfo(String mensajeCrr) {
        new Alert(Alert.AlertType.INFORMATION, mensajeCrr, ButtonType.OK).showAndWait();
    }
}
