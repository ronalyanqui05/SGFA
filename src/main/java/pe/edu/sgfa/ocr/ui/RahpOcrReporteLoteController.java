package pe.edu.sgfa.ocr.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.stage.FileChooser;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pe.edu.sgfa.common.exception.RyeSgfaException;
import pe.edu.sgfa.ocr.service.RahpLectorReportesPdf;
import pe.edu.sgfa.ocr.service.RahpLectorReportesPdf.RahpHojaLeida;
import pe.edu.sgfa.ocr.service.RahpResolutorReporte;
import pe.edu.sgfa.ocr.service.RahpResolutorReporte.RahpEstado;
import pe.edu.sgfa.ocr.service.RahpResolutorReporte.RahpPropuesta;
import pe.edu.sgfa.ocr.service.RahpResolutorReporte.RahpResultadoGuardado;
import pe.edu.sgfa.shell.RyeCerrarVistaEvent;
import pe.edu.sgfa.shell.RyeNavigationService;

@Component
@Scope("prototype")
public class RahpOcrReporteLoteController {

    private static final String LISTO_HNC = "LISTO";
    private static final String REVISAR_HNC = "REVISAR";
    private static final String GUARDADO_HNC = "GUARDADO";
    private static final String ERROR_HNC = "ERROR";

    private final RahpLectorReportesPdf lectorHnc;
    private final RahpResolutorReporte resolutorHnc;
    private final RyeNavigationService navigationServiceHnc;

    @FXML
    private Label archivoLabel;
    @FXML
    private Button leerButton;
    @FXML
    private Button cancelarButton;
    @FXML
    private ProgressBar progreso;
    @FXML
    private Label progresoLabel;
    @FXML
    private TableView<RahpFilaLote> tabla;
    @FXML
    private Label resumenLabel;
    @FXML
    private Button guardarButton;
    @FXML
    private TextArea resultadoArea;

    private File pdf;
    private final AtomicBoolean cancelarHnc = new AtomicBoolean();

    public RahpOcrReporteLoteController(RahpLectorReportesPdf lectorHnc, RahpResolutorReporte resolutorHnc,
                                     RyeNavigationService navigationServiceHnc) {
        this.lectorHnc = lectorHnc;
        this.resolutorHnc = resolutorHnc;
        this.navigationServiceHnc = navigationServiceHnc;
    }

