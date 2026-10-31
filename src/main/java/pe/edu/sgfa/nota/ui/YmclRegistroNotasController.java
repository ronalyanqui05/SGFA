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

import pe.edu.sgfa.common.exception.RyeValidacionException;
import pe.edu.sgfa.estudiante.domain.YmclEstudiante;
import pe.edu.sgfa.nota.domain.YmclSituacionAcademica;
import pe.edu.sgfa.nota.service.YmclEscalaLiteral;
import pe.edu.sgfa.nota.service.YmclRegistroAcademicoService;
import pe.edu.sgfa.nota.service.YmclResultadoPromedio;
import pe.edu.sgfa.periodo.domain.YmclPeriodoAcademico;
import pe.edu.sgfa.periodo.service.YmclPeriodoAcademicoService;
import pe.edu.sgfa.programa.domain.RacaCiclo;
import pe.edu.sgfa.programa.service.RacaCicloService;
import pe.edu.sgfa.trayectoria.domain.YmclTrayectoria;
import pe.edu.sgfa.trayectoria.service.YmclTrayectoriaService;

@Component
public class YmclRegistroNotasController {

    private final YmclTrayectoriaService trayectoriaServiceChp;
    private final YmclPeriodoAcademicoService periodoAcademicoServiceChp;
    private final RacaCicloService cicloServiceChp;
    private final YmclRegistroAcademicoService registroAcademicoServiceChp;

    private YmclEstudiante estudianteChp;
    private List<YmclSituacionAcademica> situacionesChp;

    @FXML
    private javafx.scene.control.Label estudianteLabel;
    @FXML
    private ComboBox<YmclTrayectoria> trayectoriaCombo;
    @FXML
    private ComboBox<YmclPeriodoAcademico> periodoCombo;
    @FXML
    private ComboBox<RacaCiclo> cicloCombo;
    @FXML
    private TableView<YmclFilaNotaUI> tabla;
    @FXML
    private TableColumn<YmclFilaNotaUI, String> colCurso;
    @FXML
    private TableColumn<YmclFilaNotaUI, String> colCreditos;
    @FXML
    private TableColumn<YmclFilaNotaUI, String> colNota;
    @FXML
    private TableColumn<YmclFilaNotaUI, YmclSituacionAcademica> colSituacion;
    @FXML
    private TableColumn<YmclFilaNotaUI, String> colObservacion;
    @FXML
    private TableColumn<YmclFilaNotaUI, String> colPonderado;
    @FXML
    private javafx.scene.control.Label totalesLabel;

    public YmclRegistroNotasController(YmclTrayectoriaService trayectoriaServiceChp, YmclPeriodoAcademicoService periodoAcademicoServiceChp,
                                    RacaCicloService cicloServiceChp, YmclRegistroAcademicoService registroAcademicoServiceChp) {
        this.trayectoriaServiceChp = trayectoriaServiceChp;
        this.periodoAcademicoServiceChp = periodoAcademicoServiceChp;
        this.cicloServiceChp = cicloServiceChp;
        this.registroAcademicoServiceChp = registroAcademicoServiceChp;
    }

