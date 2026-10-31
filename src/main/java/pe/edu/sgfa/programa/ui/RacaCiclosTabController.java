package pe.edu.sgfa.programa.ui;

import java.util.List;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;
import org.springframework.stereotype.Component;

import pe.edu.sgfa.common.exception.RyeValidacionException;
import pe.edu.sgfa.programa.domain.RacaCiclo;
import pe.edu.sgfa.programa.domain.RacaPlanEstudio;
import pe.edu.sgfa.programa.domain.RacaProgramaEstudio;
import pe.edu.sgfa.programa.service.RacaCicloService;
import pe.edu.sgfa.programa.service.RacaPlanEstudioService;
import pe.edu.sgfa.programa.service.RacaProgramaEstudioService;
import pe.edu.sgfa.shell.RyeComboBoxUtil;
import pe.edu.sgfa.shell.RyeRefrescable;

@Component
public class RacaCiclosTabController implements RyeRefrescable {

    private final RacaProgramaEstudioService programaEstudioServiceCrr;
    private final RacaPlanEstudioService planEstudioServiceCrr;
    private final RacaCicloService cicloServiceCrr;

    @FXML
    private ComboBox<RacaProgramaEstudio> programaCombo;
    @FXML
    private ComboBox<RacaPlanEstudio> planCombo;
    @FXML
    private TableView<RacaCiclo> tabla;
    @FXML
    private TableColumn<RacaCiclo, String> colNumero;
    @FXML
    private TableColumn<RacaCiclo, String> colNombre;
    @FXML
    private TableColumn<RacaCiclo, String> colOrden;

    public RacaCiclosTabController(RacaProgramaEstudioService programaEstudioServiceCrr, RacaPlanEstudioService planEstudioServiceCrr,
                                RacaCicloService cicloServiceCrr) {
        this.programaEstudioServiceCrr = programaEstudioServiceCrr;
        this.planEstudioServiceCrr = planEstudioServiceCrr;
        this.cicloServiceCrr = cicloServiceCrr;
    }

