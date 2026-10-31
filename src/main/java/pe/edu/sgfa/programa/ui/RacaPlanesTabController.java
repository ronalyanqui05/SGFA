package pe.edu.sgfa.programa.ui;

import java.time.LocalDate;
import java.util.List;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;
import org.springframework.stereotype.Component;

import pe.edu.sgfa.common.exception.RyeValidacionException;
import pe.edu.sgfa.programa.domain.RacaEstadoPlanEstudio;
import pe.edu.sgfa.programa.domain.RacaPlanEstudio;
import pe.edu.sgfa.programa.domain.RacaProgramaEstudio;
import pe.edu.sgfa.programa.service.RacaPlanEstudioService;
import pe.edu.sgfa.programa.service.RacaProgramaEstudioService;
import pe.edu.sgfa.shell.RyeComboBoxUtil;
import pe.edu.sgfa.shell.RyeRefrescable;

@Component
public class RacaPlanesTabController implements RyeRefrescable {

    private final RacaProgramaEstudioService programaEstudioServiceCrr;
    private final RacaPlanEstudioService planEstudioServiceCrr;

    @FXML
    private ComboBox<RacaProgramaEstudio> programaCombo;
    @FXML
    private TableView<RacaPlanEstudio> tabla;
    @FXML
    private TableColumn<RacaPlanEstudio, String> colCodigo;
    @FXML
    private TableColumn<RacaPlanEstudio, String> colNombre;
    @FXML
    private TableColumn<RacaPlanEstudio, String> colFechas;
    @FXML
    private TableColumn<RacaPlanEstudio, String> colEstado;

    public RacaPlanesTabController(RacaProgramaEstudioService programaEstudioServiceCrr, RacaPlanEstudioService planEstudioServiceCrr) {
        this.programaEstudioServiceCrr = programaEstudioServiceCrr;
        this.planEstudioServiceCrr = planEstudioServiceCrr;
    }

