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

import pe.edu.sgfa.common.exception.SgfaException;
import pe.edu.sgfa.ocr.service.LectorReportesPdf;
import pe.edu.sgfa.ocr.service.LectorReportesPdf.HojaLeida;
import pe.edu.sgfa.ocr.service.ResolutorReporte;
import pe.edu.sgfa.ocr.service.ResolutorReporte.Estado;
import pe.edu.sgfa.ocr.service.ResolutorReporte.Propuesta;
import pe.edu.sgfa.ocr.service.ResolutorReporte.ResultadoGuardado;
import pe.edu.sgfa.shell.CerrarVistaEvent;
import pe.edu.sgfa.shell.NavigationService;

@Component
@Scope("prototype")
public class OcrReporteLoteController {

    private static final String LISTO = "LISTO";
    private static final String REVISAR = "REVISAR";
    private static final String GUARDADO = "GUARDADO";
    private static final String ERROR = "ERROR";

    private final LectorReportesPdf lector;
    private final ResolutorReporte resolutor;
    private final NavigationService navigationService;

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
    private TableView<FilaLote> tabla;
    @FXML
    private Label resumenLabel;
    @FXML
    private Button guardarButton;
    @FXML
    private TextArea resultadoArea;

    private File pdf;
    private final AtomicBoolean cancelar = new AtomicBoolean();

    public OcrReporteLoteController(LectorReportesPdf lector, ResolutorReporte resolutor,
                                     NavigationService navigationService) {
        this.lector = lector;
        this.resolutor = resolutor;
        this.navigationService = navigationService;
    }

