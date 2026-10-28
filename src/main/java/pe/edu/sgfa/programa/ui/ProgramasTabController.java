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

import pe.edu.sgfa.common.exception.ValidacionException;
import pe.edu.sgfa.programa.domain.ProgramaEstudio;
import pe.edu.sgfa.programa.service.ProgramaEstudioService;
import pe.edu.sgfa.shell.Refrescable;

@Component
public class ProgramasTabController implements Refrescable {

    private final ProgramaEstudioService programaEstudioService;

    @FXML
    private TableView<ProgramaEstudio> tabla;
    @FXML
    private TableColumn<ProgramaEstudio, String> colCodigo;
    @FXML
    private TableColumn<ProgramaEstudio, String> colNombre;
    @FXML
    private TableColumn<ProgramaEstudio, String> colDuracion;
    @FXML
    private TableColumn<ProgramaEstudio, String> colEstado;

    public ProgramasTabController(ProgramaEstudioService programaEstudioService) {
        this.programaEstudioService = programaEstudioService;
    }

    @FXML
    public void initialize() {
        colCodigo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCodigo()));
        colNombre.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombre()));
        colDuracion.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getDuracionCiclos())));
        colEstado.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().isActivo() ? "Activo" : "Inactivo"));
        recargar();
    }

    private void recargar() {
        tabla.getItems().setAll(programaEstudioService.listar());
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
        ProgramaEstudio slccnd = tabla.getSelectionModel().getSelectedItem();
        if (slccnd == null) {
            mostrarAdvertencia("Seleccione un programa.");
            return;
        }
        mostrarFormulario(slccnd);
    }

    @FXML
    public void onToggleActivo() {
        ProgramaEstudio slccnd = tabla.getSelectionModel().getSelectedItem();
        if (slccnd == null) {
            mostrarAdvertencia("Seleccione un programa.");
            return;
        }
        programaEstudioService.activarDesactivar(slccnd.getId(), !slccnd.isActivo());
        recargar();
    }

    private void mostrarFormulario(ProgramaEstudio exstnt) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle(exstnt == null ? "Nuevo programa" : "Editar programa");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grd = new GridPane();
        grd.setHgap(8);
        grd.setVgap(8);
        grd.setPadding(new Insets(16));

        TextField cdgFld = new TextField();
        TextField nmbrFld = new TextField();
        TextArea dscrpcnFld = new TextArea();
        dscrpcnFld.setPrefRowCount(3);
        TextField drcnFld = new TextField();

        if (exstnt != null) {
            cdgFld.setText(exstnt.getCodigo());
            nmbrFld.setText(exstnt.getNombre());
            dscrpcnFld.setText(exstnt.getDescripcion());
            drcnFld.setText(String.valueOf(exstnt.getDuracionCiclos()));
        }

        int fl = 0;
        grd.addRow(fl++, new Label("Codigo:"), cdgFld);
        grd.addRow(fl++, new Label("Nombre:"), nmbrFld);
        grd.addRow(fl++, new Label("Descripcion:"), dscrpcnFld);
        grd.addRow(fl, new Label("Duracion (ciclos):"), drcnFld);

        dlg.getDialogPane().setContent(grd);

        dlg.showAndWait().filter(boton -> boton == ButtonType.OK).ifPresent(boton -> {
            try {
                int drcn = parseEntero(drcnFld.getText());
                if (exstnt == null) {
                    programaEstudioService.crear(cdgFld.getText().trim(), nmbrFld.getText().trim(),
                            dscrpcnFld.getText(), drcn);
                } else {
                    programaEstudioService.editar(exstnt.getId(), cdgFld.getText().trim(),
                            nmbrFld.getText().trim(), dscrpcnFld.getText(), drcn);
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
            throw new ValidacionException("Ingrese un numero valido.");
        }
    }

    private void mostrarAdvertencia(String mnsj) {
        new Alert(Alert.AlertType.WARNING, mnsj, ButtonType.OK).showAndWait();
    }
}
