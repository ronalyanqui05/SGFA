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

import pe.edu.sgfa.shell.CerrarVistaEvent;
import pe.edu.sgfa.common.exception.ValidacionException;
import pe.edu.sgfa.institucion.domain.Institucion;
import pe.edu.sgfa.institucion.service.InstitucionService;

@Component
public class InstitucionController {

    private final InstitucionService institucionService;

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

    private final pe.edu.sgfa.shell.IdentidadVisual identidadVisual;

    public InstitucionController(InstitucionService institucionService, pe.edu.sgfa.shell.IdentidadVisual identidadVisual) {
        this.institucionService = institucionService;
        this.identidadVisual = identidadVisual;
    }

    @FXML
    public void initialize() {
        institucionService.obtenerPrincipal().ifPresent(this::cargarEnFormulario);
    }

    private void cargarEnFormulario(Institucion institucion) {
        nombreField.setText(institucion.getNombre());
        siglasField.setText(institucion.getSiglas());
        direccionField.setText(institucion.getDireccion());
        telefonoField.setText(institucion.getTelefono());
        correoField.setText(institucion.getCorreo());
        mostrarLogo(institucion.getLogoPath());
        mostrarFondo(institucion.getFondoLoginPath());
    }

    private void mostrarLogo(String lgPth) {
        if (lgPth == null || lgPth.isBlank()) {
            logoPreview.setImage(null);
            return;
        }
        logoPreview.setImage(new Image(new File(lgPth).toURI().toString(), true));
    }

    @FXML
    public void onGuardar() {
        try {
            institucionService.guardar(
                    textoDe(nombreField), textoDe(siglasField), textoDe(direccionField),
                    textoDe(telefonoField), textoDe(correoField));
            mostrarInfo("Datos de la institucion guardados.");
        } catch (ValidacionException e) {
            mostrarAdvertencia(e.getMessage());
        }
    }

    @FXML
    public void onCambiarLogo() {
        FileChooser chsr = new FileChooser();
        chsr.setTitle("Seleccionar logo");
        chsr.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imagenes", "*.png", "*.jpg", "*.jpeg"));

        File archv = chsr.showOpenDialog(logoPreview.getScene().getWindow());
        if (archv == null) {
            return;
        }

        try {
            Institucion institucion = institucionService.actualizarLogo(archv.toPath());
            mostrarLogo(institucion.getLogoPath());
            identidadVisual.recargar();
        } catch (ValidacionException e) {
            mostrarAdvertencia(e.getMessage());
        }
    }

    @FXML
    public void onCambiarFondo() {
        FileChooser chsr = new FileChooser();
        chsr.setTitle("Seleccionar imagen de fondo");
        chsr.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imagenes", "*.png", "*.jpg", "*.jpeg"));
        File archv = chsr.showOpenDialog(fondoPreview.getScene().getWindow());
        if (archv == null) {
            return;
        }
        try {
            Institucion institucion = institucionService.actualizarFondoLogin(archv.toPath());
            mostrarFondo(institucion.getFondoLoginPath());
            identidadVisual.recargar();
        } catch (ValidacionException e) {
            mostrarAdvertencia(e.getMessage());
        }
    }

    @FXML
    public void onQuitarFondo() {
        try {
            institucionService.quitarFondoLogin();
            mostrarFondo(null);
            identidadVisual.recargar();
        } catch (ValidacionException e) {
            mostrarAdvertencia(e.getMessage());
        }
    }

    private void mostrarFondo(String rt) {
        File f = rt == null || rt.isBlank() ? null : new File(rt);
        fondoPreview.setImage(f != null && f.isFile() ? new Image(f.toURI().toString(), 192, 128, true, true) : null);
    }

    @FXML
    public void onCerrar() {
        nombreField.fireEvent(new CerrarVistaEvent());
    }

    private String textoDe(TextField fld) {
        String txt = fld.getText();
        return txt == null || txt.isBlank() ? null : txt.trim();
    }

    private void mostrarAdvertencia(String mnsj) {
        new Alert(Alert.AlertType.WARNING, mnsj, ButtonType.OK).showAndWait();
    }

    private void mostrarInfo(String mnsj) {
        new Alert(Alert.AlertType.INFORMATION, mnsj, ButtonType.OK).showAndWait();
    }
}
