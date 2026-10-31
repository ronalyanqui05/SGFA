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

import pe.edu.sgfa.common.exception.RyeValidacionException;
import pe.edu.sgfa.periodo.domain.YmclEstadoPeriodoAcademico;
import pe.edu.sgfa.periodo.domain.YmclPeriodoAcademico;
import pe.edu.sgfa.periodo.service.YmclPeriodoAcademicoService;
import pe.edu.sgfa.shell.RyeRefrescable;

@Component
public class YmclPeriodosTabController implements RyeRefrescable {

    private final YmclPeriodoAcademicoService periodoAcademicoServiceChp;

    @FXML
    private TableView<YmclPeriodoAcademico> tabla;
    @FXML
    private TableColumn<YmclPeriodoAcademico, String> colNombre;
    @FXML
    private TableColumn<YmclPeriodoAcademico, String> colAnio;
    @FXML
    private TableColumn<YmclPeriodoAcademico, String> colTipo;
    @FXML
    private TableColumn<YmclPeriodoAcademico, String> colFechas;
    @FXML
    private TableColumn<YmclPeriodoAcademico, String> colEstado;

    public YmclPeriodosTabController(YmclPeriodoAcademicoService periodoAcademicoServiceChp) {
        this.periodoAcademicoServiceChp = periodoAcademicoServiceChp;
    }

    @FXML
    public void initialize() {
        colNombre.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombre()));
        colAnio.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getAnio())));
        colTipo.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getTipo() == null ? "" : d.getValue().getTipo()));
        colFechas.setCellValueFactory(d -> new SimpleStringProperty(
                ymclTextoFecha(d.getValue().getFechaInicio()) + " - " + ymclTextoFecha(d.getValue().getFechaFin())));
        colEstado.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEstado().name()));
        ymclRecargar();
    }

    private void ymclRecargar() {
        tabla.getItems().setAll(periodoAcademicoServiceChp.ymclListar());
    }

    @Override
    public void ryeRefrescarAlMostrar() {
        ymclRecargar();
    }

    @FXML
    public void ymclOnNuevo() {
        ymclMostrarFormulario(null);
    }

    @FXML
    public void ymclOnEditar() {
        YmclPeriodoAcademico seleccionadoChp = tabla.getSelectionModel().getSelectedItem();
        if (seleccionadoChp == null) {
            ymclMostrarAdvertencia("Seleccione un periodo.");
            return;
        }
        ymclMostrarFormulario(seleccionadoChp);
    }

    @FXML
    public void ymclOnCerrar() {
        YmclPeriodoAcademico seleccionadoChp = tabla.getSelectionModel().getSelectedItem();
        if (seleccionadoChp == null) {
            ymclMostrarAdvertencia("Seleccione un periodo.");
            return;
        }
        periodoAcademicoServiceChp.ymclCambiarEstado(seleccionadoChp.getId(), YmclEstadoPeriodoAcademico.CERRADO);
        ymclRecargar();
    }

    private void ymclMostrarFormulario(YmclPeriodoAcademico existenteChp) {
        Dialog<ButtonType> dialogChp = new Dialog<>();
        dialogChp.setTitle(existenteChp == null ? "Nuevo periodo academico" : "Editar periodo academico");
        dialogChp.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane gridChp = new GridPane();
        gridChp.setHgap(8);
        gridChp.setVgap(8);
        gridChp.setPadding(new Insets(16));

        TextField nombreFieldChp = new TextField();
        TextField anioFieldChp = new TextField();
        TextField tipoFieldChp = new TextField();
        DatePicker inicioPickerChp = new DatePicker();
        DatePicker finPickerChp = new DatePicker();

        if (existenteChp != null) {
            nombreFieldChp.setText(existenteChp.getNombre());
            anioFieldChp.setText(String.valueOf(existenteChp.getAnio()));
            tipoFieldChp.setText(existenteChp.getTipo());
            inicioPickerChp.setValue(existenteChp.getFechaInicio());
            finPickerChp.setValue(existenteChp.getFechaFin());
        }

        int filaChp = 0;
        gridChp.addRow(filaChp++, new Label("Nombre (ej. 2026-I):"), nombreFieldChp);
        gridChp.addRow(filaChp++, new Label("Anio:"), anioFieldChp);
        gridChp.addRow(filaChp++, new Label("Tipo:"), tipoFieldChp);
        gridChp.addRow(filaChp++, new Label("Fecha inicio:"), inicioPickerChp);
        gridChp.addRow(filaChp, new Label("Fecha fin:"), finPickerChp);

        dialogChp.getDialogPane().setContent(gridChp);

        dialogChp.showAndWait().filter(botonChp -> botonChp == ButtonType.OK).ifPresent(botonChp -> {
            try {
                int anioChp = ymclParseEntero(anioFieldChp.getText());
                String tipoChp = tipoFieldChp.getText() == null || tipoFieldChp.getText().isBlank()
                        ? null : tipoFieldChp.getText().trim();
                LocalDate inicioChp = inicioPickerChp.getValue();
                LocalDate finChp = finPickerChp.getValue();

                if (existenteChp == null) {
                    periodoAcademicoServiceChp.ymclCrear(nombreFieldChp.getText().trim(), anioChp, tipoChp, inicioChp, finChp);
                } else {
                    periodoAcademicoServiceChp.ymclEditar(existenteChp.getId(), nombreFieldChp.getText().trim(), anioChp, tipoChp, inicioChp, finChp);
                }
                ymclRecargar();
            } catch (RyeValidacionException e) {
                ymclMostrarAdvertencia(e.getMessage());
            }
        });
    }

    private int ymclParseEntero(String textoChp) {
        try {
            return Integer.parseInt(textoChp.trim());
        } catch (NumberFormatException e) {
            throw new RyeValidacionException("Ingrese un anio valido.");
        }
    }

    private String ymclTextoFecha(LocalDate fechaChp) {
        return fechaChp == null ? "?" : fechaChp.toString();
    }

    private void ymclMostrarAdvertencia(String mensajeChp) {
        new Alert(Alert.AlertType.WARNING, mensajeChp, ButtonType.OK).showAndWait();
    }
}