    @FXML
    public void initialize() {
        programaCombo.setConverter(racaConvertidorPrograma());
        colCodigo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCodigo()));
        colNombre.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombre()));
        colFechas.setCellValueFactory(d -> new SimpleStringProperty(
                racaTextoFecha(d.getValue().getFechaInicio()) + " - " + racaTextoFecha(d.getValue().getFechaFin())));
        colEstado.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEstado().name()));

        programaCombo.getItems().setAll(programaEstudioServiceCrr.racaListar());
        programaCombo.valueProperty().addListener((obsCrr, anteriorCrr, nuevoCrr) -> racaRecargar());
        if (!programaCombo.getItems().isEmpty()) {
            programaCombo.getSelectionModel().selectFirst();
        }
    }

    private void racaRecargar() {
        RacaProgramaEstudio programaCrr = programaCombo.getValue();
        tabla.getItems().setAll(programaCrr == null ? List.of() : planEstudioServiceCrr.racaListarPorPrograma(programaCrr.getId()));
    }

    @Override
    public void ryeRefrescarAlMostrar() {
        RyeComboBoxUtil.ryeReemplazarPreservandoSeleccion(programaCombo, programaEstudioServiceCrr.racaListar());
        racaRecargar();
    }

    @FXML
    public void racaOnNuevo() {
        if (programaCombo.getValue() == null) {
            racaMostrarAdvertencia("Seleccione un programa primero.");
            return;
        }
        racaMostrarFormulario(null);
    }

    @FXML
    public void racaOnEditar() {
        RacaPlanEstudio seleccionadoCrr = tabla.getSelectionModel().getSelectedItem();
        if (seleccionadoCrr == null) {
            racaMostrarAdvertencia("Seleccione un plan.");
            return;
        }
        racaMostrarFormulario(seleccionadoCrr);
    }

    @FXML
    public void racaOnCerrar() {
        RacaPlanEstudio seleccionadoCrr = tabla.getSelectionModel().getSelectedItem();
        if (seleccionadoCrr == null) {
            racaMostrarAdvertencia("Seleccione un plan.");
            return;
        }
        planEstudioServiceCrr.racaCambiarEstado(seleccionadoCrr.getId(), RacaEstadoPlanEstudio.CERRADO);
        racaRecargar();
    }

    @FXML
    public void racaOnAbrir() {
        RacaPlanEstudio seleccionadoCrr = tabla.getSelectionModel().getSelectedItem();
        if (seleccionadoCrr == null) {
            racaMostrarAdvertencia("Seleccione un plan.");
            return;
        }
        planEstudioServiceCrr.racaCambiarEstado(seleccionadoCrr.getId(), RacaEstadoPlanEstudio.ACTIVO);
        racaRecargar();
    }

    @FXML
    public void racaOnEliminar() {
        RacaPlanEstudio seleccionadoCrr = tabla.getSelectionModel().getSelectedItem();
        if (seleccionadoCrr == null) {
            racaMostrarAdvertencia("Seleccione un plan.");
            return;
        }
        Alert confirmacionCrr = new Alert(Alert.AlertType.CONFIRMATION,
                "Esto eliminara el plan '" + seleccionadoCrr.getCodigo() + " - " + seleccionadoCrr.getNombre()
                        + "' de forma permanente.\n\n¿Desea continuar?",
                ButtonType.YES, ButtonType.NO);
        confirmacionCrr.setHeaderText("Eliminar plan de estudio");
        if (confirmacionCrr.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
            return;
        }
        try {
            planEstudioServiceCrr.racaEliminar(seleccionadoCrr.getId());
            racaRecargar();
        } catch (RyeValidacionException e) {
            racaMostrarAdvertencia(e.getMessage());
        }
    }

    private void racaMostrarFormulario(RacaPlanEstudio existenteCrr) {
        Dialog<ButtonType> dialogCrr = new Dialog<>();
        dialogCrr.setTitle(existenteCrr == null ? "Nuevo plan de estudio" : "Editar plan de estudio");
        dialogCrr.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane gridCrr = new GridPane();
        gridCrr.setHgap(8);
        gridCrr.setVgap(8);
        gridCrr.setPadding(new Insets(16));

        TextField codigoFieldCrr = new TextField();
        TextField nombreFieldCrr = new TextField();
        DatePicker inicioPickerCrr = new DatePicker();
        DatePicker finPickerCrr = new DatePicker();

        if (existenteCrr != null) {
            codigoFieldCrr.setText(existenteCrr.getCodigo());
            nombreFieldCrr.setText(existenteCrr.getNombre());
            inicioPickerCrr.setValue(existenteCrr.getFechaInicio());
            finPickerCrr.setValue(existenteCrr.getFechaFin());
        }

        int filaCrr = 0;
        gridCrr.addRow(filaCrr++, new Label("Codigo:"), codigoFieldCrr);
        gridCrr.addRow(filaCrr++, new Label("Nombre:"), nombreFieldCrr);
        gridCrr.addRow(filaCrr++, new Label("Fecha inicio:"), inicioPickerCrr);
        gridCrr.addRow(filaCrr, new Label("Fecha fin:"), finPickerCrr);

        dialogCrr.getDialogPane().setContent(gridCrr);

        dialogCrr.showAndWait().filter(botonCrr -> botonCrr == ButtonType.OK).ifPresent(botonCrr -> {
            try {
                LocalDate inicioCrr = inicioPickerCrr.getValue();
                LocalDate finCrr = finPickerCrr.getValue();
                if (existenteCrr == null) {
                    planEstudioServiceCrr.racaCrear(programaCombo.getValue().getId(), codigoFieldCrr.getText().trim(),
                            nombreFieldCrr.getText().trim(), inicioCrr, finCrr);
                } else {
                    planEstudioServiceCrr.racaEditar(existenteCrr.getId(), codigoFieldCrr.getText().trim(),
                            nombreFieldCrr.getText().trim(), inicioCrr, finCrr);
                }
                racaRecargar();
            } catch (RyeValidacionException e) {
                racaMostrarAdvertencia(e.getMessage());
            }
        });
    }

    private StringConverter<RacaProgramaEstudio> racaConvertidorPrograma() {
        return new StringConverter<>() {
            @Override
            public String toString(RacaProgramaEstudio programaCrr) {
                return programaCrr == null ? "" : programaCrr.getCodigo() + " - " + programaCrr.getNombre();
            }

            @Override
            public RacaProgramaEstudio fromString(String s) {
                return null;
            }
        };
    }

    private String racaTextoFecha(LocalDate fechaCrr) {
        return fechaCrr == null ? "?" : fechaCrr.toString();
    }

    private void racaMostrarAdvertencia(String mensajeCrr) {
        new Alert(Alert.AlertType.WARNING, mensajeCrr, ButtonType.OK).showAndWait();
    }
}
