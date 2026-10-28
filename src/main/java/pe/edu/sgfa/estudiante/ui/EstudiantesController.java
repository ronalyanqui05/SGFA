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

import pe.edu.sgfa.common.exception.SgfaException;
import pe.edu.sgfa.common.exception.ValidacionException;
import pe.edu.sgfa.estudiante.domain.EstadoEstudiante;
import pe.edu.sgfa.estudiante.domain.Estudiante;
import pe.edu.sgfa.estudiante.domain.Sexo;
import pe.edu.sgfa.estudiante.service.EstudianteService;
import net.sf.jasperreports.engine.JasperPrint;

import pe.edu.sgfa.ficha.service.FichaService;
import pe.edu.sgfa.ficha.ui.HistorialFichasController;
import pe.edu.sgfa.ficha.ui.VistaPreviaFichaController;
import pe.edu.sgfa.nota.ui.RegistroNotasController;
import pe.edu.sgfa.ocr.service.ExtractorDatosEstudiante;
import pe.edu.sgfa.ocr.service.OcrService;
import pe.edu.sgfa.shell.NavigationService;
import pe.edu.sgfa.trayectoria.domain.Trayectoria;
import pe.edu.sgfa.trayectoria.service.TrayectoriaService;
import pe.edu.sgfa.trayectoria.ui.TrayectoriaController;

@Component
public class EstudiantesController {

    private final EstudianteService estudianteService;
    private final NavigationService navigationService;
    private final TrayectoriaService trayectoriaService;
    private final FichaService fichaService;
    private final OcrService ocrService;

    @FXML
    private TextField busquedaField;
    @FXML
    private CheckBox incluirInactivosCheck;
    @FXML
    private TableView<Estudiante> tabla;
    @FXML
    private TableColumn<Estudiante, String> colCodigo;
    @FXML
    private TableColumn<Estudiante, String> colDni;
    @FXML
    private TableColumn<Estudiante, String> colApellidosNombres;
    @FXML
    private TableColumn<Estudiante, String> colEstado;

    public EstudiantesController(EstudianteService estudianteService, NavigationService navigationService,
                                  TrayectoriaService trayectoriaService, FichaService fichaService,
                                  OcrService ocrService) {
        this.estudianteService = estudianteService;
        this.navigationService = navigationService;
        this.trayectoriaService = trayectoriaService;
        this.fichaService = fichaService;
        this.ocrService = ocrService;
    }

