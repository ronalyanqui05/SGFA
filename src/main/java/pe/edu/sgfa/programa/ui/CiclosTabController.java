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

import pe.edu.sgfa.common.exception.ValidacionException;
import pe.edu.sgfa.programa.domain.Ciclo;
import pe.edu.sgfa.programa.domain.PlanEstudio;
import pe.edu.sgfa.programa.domain.ProgramaEstudio;
import pe.edu.sgfa.programa.service.CicloService;
import pe.edu.sgfa.programa.service.PlanEstudioService;
import pe.edu.sgfa.programa.service.ProgramaEstudioService;
import pe.edu.sgfa.shell.ComboBoxUtil;
import pe.edu.sgfa.shell.Refrescable;

@Component
public class CiclosTabController implements Refrescable {

    private final ProgramaEstudioService programaEstudioService;
    private final PlanEstudioService planEstudioService;
    private final CicloService cicloService;

    @FXML
    private ComboBox<ProgramaEstudio> programaCombo;
    @FXML
    private ComboBox<PlanEstudio> planCombo;
    @FXML
    private TableView<Ciclo> tabla;
    @FXML
    private TableColumn<Ciclo, String> colNumero;
    @FXML
    private TableColumn<Ciclo, String> colNombre;
    @FXML
    private TableColumn<Ciclo, String> colOrden;

    public CiclosTabController(ProgramaEstudioService programaEstudioService, PlanEstudioService planEstudioService,
                                CicloService cicloService) {
        this.programaEstudioService = programaEstudioService;
        this.planEstudioService = planEstudioService;
        this.cicloService = cicloService;
    }

    @FXML
    public void initialize() {
        programaCombo.setConverter(convertidor(p -> p.getCodigo() + " - " + p.getNombre()));
        planCombo.setConverter(convertidor(p -> p.getCodigo() + " - " + p.getNombre()));

        colNumero.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getNumero())));
        colNombre.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombre()));
        colOrden.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getOrden())));

        programaCombo.getItems().setAll(programaEstudioService.listar());
        programaCombo.valueProperty().addListener((obs, anterior, nuevo) -> recargarPlanes());
        planCombo.valueProperty().addListener((obs, anterior, nuevo) -> recargarCiclos());
        if (!programaCombo.getItems().isEmpty()) {
            programaCombo.getSelectionModel().selectFirst();
        }
    }

    private void recargarPlanes() {
        ProgramaEstudio programa = programaCombo.getValue();
        planCombo.getItems().setAll(programa == null ? List.of() : planEstudioService.listarPorPrograma(programa.getId()));
        if (!planCombo.getItems().isEmpty()) {
            planCombo.getSelectionModel().selectFirst();
        } else {
            recargarCiclos();
        }
    }

    private void recargarCiclos() {
        PlanEstudio pln = planCombo.getValue();
        tabla.getItems().setAll(pln == null ? List.of() : cicloService.listarPorPlan(pln.getId()));
    }

    @Override
    public void refrescarAlMostrar() {
        ComboBoxUtil.reemplazarPreservandoSeleccion(programaCombo, programaEstudioService.listar());
        recargarPlanes();
    }

    @FXML
    public void onNuevo() {
        if (planCombo.getValue() == null) {
            mostrarAdvertencia("Seleccione un plan de estudio primero.");
            return;
        }
        mostrarFormulario(null);
    }

    @FXML
    public void onEditar() {
        Ciclo slccnd = tabla.getSelectionModel().getSelectedItem();
        if (slccnd == null) {
            mostrarAdvertencia("Seleccione un ciclo.");
            return;
        }
        mostrarFormulario(slccnd);
    }

    @FXML
    public void onEliminar() {
        Ciclo slccnd = tabla.getSelectionModel().getSelectedItem();
        if (slccnd == null) {
            mostrarAdvertencia("Seleccione un ciclo.");
            return;
        }
        Alert cnfrmcn = new Alert(Alert.AlertType.CONFIRMATION,
                "Esto eliminara el ciclo " + slccnd.getNumero() + " - " + slccnd.getNombre()
                        + " de forma permanente.\n\n¿Desea continuar?",
                ButtonType.YES, ButtonType.NO);
        cnfrmcn.setHeaderText("Eliminar ciclo");
        if (cnfrmcn.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
            return;
        }
        try {
            cicloService.eliminar(slccnd.getId());
            recargarCiclos();
        } catch (ValidacionException e) {
            mostrarAdvertencia(e.getMessage());
        }
    }

    private void mostrarFormulario(Ciclo exstnt) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle(exstnt == null ? "Nuevo ciclo" : "Editar ciclo");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grd = new GridPane();
        grd.setHgap(8);
        grd.setVgap(8);
        grd.setPadding(new Insets(16));

        TextField nmrFld = new TextField();
        TextField nmbrFld = new TextField();
        TextField ordnFld = new TextField();

        if (exstnt != null) {
            nmrFld.setText(String.valueOf(exstnt.getNumero()));
            nmbrFld.setText(exstnt.getNombre());
            ordnFld.setText(String.valueOf(exstnt.getOrden()));
        }

        int fl = 0;
        grd.addRow(fl++, new Label("Numero:"), nmrFld);
        grd.addRow(fl++, new Label("Nombre:"), nmbrFld);
        grd.addRow(fl, new Label("Orden:"), ordnFld);

        dlg.getDialogPane().setContent(grd);

        dlg.showAndWait().filter(boton -> boton == ButtonType.OK).ifPresent(boton -> {
            try {
                int nmr = parseEntero(nmrFld.getText());
                int ordn = parseEntero(ordnFld.getText());
                if (exstnt == null) {
                    cicloService.crear(planCombo.getValue().getId(), nmr, nmbrFld.getText().trim(), ordn);
                } else {
                    cicloService.editar(exstnt.getId(), nmr, nmbrFld.getText().trim(), ordn);
                }
                recargarCiclos();
            } catch (ValidacionException e) {
                mostrarAdvertencia(e.getMessage());
            }
        });
    }

    private int parseEntero(String txt) {
        try {
            return Integer.parseInt(txt.trim());
        } catch (NumberFormatException e) {
            throw new ValidacionException("Ingrese un numero valido.");
        }
    }

    private <T> StringConverter<T> convertidor(java.util.function.Function<T, String> textoDe) {
        return new StringConverter<>() {
            @Override
            public String toString(T objt) {
                return objt == null ? "" : textoDe.apply(objt);
            }

            @Override
            public T fromString(String s) {
                return null;
            }
        };
    }

    private void mostrarAdvertencia(String mnsj) {
        new Alert(Alert.AlertType.WARNING, mnsj, ButtonType.OK).showAndWait();
    }
}
