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

import pe.edu.sgfa.common.exception.ValidacionException;
import pe.edu.sgfa.programa.domain.Curso;
import pe.edu.sgfa.programa.service.CursoService;
import pe.edu.sgfa.shell.Refrescable;

@Component
public class CursosTabController implements Refrescable {

    private final CursoService cursoService;

    @FXML
    private TableView<Curso> tabla;
    @FXML
    private TableColumn<Curso, String> colCodigo;
    @FXML
    private TableColumn<Curso, String> colNombre;
    @FXML
    private TableColumn<Curso, String> colCreditos;
    @FXML
    private TableColumn<Curso, String> colHoras;
    @FXML
    private TableColumn<Curso, String> colTipo;
    @FXML
    private TableColumn<Curso, String> colEstado;

    public CursosTabController(CursoService cursoService) {
        this.cursoService = cursoService;
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
        recargar();
    }

    private void recargar() {
        tabla.getItems().setAll(cursoService.listar());
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
        Curso slccnd = tabla.getSelectionModel().getSelectedItem();
        if (slccnd == null) {
            mostrarAdvertencia("Seleccione un curso.");
            return;
        }
        mostrarFormulario(slccnd);
    }

    @FXML
    public void onToggleActivo() {
        Curso slccnd = tabla.getSelectionModel().getSelectedItem();
        if (slccnd == null) {
            mostrarAdvertencia("Seleccione un curso.");
            return;
        }
        cursoService.activarDesactivar(slccnd.getId(), !slccnd.isActivo());
        recargar();
    }

    private void mostrarFormulario(Curso exstnt) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle(exstnt == null ? "Nuevo curso" : "Editar curso");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grd = new GridPane();
        grd.setHgap(8);
        grd.setVgap(8);
        grd.setPadding(new Insets(16));

        TextField cdgFld = new TextField();
        TextField nmbrFld = new TextField();
        TextField crdtsFld = new TextField();
        TextField hrsFld = new TextField();
        TextField tpFld = new TextField();

        if (exstnt != null) {
            cdgFld.setText(exstnt.getCodigo());
            nmbrFld.setText(exstnt.getNombre());
            crdtsFld.setText(String.valueOf(exstnt.getCreditos()));
            hrsFld.setText(exstnt.getHoras() == null ? "" : String.valueOf(exstnt.getHoras()));
            tpFld.setText(exstnt.getTipo());
        }

        int fl = 0;
        grd.addRow(fl++, new Label("Codigo:"), cdgFld);
        grd.addRow(fl++, new Label("Nombre:"), nmbrFld);
        grd.addRow(fl++, new Label("Creditos:"), crdtsFld);
        grd.addRow(fl++, new Label("Horas:"), hrsFld);
        grd.addRow(fl, new Label("Tipo:"), tpFld);

        dlg.getDialogPane().setContent(grd);

        dlg.showAndWait().filter(boton -> boton == ButtonType.OK).ifPresent(boton -> {
            try {
                int crdts = parseEntero(crdtsFld.getText(), "creditos");
                Integer hrs = hrsFld.getText() == null || hrsFld.getText().isBlank()
                        ? null : parseEntero(hrsFld.getText(), "horas");
                String tp = tpFld.getText() == null || tpFld.getText().isBlank()
                        ? null : tpFld.getText().trim();

                if (exstnt == null) {
                    cursoService.crear(cdgFld.getText().trim(), nmbrFld.getText().trim(), crdts, hrs, tp);
                } else {
                    cursoService.editar(exstnt.getId(), cdgFld.getText().trim(), nmbrFld.getText().trim(),
                            crdts, hrs, tp);
                }
                recargar();
            } catch (ValidacionException e) {
                mostrarAdvertencia(e.getMessage());
            }
        });
    }

    private int parseEntero(String txt, String cmp) {
        try {
            return Integer.parseInt(txt.trim());
        } catch (NumberFormatException e) {
            throw new ValidacionException("Ingrese un numero valido para " + cmp + ".");
        }
    }

    private void mostrarAdvertencia(String mnsj) {
        new Alert(Alert.AlertType.WARNING, mnsj, ButtonType.OK).showAndWait();
    }
}