    @FXML
    public void initialize() {
        colCodigo.setCellValueFactory(d -> new SimpleStringProperty(vacioSiNulo(d.getValue().getCodigo())));
        colDni.setCellValueFactory(d -> new SimpleStringProperty(vacioSiNulo(d.getValue().getDni())));
        colApellidosNombres.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getApellidoPaterno()
                        + (d.getValue().getApellidoMaterno() == null ? "" : " " + d.getValue().getApellidoMaterno())
                        + ", " + d.getValue().getNombres()));
        colEstado.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEstado().name()));

        javafx.animation.PauseTransition pausa = new javafx.animation.PauseTransition(javafx.util.Duration.millis(200));
        pausa.setOnFinished(e -> onBuscar());
        busquedaField.textProperty().addListener((obs, antes, ahora) -> pausa.playFromStart());
        incluirInactivosCheck.selectedProperty().addListener((obs, antes, ahora) -> onBuscar());

        onMostrarTodos();
    }

    @FXML
    public void onBuscar() {
        String txt = busquedaField.getText();
        Estudiante slccnd = tabla.getSelectionModel().getSelectedItem();
        if (txt == null || txt.isBlank()) {
            tabla.getItems().setAll(estudianteService.listar(incluirInactivosCheck.isSelected()));
        } else {
            tabla.getItems().setAll(estudianteService.buscar(txt));
        }
        if (slccnd != null) {
            tabla.getItems().stream().filter(e -> e.getId().equals(slccnd.getId())).findFirst()
                    .ifPresent(e -> tabla.getSelectionModel().select(e));
        }
    }

    @FXML
    public void onMostrarTodos() {
        busquedaField.clear();
        tabla.getItems().setAll(estudianteService.listar(incluirInactivosCheck.isSelected()));
    }

    @FXML
    public void onNuevo() {
        mostrarFormulario(null);
    }

    @FXML
    public void onEditar() {
        Estudiante slccnd = tabla.getSelectionModel().getSelectedItem();
        if (slccnd == null) {
            mostrarAdvertencia("Seleccione un estudiante.");
            return;
        }
        mostrarFormulario(slccnd);
    }

    @FXML
    public void onToggleEstado() {
        Estudiante slccnd = tabla.getSelectionModel().getSelectedItem();
        if (slccnd == null) {
            mostrarAdvertencia("Seleccione un estudiante.");
            return;
        }
        EstadoEstudiante nvEstd = slccnd.getEstado() == EstadoEstudiante.ACTIVO
                ? EstadoEstudiante.INACTIVO : EstadoEstudiante.ACTIVO;
        estudianteService.cambiarEstado(slccnd.getId(), nvEstd);
        onMostrarTodos();
    }

    @FXML
    public void onTrayectoria() {
        Estudiante slccnd = tabla.getSelectionModel().getSelectedItem();
        if (slccnd == null) {
            mostrarAdvertencia("Seleccione un estudiante.");
            return;
        }
        navigationService.abrirVista("/fxml/trayectoria/trayectoria.fxml",
                "Trayectoria academica",
                controlador -> ((TrayectoriaController) controlador).setEstudiante(slccnd));
    }

    @FXML
    public void onRegistrarNotas() {
        Estudiante slccnd = tabla.getSelectionModel().getSelectedItem();
        if (slccnd == null) {
            mostrarAdvertencia("Seleccione un estudiante.");
            return;
        }
        navigationService.abrirVista("/fxml/nota/registro-notas.fxml",
                "Registro de notas",
                controlador -> ((RegistroNotasController) controlador).setEstudiante(slccnd));
    }

    @FXML
    public void onGenerarFicha() {
        Estudiante slccnd = tabla.getSelectionModel().getSelectedItem();
        if (slccnd == null) {
            mostrarAdvertencia("Seleccione un estudiante.");
            return;
        }

        List<Trayectoria> tryctrs = trayectoriaService.listarPorEstudiante(slccnd.getId());
        if (tryctrs.isEmpty()) {
            mostrarAdvertencia("El estudiante no tiene ninguna trayectoria registrada. Registre una antes de generar la ficha.");
            return;
        }

        if (tryctrs.size() == 1) {
            generarFichaPara(tryctrs.get(0));
            return;
        }

        elegirTrayectoria(tryctrs).ifPresent(this::generarFichaPara);
    }

    private Optional<Trayectoria> elegirTrayectoria(List<Trayectoria> tryctrs) {
        ChoiceDialog<Trayectoria> dlgr = new ChoiceDialog<>(tryctrs.get(0), tryctrs);
        dlgr.setTitle("Generar ficha");
        dlgr.setHeaderText("El estudiante tiene varias trayectorias.");
        dlgr.setContentText("Seleccione la trayectoria para la ficha:");
        dlgr.getItems().setAll(tryctrs);
        return dlgr.showAndWait();
    }

    private void generarFichaPara(Trayectoria trayectoria) {
        try {
            JasperPrint prnt = fichaService.previsualizar(trayectoria.getId());
            navigationService.abrirVista("/fxml/ficha/vista-previa-ficha.fxml",
                    "Vista previa - Ficha academica",
                    controlador -> ((VistaPreviaFichaController) controlador).setDatos(prnt, trayectoria.getId()));
        } catch (SgfaException e) {
            mostrarAdvertencia(e.getMessage());
        }
    }

    @FXML
    public void onHistorialFichas() {
        Estudiante slccnd = tabla.getSelectionModel().getSelectedItem();
        if (slccnd == null) {
            mostrarAdvertencia("Seleccione un estudiante.");
            return;
        }

        List<Trayectoria> tryctrs = trayectoriaService.listarPorEstudiante(slccnd.getId());
        if (tryctrs.isEmpty()) {
            mostrarAdvertencia("El estudiante no tiene ninguna trayectoria registrada.");
            return;
        }

        if (tryctrs.size() == 1) {
            abrirHistorial(tryctrs.get(0));
            return;
        }

        elegirTrayectoria(tryctrs).ifPresent(this::abrirHistorial);
    }

    private void abrirHistorial(Trayectoria trayectoria) {
        navigationService.abrirVista("/fxml/ficha/historial-fichas.fxml",
                "Historial de fichas",
                controlador -> ((HistorialFichasController) controlador).setTrayectoria(trayectoria));
    }

    private void mostrarFormulario(Estudiante exstnt) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle(exstnt == null ? "Nuevo estudiante" : "Editar estudiante");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grd = new GridPane();
        grd.setHgap(8);
        grd.setVgap(8);
        grd.setPadding(new Insets(16));

        TextField cdgFld = new TextField();
        TextField dniFld = new TextField();
        TextField nmbrsFld = new TextField();
        TextField aplldPtrnFld = new TextField();
        TextField aplldMtrnFld = new TextField();
        DatePicker fchNcmntPckr = new DatePicker();
        ComboBox<Sexo> sxCmb = new ComboBox<>();
        sxCmb.getItems().add(null);
        sxCmb.getItems().addAll(Sexo.values());
        TextField drccnFld = new TextField();
        TextField tlfnFld = new TextField();
        TextField crrFld = new TextField();

        Button escnrBttn = new Button("📷 Escanear documento (OCR)");
        Label estdOcrLbl = new Label();
        estdOcrLbl.getStyleClass().add("subtitle-label");
        escnrBttn.setOnAction(e -> escanearDocumento(dlg, escnrBttn, estdOcrLbl, dniFld,
                nmbrsFld, aplldPtrnFld, aplldMtrnFld, fchNcmntPckr));

        if (exstnt != null) {
            cdgFld.setText(exstnt.getCodigo());
            dniFld.setText(exstnt.getDni());
            nmbrsFld.setText(exstnt.getNombres());
            aplldPtrnFld.setText(exstnt.getApellidoPaterno());
            aplldMtrnFld.setText(exstnt.getApellidoMaterno());
            fchNcmntPckr.setValue(exstnt.getFechaNacimiento());
            sxCmb.setValue(exstnt.getSexo());
            drccnFld.setText(exstnt.getDireccion());
            tlfnFld.setText(exstnt.getTelefono());
            crrFld.setText(exstnt.getCorreo());
        }

        int fl = 0;
        if (exstnt == null) {
            grd.addRow(fl++, escnrBttn, estdOcrLbl);
        }
        grd.addRow(fl++, new Label("Codigo:"), cdgFld);
        grd.addRow(fl++, new Label("DNI:"), dniFld);
        grd.addRow(fl++, new Label("Nombres:"), nmbrsFld);
        grd.addRow(fl++, new Label("Apellido paterno:"), aplldPtrnFld);
        grd.addRow(fl++, new Label("Apellido materno:"), aplldMtrnFld);
        grd.addRow(fl++, new Label("Fecha de nacimiento:"), fchNcmntPckr);
        grd.addRow(fl++, new Label("Sexo:"), sxCmb);
        grd.addRow(fl++, new Label("Direccion:"), drccnFld);
        grd.addRow(fl++, new Label("Telefono:"), tlfnFld);
        grd.addRow(fl, new Label("Correo:"), crrFld);

        dlg.getDialogPane().setContent(grd);

        dlg.showAndWait().filter(boton -> boton == ButtonType.OK).ifPresent(boton -> {
            try {
                LocalDate fechaNacimiento = fchNcmntPckr.getValue();
                Sexo sx = sxCmb.getValue();
                if (exstnt == null) {
                    estudianteService.crear(cdgFld.getText(), dniFld.getText(), nmbrsFld.getText(),
                            aplldPtrnFld.getText(), aplldMtrnFld.getText(), fechaNacimiento, sx,
                            drccnFld.getText(), tlfnFld.getText(), crrFld.getText());
                } else {
                    estudianteService.editar(exstnt.getId(), cdgFld.getText(), dniFld.getText(),
                            nmbrsFld.getText(), aplldPtrnFld.getText(), aplldMtrnFld.getText(),
                            fechaNacimiento, sx, drccnFld.getText(), tlfnFld.getText(),
                            crrFld.getText());
                }
                onMostrarTodos();
            } catch (ValidacionException e) {
                mostrarAdvertencia(e.getMessage());
            }
        });
    }

    private void escanearDocumento(Dialog<ButtonType> dlg, Button escnrBttn, Label estdLbl,
                                    TextField dniFld, TextField nmbrsFld, TextField aplldPtrnFld,
                                    TextField aplldMtrnFld, DatePicker fchNcmntPckr) {
        FileChooser chsr = new FileChooser();
        chsr.setTitle("Seleccionar documento escaneado");
        chsr.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imagenes y PDF", "*.png", "*.jpg", "*.jpeg", "*.pdf"));
        File archv = chsr.showOpenDialog(dlg.getDialogPane().getScene().getWindow());
        if (archv == null) {
            return;
        }

        escnrBttn.setDisable(true);
        estdLbl.setText("Reconociendo texto, un momento...");

        Task<ExtractorDatosEstudiante.DatosDetectados> tr = new Task<>() {
            @Override
            protected ExtractorDatosEstudiante.DatosDetectados call() {
                String txt = archv.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".pdf")
                        ? ocrService.reconocerTextoPdf(archv)
                        : ocrService.reconocerTexto(archv);
                return ExtractorDatosEstudiante.extraer(txt);
            }
        };
        tr.setOnSucceeded(e -> {
            ExtractorDatosEstudiante.DatosDetectados dts = tr.getValue();
            if (dts.dni() != null) {
                dniFld.setText(dts.dni());
            }
            if (dts.nombres() != null) {
                nmbrsFld.setText(dts.nombres());
            }
            if (dts.apellidoPaterno() != null) {
                aplldPtrnFld.setText(dts.apellidoPaterno());
            }
            if (dts.apellidoMaterno() != null) {
                aplldMtrnFld.setText(dts.apellidoMaterno());
            }
            if (dts.fechaNacimiento() != null) {
                fchNcmntPckr.setValue(dts.fechaNacimiento());
            }
            escnrBttn.setDisable(false);
            estdLbl.setText(dts.dni() == null && dts.nombres() == null && dts.apellidoPaterno() == null
                    ? "No se reconocio ningun dato; revise manualmente."
                    : "Datos precargados -- revise y corrija antes de guardar.");
        });
        tr.setOnFailed(e -> {
            escnrBttn.setDisable(false);
            estdLbl.setText("");
            String mnsj = tr.getException() instanceof SgfaException se
                    ? se.getMessage() : "No se pudo procesar el documento.";
            mostrarAdvertencia(mnsj);
        });
        Thread hl = new Thread(tr, "sgfa-ocr-estudiante");
        hl.setDaemon(true);
        hl.start();
    }

    private String vacioSiNulo(String txt) {
        return txt == null ? "" : txt;
    }

    private void mostrarAdvertencia(String mnsj) {
        new Alert(Alert.AlertType.WARNING, mnsj, ButtonType.OK).showAndWait();
    }
}
