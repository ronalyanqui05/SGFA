package pe.edu.sgfa.programa.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import org.springframework.stereotype.Component;

import pe.edu.sgfa.common.exception.RyeValidacionException;
import pe.edu.sgfa.programa.domain.RacaProgramaEstudio;
import pe.edu.sgfa.programa.service.RacaProgramaEstudioService;
import pe.edu.sgfa.shell.RyeRefrescable;

@Component
public class RacaProgramasTabController implements RyeRefrescable {

    private final RacaProgramaEstudioService programaEstudioServiceCrr;

    @FXML
    private TableView<RacaProgramaEstudio> tabla;
    @FXML
    private TableColumn<RacaProgramaEstudio, String> colCodigo;
    @FXML
    private TableColumn<RacaProgramaEstudio, String> colNombre;
    @FXML
    private TableColumn<RacaProgramaEstudio, String> colDuracion;
    @FXML
    private TableColumn<RacaProgramaEstudio, String> colEstado;

    public RacaProgramasTabController(RacaProgramaEstudioService programaEstudioServiceCrr) {
        this.programaEstudioServiceCrr = programaEstudioServiceCrr;
    }

    @FXML
    public void initialize() {
        colCodigo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCodigo()));
        colNombre.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombre()));
        colDuracion.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getDuracionCiclos())));
        colEstado.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().isActivo() ? "Activo" : "Inactivo"));
        racaRecargar();
    }

    private void racaRecargar() {
        tabla.getItems().setAll(programaEstudioServiceCrr.racaListar());
    }

    @Override
    public void ryeRefrescarAlMostrar() {
        racaRecargar();
    }

    @FXML
    public void racaOnNuevo() {
        racaMostrarFormulario(null);
    }

    @FXML
    public void racaOnEditar() {
        RacaProgramaEstudio seleccionadoCrr = tabla.getSelectionModel().getSelectedItem();
        if (seleccionadoCrr == null) {
            racaMostrarAdvertencia("Seleccione un programa.");
            return;
        }
        racaMostrarFormulario(seleccionadoCrr);
    }

    @FXML
    public void racaOnToggleActivo() {
        RacaProgramaEstudio seleccionadoCrr = tabla.getSelectionModel().getSelectedItem();
        if (seleccionadoCrr == null) {
            racaMostrarAdvertencia("Seleccione un programa.");
            return;
        }
        programaEstudioServiceCrr.racaActivarDesactivar(seleccionadoCrr.getId(), !seleccionadoCrr.isActivo());
        racaRecargar();
    }

    private void racaMostrarFormulario(RacaProgramaEstudio existenteCrr) {
        Dialog<ButtonType> dialogCrr = new Dialog<>();
        dialogCrr.setTitle(existenteCrr == null ? "Nuevo programa" : "Editar programa");
        dialogCrr.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane gridCrr = new GridPane();
        gridCrr.setHgap(8);
        gridCrr.setVgap(8);
        gridCrr.setPadding(new Insets(16));

        TextField codigoFieldCrr = new TextField();
        TextField nombreFieldCrr = new TextField();
        TextArea descripcionFieldCrr = new TextArea();
        descripcionFieldCrr.setPrefRowCount(3);
        TextField duracionFieldCrr = new TextField();

        if (existenteCrr != null) {
            codigoFieldCrr.setText(existenteCrr.getCodigo());
            nombreFieldCrr.setText(existenteCrr.getNombre());
            descripcionFieldCrr.setText(existenteCrr.getDescripcion());
            duracionFieldCrr.setText(String.valueOf(existenteCrr.getDuracionCiclos()));
        }

        int filaCrr = 0;
        gridCrr.addRow(filaCrr++, new Label("Codigo:"), codigoFieldCrr);
        gridCrr.addRow(filaCrr++, new Label("Nombre:"), nombreFieldCrr);
        gridCrr.addRow(filaCrr++, new Label("Descripcion:"), descripcionFieldCrr);
        gridCrr.addRow(filaCrr, new Label("Duracion (ciclos):"), duracionFieldCrr);

        dialogCrr.getDialogPane().setContent(gridCrr);

        dialogCrr.showAndWait().filter(botonCrr -> botonCrr == ButtonType.OK).ifPresent(botonCrr -> {
            try {
                int duracionCrr = racaParseEntero(duracionFieldCrr.getText());
                if (existenteCrr == null) {
                    programaEstudioServiceCrr.racaCrear(codigoFieldCrr.getText().trim(), nombreFieldCrr.getText().trim(),
                            descripcionFieldCrr.getText(), duracionCrr);
                } else {
                    programaEstudioServiceCrr.racaEditar(existenteCrr.getId(), codigoFieldCrr.getText().trim(),
                            nombreFieldCrr.getText().trim(), descripcionFieldCrr.getText(), duracionCrr);
                }
                racaRecargar();
            } catch (RyeValidacionException e) {
                racaMostrarAdvertencia(e.getMessage());
            }
        });
    }

    private int racaParseEntero(String textoCrr) {
        try {
            return Integer.parseInt(textoCrr.trim());
        } catch (NumberFormatException e) {
            throw new RyeValidacionException("Ingrese un numero valido.");
        }
    }

    private void racaMostrarAdvertencia(String mensajeCrr) {
        new Alert(Alert.AlertType.WARNING, mensajeCrr, ButtonType.OK).showAndWait();
    }
}
