package pe.edu.sgfa.nota.ui;

import java.util.List;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.StringConverter;
import org.springframework.stereotype.Component;

import pe.edu.sgfa.common.exception.ValidacionException;
import pe.edu.sgfa.estudiante.domain.Estudiante;
import pe.edu.sgfa.nota.domain.SituacionAcademica;
import pe.edu.sgfa.nota.service.EscalaLiteral;
import pe.edu.sgfa.nota.service.RegistroAcademicoService;
import pe.edu.sgfa.nota.service.ResultadoPromedio;
import pe.edu.sgfa.periodo.domain.PeriodoAcademico;
import pe.edu.sgfa.periodo.service.PeriodoAcademicoService;
import pe.edu.sgfa.programa.domain.Ciclo;
import pe.edu.sgfa.programa.service.CicloService;
import pe.edu.sgfa.trayectoria.domain.Trayectoria;
import pe.edu.sgfa.trayectoria.service.TrayectoriaService;

@Component
public class RegistroNotasController {

    private final TrayectoriaService trayectoriaService;
    private final PeriodoAcademicoService periodoAcademicoService;
    private final CicloService cicloService;
    private final RegistroAcademicoService registroAcademicoService;

    private Estudiante estudiante;
    private List<SituacionAcademica> situaciones;

    @FXML
    private javafx.scene.control.Label estudianteLabel;
    @FXML
    private ComboBox<Trayectoria> trayectoriaCombo;
    @FXML
    private ComboBox<PeriodoAcademico> periodoCombo;
    @FXML
    private ComboBox<Ciclo> cicloCombo;
    @FXML
    private TableView<FilaNotaUI> tabla;
    @FXML
    private TableColumn<FilaNotaUI, String> colCurso;
    @FXML
    private TableColumn<FilaNotaUI, String> colCreditos;
    @FXML
    private TableColumn<FilaNotaUI, String> colNota;
    @FXML
    private TableColumn<FilaNotaUI, SituacionAcademica> colSituacion;
    @FXML
    private TableColumn<FilaNotaUI, String> colObservacion;
    @FXML
    private TableColumn<FilaNotaUI, String> colPonderado;
    @FXML
    private javafx.scene.control.Label totalesLabel;

    public RegistroNotasController(TrayectoriaService trayectoriaService, PeriodoAcademicoService periodoAcademicoService,
                                    CicloService cicloService, RegistroAcademicoService registroAcademicoService) {
        this.trayectoriaService = trayectoriaService;
        this.periodoAcademicoService = periodoAcademicoService;
        this.cicloService = cicloService;
        this.registroAcademicoService = registroAcademicoService;
    }