    @FXML
    public void initialize() {
        programaCombo.setConverter(racaConvertidor(p -> p.getCodigo() + " - " + p.getNombre()));
        planCombo.setConverter(racaConvertidor(p -> p.getCodigo() + " - " + p.getNombre()));

        colNumero.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getNumero())));
        colNombre.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombre()));
        colOrden.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getOrden())));

        programaCombo.getItems().setAll(programaEstudioServiceCrr.racaListar());
        programaCombo.valueProperty().addListener((obsCrr, anteriorCrr, nuevoCrr) -> racaRecargarPlanes());
        planCombo.valueProperty().addListener((obsCrr, anteriorCrr, nuevoCrr) -> racaRecargarCiclos());
        if (!programaCombo.getItems().isEmpty()) {
            programaCombo.getSelectionModel().selectFirst();
        }
    }

    private void racaRecargarPlanes() {
        RacaProgramaEstudio programaCrr = programaCombo.getValue();
        planCombo.getItems().setAll(programaCrr == null ? List.of() : planEstudioServiceCrr.racaListarPorPrograma(programaCrr.getId()));
        if (!planCombo.getItems().isEmpty()) {
            planCombo.getSelectionModel().selectFirst();
        } else {
            racaRecargarCiclos();
        }
    }

    private void racaRecargarCiclos() {
        RacaPlanEstudio planCrr = planCombo.getValue();
        tabla.getItems().setAll(planCrr == null ? List.of() : cicloServiceCrr.racaListarPorPlan(planCrr.getId()));
    }

    @Override
    public void ryeRefrescarAlMostrar() {
        RyeComboBoxUtil.ryeReemplazarPreservandoSeleccion(programaCombo, programaEstudioServiceCrr.racaListar());
        racaRecargarPlanes();
    }

    @FXML
    public void racaOnNuevo() {
        if (planCombo.getValue() == null) {
            racaMostrarAdvertencia("Seleccione un plan de estudio primero.");
            return;
        }
        racaMostrarFormulario(null);
    }

    @FXML
    public void racaOnEditar() {
        RacaCiclo seleccionadoCrr = tabla.getSelectionModel().getSelectedItem();
        if (seleccionadoCrr == null) {
            racaMostrarAdvertencia("Seleccione un ciclo.");
            return;
        }
        racaMostrarFormulario(seleccionadoCrr);
    }

    @FXML
    public void racaOnEliminar() {
        RacaCiclo seleccionadoCrr = tabla.getSelectionModel().getSelectedItem();
        if (seleccionadoCrr == null) {
            racaMostrarAdvertencia("Seleccione un ciclo.");
            return;
        }
        Alert confirmacionCrr = new Alert(Alert.AlertType.CONFIRMATION,
                "Esto eliminara el ciclo " + seleccionadoCrr.getNumero() + " - " + seleccionadoCrr.getNombre()
                        + " de forma permanente.\n\n¿Desea continuar?",
                ButtonType.YES, ButtonType.NO);
        confirmacionCrr.setHeaderText("Eliminar ciclo");
        if (confirmacionCrr.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
            return;
        }
        try {
            cicloServiceCrr.racaEliminar(seleccionadoCrr.getId());
            racaRecargarCiclos();
        } catch (RyeValidacionException e) {
            racaMostrarAdvertencia(e.getMessage());
        }
    }

    private void racaMostrarFormulario(RacaCiclo existenteCrr) {
        Dialog<ButtonType> dialogCrr = new Dialog<>();
        dialogCrr.setTitle(existenteCrr == null ? "Nuevo ciclo" : "Editar ciclo");
        dialogCrr.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane gridCrr = new GridPane();
        gridCrr.setHgap(8);
        gridCrr.setVgap(8);
        gridCrr.setPadding(new Insets(16));

        TextField numeroFieldCrr = new TextField();
        TextField nombreFieldCrr = new TextField();
        TextField ordenFieldCrr = new TextField();

        if (existenteCrr != null) {
            numeroFieldCrr.setText(String.valueOf(existenteCrr.getNumero()));
            nombreFieldCrr.setText(existenteCrr.getNombre());
            ordenFieldCrr.setText(String.valueOf(existenteCrr.getOrden()));
        }

        int filaCrr = 0;
        gridCrr.addRow(filaCrr++, new Label("Numero:"), numeroFieldCrr);
        gridCrr.addRow(filaCrr++, new Label("Nombre:"), nombreFieldCrr);
        gridCrr.addRow(filaCrr, new Label("Orden:"), ordenFieldCrr);

        dialogCrr.getDialogPane().setContent(gridCrr);

        dialogCrr.showAndWait().filter(botonCrr -> botonCrr == ButtonType.OK).ifPresent(botonCrr -> {
            try {
                int numeroCrr = racaParseEntero(numeroFieldCrr.getText());
                int ordenCrr = racaParseEntero(ordenFieldCrr.getText());
                if (existenteCrr == null) {
                    cicloServiceCrr.racaCrear(planCombo.getValue().getId(), numeroCrr, nombreFieldCrr.getText().trim(), ordenCrr);
                } else {
                    cicloServiceCrr.racaEditar(existenteCrr.getId(), numeroCrr, nombreFieldCrr.getText().trim(), ordenCrr);
                }
                racaRecargarCiclos();
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

    private <T> StringConverter<T> racaConvertidor(java.util.function.Function<T, String> textoDeCrr) {
        return new StringConverter<>() {
            @Override
            public String toString(T objetoCrr) {
                return objetoCrr == null ? "" : textoDeCrr.apply(objetoCrr);
            }

            @Override
            public T fromString(String s) {
                return null;
            }
        };
    }

    private void racaMostrarAdvertencia(String mensajeCrr) {
        new Alert(Alert.AlertType.WARNING, mensajeCrr, ButtonType.OK).showAndWait();
    }
}
