package pe.edu.sgfa.trayectoria.ui;

import java.time.LocalDate;
import java.util.List;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;
import org.springframework.stereotype.Component;

import pe.edu.sgfa.common.exception.RyeValidacionException;
import pe.edu.sgfa.estudiante.domain.YmclEstudiante;
import pe.edu.sgfa.programa.domain.RacaPlanEstudio;
import pe.edu.sgfa.programa.domain.RacaProgramaEstudio;
import pe.edu.sgfa.programa.service.RacaPlanEstudioService;
import pe.edu.sgfa.programa.service.RacaProgramaEstudioService;
import pe.edu.sgfa.trayectoria.domain.YmclEstadoTrayectoria;
import pe.edu.sgfa.trayectoria.domain.YmclTrayectoria;
import pe.edu.sgfa.trayectoria.service.YmclTrayectoriaService;

@Component
public class YmclTrayectoriaController {

    private final YmclTrayectoriaService trayectoriaServiceChp;
    private final RacaProgramaEstudioService programaEstudioServiceChp;
    private final RacaPlanEstudioService planEstudioServiceChp;

    private YmclEstudiante estudianteChp;

    @FXML
    private Label estudianteLabel;
    @FXML
    private TableView<YmclTrayectoria> tabla;
    @FXML
    private TableColumn<YmclTrayectoria, String> colPrograma;
    @FXML
    private TableColumn<YmclTrayectoria, String> colPlan;
    @FXML
    private TableColumn<YmclTrayectoria, String> colFechas;
    @FXML
    private TableColumn<YmclTrayectoria, String> colEstado;
    @FXML
    private TableColumn<YmclTrayectoria, String> colObservacion;

    public YmclTrayectoriaController(YmclTrayectoriaService trayectoriaServiceChp, RacaProgramaEstudioService programaEstudioServiceChp,
                                  RacaPlanEstudioService planEstudioServiceChp) {
        this.trayectoriaServiceChp = trayectoriaServiceChp;
        this.programaEstudioServiceChp = programaEstudioServiceChp;
        this.planEstudioServiceChp = planEstudioServiceChp;
    }

