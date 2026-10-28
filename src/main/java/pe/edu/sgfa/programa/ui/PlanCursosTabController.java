package pe.edu.sgfa.programa.ui;

import java.util.List;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import org.springframework.stereotype.Component;

import pe.edu.sgfa.common.exception.ValidacionException;
import pe.edu.sgfa.programa.domain.Ciclo;
import pe.edu.sgfa.programa.domain.Curso;
import pe.edu.sgfa.programa.domain.PlanCurso;
import pe.edu.sgfa.programa.domain.PlanEstudio;
import pe.edu.sgfa.programa.domain.ProgramaEstudio;
import pe.edu.sgfa.programa.service.CicloService;
import pe.edu.sgfa.programa.service.CursoService;
import pe.edu.sgfa.programa.service.PlanCursoService;
import pe.edu.sgfa.programa.service.PlanEstudioService;
import pe.edu.sgfa.programa.service.ProgramaEstudioService;
import pe.edu.sgfa.shell.ComboBoxUtil;
import pe.edu.sgfa.shell.Refrescable;

@Component
public class PlanCursosTabController implements Refrescable {

    private final ProgramaEstudioService programaEstudioService;
    private final PlanEstudioService planEstudioService;
    private final CicloService cicloService;
    private final CursoService cursoService;
    private final PlanCursoService planCursoService;

    @FXML
    private ComboBox<ProgramaEstudio> programaCombo;
    @FXML
    private ComboBox<PlanEstudio> planCombo;
    @FXML
    private ComboBox<Ciclo> cicloCombo;
    @FXML
    private ComboBox<Curso> cursoCombo;
    @FXML
    private TextField ordenField;
    @FXML
    private CheckBox obligatorioCheck;
    @FXML
    private TableView<PlanCurso> tabla;
    @FXML
    private TableColumn<PlanCurso, String> colCurso;
    @FXML
    private TableColumn<PlanCurso, String> colCreditos;
    @FXML
    private TableColumn<PlanCurso, String> colOrden;
    @FXML
    private TableColumn<PlanCurso, String> colObligatorio;

    public PlanCursosTabController(ProgramaEstudioService programaEstudioService, PlanEstudioService planEstudioService,
                                    CicloService cicloService, CursoService cursoService,
                                    PlanCursoService planCursoService) {
        this.programaEstudioService = programaEstudioService;
        this.planEstudioService = planEstudioService;
        this.cicloService = cicloService;
        this.cursoService = cursoService;
        this.planCursoService = planCursoService;
    }

    @FXML
    public void initialize() {
        programaCombo.setConverter(convertidor(p -> p.getCodigo() + " - " + p.getNombre()));
        planCombo.setConverter(convertidor(p -> p.getCodigo() + " - " + p.getNombre()));
        cicloCombo.setConverter(convertidor(c -> "Ciclo " + c.getNumero() + " - " + c.getNombre()));
        cursoCombo.setConverter(convertidor(c -> c.getCodigo() + " - " + c.getNombre()));

        colCurso.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCurso().getCodigo() + " - " + d.getValue().getCurso().getNombre()));
        colCreditos.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getCurso().getCreditos())));
        colOrden.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getOrden())));
        colObligatorio.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().isObligatorio() ? "Si" : "No"));

        cursoCombo.getItems().setAll(cursoService.listar());
        programaCombo.getItems().setAll(programaEstudioService.listar());
        programaCombo.valueProperty().addListener((obs, anterior, nuevo) -> recargarPlanes());
        planCombo.valueProperty().addListener((obs, anterior, nuevo) -> recargarCiclos());
        cicloCombo.valueProperty().addListener((obs, anterior, nuevo) -> recargarAsignaciones());
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
        cicloCombo.getItems().setAll(pln == null ? List.of() : cicloService.listarPorPlan(pln.getId()));
        if (!cicloCombo.getItems().isEmpty()) {
            cicloCombo.getSelectionModel().selectFirst();
        } else {
            recargarAsignaciones();
        }
    }

    private void recargarAsignaciones() {
        Ciclo ccl = cicloCombo.getValue();
        tabla.getItems().setAll(ccl == null ? List.of() : planCursoService.listarPorCiclo(ccl.getId()));
    }

    @Override
    public void refrescarAlMostrar() {
        ComboBoxUtil.reemplazarPreservandoSeleccion(cursoCombo, cursoService.listar());
        ComboBoxUtil.reemplazarPreservandoSeleccion(programaCombo, programaEstudioService.listar());
        recargarPlanes();
    }

    @FXML
    public void onAgregar() {
        PlanEstudio pln = planCombo.getValue();
        Ciclo ccl = cicloCombo.getValue();
        Curso crs = cursoCombo.getValue();
        if (pln == null || ccl == null || crs == null) {
            mostrarAdvertencia("Seleccione plan, ciclo y curso.");
            return;
        }

        try {
            int ordn = ordenField.getText() == null || ordenField.getText().isBlank()
                    ? 0 : Integer.parseInt(ordenField.getText().trim());
            planCursoService.asignar(pln.getId(), ccl.getId(), crs.getId(), ordn, obligatorioCheck.isSelected());
            recargarAsignaciones();
        } catch (NumberFormatException e) {
            mostrarAdvertencia("Ingrese un numero valido para el orden.");
        } catch (ValidacionException e) {
            mostrarAdvertencia(e.getMessage());
        }
    }

    @FXML
    public void onQuitar() {
        PlanCurso slccnd = tabla.getSelectionModel().getSelectedItem();
        if (slccnd == null) {
            mostrarAdvertencia("Seleccione una asignacion.");
            return;
        }
        try {
            planCursoService.quitar(slccnd.getId());
            recargarAsignaciones();
        } catch (ValidacionException e) {
            mostrarAdvertencia(e.getMessage());
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