    @FXML
    public void initialize() {
        trayectoriaCombo.setConverter(ymclConvertidor(t -> t.getPlanEstudio().getCodigo() + " (" + t.getEstado() + ")"));
        periodoCombo.setConverter(ymclConvertidor(YmclPeriodoAcademico::getNombre));
        cicloCombo.setConverter(ymclConvertidor(c -> "Ciclo " + c.getNumero() + " - " + c.getNombre()));

        colCurso.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getPlanCurso().getCurso().getCodigo() + " - " + d.getValue().getPlanCurso().getCurso().getNombre()));
        colCreditos.setCellValueFactory(d -> new SimpleStringProperty(
                String.valueOf(d.getValue().getPlanCurso().getCurso().getCreditos())));
        colPonderado.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPonderadoTexto()));

        colNota.setCellValueFactory(d -> d.getValue().notaProperty());
        colNota.setCellFactory(TextFieldTableCell.forTableColumn());
        colNota.setOnEditCommit(eventoChp -> {
            YmclFilaNotaUI filaChp = eventoChp.getRowValue();
            filaChp.notaProperty().set(eventoChp.getNewValue());
            ymclAutocompletarSituacion(filaChp);
            ymclActualizarTotales();
        });

        colObservacion.setCellValueFactory(d -> d.getValue().observacionProperty());
        colObservacion.setCellFactory(TextFieldTableCell.forTableColumn());
        colObservacion.setOnEditCommit(eventoChp -> eventoChp.getRowValue().observacionProperty().set(eventoChp.getNewValue()));

        periodoCombo.getItems().setAll(periodoAcademicoServiceChp.ymclListar());

        trayectoriaCombo.valueProperty().addListener((obsChp, anteriorChp, nuevoChp) -> ymclCargarCiclos());
        cicloCombo.valueProperty().addListener((obsChp, anteriorChp, nuevoChp) -> ymclCargarGrilla());
        periodoCombo.valueProperty().addListener((obsChp, anteriorChp, nuevoChp) -> ymclCargarGrilla());
    }

    public void setEstudiante(YmclEstudiante estudianteChp) {
        this.estudianteChp = estudianteChp;
        estudianteLabel.setText(estudianteChp.getApellidoPaterno()
                + (estudianteChp.getApellidoMaterno() == null ? "" : " " + estudianteChp.getApellidoMaterno())
                + ", " + estudianteChp.getNombres());

        situacionesChp = registroAcademicoServiceChp.ymclListarSituaciones();
        colSituacion.setCellValueFactory(d -> d.getValue().situacionProperty());
        colSituacion.setCellFactory(ComboBoxTableCell.forTableColumn(
                ymclConvertidor(YmclSituacionAcademica::getNombre), FXCollections.observableArrayList(situacionesChp)));
        colSituacion.setOnEditCommit(eventoChp -> {
            eventoChp.getRowValue().situacionProperty().set(eventoChp.getNewValue());
            ymclActualizarTotales();
        });

        trayectoriaCombo.getItems().setAll(trayectoriaServiceChp.ymclListarPorEstudiante(estudianteChp.getId()));
        if (!trayectoriaCombo.getItems().isEmpty()) {
            trayectoriaCombo.getSelectionModel().selectFirst();
        }
        if (!periodoCombo.getItems().isEmpty()) {
            periodoCombo.getSelectionModel().selectFirst();
        }
    }

    private void ymclCargarCiclos() {
        YmclTrayectoria trayectoriaChp = trayectoriaCombo.getValue();
        List<RacaCiclo> ciclosChp = trayectoriaChp == null
                ? List.of() : cicloServiceChp.racaListarPorPlan(trayectoriaChp.getPlanEstudio().getId());
        cicloCombo.getItems().setAll(ciclosChp);
        if (!ciclosChp.isEmpty()) {
            cicloCombo.getSelectionModel().selectFirst();
        } else {
            ymclCargarGrilla();
        }
    }

    private void ymclCargarGrilla() {
        YmclTrayectoria trayectoriaChp = trayectoriaCombo.getValue();
        RacaCiclo cicloChp = cicloCombo.getValue();
        YmclPeriodoAcademico periodoChp = periodoCombo.getValue();
        if (trayectoriaChp == null || cicloChp == null || periodoChp == null) {
            tabla.getItems().clear();
            totalesLabel.setText("");
            return;
        }
        tabla.getItems().setAll(registroAcademicoServiceChp
                .ymclListarParaRegistro(trayectoriaChp.getId(), cicloChp.getId(), periodoChp.getId())
                .stream().map(YmclFilaNotaUI::new).toList());
        ymclActualizarTotales();
    }

    private void ymclAutocompletarSituacion(YmclFilaNotaUI filaChp) {
        String codigoChp = YmclEscalaLiteral.ymclCodigoPara(filaChp.getNotaComoNumero());
        situacionesChp.stream().filter(s -> s.getCodigo().equals(codigoChp)).findFirst()
                .ifPresent(s -> filaChp.situacionProperty().set(s));
    }

    private void ymclActualizarTotales() {
        tabla.refresh();
        YmclTrayectoria trayectoriaChp = trayectoriaCombo.getValue();
        RacaCiclo cicloChp = cicloCombo.getValue();
        YmclPeriodoAcademico periodoChp = periodoCombo.getValue();
        if (trayectoriaChp == null || cicloChp == null || periodoChp == null) {
            return;
        }
        YmclResultadoPromedio resultadoChp = registroAcademicoServiceChp.ymclCalcularPromedio(
                trayectoriaChp.getId(), cicloChp.getId(), periodoChp.getId());
        String promedioTextoChp = resultadoChp.promedio() == null ? "sin cursos registrados aun"
                : String.format("%.2f", resultadoChp.promedio());
        totalesLabel.setText("Total creditos: " + resultadoChp.totalCreditos()
                + "  |  Suma ponderada: " + resultadoChp.sumaPonderada()
                + "  |  Promedio ponderado: " + promedioTextoChp);
    }

    @FXML
    public void ymclOnGuardar() {
        YmclTrayectoria trayectoriaChp = trayectoriaCombo.getValue();
        YmclPeriodoAcademico periodoChp = periodoCombo.getValue();
        if (trayectoriaChp == null || periodoChp == null) {
            ymclMostrarAdvertencia("Seleccione trayectoria y periodo.");
            return;
        }
        try {
            registroAcademicoServiceChp.ymclGuardarMasivo(trayectoriaChp.getId(), periodoChp.getId(),
                    tabla.getItems().stream().map(YmclFilaNotaUI::ymclAItem).toList());
            ymclCargarGrilla();
            ymclMostrarInfo("Notas guardadas.");
        } catch (RyeValidacionException e) {
            ymclMostrarAdvertencia(e.getMessage());
        }
    }

    private <T> StringConverter<T> ymclConvertidor(java.util.function.Function<T, String> textoDeChp) {
        return new StringConverter<>() {
            @Override
            public String toString(T objetoChp) {
                return objetoChp == null ? "" : textoDeChp.apply(objetoChp);
            }

            @Override
            public T fromString(String s) {
                return null;
            }
        };
    }

    private void ymclMostrarAdvertencia(String mensajeChp) {
        new Alert(Alert.AlertType.WARNING, mensajeChp, ButtonType.OK).showAndWait();
    }

    private void ymclMostrarInfo(String mensajeChp) {
        new Alert(Alert.AlertType.INFORMATION, mensajeChp, ButtonType.OK).showAndWait();
    }
}
