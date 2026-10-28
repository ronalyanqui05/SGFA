package pe.edu.sgfa.periodo.ui;

import java.time.LocalDate;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import org.springframework.stereotype.Component;

import pe.edu.sgfa.common.exception.ValidacionException;
import pe.edu.sgfa.periodo.domain.EstadoPeriodoAcademico;
import pe.edu.sgfa.periodo.domain.PeriodoAcademico;
import pe.edu.sgfa.periodo.service.PeriodoAcademicoService;
import pe.edu.sgfa.shell.Refrescable;

@Component
public class PeriodosTabController implements Refrescable {

    private final PeriodoAcademicoService periodoAcademicoService;

    @FXML
    private TableView<PeriodoAcademico> tabla;
    @FXML
    private TableColumn<PeriodoAcademico, String> colNombre;
    @FXML
    private TableColumn<PeriodoAcademico, String> colAnio;
    @FXML
    private TableColumn<PeriodoAcademico, String> colTipo;
    @FXML
    private TableColumn<PeriodoAcademico, String> colFechas;
    @FXML
    private TableColumn<PeriodoAcademico, String> colEstado;

    public PeriodosTabController(PeriodoAcademicoService periodoAcademicoService) {
        this.periodoAcademicoService = periodoAcademicoService;
    }

    @FXML
    public void initialize() {
        colNombre.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombre()));
        colAnio.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getAnio())));
        colTipo.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getTipo() == null ? "" : d.getValue().getTipo()));
        colFechas.setCellValueFactory(d -> new SimpleStringProperty(
                textoFecha(d.getValue().getFechaInicio()) + " - " + textoFecha(d.getValue().getFechaFin())));
        colEstado.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEstado().name()));
        recargar();
    }

    private void recargar() {
        tabla.getItems().setAll(periodoAcademicoService.listar());
    }

    @Override
    public void refrescarAlMostrar() {
        recargar();
    }

    @FXML
    public void onNuevo() {
        mostrarFormulario(null);
    }

    @FXML
    public void onEditar() {
        PeriodoAcademico slccnd = tabla.getSelectionModel().getSelectedItem();
        if (slccnd == null) {
            mostrarAdvertencia("Seleccione un periodo.");
            return;
        }
        mostrarFormulario(slccnd);
    }

    @FXML
    public void onCerrar() {
        PeriodoAcademico slccnd = tabla.getSelectionModel().getSelectedItem();
        if (slccnd == null) {
            mostrarAdvertencia("Seleccione un periodo.");
            return;
        }
        periodoAcademicoService.cambiarEstado(slccnd.getId(), EstadoPeriodoAcademico.CERRADO);
        recargar();
    }

    private void mostrarFormulario(PeriodoAcademico exstnt) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle(exstnt == null ? "Nuevo periodo academico" : "Editar periodo academico");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grd = new GridPane();
        grd.setHgap(8);
        grd.setVgap(8);
        grd.setPadding(new Insets(16));

        TextField nmbrFld = new TextField();
        TextField anFld = new TextField();
        TextField tpFld = new TextField();
        DatePicker incPckr = new DatePicker();
        DatePicker finPckr = new DatePicker();

        if (exstnt != null) {
            nmbrFld.setText(exstnt.getNombre());
            anFld.setText(String.valueOf(exstnt.getAnio()));
            tpFld.setText(exstnt.getTipo());
            incPckr.setValue(exstnt.getFechaInicio());
            finPckr.setValue(exstnt.getFechaFin());
        }

        int fl = 0;
        grd.addRow(fl++, new Label("Nombre (ej. 2026-I):"), nmbrFld);
        grd.addRow(fl++, new Label("Anio:"), anFld);
        grd.addRow(fl++, new Label("Tipo:"), tpFld);
        grd.addRow(fl++, new Label("Fecha inicio:"), incPckr);
        grd.addRow(fl, new Label("Fecha fin:"), finPckr);

        dlg.getDialogPane().setContent(grd);

        dlg.showAndWait().filter(boton -> boton == ButtonType.OK).ifPresent(boton -> {
            try {
                int an = parseEntero(anFld.getText());
                String tp = tpFld.getText() == null || tpFld.getText().isBlank()
                        ? null : tpFld.getText().trim();
                LocalDate inc = incPckr.getValue();
                LocalDate fin = finPckr.getValue();

                if (exstnt == null) {
                    periodoAcademicoService.crear(nmbrFld.getText().trim(), an, tp, inc, fin);
                } else {
                    periodoAcademicoService.editar(exstnt.getId(), nmbrFld.getText().trim(), an, tp, inc, fin);
                }
                recargar();
            } catch (ValidacionException e) {
                mostrarAdvertencia(e.getMessage());
            }
        });
    }

    private int parseEntero(String txt) {
        try {
            return Integer.parseInt(txt.trim());
        } catch (NumberFormatException e) {
            throw new ValidacionException("Ingrese un anio valido.");
        }
    }

    private String textoFecha(LocalDate fch) {
        return fch == null ? "?" : fch.toString();
    }

    private void mostrarAdvertencia(String mnsj) {
        new Alert(Alert.AlertType.WARNING, mnsj, ButtonType.OK).showAndWait();
    }
}
