package pe.edu.sgfa.estudiante.ui;

import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import org.springframework.stereotype.Component;

import pe.edu.sgfa.common.exception.RyeSgfaException;
import pe.edu.sgfa.common.exception.RyeValidacionException;
import pe.edu.sgfa.estudiante.domain.YmclEstadoEstudiante;
import pe.edu.sgfa.estudiante.domain.YmclEstudiante;
import pe.edu.sgfa.estudiante.domain.YmclSexo;
import pe.edu.sgfa.estudiante.service.YmclEstudianteService;
import net.sf.jasperreports.engine.JasperPrint;

import pe.edu.sgfa.ficha.service.RacaFichaService;
import pe.edu.sgfa.ficha.ui.RacaHistorialFichasController;
import pe.edu.sgfa.ficha.ui.RacaVistaPreviaFichaController;
import pe.edu.sgfa.nota.ui.YmclRegistroNotasController;
import pe.edu.sgfa.ocr.service.RahpExtractorDatosEstudiante;
import pe.edu.sgfa.ocr.service.RahpOcrService;
import pe.edu.sgfa.shell.RyeNavigationService;
import pe.edu.sgfa.trayectoria.domain.YmclTrayectoria;
import pe.edu.sgfa.trayectoria.service.YmclTrayectoriaService;
import pe.edu.sgfa.trayectoria.ui.YmclTrayectoriaController;

@Component
public class YmclEstudiantesController {

    private final YmclEstudianteService estudianteServiceChp;
    private final RyeNavigationService navigationServiceChp;
    private final YmclTrayectoriaService trayectoriaServiceChp;
    private final RacaFichaService fichaServiceChp;
    private final RahpOcrService ocrServiceChp;

    @FXML
    private TextField busquedaField;
    @FXML
    private CheckBox incluirInactivosCheck;
    @FXML
    private TableView<YmclEstudiante> tabla;
    @FXML
    private TableColumn<YmclEstudiante, String> colCodigo;
    @FXML
    private TableColumn<YmclEstudiante, String> colDni;
    @FXML
    private TableColumn<YmclEstudiante, String> colApellidosNombres;
    @FXML
    private TableColumn<YmclEstudiante, String> colEstado;

    public YmclEstudiantesController(YmclEstudianteService estudianteServiceChp, RyeNavigationService navigationServiceChp,
                                  YmclTrayectoriaService trayectoriaServiceChp, RacaFichaService fichaServiceChp,
                                  RahpOcrService ocrServiceChp) {
        this.estudianteServiceChp = estudianteServiceChp;
        this.navigationServiceChp = navigationServiceChp;
        this.trayectoriaServiceChp = trayectoriaServiceChp;
        this.fichaServiceChp = fichaServiceChp;
        this.ocrServiceChp = ocrServiceChp;
    }