    @FXML
    public void initialize() {
        tabla.setEditable(true);

        TableColumn<RahpFilaLote, Boolean> colMarcaHnc = new TableColumn<>("Guardar");
        colMarcaHnc.setCellValueFactory(d -> d.getValue().marcada);
        colMarcaHnc.setCellFactory(CheckBoxTableCell.forTableColumn(colMarcaHnc));
        colMarcaHnc.setEditable(true);
        colMarcaHnc.setPrefWidth(62);

        TableColumn<RahpFilaLote, String> colHojaHnc = rahpTexto("Hoja", f -> String.valueOf(f.hoja.numero()), 48);
        TableColumn<RahpFilaLote, String> colMatriculaHnc = rahpTexto("Matricula", f -> f.propuesta.matricula() == null
                ? "(no se leyo)" : f.propuesta.matricula(), 90);
        TableColumn<RahpFilaLote, String> colEstudianteHnc = rahpTexto("Estudiante", RahpFilaLote::rahpEstudiante, 220);
        TableColumn<RahpFilaLote, String> colDestinoHnc = rahpTexto("Plan / Ciclo / Periodo", RahpFilaLote::rahpDestino, 230);
        TableColumn<RahpFilaLote, String> colCursosHnc = rahpTexto("Cursos", f -> f.propuesta.rahpCursosEmparejados() + "/"
                + f.propuesta.cursos().size(), 60);
        TableColumn<RahpFilaLote, String> colPromedioHnc = rahpTexto("Promedio (calc. / impreso)", RahpFilaLote::rahpPromedios, 150);

        TableColumn<RahpFilaLote, String> colEstadoHnc = new TableColumn<>("Estado");
        colEstadoHnc.setCellValueFactory(d -> d.getValue().estado);
        colEstadoHnc.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String estadoHnc, boolean vacioHnc) {
                super.updateItem(estadoHnc, vacioHnc);
                setText(vacioHnc ? null : estadoHnc);
                setStyle(vacioHnc ? "" : "-fx-font-weight: bold; -fx-text-fill: " + switch (estadoHnc) {
                    case LISTO_HNC -> "#1e8e3e";
                    case REVISAR_HNC -> "#c77700";
                    case ERROR_HNC -> "#c62828";
                    default -> "#6b7280";
                } + ";");
            }
        });
        colEstadoHnc.setPrefWidth(90);

        TableColumn<RahpFilaLote, String> colAvisosHnc = new TableColumn<>("Avisos");
        colAvisosHnc.setCellValueFactory(d -> d.getValue().mensaje);
        colAvisosHnc.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String mensajeHnc, boolean vacioHnc) {
                super.updateItem(mensajeHnc, vacioHnc);
                setText(vacioHnc || mensajeHnc == null ? null : mensajeHnc.lines().findFirst().orElse(""));
                setTooltip(vacioHnc || mensajeHnc == null || mensajeHnc.isBlank() ? null : new Tooltip(mensajeHnc));
            }
        });
        colAvisosHnc.setPrefWidth(420);

        tabla.getColumns().addAll(colMarcaHnc, colHojaHnc, colMatriculaHnc, colEstudianteHnc, colDestinoHnc, colCursosHnc, colPromedioHnc,
                colEstadoHnc, colAvisosHnc);
        tabla.setPlaceholder(new Label("Elija un PDF y pulse \"Leer PDF\"."));
        cancelarButton.setDisable(true);
        guardarButton.setDisable(true);
    }

    private TableColumn<RahpFilaLote, String> rahpTexto(String tituloHnc, java.util.function.Function<RahpFilaLote, String> valorHnc,
                                                  double anchoHnc) {
        TableColumn<RahpFilaLote, String> columnaHnc = new TableColumn<>(tituloHnc);
        columnaHnc.setCellValueFactory(d -> new ReadOnlyStringWrapper(valorHnc.apply(d.getValue())));
        columnaHnc.setPrefWidth(anchoHnc);
        return columnaHnc;
    }

    @FXML
    public void rahpOnElegirPdf() {
        FileChooser chooserHnc = new FileChooser();
        chooserHnc.setTitle("Seleccionar PDF con los reportes");
        chooserHnc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File archivoHnc = chooserHnc.showOpenDialog(tabla.getScene().getWindow());
        if (archivoHnc != null) {
            pdf = archivoHnc;
            archivoLabel.setText(archivoHnc.getName());
        }
    }

    @FXML
    public void rahpOnLeer() {
        if (pdf == null) {
            rahpMostrarAdvertencia("Elija primero el PDF.");
            return;
        }
        cancelarHnc.set(false);
        leerButton.setDisable(true);
        cancelarButton.setDisable(false);
        guardarButton.setDisable(true);
        tabla.getItems().clear();
        resultadoArea.clear();
        File archivoHnc = pdf;

        Task<List<RahpFilaLote>> tareaHnc = new Task<>() {
            @Override
            protected List<RahpFilaLote> call() {
                List<RahpHojaLeida> hojasHnc = lectorHnc.rahpLeer(archivoHnc, new RahpLectorReportesPdf.RahpProgreso() {
                    @Override
                    public void hoja(int actualHnc, int totalHnc) {
                        updateProgress(actualHnc - 1, totalHnc);
                        updateMessage("Leyendo hoja " + actualHnc + " de " + totalHnc + "...");
                    }

                    @Override
                    public boolean cancelado() {
                        return cancelarHnc.get();
                    }
                });
                updateMessage("Identificando estudiantes, planes y cursos...");
                List<RahpPropuesta> propuestasHnc = resolutorHnc.rahpProponerTodos(hojasHnc.stream().map(RahpHojaLeida::reporte).toList());
                List<RahpFilaLote> filasHnc = new ArrayList<>();
                for (int i = 0; i < hojasHnc.size(); i++) {
                    filasHnc.add(new RahpFilaLote(hojasHnc.get(i), propuestasHnc.get(i)));
                }
                return filasHnc;
            }
        };
        progreso.progressProperty().bind(tareaHnc.progressProperty());
        progresoLabel.textProperty().bind(tareaHnc.messageProperty());
        tareaHnc.setOnSucceeded(e -> {
            rahpTerminarLectura();
            tabla.getItems().setAll(tareaHnc.getValue());
            rahpActualizarResumen();
            if (cancelarHnc.get()) {
                resultadoArea.setText("Lectura cancelada: se muestran las hojas leidas hasta ese momento.");
            }
        });
        tareaHnc.setOnFailed(e -> {
            rahpTerminarLectura();
            rahpMostrarAdvertencia(tareaHnc.getException() instanceof RyeSgfaException se ? se.getMessage()
                    : "No se pudo leer el PDF: " + tareaHnc.getException().getMessage());
        });
        Thread hiloHnc = new Thread(tareaHnc, "sgfa-ocr-lote");
        hiloHnc.setDaemon(true);
        hiloHnc.start();
    }

    private void rahpTerminarLectura() {
        progreso.progressProperty().unbind();
        progresoLabel.textProperty().unbind();
        progreso.setProgress(0);
        progresoLabel.setText("");
        leerButton.setDisable(false);
        cancelarButton.setDisable(true);
    }

    @FXML
    public void rahpOnCancelar() {
        cancelarHnc.set(true);
        cancelarButton.setDisable(true);
        progresoLabel.textProperty().unbind();
        progresoLabel.setText("Cancelando al terminar la hoja actual...");
    }

    @FXML
    public void rahpOnMarcarListas() {
        tabla.getItems().forEach(f -> f.marcada.set(LISTO_HNC.equals(f.estado.get())));
        rahpActualizarResumen();
    }

    @FXML
    public void rahpOnDesmarcarTodas() {
        tabla.getItems().forEach(f -> f.marcada.set(false));
        rahpActualizarResumen();
    }

    @FXML
    public void rahpOnRevisar() {
        RahpFilaLote filaHnc = tabla.getSelectionModel().getSelectedItem();
        if (filaHnc == null) {
            rahpMostrarAdvertencia("Seleccione una hoja de la lista.");
            return;
        }
        navigationServiceHnc.ryeAbrirVista("/fxml/ocr/importar-reporte.fxml", "Revisar hoja " + filaHnc.hoja.numero(),
                controladorHnc -> ((RahpOcrReporteController) controladorHnc).rahpPrecargar(filaHnc.hoja.rahpImagen(), filaHnc.hoja.reporte(),
                        () -> Platform.runLater(() -> {
                            filaHnc.estado.set(GUARDADO_HNC);
                            filaHnc.mensaje.set("Guardada desde la revision.");
                            filaHnc.marcada.set(false);
                            rahpActualizarResumen();
                        })));
    }

    @FXML
    public void rahpOnGuardar() {
        List<RahpFilaLote> marcadasHnc = tabla.getItems().stream()
                .filter(f -> f.marcada.get() && !GUARDADO_HNC.equals(f.estado.get())).toList();
        if (marcadasHnc.isEmpty()) {
            rahpMostrarAdvertencia("No hay hojas marcadas para guardar.");
            return;
        }
        long conAvisosHnc = marcadasHnc.stream().filter(f -> !LISTO_HNC.equals(f.estado.get())).count();
        String detalleHnc = conAvisosHnc == 0 ? "" : "\n\n" + conAvisosHnc + " de ellas tienen avisos (REVISAR): solo se guardan "
                + "los cursos que se emparejaron con el plan.";
        Alert confirmarHnc = new Alert(Alert.AlertType.CONFIRMATION, "Se guardaran las notas de " + marcadasHnc.size()
                + " hoja(s). Los estudiantes nuevos se registraran primero." + detalleHnc + "\n\n¿Continuar?",
                ButtonType.OK, ButtonType.CANCEL);
        if (confirmarHnc.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        guardarButton.setDisable(true);
        leerButton.setDisable(true);
        Task<String> tareaHnc = new Task<>() {
            @Override
            protected String call() {
                int guardadasHnc = 0;
                int nuevosHnc = 0;
                List<String> erroresHnc = new ArrayList<>();
                for (int i = 0; i < marcadasHnc.size(); i++) {
                    RahpFilaLote filaHnc = marcadasHnc.get(i);
                    updateMessage("Guardando hoja " + filaHnc.hoja.numero() + " (" + (i + 1) + " de " + marcadasHnc.size() + ")...");
                    updateProgress(i, marcadasHnc.size());
                    try {
                        RahpResultadoGuardado r = resolutorHnc.rahpGuardar(filaHnc.propuesta);
                        guardadasHnc++;
                        nuevosHnc += r.estudianteNuevo() ? 1 : 0;
                        Platform.runLater(() -> {
                            filaHnc.estado.set(GUARDADO_HNC);
                            filaHnc.mensaje.set(r.notasGuardadas() + " nota(s) guardada(s)"
                                    + (r.estudianteNuevo() ? "; estudiante registrado." : "."));
                            filaHnc.marcada.set(false);
                        });
                    } catch (RyeSgfaException e) {
                        erroresHnc.add("Hoja " + filaHnc.hoja.numero() + ": " + e.getMessage());
                        Platform.runLater(() -> {
                            filaHnc.estado.set(ERROR_HNC);
                            filaHnc.mensaje.set(e.getMessage());
                        });
                    }
                }
                StringBuilder textoHnc = new StringBuilder();
                textoHnc.append("Guardadas: ").append(guardadasHnc).append(" hoja(s) (").append(nuevosHnc)
                        .append(" estudiante(s) nuevo(s)) | Con error: ").append(erroresHnc.size()).append('\n');
                erroresHnc.forEach(errHnc -> textoHnc.append("  ").append(errHnc).append('\n'));
                return textoHnc.toString();
            }
        };
        progreso.progressProperty().bind(tareaHnc.progressProperty());
        progresoLabel.textProperty().bind(tareaHnc.messageProperty());
        tareaHnc.setOnSucceeded(e -> {
            rahpTerminarLectura();
            resultadoArea.setText(tareaHnc.getValue());
            rahpActualizarResumen();
        });
        tareaHnc.setOnFailed(e -> {
            rahpTerminarLectura();
            rahpActualizarResumen();
            rahpMostrarAdvertencia("No se pudo completar el guardado: " + tareaHnc.getException().getMessage());
        });
        Thread hiloHnc = new Thread(tareaHnc, "sgfa-ocr-lote-guardar");
        hiloHnc.setDaemon(true);
        hiloHnc.start();
    }

    private void rahpActualizarResumen() {
        List<RahpFilaLote> filasHnc = tabla.getItems();
        long listasHnc = filasHnc.stream().filter(f -> LISTO_HNC.equals(f.estado.get())).count();
        long revisarHnc = filasHnc.stream().filter(f -> REVISAR_HNC.equals(f.estado.get())).count();
        long guardadasHnc = filasHnc.stream().filter(f -> GUARDADO_HNC.equals(f.estado.get())).count();
        long marcadasHnc = filasHnc.stream().filter(f -> f.marcada.get()).count();
        resumenLabel.setText(filasHnc.size() + " hoja(s): " + listasHnc + " lista(s), " + revisarHnc + " por revisar, "
                + guardadasHnc + " guardada(s) | Marcadas: " + marcadasHnc);
        guardarButton.setDisable(marcadasHnc == 0);
    }

    @FXML
    public void rahpOnCerrar() {
        tabla.fireEvent(new RyeCerrarVistaEvent());
    }

    private void rahpMostrarAdvertencia(String mensajeHnc) {
        new Alert(Alert.AlertType.WARNING, mensajeHnc, ButtonType.OK).showAndWait();
    }

    static final class RahpFilaLote {
        final RahpHojaLeida hoja;
        final RahpPropuesta propuesta;
        final BooleanProperty marcada;
        final StringProperty estado;
        final StringProperty mensaje;

        RahpFilaLote(RahpHojaLeida hojaHnc, RahpPropuesta propuestaHnc) {
            this.hoja = hojaHnc;
            this.propuesta = propuestaHnc;
            this.estado = new SimpleStringProperty(propuestaHnc.rahpEstado() == RahpEstado.LISTO ? LISTO_HNC : REVISAR_HNC);
            this.marcada = new SimpleBooleanProperty(propuestaHnc.rahpEstado() == RahpEstado.LISTO);

            this.mensaje = new SimpleStringProperty(String.join("\n",
                    propuestaHnc.avisos().isEmpty() ? propuestaHnc.notas() : propuestaHnc.avisos()));
        }

        String rahpEstudiante() {
            if (propuesta.matricula() == null && propuesta.apellidoPaterno() == null) {
                return "(no se leyo)";
            }
            String maternoHnc = propuesta.apellidoMaterno() == null ? "" : " " + propuesta.apellidoMaterno();
            return (propuesta.apellidoPaterno() == null ? "?" : propuesta.apellidoPaterno()) + maternoHnc + ", "
                    + (propuesta.nombres() == null ? "?" : propuesta.nombres()) + (propuesta.rahpEsNuevo() ? "  (nuevo)" : "");
        }

        String rahpDestino() {
            return (propuesta.plan() == null ? "?" : propuesta.plan().getCodigo()) + " / "
                    + (propuesta.ciclo() == null ? "?" : propuesta.ciclo().getNombre()) + " / "
                    + (propuesta.periodo() == null ? "?" : propuesta.periodo().getNombre());
        }

        String rahpPromedios() {
            String calculadoHnc = propuesta.promedioCalculado() == null ? "-" : String.format("%.2f", propuesta.promedioCalculado());
            String impresoHnc = propuesta.promedioImpreso() == null ? "-" : String.format("%.2f", propuesta.promedioImpreso());
            return calculadoHnc + " / " + impresoHnc;
        }
    }
}
