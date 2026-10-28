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

import pe.edu.sgfa.common.exception.ValidacionException;
import pe.edu.sgfa.estudiante.domain.Estudiante;
import pe.edu.sgfa.programa.domain.PlanEstudio;
import pe.edu.sgfa.programa.domain.ProgramaEstudio;
import pe.edu.sgfa.programa.service.PlanEstudioService;
import pe.edu.sgfa.programa.service.ProgramaEstudioService;
import pe.edu.sgfa.trayectoria.domain.EstadoTrayectoria;
import pe.edu.sgfa.trayectoria.domain.Trayectoria;
import pe.edu.sgfa.trayectoria.service.TrayectoriaService;

@Component
public class TrayectoriaController {

    private final TrayectoriaService trayectoriaService;
    private final ProgramaEstudioService programaEstudioService;
    private final PlanEstudioService planEstudioService;

    private Estudiante estudiante;

    @FXML
    private Label estudianteLabel;
    @FXML
    private TableView<Trayectoria> tabla;
    @FXML
    private TableColumn<Trayectoria, String> colPrograma;
    @FXML
    private TableColumn<Trayectoria, String> colPlan;
    @FXML
    private TableColumn<Trayectoria, String> colFechas;
    @FXML
    private TableColumn<Trayectoria, String> colEstado;
    @FXML
    private TableColumn<Trayectoria, String> colObservacion;

    public TrayectoriaController(TrayectoriaService trayectoriaService, ProgramaEstudioService programaEstudioService,
                                  PlanEstudioService planEstudioService) {
        this.trayectoriaService = trayectoriaService;
        this.programaEstudioService = programaEstudioService;
        this.planEstudioService = planEstudioService;
    }

