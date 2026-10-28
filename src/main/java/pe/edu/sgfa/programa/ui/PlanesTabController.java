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

import pe.edu.sgfa.common.exception.ValidacionException;
import pe.edu.sgfa.programa.domain.EstadoPlanEstudio;
import pe.edu.sgfa.programa.domain.PlanEstudio;
import pe.edu.sgfa.programa.domain.ProgramaEstudio;
import pe.edu.sgfa.programa.service.PlanEstudioService;
import pe.edu.sgfa.programa.service.ProgramaEstudioService;
import pe.edu.sgfa.shell.ComboBoxUtil;
import pe.edu.sgfa.shell.Refrescable;

@Component
public class PlanesTabController implements Refrescable {

    private final ProgramaEstudioService programaEstudioService;
    private final PlanEstudioService planEstudioService;

    @FXML
    private ComboBox<ProgramaEstudio> programaCombo;
    @FXML
    private TableView<PlanEstudio> tabla;
    @FXML
    private TableColumn<PlanEstudio, String> colCodigo;
    @FXML
    private TableColumn<PlanEstudio, String> colNombre;
    @FXML
    private TableColumn<PlanEstudio, String> colFechas;
    @FXML
    private TableColumn<PlanEstudio, String> colEstado;

    public PlanesTabController(ProgramaEstudioService programaEstudioService, PlanEstudioService planEstudioService) {
        this.programaEstudioService = programaEstudioService;
        this.planEstudioService = planEstudioService;
    }

    @FXML
    public void initialize() {
        programaCombo.setConverter(convertidorPrograma());
        colCodigo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCodigo()));
        colNombre.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombre()));
        colFechas.setCellValueFactory(d -> new SimpleStringProperty(
                textoFecha(d.getValue().getFechaInicio()) + " - " + textoFecha(d.getValue().getFechaFin())));
        colEstado.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEstado().name()));

        programaCombo.getItems().setAll(programaEstudioService.listar());
        programaCombo.valueProperty().addListener((obs, anterior, nuevo) -> recargar());
        if (!programaCombo.getItems().isEmpty()) {
            programaCombo.getSelectionModel().selectFirst();
        }
    }

    private void recargar() {
        ProgramaEstudio programa = programaCombo.getValue();
        tabla.getItems().setAll(programa == null ? List.of() : planEstudioService.listarPorPrograma(programa.getId()));
    }

    @Override
    public void refrescarAlMostrar() {
        ComboBoxUtil.reemplazarPreservandoSeleccion(programaCombo, programaEstudioService.listar());
        recargar();
    }

    @FXML
    public void onNuevo() {
        if (programaCombo.getValue() == null) {
            mostrarAdvertencia("Seleccione un programa primero.");
            return;
        }
        mostrarFormulario(null);
    }

    @FXML
    public void onEditar() {
        PlanEstudio slccnd = tabla.getSelectionModel().getSelectedItem();
        if (slccnd == null) {
            mostrarAdvertencia("Seleccione un plan.");
            return;
        }
        mostrarFormulario(slccnd);
    }

    @FXML
    public void onCerrar() {
        PlanEstudio slccnd = tabla.getSelectionModel().getSelectedItem();
        if (slccnd == null) {
            mostrarAdvertencia("Seleccione un plan.");
            return;
        }
        planEstudioService.cambiarEstado(slccnd.getId(), EstadoPlanEstudio.CERRADO);
        recargar();
    }

    @FXML
    public void onAbrir() {
        PlanEstudio slccnd = tabla.getSelectionModel().getSelectedItem();
        if (slccnd == null) {
            mostrarAdvertencia("Seleccione un plan.");
            return;
        }
        planEstudioService.cambiarEstado(slccnd.getId(), EstadoPlanEstudio.ACTIVO);
        recargar();
    }

    @FXML
    public void onEliminar() {
        PlanEstudio slccnd = tabla.getSelectionModel().getSelectedItem();
        if (slccnd == null) {
            mostrarAdvertencia("Seleccione un plan.");
            return;
        }
        Alert cnfrmcn = new Alert(Alert.AlertType.CONFIRMATION,
                "Esto eliminara el plan '" + slccnd.getCodigo() + " - " + slccnd.getNombre()
                        + "' de forma permanente.\n\n¿Desea continuar?",
                ButtonType.YES, ButtonType.NO);
        cnfrmcn.setHeaderText("Eliminar plan de estudio");
        if (cnfrmcn.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
            return;
        }
        try {
            planEstudioService.eliminar(slccnd.getId());
            recargar();
        } catch (ValidacionException e) {
            mostrarAdvertencia(e.getMessage());
        }
    }

    private void mostrarFormulario(PlanEstudio exstnt) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle(exstnt == null ? "Nuevo plan de estudio" : "Editar plan de estudio");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grd = new GridPane();
        grd.setHgap(8);
        grd.setVgap(8);
        grd.setPadding(new Insets(16));

        TextField cdgFld = new TextField();
        TextField nmbrFld = new TextField();
        DatePicker incPckr = new DatePicker();
        DatePicker finPckr = new DatePicker();

        if (exstnt != null) {
            cdgFld.setText(exstnt.getCodigo());
            nmbrFld.setText(exstnt.getNombre());
            incPckr.setValue(exstnt.getFechaInicio());
            finPckr.setValue(exstnt.getFechaFin());
        }

        int fl = 0;
        grd.addRow(fl++, new Label("Codigo:"), cdgFld);
        grd.addRow(fl++, new Label("Nombre:"), nmbrFld);
        grd.addRow(fl++, new Label("Fecha inicio:"), incPckr);
        grd.addRow(fl, new Label("Fecha fin:"), finPckr);

        dlg.getDialogPane().setContent(grd);

        dlg.showAndWait().filter(boton -> boton == ButtonType.OK).ifPresent(boton -> {
            try {
                LocalDate inc = incPckr.getValue();
                LocalDate fin = finPckr.getValue();
                if (exstnt == null) {
                    planEstudioService.crear(programaCombo.getValue().getId(), cdgFld.getText().trim(),
                            nmbrFld.getText().trim(), inc, fin);
                } else {
                    planEstudioService.editar(exstnt.getId(), cdgFld.getText().trim(),
                            nmbrFld.getText().trim(), inc, fin);
                }
                recargar();
            } catch (ValidacionException e) {
                mostrarAdvertencia(e.getMessage());
            }
        });
    }

    private StringConverter<ProgramaEstudio> convertidorPrograma() {
        return new StringConverter<>() {
            @Override
            public String toString(ProgramaEstudio programa) {
                return programa == null ? "" : programa.getCodigo() + " - " + programa.getNombre();
            }

            @Override
            public ProgramaEstudio fromString(String s) {
                return null;
            }
        };
    }

    private String textoFecha(LocalDate fch) {
        return fch == null ? "?" : fch.toString();
    }

    private void mostrarAdvertencia(String mnsj) {
        new Alert(Alert.AlertType.WARNING, mnsj, ButtonType.OK).showAndWait();
    }
}
