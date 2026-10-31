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
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import org.springframework.stereotype.Component;

import pe.edu.sgfa.common.exception.RyeValidacionException;
import pe.edu.sgfa.programa.domain.RacaCurso;
import pe.edu.sgfa.programa.service.RacaCursoService;
import pe.edu.sgfa.shell.RyeRefrescable;

@Component
public class RacaCursosTabController implements RyeRefrescable {

    private final RacaCursoService cursoServiceCrr;

    @FXML
    private TableView<RacaCurso> tabla;
    @FXML
    private TableColumn<RacaCurso, String> colCodigo;
    @FXML
    private TableColumn<RacaCurso, String> colNombre;
    @FXML
    private TableColumn<RacaCurso, String> colCreditos;
    @FXML
    private TableColumn<RacaCurso, String> colHoras;
    @FXML
    private TableColumn<RacaCurso, String> colTipo;
    @FXML
    private TableColumn<RacaCurso, String> colEstado;

    public RacaCursosTabController(RacaCursoService cursoServiceCrr) {
        this.cursoServiceCrr = cursoServiceCrr;
    }

    @FXML
    public void initialize() {
        colCodigo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCodigo()));
        colNombre.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombre()));
        colCreditos.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getCreditos())));
        colHoras.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getHoras() == null ? "" : String.valueOf(d.getValue().getHoras())));
        colTipo.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getTipo() == null ? "" : d.getValue().getTipo()));
        colEstado.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().isActivo() ? "Activo" : "Inactivo"));
        racaRecargar();
    }

    private void racaRecargar() {
        tabla.getItems().setAll(cursoServiceCrr.racaListar());
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
        RacaCurso seleccionadoCrr = tabla.getSelectionModel().getSelectedItem();
        if (seleccionadoCrr == null) {
            racaMostrarAdvertencia("Seleccione un curso.");
            return;
        }
        racaMostrarFormulario(seleccionadoCrr);
    }

    @FXML
    public void racaOnToggleActivo() {
        RacaCurso seleccionadoCrr = tabla.getSelectionModel().getSelectedItem();
        if (seleccionadoCrr == null) {
            racaMostrarAdvertencia("Seleccione un curso.");
            return;
        }
        cursoServiceCrr.racaActivarDesactivar(seleccionadoCrr.getId(), !seleccionadoCrr.isActivo());
        racaRecargar();
    }

    private void racaMostrarFormulario(RacaCurso existenteCrr) {
        Dialog<ButtonType> dialogCrr = new Dialog<>();
        dialogCrr.setTitle(existenteCrr == null ? "Nuevo curso" : "Editar curso");
        dialogCrr.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane gridCrr = new GridPane();
        gridCrr.setHgap(8);
        gridCrr.setVgap(8);
        gridCrr.setPadding(new Insets(16));

        TextField codigoFieldCrr = new TextField();
        TextField nombreFieldCrr = new TextField();
        TextField creditosFieldCrr = new TextField();
        TextField horasFieldCrr = new TextField();
        TextField tipoFieldCrr = new TextField();

        if (existenteCrr != null) {
            codigoFieldCrr.setText(existenteCrr.getCodigo());
            nombreFieldCrr.setText(existenteCrr.getNombre());
            creditosFieldCrr.setText(String.valueOf(existenteCrr.getCreditos()));
            horasFieldCrr.setText(existenteCrr.getHoras() == null ? "" : String.valueOf(existenteCrr.getHoras()));
            tipoFieldCrr.setText(existenteCrr.getTipo());
        }

        int filaCrr = 0;
        gridCrr.addRow(filaCrr++, new Label("Codigo:"), codigoFieldCrr);
        gridCrr.addRow(filaCrr++, new Label("Nombre:"), nombreFieldCrr);
        gridCrr.addRow(filaCrr++, new Label("Creditos:"), creditosFieldCrr);
        gridCrr.addRow(filaCrr++, new Label("Horas:"), horasFieldCrr);
        gridCrr.addRow(filaCrr, new Label("Tipo:"), tipoFieldCrr);

        dialogCrr.getDialogPane().setContent(gridCrr);

        dialogCrr.showAndWait().filter(botonCrr -> botonCrr == ButtonType.OK).ifPresent(botonCrr -> {
            try {
                int creditosCrr = racaParseEntero(creditosFieldCrr.getText(), "creditos");
                Integer horasCrr = horasFieldCrr.getText() == null || horasFieldCrr.getText().isBlank()
                        ? null : racaParseEntero(horasFieldCrr.getText(), "horas");
                String tipoCrr = tipoFieldCrr.getText() == null || tipoFieldCrr.getText().isBlank()
                        ? null : tipoFieldCrr.getText().trim();

                if (existenteCrr == null) {
                    cursoServiceCrr.racaCrear(codigoFieldCrr.getText().trim(), nombreFieldCrr.getText().trim(), creditosCrr, horasCrr, tipoCrr);
                } else {
                    cursoServiceCrr.racaEditar(existenteCrr.getId(), codigoFieldCrr.getText().trim(), nombreFieldCrr.getText().trim(),
                            creditosCrr, horasCrr, tipoCrr);
                }
                racaRecargar();
            } catch (RyeValidacionException e) {
                racaMostrarAdvertencia(e.getMessage());
            }
        });
    }

    private int racaParseEntero(String textoCrr, String campoCrr) {
        try {
            return Integer.parseInt(textoCrr.trim());
        } catch (NumberFormatException e) {
            throw new RyeValidacionException("Ingrese un numero valido para " + campoCrr + ".");
        }
    }

    private void racaMostrarAdvertencia(String mensajeCrr) {
        new Alert(Alert.AlertType.WARNING, mensajeCrr, ButtonType.OK).showAndWait();
    }
}