    @FXML
    public void initialize() {
        colPrograma.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPrograma().getNombre()));
        colPlan.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPlanEstudio().getCodigo()));
        colFechas.setCellValueFactory(d -> new SimpleStringProperty(
                ymclTextoFecha(d.getValue().getFechaInicio()) + " - " + ymclTextoFecha(d.getValue().getFechaFin())));
        colEstado.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEstado().name()));
        colObservacion.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getObservacion() == null ? "" : d.getValue().getObservacion()));
    }

    public void setEstudiante(YmclEstudiante estudianteChp) {
        this.estudianteChp = estudianteChp;
        estudianteLabel.setText(estudianteChp.getApellidoPaterno()
                + (estudianteChp.getApellidoMaterno() == null ? "" : " " + estudianteChp.getApellidoMaterno())
                + ", " + estudianteChp.getNombres());
        ymclRecargar();
    }

    private void ymclRecargar() {
        tabla.getItems().setAll(trayectoriaServiceChp.ymclListarPorEstudiante(estudianteChp.getId()));
    }

    @FXML
    public void ymclOnIniciar() {
        ymclMostrarFormularioPlan("Iniciar trayectoria", (programaChp, planChp, fechaChp, observacionChp) -> {
            trayectoriaServiceChp.ymclIniciar(estudianteChp.getId(), programaChp.getId(), planChp.getId(), fechaChp, observacionChp);
            ymclRecargar();
        });
    }

    @FXML
    public void ymclOnCambiarPlan() {
        ymclMostrarFormularioPlan("Cambiar de plan", (programaChp, planChp, fechaChp, observacionChp) -> {
            trayectoriaServiceChp.ymclCambiarPlan(estudianteChp.getId(), programaChp.getId(), planChp.getId(), fechaChp, observacionChp);
            ymclRecargar();
        });
    }

    @FXML
    public void ymclOnFinalizar() {
        trayectoriaServiceChp.ymclObtenerActiva(estudianteChp.getId()).ifPresentOrElse(
                activaChp -> {
                    trayectoriaServiceChp.ymclFinalizar(activaChp.getId(), LocalDate.now(), null);
                    ymclRecargar();
                },
                () -> ymclMostrarAdvertencia("El estudiante no tiene una trayectoria activa."));
    }

    @FXML
    public void ymclOnRetirar() {
        trayectoriaServiceChp.ymclObtenerActiva(estudianteChp.getId()).ifPresentOrElse(
                activaChp -> {
                    trayectoriaServiceChp.ymclRetirar(activaChp.getId(), LocalDate.now(), null);
                    ymclRecargar();
                },
                () -> ymclMostrarAdvertencia("El estudiante no tiene una trayectoria activa."));
    }

    @FXML
    public void ymclOnEditar() {
        YmclTrayectoria seleccionadaChp = tabla.getSelectionModel().getSelectedItem();
        if (seleccionadaChp == null) {
            ymclMostrarAdvertencia("Seleccione una trayectoria de la tabla.");
            return;
        }
        ymclMostrarFormularioPlan("Editar trayectoria", seleccionadaChp, (programaChp, planChp, fechaChp, observacionChp) -> {
            trayectoriaServiceChp.ymclEditar(seleccionadaChp.getId(), programaChp.getId(), planChp.getId(), fechaChp, observacionChp);
            ymclRecargar();
        });
    }

    @FXML
    public void ymclOnReactivar() {
        YmclTrayectoria seleccionadaChp = tabla.getSelectionModel().getSelectedItem();
        if (seleccionadaChp == null) {
            ymclMostrarAdvertencia("Seleccione una trayectoria de la tabla.");
            return;
        }
        try {
            trayectoriaServiceChp.ymclReactivar(seleccionadaChp.getId());
            ymclRecargar();
        } catch (RyeValidacionException e) {
            ymclMostrarAdvertencia(e.getMessage());
        }
    }

    @FXML
    public void ymclOnEliminar() {
        YmclTrayectoria seleccionadaChp = tabla.getSelectionModel().getSelectedItem();
        if (seleccionadaChp == null) {
            ymclMostrarAdvertencia("Seleccione una trayectoria de la tabla.");
            return;
        }
        Alert confirmacionChp = new Alert(Alert.AlertType.CONFIRMATION,
                "Esto eliminara de forma permanente la trayectoria en el plan '" + seleccionadaChp.getPlanEstudio().getCodigo()
                        + "' y todas las notas registradas en ella.\n\n¿Desea continuar?",
                ButtonType.YES, ButtonType.NO);
        confirmacionChp.setHeaderText("Eliminar trayectoria");
        if (confirmacionChp.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
            return;
        }
        trayectoriaServiceChp.ymclEliminar(seleccionadaChp.getId());
        ymclRecargar();
    }

    private void ymclMostrarFormularioPlan(String tituloChp, YmclAccionPlan accionChp) {
        ymclMostrarFormularioPlan(tituloChp, null, accionChp);
    }

    private void ymclMostrarFormularioPlan(String tituloChp, YmclTrayectoria existenteChp, YmclAccionPlan accionChp) {
        Dialog<ButtonType> dialogChp = new Dialog<>();
        dialogChp.setTitle(tituloChp);
        dialogChp.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane gridChp = new GridPane();
        gridChp.setHgap(8);
        gridChp.setVgap(8);
        gridChp.setPadding(new Insets(16));

        ComboBox<RacaProgramaEstudio> programaComboChp = new ComboBox<>();
        programaComboChp.setConverter(ymclConvertidor(p -> p.getCodigo() + " - " + p.getNombre()));
        programaComboChp.getItems().setAll(programaEstudioServiceChp.racaListar());

        ComboBox<RacaPlanEstudio> planComboChp = new ComboBox<>();
        planComboChp.setConverter(ymclConvertidor(p -> p.getCodigo() + " - " + p.getNombre()));
        programaComboChp.valueProperty().addListener((obsChp, anteriorChp, nuevoChp) -> {
            List<RacaPlanEstudio> planesChp = nuevoChp == null ? List.of() : planEstudioServiceChp.racaListarPorPrograma(nuevoChp.getId());
            planComboChp.getItems().setAll(planesChp);
            if (existenteChp != null && nuevoChp != null && nuevoChp.getId().equals(existenteChp.getPrograma().getId())) {
                planesChp.stream().filter(p -> p.getId().equals(existenteChp.getPlanEstudio().getId())).findFirst()
                        .ifPresentOrElse(planComboChp.getSelectionModel()::select,
                                () -> { if (!planesChp.isEmpty()) { planComboChp.getSelectionModel().selectFirst(); } });
            } else if (!planesChp.isEmpty()) {
                planComboChp.getSelectionModel().selectFirst();
            }
        });

        DatePicker fechaPickerChp = new DatePicker(LocalDate.now());
        TextArea observacionFieldChp = new TextArea();
        observacionFieldChp.setPrefRowCount(3);

        if (existenteChp != null) {
            fechaPickerChp.setValue(existenteChp.getFechaInicio());
            observacionFieldChp.setText(existenteChp.getObservacion() == null ? "" : existenteChp.getObservacion());

            programaComboChp.getItems().stream().filter(p -> p.getId().equals(existenteChp.getPrograma().getId()))
                    .findFirst()

                    .ifPresent(programaComboChp.getSelectionModel()::select);
        }

        int filaChp = 0;
        gridChp.addRow(filaChp++, new Label("Programa:"), programaComboChp);
        gridChp.addRow(filaChp++, new Label("Plan:"), planComboChp);
        gridChp.addRow(filaChp++, new Label("Fecha:"), fechaPickerChp);
        gridChp.addRow(filaChp, new Label("Observacion:"), observacionFieldChp);

        dialogChp.getDialogPane().setContent(gridChp);

        dialogChp.showAndWait().filter(botonChp -> botonChp == ButtonType.OK).ifPresent(botonChp -> {
            if (programaComboChp.getValue() == null || planComboChp.getValue() == null) {
                ymclMostrarAdvertencia("Seleccione programa y plan.");
                return;
            }
            try {
                accionChp.ymclEjecutar(programaComboChp.getValue(), planComboChp.getValue(), fechaPickerChp.getValue(),
                        observacionFieldChp.getText());
            } catch (RyeValidacionException e) {
                ymclMostrarAdvertencia(e.getMessage());
            }
        });
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

    private String ymclTextoFecha(LocalDate fechaChp) {
        return fechaChp == null ? "?" : fechaChp.toString();
    }

    private void ymclMostrarAdvertencia(String mensajeChp) {
        new Alert(Alert.AlertType.WARNING, mensajeChp, ButtonType.OK).showAndWait();
    }

    @FunctionalInterface
    private interface YmclAccionPlan {
        void ymclEjecutar(RacaProgramaEstudio programaChp, RacaPlanEstudio planChp, LocalDate fechaChp, String observacionChp);
    }
}