    @FXML
    public void initialize() {
        tabla.setEditable(true);

        TableColumn<FilaLote, Boolean> colMrc = new TableColumn<>("Guardar");
        colMrc.setCellValueFactory(d -> d.getValue().marcada);
        colMrc.setCellFactory(CheckBoxTableCell.forTableColumn(colMrc));
        colMrc.setEditable(true);
        colMrc.setPrefWidth(62);

        TableColumn<FilaLote, String> colHj = texto("Hoja", f -> String.valueOf(f.hoja.numero()), 48);
        TableColumn<FilaLote, String> colMtrcl = texto("Matricula", f -> f.propuesta.matricula() == null
                ? "(no se leyo)" : f.propuesta.matricula(), 90);
        TableColumn<FilaLote, String> colEstdnt = texto("Estudiante", FilaLote::estudiante, 220);
        TableColumn<FilaLote, String> colDstn = texto("Plan / Ciclo / Periodo", FilaLote::destino, 230);
        TableColumn<FilaLote, String> colCrss = texto("Cursos", f -> f.propuesta.cursosEmparejados() + "/"
                + f.propuesta.cursos().size(), 60);
        TableColumn<FilaLote, String> colPrmd = texto("Promedio (calc. / impreso)", FilaLote::promedios, 150);

        TableColumn<FilaLote, String> colEstd = new TableColumn<>("Estado");
        colEstd.setCellValueFactory(d -> d.getValue().estado);
        colEstd.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String estado, boolean vc) {
                super.updateItem(estado, vc);
                setText(vc ? null : estado);
                setStyle(vc ? "" : "-fx-font-weight: bold; -fx-text-fill: " + switch (estado) {
                    case LISTO -> "#1e8e3e";
                    case REVISAR -> "#c77700";
                    case ERROR -> "#c62828";
                    default -> "#6b7280";
                } + ";");
            }
        });
        colEstd.setPrefWidth(90);

        TableColumn<FilaLote, String> colAvss = new TableColumn<>("Avisos");
        colAvss.setCellValueFactory(d -> d.getValue().mensaje);
        colAvss.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String mensaje, boolean vc) {
                super.updateItem(mensaje, vc);
                setText(vc || mensaje == null ? null : mensaje.lines().findFirst().orElse(""));
                setTooltip(vc || mensaje == null || mensaje.isBlank() ? null : new Tooltip(mensaje));
            }
        });
        colAvss.setPrefWidth(420);

        tabla.getColumns().addAll(colMrc, colHj, colMtrcl, colEstdnt, colDstn, colCrss, colPrmd,
                colEstd, colAvss);
        tabla.setPlaceholder(new Label("Elija un PDF y pulse \"Leer PDF\"."));
        cancelarButton.setDisable(true);
        guardarButton.setDisable(true);
    }

    private TableColumn<FilaLote, String> texto(String ttl, java.util.function.Function<FilaLote, String> valor,
                                                  double anch) {
        TableColumn<FilaLote, String> clmn = new TableColumn<>(ttl);
        clmn.setCellValueFactory(d -> new ReadOnlyStringWrapper(valor.apply(d.getValue())));
        clmn.setPrefWidth(anch);
        return clmn;
    }

    @FXML
    public void onElegirPdf() {
        FileChooser chsr = new FileChooser();
        chsr.setTitle("Seleccionar PDF con los reportes");
        chsr.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File archv = chsr.showOpenDialog(tabla.getScene().getWindow());
        if (archv != null) {
            pdf = archv;
            archivoLabel.setText(archv.getName());
        }
    }

    @FXML
    public void onLeer() {
        if (pdf == null) {
            mostrarAdvertencia("Elija primero el PDF.");
            return;
        }
        cancelar.set(false);
        leerButton.setDisable(true);
        cancelarButton.setDisable(false);
        guardarButton.setDisable(true);
        tabla.getItems().clear();
        resultadoArea.clear();
        File archv = pdf;

        Task<List<FilaLote>> tr = new Task<>() {
            @Override
            protected List<FilaLote> call() {
                List<HojaLeida> hjs = lector.leer(archv, new LectorReportesPdf.Progreso() {
                    @Override
                    public void hoja(int actl, int ttlr) {
                        updateProgress(actl - 1, ttlr);
                        updateMessage("Leyendo hoja " + actl + " de " + ttlr + "...");
                    }

                    @Override
                    public boolean cancelado() {
                        return cancelar.get();
                    }
                });
                updateMessage("Identificando estudiantes, planes y cursos...");
                List<Propuesta> prpsts = resolutor.proponerTodos(hjs.stream().map(HojaLeida::reporte).toList());
                List<FilaLote> fls = new ArrayList<>();
                for (int i = 0; i < hjs.size(); i++) {
                    fls.add(new FilaLote(hjs.get(i), prpsts.get(i)));
                }
                return fls;
            }
        };
        progreso.progressProperty().bind(tr.progressProperty());
        progresoLabel.textProperty().bind(tr.messageProperty());
        tr.setOnSucceeded(e -> {
            terminarLectura();
            tabla.getItems().setAll(tr.getValue());
            actualizarResumen();
            if (cancelar.get()) {
                resultadoArea.setText("Lectura cancelada: se muestran las hojas leidas hasta ese momento.");
            }
        });
        tr.setOnFailed(e -> {
            terminarLectura();
            mostrarAdvertencia(tr.getException() instanceof SgfaException se ? se.getMessage()
                    : "No se pudo leer el PDF: " + tr.getException().getMessage());
        });
        Thread hl = new Thread(tr, "sgfa-ocr-lote");
        hl.setDaemon(true);
        hl.start();
    }

    private void terminarLectura() {
        progreso.progressProperty().unbind();
        progresoLabel.textProperty().unbind();
        progreso.setProgress(0);
        progresoLabel.setText("");
        leerButton.setDisable(false);
        cancelarButton.setDisable(true);
    }

    @FXML
    public void onCancelar() {
        cancelar.set(true);
        cancelarButton.setDisable(true);
        progresoLabel.textProperty().unbind();
        progresoLabel.setText("Cancelando al terminar la hoja actual...");
    }

    @FXML
    public void onMarcarListas() {
        tabla.getItems().forEach(f -> f.marcada.set(LISTO.equals(f.estado.get())));
        actualizarResumen();
    }

    @FXML
    public void onDesmarcarTodas() {
        tabla.getItems().forEach(f -> f.marcada.set(false));
        actualizarResumen();
    }

    @FXML
    public void onRevisar() {
        FilaLote fl = tabla.getSelectionModel().getSelectedItem();
        if (fl == null) {
            mostrarAdvertencia("Seleccione una hoja de la lista.");
            return;
        }
        navigationService.abrirVista("/fxml/ocr/importar-reporte.fxml", "Revisar hoja " + fl.hoja.numero(),
                controlador -> ((OcrReporteController) controlador).precargar(fl.hoja.imagen(), fl.hoja.reporte(),
                        () -> Platform.runLater(() -> {
                            fl.estado.set(GUARDADO);
                            fl.mensaje.set("Guardada desde la revision.");
                            fl.marcada.set(false);
                            actualizarResumen();
                        })));
    }

    @FXML
    public void onGuardar() {
        List<FilaLote> mrcds = tabla.getItems().stream()
                .filter(f -> f.marcada.get() && !GUARDADO.equals(f.estado.get())).toList();
        if (mrcds.isEmpty()) {
            mostrarAdvertencia("No hay hojas marcadas para guardar.");
            return;
        }
        long conAvss = mrcds.stream().filter(f -> !LISTO.equals(f.estado.get())).count();
        String dtll = conAvss == 0 ? "" : "\n\n" + conAvss + " de ellas tienen avisos (REVISAR): solo se guardan "
                + "los cursos que se emparejaron con el plan.";
        Alert cnfrmr = new Alert(Alert.AlertType.CONFIRMATION, "Se guardaran las notas de " + mrcds.size()
                + " hoja(s). Los estudiantes nuevos se registraran primero." + dtll + "\n\n¿Continuar?",
                ButtonType.OK, ButtonType.CANCEL);
        if (cnfrmr.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        guardarButton.setDisable(true);
        leerButton.setDisable(true);
        Task<String> tr = new Task<>() {
            @Override
            protected String call() {
                int grdds = 0;
                int nvs = 0;
                List<String> errrs = new ArrayList<>();
                for (int i = 0; i < mrcds.size(); i++) {
                    FilaLote fl = mrcds.get(i);
                    updateMessage("Guardando hoja " + fl.hoja.numero() + " (" + (i + 1) + " de " + mrcds.size() + ")...");
                    updateProgress(i, mrcds.size());
                    try {
                        ResultadoGuardado r = resolutor.guardar(fl.propuesta);
                        grdds++;
                        nvs += r.estudianteNuevo() ? 1 : 0;
                        Platform.runLater(() -> {
                            fl.estado.set(GUARDADO);
                            fl.mensaje.set(r.notasGuardadas() + " nota(s) guardada(s)"
                                    + (r.estudianteNuevo() ? "; estudiante registrado." : "."));
                            fl.marcada.set(false);
                        });
                    } catch (SgfaException e) {
                        errrs.add("Hoja " + fl.hoja.numero() + ": " + e.getMessage());
                        Platform.runLater(() -> {
                            fl.estado.set(ERROR);
                            fl.mensaje.set(e.getMessage());
                        });
                    }
                }
                StringBuilder texto = new StringBuilder();
                texto.append("Guardadas: ").append(grdds).append(" hoja(s) (").append(nvs)
                        .append(" estudiante(s) nuevo(s)) | Con error: ").append(errrs.size()).append('\n');
                errrs.forEach(err -> texto.append("  ").append(err).append('\n'));
                return texto.toString();
            }
        };
        progreso.progressProperty().bind(tr.progressProperty());
        progresoLabel.textProperty().bind(tr.messageProperty());
        tr.setOnSucceeded(e -> {
            terminarLectura();
            resultadoArea.setText(tr.getValue());
            actualizarResumen();
        });
        tr.setOnFailed(e -> {
            terminarLectura();
            actualizarResumen();
            mostrarAdvertencia("No se pudo completar el guardado: " + tr.getException().getMessage());
        });
        Thread hl = new Thread(tr, "sgfa-ocr-lote-guardar");
        hl.setDaemon(true);
        hl.start();
    }

    private void actualizarResumen() {
        List<FilaLote> fls = tabla.getItems();
        long lsts = fls.stream().filter(f -> LISTO.equals(f.estado.get())).count();
        long rvsr = fls.stream().filter(f -> REVISAR.equals(f.estado.get())).count();
        long grdds = fls.stream().filter(f -> GUARDADO.equals(f.estado.get())).count();
        long mrcds = fls.stream().filter(f -> f.marcada.get()).count();
        resumenLabel.setText(fls.size() + " hoja(s): " + lsts + " lista(s), " + rvsr + " por revisar, "
                + grdds + " guardada(s) | Marcadas: " + mrcds);
        guardarButton.setDisable(mrcds == 0);
    }

    @FXML
    public void onCerrar() {
        tabla.fireEvent(new CerrarVistaEvent());
    }

    private void mostrarAdvertencia(String mensaje) {
        new Alert(Alert.AlertType.WARNING, mensaje, ButtonType.OK).showAndWait();
    }

    static final class FilaLote {
        final HojaLeida hoja;
        final Propuesta propuesta;
        final BooleanProperty marcada;
        final StringProperty estado;
        final StringProperty mensaje;

        FilaLote(HojaLeida hoja, Propuesta propuesta) {
            this.hoja = hoja;
            this.propuesta = propuesta;
            this.estado = new SimpleStringProperty(propuesta.estado() == Estado.LISTO ? LISTO : REVISAR);
            this.marcada = new SimpleBooleanProperty(propuesta.estado() == Estado.LISTO);

            this.mensaje = new SimpleStringProperty(String.join("\n",
                    propuesta.avisos().isEmpty() ? propuesta.notas() : propuesta.avisos()));
        }

        String estudiante() {
            if (propuesta.matricula() == null && propuesta.apellidoPaterno() == null) {
                return "(no se leyo)";
            }
            String mtrn = propuesta.apellidoMaterno() == null ? "" : " " + propuesta.apellidoMaterno();
            return (propuesta.apellidoPaterno() == null ? "?" : propuesta.apellidoPaterno()) + mtrn + ", "
                    + (propuesta.nombres() == null ? "?" : propuesta.nombres()) + (propuesta.esNuevo() ? "  (nuevo)" : "");
        }

        String destino() {
            return (propuesta.plan() == null ? "?" : propuesta.plan().getCodigo()) + " / "
                    + (propuesta.ciclo() == null ? "?" : propuesta.ciclo().getNombre()) + " / "
                    + (propuesta.periodo() == null ? "?" : propuesta.periodo().getNombre());
        }

        String promedios() {
            String clcld = propuesta.promedioCalculado() == null ? "-" : String.format("%.2f", propuesta.promedioCalculado());
            String imprs = propuesta.promedioImpreso() == null ? "-" : String.format("%.2f", propuesta.promedioImpreso());
            return clcld + " / " + imprs;
        }
    }
}