    @FXML
    public void initialize() {
        colPrograma.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPrograma().getNombre()));
        colPlan.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPlanEstudio().getCodigo()));
        colFechas.setCellValueFactory(d -> new SimpleStringProperty(
                textoFecha(d.getValue().getFechaInicio()) + " - " + textoFecha(d.getValue().getFechaFin())));
        colEstado.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEstado().name()));
        colObservacion.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getObservacion() == null ? "" : d.getValue().getObservacion()));
    }

    public void setEstudiante(Estudiante estudiante) {
        this.estudiante = estudiante;
        estudianteLabel.setText(estudiante.getApellidoPaterno()
                + (estudiante.getApellidoMaterno() == null ? "" : " " + estudiante.getApellidoMaterno())
                + ", " + estudiante.getNombres());
        recargar();
    }

    private void recargar() {
        tabla.getItems().setAll(trayectoriaService.listarPorEstudiante(estudiante.getId()));
    }

    @FXML
    public void onIniciar() {
        mostrarFormularioPlan("Iniciar trayectoria", (programa, pln, fch, obsrvcn) -> {
            trayectoriaService.iniciar(estudiante.getId(), programa.getId(), pln.getId(), fch, obsrvcn);
            recargar();
        });
    }

    @FXML
    public void onCambiarPlan() {
        mostrarFormularioPlan("Cambiar de plan", (programa, pln, fch, obsrvcn) -> {
            trayectoriaService.cambiarPlan(estudiante.getId(), programa.getId(), pln.getId(), fch, obsrvcn);
            recargar();
        });
    }

    @FXML
    public void onFinalizar() {
        trayectoriaService.obtenerActiva(estudiante.getId()).ifPresentOrElse(
                activa -> {
                    trayectoriaService.finalizar(activa.getId(), LocalDate.now(), null);
                    recargar();
                },
                () -> mostrarAdvertencia("El estudiante no tiene una trayectoria activa."));
    }

    @FXML
    public void onRetirar() {
        trayectoriaService.obtenerActiva(estudiante.getId()).ifPresentOrElse(
                activa -> {
                    trayectoriaService.retirar(activa.getId(), LocalDate.now(), null);
                    recargar();
                },
                () -> mostrarAdvertencia("El estudiante no tiene una trayectoria activa."));
    }

    @FXML
    public void onEditar() {
        Trayectoria slccnd = tabla.getSelectionModel().getSelectedItem();
        if (slccnd == null) {
            mostrarAdvertencia("Seleccione una trayectoria de la tabla.");
            return;
        }
        mostrarFormularioPlan("Editar trayectoria", slccnd, (programa, pln, fch, obsrvcn) -> {
            trayectoriaService.editar(slccnd.getId(), programa.getId(), pln.getId(), fch, obsrvcn);
            recargar();
        });
    }

    @FXML
    public void onReactivar() {
        Trayectoria slccnd = tabla.getSelectionModel().getSelectedItem();
        if (slccnd == null) {
            mostrarAdvertencia("Seleccione una trayectoria de la tabla.");
            return;
        }
        try {
            trayectoriaService.reactivar(slccnd.getId());
            recargar();
        } catch (ValidacionException e) {
            mostrarAdvertencia(e.getMessage());
        }
    }

    @FXML
    public void onEliminar() {
        Trayectoria slccnd = tabla.getSelectionModel().getSelectedItem();
        if (slccnd == null) {
            mostrarAdvertencia("Seleccione una trayectoria de la tabla.");
            return;
        }
        Alert cnfrmcn = new Alert(Alert.AlertType.CONFIRMATION,
                "Esto eliminara de forma permanente la trayectoria en el plan '" + slccnd.getPlanEstudio().getCodigo()
                        + "' y todas las notas registradas en ella.\n\n¿Desea continuar?",
                ButtonType.YES, ButtonType.NO);
        cnfrmcn.setHeaderText("Eliminar trayectoria");
        if (cnfrmcn.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
            return;
        }
        trayectoriaService.eliminar(slccnd.getId());
        recargar();
    }

    private void mostrarFormularioPlan(String ttl, AccionPlan accn) {
        mostrarFormularioPlan(ttl, null, accn);
    }

    private void mostrarFormularioPlan(String ttl, Trayectoria exstnt, AccionPlan accn) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle(ttl);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grd = new GridPane();
        grd.setHgap(8);
        grd.setVgap(8);
        grd.setPadding(new Insets(16));

        ComboBox<ProgramaEstudio> prgrmCmb = new ComboBox<>();
        prgrmCmb.setConverter(convertidor(p -> p.getCodigo() + " - " + p.getNombre()));
        prgrmCmb.getItems().setAll(programaEstudioService.listar());

        ComboBox<PlanEstudio> plnCmb = new ComboBox<>();
        plnCmb.setConverter(convertidor(p -> p.getCodigo() + " - " + p.getNombre()));
        prgrmCmb.valueProperty().addListener((obs, anterior, nuevo) -> {
            List<PlanEstudio> plns = nuevo == null ? List.of() : planEstudioService.listarPorPrograma(nuevo.getId());
            plnCmb.getItems().setAll(plns);
            if (exstnt != null && nuevo != null && nuevo.getId().equals(exstnt.getPrograma().getId())) {
                plns.stream().filter(p -> p.getId().equals(exstnt.getPlanEstudio().getId())).findFirst()
                        .ifPresentOrElse(plnCmb.getSelectionModel()::select,
                                () -> { if (!plns.isEmpty()) { plnCmb.getSelectionModel().selectFirst(); } });
            } else if (!plns.isEmpty()) {
                plnCmb.getSelectionModel().selectFirst();
            }
        });

        DatePicker fchPckr = new DatePicker(LocalDate.now());
        TextArea obsrvcnFld = new TextArea();
        obsrvcnFld.setPrefRowCount(3);

        if (exstnt != null) {
            fchPckr.setValue(exstnt.getFechaInicio());
            obsrvcnFld.setText(exstnt.getObservacion() == null ? "" : exstnt.getObservacion());

            prgrmCmb.getItems().stream().filter(p -> p.getId().equals(exstnt.getPrograma().getId()))
                    .findFirst()

                    .ifPresent(prgrmCmb.getSelectionModel()::select);
        }

        int fl = 0;
        grd.addRow(fl++, new Label("Programa:"), prgrmCmb);
        grd.addRow(fl++, new Label("Plan:"), plnCmb);
        grd.addRow(fl++, new Label("Fecha:"), fchPckr);
        grd.addRow(fl, new Label("Observacion:"), obsrvcnFld);

        dlg.getDialogPane().setContent(grd);

        dlg.showAndWait().filter(boton -> boton == ButtonType.OK).ifPresent(boton -> {
            if (prgrmCmb.getValue() == null || plnCmb.getValue() == null) {
                mostrarAdvertencia("Seleccione programa y plan.");
                return;
            }
            try {
                accn.ejecutar(prgrmCmb.getValue(), plnCmb.getValue(), fchPckr.getValue(),
                        obsrvcnFld.getText());
            } catch (ValidacionException e) {
                mostrarAdvertencia(e.getMessage());
            }
        });
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

    private String textoFecha(LocalDate fch) {
        return fch == null ? "?" : fch.toString();
    }

    private void mostrarAdvertencia(String mnsj) {
        new Alert(Alert.AlertType.WARNING, mnsj, ButtonType.OK).showAndWait();
    }

    @FunctionalInterface
    private interface AccionPlan {
        void ejecutar(ProgramaEstudio programa, PlanEstudio pln, LocalDate fch, String obsrvcn);
    }
}