    @FXML
    public void initialize() {
        colCodigo.setCellValueFactory(d -> new SimpleStringProperty(ymclVacioSiNulo(d.getValue().getCodigo())));
        colDni.setCellValueFactory(d -> new SimpleStringProperty(ymclVacioSiNulo(d.getValue().getDni())));
        colApellidosNombres.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getApellidoPaterno()
                        + (d.getValue().getApellidoMaterno() == null ? "" : " " + d.getValue().getApellidoMaterno())
                        + ", " + d.getValue().getNombres()));
        colEstado.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEstado().name()));

        javafx.animation.PauseTransition pausaChp = new javafx.animation.PauseTransition(javafx.util.Duration.millis(200));
        pausaChp.setOnFinished(e -> ymclOnBuscar());
        busquedaField.textProperty().addListener((obsChp, antesChp, ahoraChp) -> pausaChp.playFromStart());
        incluirInactivosCheck.selectedProperty().addListener((obsChp, antesChp, ahoraChp) -> ymclOnBuscar());

        ymclOnMostrarTodos();
    }

    @FXML
    public void ymclOnBuscar() {
        String textoChp = busquedaField.getText();
        YmclEstudiante seleccionadoChp = tabla.getSelectionModel().getSelectedItem();
        if (textoChp == null || textoChp.isBlank()) {
            tabla.getItems().setAll(estudianteServiceChp.ymclListar(incluirInactivosCheck.isSelected()));
        } else {
            tabla.getItems().setAll(estudianteServiceChp.ymclBuscar(textoChp));
        }
        if (seleccionadoChp != null) {
            tabla.getItems().stream().filter(e -> e.getId().equals(seleccionadoChp.getId())).findFirst()
                    .ifPresent(e -> tabla.getSelectionModel().select(e));
        }
    }

    @FXML
    public void ymclOnMostrarTodos() {
        busquedaField.clear();
        tabla.getItems().setAll(estudianteServiceChp.ymclListar(incluirInactivosCheck.isSelected()));
    }

    @FXML
    public void ymclOnNuevo() {
        ymclMostrarFormulario(null);
    }

    @FXML
    public void ymclOnEditar() {
        YmclEstudiante seleccionadoChp = tabla.getSelectionModel().getSelectedItem();
        if (seleccionadoChp == null) {
            ymclMostrarAdvertencia("Seleccione un estudiante.");
            return;
        }
        ymclMostrarFormulario(seleccionadoChp);
    }

    @FXML
    public void ymclOnToggleEstado() {
        YmclEstudiante seleccionadoChp = tabla.getSelectionModel().getSelectedItem();
        if (seleccionadoChp == null) {
            ymclMostrarAdvertencia("Seleccione un estudiante.");
            return;
        }
        YmclEstadoEstudiante nuevoEstadoChp = seleccionadoChp.getEstado() == YmclEstadoEstudiante.ACTIVO
                ? YmclEstadoEstudiante.INACTIVO : YmclEstadoEstudiante.ACTIVO;
        estudianteServiceChp.ymclCambiarEstado(seleccionadoChp.getId(), nuevoEstadoChp);
        ymclOnMostrarTodos();
    }

    @FXML
    public void ymclOnTrayectoria() {
        YmclEstudiante seleccionadoChp = tabla.getSelectionModel().getSelectedItem();
        if (seleccionadoChp == null) {
            ymclMostrarAdvertencia("Seleccione un estudiante.");
            return;
        }
        navigationServiceChp.ryeAbrirVista("/fxml/trayectoria/trayectoria.fxml",
                "Trayectoria academica",
                controladorChp -> ((YmclTrayectoriaController) controladorChp).setEstudiante(seleccionadoChp));
    }

    @FXML
    public void ymclOnRegistrarNotas() {
        YmclEstudiante seleccionadoChp = tabla.getSelectionModel().getSelectedItem();
        if (seleccionadoChp == null) {
            ymclMostrarAdvertencia("Seleccione un estudiante.");
            return;
        }
        navigationServiceChp.ryeAbrirVista("/fxml/nota/registro-notas.fxml",
                "Registro de notas",
                controladorChp -> ((YmclRegistroNotasController) controladorChp).setEstudiante(seleccionadoChp));
    }

    @FXML
    public void ymclOnGenerarFicha() {
        YmclEstudiante seleccionadoChp = tabla.getSelectionModel().getSelectedItem();
        if (seleccionadoChp == null) {
            ymclMostrarAdvertencia("Seleccione un estudiante.");
            return;
        }

        List<YmclTrayectoria> trayectoriasChp = trayectoriaServiceChp.ymclListarPorEstudiante(seleccionadoChp.getId());
        if (trayectoriasChp.isEmpty()) {
            ymclMostrarAdvertencia("El estudiante no tiene ninguna trayectoria registrada. Registre una antes de generar la ficha.");
            return;
        }

        if (trayectoriasChp.size() == 1) {
            ymclGenerarFichaPara(trayectoriasChp.get(0));
            return;
        }

        ymclElegirTrayectoria(trayectoriasChp).ifPresent(this::ymclGenerarFichaPara);
    }

    private Optional<YmclTrayectoria> ymclElegirTrayectoria(List<YmclTrayectoria> trayectoriasChp) {
        ChoiceDialog<YmclTrayectoria> dialogoChp = new ChoiceDialog<>(trayectoriasChp.get(0), trayectoriasChp);
        dialogoChp.setTitle("Generar ficha");
        dialogoChp.setHeaderText("El estudiante tiene varias trayectorias.");
        dialogoChp.setContentText("Seleccione la trayectoria para la ficha:");
        dialogoChp.getItems().setAll(trayectoriasChp);
        return dialogoChp.showAndWait();
    }

    private void ymclGenerarFichaPara(YmclTrayectoria trayectoriaChp) {
        try {
            JasperPrint printChp = fichaServiceChp.racaPrevisualizar(trayectoriaChp.getId());
            navigationServiceChp.ryeAbrirVista("/fxml/ficha/vista-previa-ficha.fxml",
                    "Vista previa - Ficha academica",
                    controladorChp -> ((RacaVistaPreviaFichaController) controladorChp).setDatos(printChp, trayectoriaChp.getId()));
        } catch (RyeSgfaException e) {
            ymclMostrarAdvertencia(e.getMessage());
        }
    }

    @FXML
    public void ymclOnHistorialFichas() {
        YmclEstudiante seleccionadoChp = tabla.getSelectionModel().getSelectedItem();
        if (seleccionadoChp == null) {
            ymclMostrarAdvertencia("Seleccione un estudiante.");
            return;
        }

        List<YmclTrayectoria> trayectoriasChp = trayectoriaServiceChp.ymclListarPorEstudiante(seleccionadoChp.getId());
        if (trayectoriasChp.isEmpty()) {
            ymclMostrarAdvertencia("El estudiante no tiene ninguna trayectoria registrada.");
            return;
        }

        if (trayectoriasChp.size() == 1) {
            ymclAbrirHistorial(trayectoriasChp.get(0));
            return;
        }

        ymclElegirTrayectoria(trayectoriasChp).ifPresent(this::ymclAbrirHistorial);
    }

    private void ymclAbrirHistorial(YmclTrayectoria trayectoriaChp) {
        navigationServiceChp.ryeAbrirVista("/fxml/ficha/historial-fichas.fxml",
                "Historial de fichas",
                controladorChp -> ((RacaHistorialFichasController) controladorChp).setTrayectoria(trayectoriaChp));
    }

    private void ymclMostrarFormulario(YmclEstudiante existenteChp) {
        Dialog<ButtonType> dialogChp = new Dialog<>();
        dialogChp.setTitle(existenteChp == null ? "Nuevo estudiante" : "Editar estudiante");
        dialogChp.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane gridChp = new GridPane();
        gridChp.setHgap(8);
        gridChp.setVgap(8);
        gridChp.setPadding(new Insets(16));

        TextField codigoFieldChp = new TextField();
        TextField dniFieldChp = new TextField();
        TextField nombresFieldChp = new TextField();
        TextField apellidoPaternoFieldChp = new TextField();
        TextField apellidoMaternoFieldChp = new TextField();
        DatePicker fechaNacimientoPickerChp = new DatePicker();
        ComboBox<YmclSexo> sexoComboChp = new ComboBox<>();
        sexoComboChp.getItems().add(null);
        sexoComboChp.getItems().addAll(YmclSexo.values());
        TextField direccionFieldChp = new TextField();
        TextField telefonoFieldChp = new TextField();
        TextField correoFieldChp = new TextField();

        Button escanearButtonChp = new Button("📷 Escanear documento (OCR)");
        Label estadoOcrLabelChp = new Label();
        estadoOcrLabelChp.getStyleClass().add("subtitle-label");
        escanearButtonChp.setOnAction(e -> ymclEscanearDocumento(dialogChp, escanearButtonChp, estadoOcrLabelChp, dniFieldChp,
                nombresFieldChp, apellidoPaternoFieldChp, apellidoMaternoFieldChp, fechaNacimientoPickerChp));

        if (existenteChp != null) {
            codigoFieldChp.setText(existenteChp.getCodigo());
            dniFieldChp.setText(existenteChp.getDni());
            nombresFieldChp.setText(existenteChp.getNombres());
            apellidoPaternoFieldChp.setText(existenteChp.getApellidoPaterno());
            apellidoMaternoFieldChp.setText(existenteChp.getApellidoMaterno());
            fechaNacimientoPickerChp.setValue(existenteChp.getFechaNacimiento());
            sexoComboChp.setValue(existenteChp.getSexo());
            direccionFieldChp.setText(existenteChp.getDireccion());
            telefonoFieldChp.setText(existenteChp.getTelefono());
            correoFieldChp.setText(existenteChp.getCorreo());
        }

        int filaChp = 0;
        if (existenteChp == null) {
            gridChp.addRow(filaChp++, escanearButtonChp, estadoOcrLabelChp);
        }
        gridChp.addRow(filaChp++, new Label("Codigo:"), codigoFieldChp);
        gridChp.addRow(filaChp++, new Label("DNI:"), dniFieldChp);
        gridChp.addRow(filaChp++, new Label("Nombres:"), nombresFieldChp);
        gridChp.addRow(filaChp++, new Label("Apellido paterno:"), apellidoPaternoFieldChp);
        gridChp.addRow(filaChp++, new Label("Apellido materno:"), apellidoMaternoFieldChp);
        gridChp.addRow(filaChp++, new Label("Fecha de nacimiento:"), fechaNacimientoPickerChp);
        gridChp.addRow(filaChp++, new Label("Sexo:"), sexoComboChp);
        gridChp.addRow(filaChp++, new Label("Direccion:"), direccionFieldChp);
        gridChp.addRow(filaChp++, new Label("Telefono:"), telefonoFieldChp);
        gridChp.addRow(filaChp, new Label("Correo:"), correoFieldChp);

        dialogChp.getDialogPane().setContent(gridChp);

        dialogChp.showAndWait().filter(botonChp -> botonChp == ButtonType.OK).ifPresent(botonChp -> {
            try {
                LocalDate fechaNacimientoChp = fechaNacimientoPickerChp.getValue();
                YmclSexo sexoChp = sexoComboChp.getValue();
                if (existenteChp == null) {
                    estudianteServiceChp.ymclCrear(codigoFieldChp.getText(), dniFieldChp.getText(), nombresFieldChp.getText(),
                            apellidoPaternoFieldChp.getText(), apellidoMaternoFieldChp.getText(), fechaNacimientoChp, sexoChp,
                            direccionFieldChp.getText(), telefonoFieldChp.getText(), correoFieldChp.getText());
                } else {
                    estudianteServiceChp.ymclEditar(existenteChp.getId(), codigoFieldChp.getText(), dniFieldChp.getText(),
                            nombresFieldChp.getText(), apellidoPaternoFieldChp.getText(), apellidoMaternoFieldChp.getText(),
                            fechaNacimientoChp, sexoChp, direccionFieldChp.getText(), telefonoFieldChp.getText(),
                            correoFieldChp.getText());
                }
                ymclOnMostrarTodos();
            } catch (RyeValidacionException e) {
                ymclMostrarAdvertencia(e.getMessage());
            }
        });
    }

    private void ymclEscanearDocumento(Dialog<ButtonType> dialogChp, Button escanearButtonChp, Label estadoLabelChp,
                                    TextField dniFieldChp, TextField nombresFieldChp, TextField apellidoPaternoFieldChp,
                                    TextField apellidoMaternoFieldChp, DatePicker fechaNacimientoPickerChp) {
        FileChooser chooserChp = new FileChooser();
        chooserChp.setTitle("Seleccionar documento escaneado");
        chooserChp.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imagenes y PDF", "*.png", "*.jpg", "*.jpeg", "*.pdf"));
        File archivoChp = chooserChp.showOpenDialog(dialogChp.getDialogPane().getScene().getWindow());
        if (archivoChp == null) {
            return;
        }

        escanearButtonChp.setDisable(true);
        estadoLabelChp.setText("Reconociendo texto, un momento...");

        Task<RahpExtractorDatosEstudiante.RahpDatosDetectados> tareaChp = new Task<>() {
            @Override
            protected RahpExtractorDatosEstudiante.RahpDatosDetectados call() {
                String textoChp = archivoChp.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".pdf")
                        ? ocrServiceChp.rahpReconocerTextoPdf(archivoChp)
                        : ocrServiceChp.rahpReconocerTexto(archivoChp);
                return RahpExtractorDatosEstudiante.rahpExtraer(textoChp);
            }
        };
        tareaChp.setOnSucceeded(e -> {
            RahpExtractorDatosEstudiante.RahpDatosDetectados datosChp = tareaChp.getValue();
            if (datosChp.dni() != null) {
                dniFieldChp.setText(datosChp.dni());
            }
            if (datosChp.nombres() != null) {
                nombresFieldChp.setText(datosChp.nombres());
            }
            if (datosChp.apellidoPaterno() != null) {
                apellidoPaternoFieldChp.setText(datosChp.apellidoPaterno());
            }
            if (datosChp.apellidoMaterno() != null) {
                apellidoMaternoFieldChp.setText(datosChp.apellidoMaterno());
            }
            if (datosChp.fechaNacimiento() != null) {
                fechaNacimientoPickerChp.setValue(datosChp.fechaNacimiento());
            }
            escanearButtonChp.setDisable(false);
            estadoLabelChp.setText(datosChp.dni() == null && datosChp.nombres() == null && datosChp.apellidoPaterno() == null
                    ? "No se reconocio ningun dato; revise manualmente."
                    : "Datos precargados -- revise y corrija antes de guardar.");
        });
        tareaChp.setOnFailed(e -> {
            escanearButtonChp.setDisable(false);
            estadoLabelChp.setText("");
            String mensajeChp = tareaChp.getException() instanceof RyeSgfaException se
                    ? se.getMessage() : "No se pudo procesar el documento.";
            ymclMostrarAdvertencia(mensajeChp);
        });
        Thread hiloChp = new Thread(tareaChp, "sgfa-ocr-estudiante");
        hiloChp.setDaemon(true);
        hiloChp.start();
    }

    private String ymclVacioSiNulo(String textoChp) {
        return textoChp == null ? "" : textoChp;
    }

    private void ymclMostrarAdvertencia(String mensajeChp) {
        new Alert(Alert.AlertType.WARNING, mensajeChp, ButtonType.OK).showAndWait();
    }
}