    @FXML
    public void initialize() {
        trayectoriaCombo.setConverter(convertidor(t -> t.getPlanEstudio().getCodigo() + " (" + t.getEstado() + ")"));
        periodoCombo.setConverter(convertidor(PeriodoAcademico::getNombre));
        cicloCombo.setConverter(convertidor(c -> "Ciclo " + c.getNumero() + " - " + c.getNombre()));

        colCurso.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getPlanCurso().getCurso().getCodigo() + " - " + d.getValue().getPlanCurso().getCurso().getNombre()));
        colCreditos.setCellValueFactory(d -> new SimpleStringProperty(
                String.valueOf(d.getValue().getPlanCurso().getCurso().getCreditos())));
        colPonderado.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPonderadoTexto()));

        colNota.setCellValueFactory(d -> d.getValue().notaProperty());
        colNota.setCellFactory(TextFieldTableCell.forTableColumn());
        colNota.setOnEditCommit(evento -> {
            FilaNotaUI fl = evento.getRowValue();
            fl.notaProperty().set(evento.getNewValue());
            autocompletarSituacion(fl);
            actualizarTotales();
        });

        colObservacion.setCellValueFactory(d -> d.getValue().observacionProperty());
        colObservacion.setCellFactory(TextFieldTableCell.forTableColumn());
        colObservacion.setOnEditCommit(evento -> evento.getRowValue().observacionProperty().set(evento.getNewValue()));

        periodoCombo.getItems().setAll(periodoAcademicoService.listar());

        trayectoriaCombo.valueProperty().addListener((obs, anterior, nuevo) -> cargarCiclos());
        cicloCombo.valueProperty().addListener((obs, anterior, nuevo) -> cargarGrilla());
        periodoCombo.valueProperty().addListener((obs, anterior, nuevo) -> cargarGrilla());
    }

    public void setEstudiante(Estudiante estudiante) {
        this.estudiante = estudiante;
        estudianteLabel.setText(estudiante.getApellidoPaterno()
                + (estudiante.getApellidoMaterno() == null ? "" : " " + estudiante.getApellidoMaterno())
                + ", " + estudiante.getNombres());

        situaciones = registroAcademicoService.listarSituaciones();
        colSituacion.setCellValueFactory(d -> d.getValue().situacionProperty());
        colSituacion.setCellFactory(ComboBoxTableCell.forTableColumn(
                convertidor(SituacionAcademica::getNombre), FXCollections.observableArrayList(situaciones)));
        colSituacion.setOnEditCommit(evento -> {
            evento.getRowValue().situacionProperty().set(evento.getNewValue());
            actualizarTotales();
        });

        trayectoriaCombo.getItems().setAll(trayectoriaService.listarPorEstudiante(estudiante.getId()));
        if (!trayectoriaCombo.getItems().isEmpty()) {
            trayectoriaCombo.getSelectionModel().selectFirst();
        }
        if (!periodoCombo.getItems().isEmpty()) {
            periodoCombo.getSelectionModel().selectFirst();
        }
    }

    private void cargarCiclos() {
        Trayectoria trayectoria = trayectoriaCombo.getValue();
        List<Ciclo> ccls = trayectoria == null
                ? List.of() : cicloService.listarPorPlan(trayectoria.getPlanEstudio().getId());
        cicloCombo.getItems().setAll(ccls);
        if (!ccls.isEmpty()) {
            cicloCombo.getSelectionModel().selectFirst();
        } else {
            cargarGrilla();
        }
    }

    private void cargarGrilla() {
        Trayectoria trayectoria = trayectoriaCombo.getValue();
        Ciclo ccl = cicloCombo.getValue();
        PeriodoAcademico periodo = periodoCombo.getValue();
        if (trayectoria == null || ccl == null || periodo == null) {
            tabla.getItems().clear();
            totalesLabel.setText("");
            return;
        }
        tabla.getItems().setAll(registroAcademicoService
                .listarParaRegistro(trayectoria.getId(), ccl.getId(), periodo.getId())
                .stream().map(FilaNotaUI::new).toList());
        actualizarTotales();
    }

    private void autocompletarSituacion(FilaNotaUI fl) {
        String cdg = EscalaLiteral.codigoPara(fl.getNotaComoNumero());
        situaciones.stream().filter(s -> s.getCodigo().equals(cdg)).findFirst()
                .ifPresent(s -> fl.situacionProperty().set(s));
    }

    private void actualizarTotales() {
        tabla.refresh();
        Trayectoria trayectoria = trayectoriaCombo.getValue();
        Ciclo ccl = cicloCombo.getValue();
        PeriodoAcademico periodo = periodoCombo.getValue();
        if (trayectoria == null || ccl == null || periodo == null) {
            return;
        }
        ResultadoPromedio rsltd = registroAcademicoService.calcularPromedio(
                trayectoria.getId(), ccl.getId(), periodo.getId());
        String prmdTxt = rsltd.promedio() == null ? "sin cursos registrados aun"
                : String.format("%.2f", rsltd.promedio());
        totalesLabel.setText("Total creditos: " + rsltd.totalCreditos()
                + "  |  Suma ponderada: " + rsltd.sumaPonderada()
                + "  |  Promedio ponderado: " + prmdTxt);
    }

    @FXML
    public void onGuardar() {
        Trayectoria trayectoria = trayectoriaCombo.getValue();
        PeriodoAcademico periodo = periodoCombo.getValue();
        if (trayectoria == null || periodo == null) {
            mostrarAdvertencia("Seleccione trayectoria y periodo.");
            return;
        }
        try {
            registroAcademicoService.guardarMasivo(trayectoria.getId(), periodo.getId(),
                    tabla.getItems().stream().map(FilaNotaUI::aItem).toList());
            cargarGrilla();
            mostrarInfo("Notas guardadas.");
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

    private void mostrarInfo(String mnsj) {
        new Alert(Alert.AlertType.INFORMATION, mnsj, ButtonType.OK).showAndWait();
    }
}
