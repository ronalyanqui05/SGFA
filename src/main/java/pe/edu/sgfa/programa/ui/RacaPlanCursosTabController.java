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

import pe.edu.sgfa.common.exception.RyeValidacionException;
import pe.edu.sgfa.programa.domain.RacaCiclo;
import pe.edu.sgfa.programa.domain.RacaCurso;
import pe.edu.sgfa.programa.domain.RacaPlanCurso;
import pe.edu.sgfa.programa.domain.RacaPlanEstudio;
import pe.edu.sgfa.programa.domain.RacaProgramaEstudio;
import pe.edu.sgfa.programa.service.RacaCicloService;
import pe.edu.sgfa.programa.service.RacaCursoService;
import pe.edu.sgfa.programa.service.RacaPlanCursoService;
import pe.edu.sgfa.programa.service.RacaPlanEstudioService;
import pe.edu.sgfa.programa.service.RacaProgramaEstudioService;
import pe.edu.sgfa.shell.RyeComboBoxUtil;
import pe.edu.sgfa.shell.RyeRefrescable;

@Component
public class RacaPlanCursosTabController implements RyeRefrescable {

    private final RacaProgramaEstudioService programaEstudioServiceCrr;
    private final RacaPlanEstudioService planEstudioServiceCrr;
    private final RacaCicloService cicloServiceCrr;
    private final RacaCursoService cursoServiceCrr;
    private final RacaPlanCursoService planCursoServiceCrr;

    @FXML
    private ComboBox<RacaProgramaEstudio> programaCombo;
    @FXML
    private ComboBox<RacaPlanEstudio> planCombo;
    @FXML
    private ComboBox<RacaCiclo> cicloCombo;
    @FXML
    private ComboBox<RacaCurso> cursoCombo;
    @FXML
    private TextField ordenField;
    @FXML
    private CheckBox obligatorioCheck;
    @FXML
    private TableView<RacaPlanCurso> tabla;
    @FXML
    private TableColumn<RacaPlanCurso, String> colCurso;
    @FXML
    private TableColumn<RacaPlanCurso, String> colCreditos;
    @FXML
    private TableColumn<RacaPlanCurso, String> colOrden;
    @FXML
    private TableColumn<RacaPlanCurso, String> colObligatorio;

    public RacaPlanCursosTabController(RacaProgramaEstudioService programaEstudioServiceCrr, RacaPlanEstudioService planEstudioServiceCrr,
                                    RacaCicloService cicloServiceCrr, RacaCursoService cursoServiceCrr,
                                    RacaPlanCursoService planCursoServiceCrr) {
        this.programaEstudioServiceCrr = programaEstudioServiceCrr;
        this.planEstudioServiceCrr = planEstudioServiceCrr;
        this.cicloServiceCrr = cicloServiceCrr;
        this.cursoServiceCrr = cursoServiceCrr;
        this.planCursoServiceCrr = planCursoServiceCrr;
    }

    @FXML
    public void initialize() {
        programaCombo.setConverter(racaConvertidor(p -> p.getCodigo() + " - " + p.getNombre()));
        planCombo.setConverter(racaConvertidor(p -> p.getCodigo() + " - " + p.getNombre()));
        cicloCombo.setConverter(racaConvertidor(c -> "Ciclo " + c.getNumero() + " - " + c.getNombre()));
        cursoCombo.setConverter(racaConvertidor(c -> c.getCodigo() + " - " + c.getNombre()));

        colCurso.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCurso().getCodigo() + " - " + d.getValue().getCurso().getNombre()));
        colCreditos.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getCurso().getCreditos())));
        colOrden.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getOrden())));
        colObligatorio.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().isObligatorio() ? "Si" : "No"));

        cursoCombo.getItems().setAll(cursoServiceCrr.racaListar());
        programaCombo.getItems().setAll(programaEstudioServiceCrr.racaListar());
        programaCombo.valueProperty().addListener((obsCrr, anteriorCrr, nuevoCrr) -> racaRecargarPlanes());
        planCombo.valueProperty().addListener((obsCrr, anteriorCrr, nuevoCrr) -> racaRecargarCiclos());
        cicloCombo.valueProperty().addListener((obsCrr, anteriorCrr, nuevoCrr) -> racaRecargarAsignaciones());
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
        cicloCombo.getItems().setAll(planCrr == null ? List.of() : cicloServiceCrr.racaListarPorPlan(planCrr.getId()));
        if (!cicloCombo.getItems().isEmpty()) {
            cicloCombo.getSelectionModel().selectFirst();
        } else {
            racaRecargarAsignaciones();
        }
    }

    private void racaRecargarAsignaciones() {
        RacaCiclo cicloCrr = cicloCombo.getValue();
        tabla.getItems().setAll(cicloCrr == null ? List.of() : planCursoServiceCrr.racaListarPorCiclo(cicloCrr.getId()));
    }

    @Override
    public void ryeRefrescarAlMostrar() {
        RyeComboBoxUtil.ryeReemplazarPreservandoSeleccion(cursoCombo, cursoServiceCrr.racaListar());
        RyeComboBoxUtil.ryeReemplazarPreservandoSeleccion(programaCombo, programaEstudioServiceCrr.racaListar());
        racaRecargarPlanes();
    }

    @FXML
    public void racaOnAgregar() {
        RacaPlanEstudio planCrr = planCombo.getValue();
        RacaCiclo cicloCrr = cicloCombo.getValue();
        RacaCurso cursoCrr = cursoCombo.getValue();
        if (planCrr == null || cicloCrr == null || cursoCrr == null) {
            racaMostrarAdvertencia("Seleccione plan, ciclo y curso.");
            return;
        }

        try {
            int ordenCrr = ordenField.getText() == null || ordenField.getText().isBlank()
                    ? 0 : Integer.parseInt(ordenField.getText().trim());
            planCursoServiceCrr.racaAsignar(planCrr.getId(), cicloCrr.getId(), cursoCrr.getId(), ordenCrr, obligatorioCheck.isSelected());
            racaRecargarAsignaciones();
        } catch (NumberFormatException e) {
            racaMostrarAdvertencia("Ingrese un numero valido para el orden.");
        } catch (RyeValidacionException e) {
            racaMostrarAdvertencia(e.getMessage());
        }
    }

    @FXML
    public void racaOnQuitar() {
        RacaPlanCurso seleccionadoCrr = tabla.getSelectionModel().getSelectedItem();
        if (seleccionadoCrr == null) {
            racaMostrarAdvertencia("Seleccione una asignacion.");
            return;
        }
        try {
            planCursoServiceCrr.racaQuitar(seleccionadoCrr.getId());
            racaRecargarAsignaciones();
        } catch (RyeValidacionException e) {
            racaMostrarAdvertencia(e.getMessage());
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
