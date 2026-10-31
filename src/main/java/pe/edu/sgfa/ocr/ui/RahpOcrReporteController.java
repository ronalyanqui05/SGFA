package pe.edu.sgfa.ocr.ui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pe.edu.sgfa.common.exception.RyeSgfaException;
import pe.edu.sgfa.common.exception.RyeValidacionException;
import pe.edu.sgfa.estudiante.domain.YmclEstudiante;
import pe.edu.sgfa.estudiante.repository.YmclEstudianteRepository;
import pe.edu.sgfa.estudiante.service.YmclEstudianteService;
import pe.edu.sgfa.nota.domain.YmclSituacionAcademica;
import pe.edu.sgfa.nota.service.YmclEscalaLiteral;
import pe.edu.sgfa.nota.service.YmclItemNota;
import pe.edu.sgfa.nota.service.YmclRegistroAcademicoService;
import pe.edu.sgfa.nota.service.YmclResultadoPromedio;
import pe.edu.sgfa.ocr.service.RahpEmparejadorNombres;
import pe.edu.sgfa.ocr.service.RahpOcrService;
import pe.edu.sgfa.ocr.service.RahpReconocedorReporteCalificaciones;
import pe.edu.sgfa.ocr.service.RahpResolutorReporte;
import pe.edu.sgfa.ocr.service.RahpResolutorReporte.RahpPropuesta;
import pe.edu.sgfa.ocr.service.RahpReconocedorReporteCalificaciones.RahpCursoDetectado;
import pe.edu.sgfa.ocr.service.RahpReconocedorReporteCalificaciones.RahpReporteDetectado;
import pe.edu.sgfa.periodo.domain.YmclPeriodoAcademico;
import pe.edu.sgfa.periodo.service.YmclPeriodoAcademicoService;
import pe.edu.sgfa.programa.domain.RacaCiclo;
import pe.edu.sgfa.programa.domain.RacaEstadoPlanEstudio;
import pe.edu.sgfa.programa.domain.RacaPlanCurso;
import pe.edu.sgfa.programa.domain.RacaPlanEstudio;
import pe.edu.sgfa.programa.domain.RacaProgramaEstudio;
import pe.edu.sgfa.programa.repository.RacaPlanCursoRepository;
import pe.edu.sgfa.programa.service.RacaCicloService;
import pe.edu.sgfa.programa.service.RacaPlanEstudioService;
import pe.edu.sgfa.programa.service.RacaProgramaEstudioService;
import pe.edu.sgfa.shell.RyeCerrarVistaEvent;
import pe.edu.sgfa.trayectoria.domain.YmclTrayectoria;
import pe.edu.sgfa.trayectoria.service.YmclTrayectoriaService;

@Component
@Scope("prototype")
public class RahpOcrReporteController {

    private final RacaProgramaEstudioService programaEstudioServiceHnc;
    private final RacaPlanEstudioService planEstudioServiceHnc;
    private final RacaCicloService cicloServiceHnc;
    private final YmclPeriodoAcademicoService periodoAcademicoServiceHnc;
    private final RacaPlanCursoRepository planCursoRepositoryHnc;
    private final YmclRegistroAcademicoService registroAcademicoServiceHnc;
    private final YmclEstudianteService estudianteServiceHnc;
    private final YmclEstudianteRepository estudianteRepositoryHnc;
    private final YmclTrayectoriaService trayectoriaServiceHnc;
    private final RahpOcrService ocrServiceHnc;
    private final RahpResolutorReporte resolutorHnc;

    @FXML
    private TextField matriculaField;
    @FXML
    private TextField paternoField;
    @FXML
    private TextField maternoField;
    @FXML
    private TextField nombresField;
    @FXML
    private ComboBox<RacaProgramaEstudio> programaCombo;
    @FXML
    private ComboBox<RacaPlanEstudio> planCombo;
    @FXML
    private ComboBox<RacaCiclo> cicloCombo;
    @FXML
    private ComboBox<YmclPeriodoAcademico> periodoCombo;
    @FXML
    private Label estadoEstudianteLabel;
    @FXML
    private ImageView previewImageView;
    @FXML
    private Button reconocerButton;
    @FXML
    private TableView<RahpFilaCurso> tabla;
    @FXML
    private TextArea resultadoArea;

    private final ObservableList<RacaPlanCurso> planCursosDelCicloHnc = FXCollections.observableArrayList();
    private BufferedImage imagenOriginalHnc;
    private int rotacionHnc;
    private Double promedioReporteHnc;
    private Map<String, YmclSituacionAcademica> situacionesPorCodigoHnc = Map.of();

    private Runnable alGuardarHnc;

    public RahpOcrReporteController(RacaProgramaEstudioService programaEstudioServiceHnc, RacaPlanEstudioService planEstudioServiceHnc,
                                 RacaCicloService cicloServiceHnc, YmclPeriodoAcademicoService periodoAcademicoServiceHnc,
                                 RacaPlanCursoRepository planCursoRepositoryHnc, YmclRegistroAcademicoService registroAcademicoServiceHnc,
                                 YmclEstudianteService estudianteServiceHnc, YmclEstudianteRepository estudianteRepositoryHnc,
                                 YmclTrayectoriaService trayectoriaServiceHnc, RahpOcrService ocrServiceHnc, RahpResolutorReporte resolutorHnc) {
        this.programaEstudioServiceHnc = programaEstudioServiceHnc;
        this.planEstudioServiceHnc = planEstudioServiceHnc;
        this.cicloServiceHnc = cicloServiceHnc;
        this.periodoAcademicoServiceHnc = periodoAcademicoServiceHnc;
        this.planCursoRepositoryHnc = planCursoRepositoryHnc;
        this.registroAcademicoServiceHnc = registroAcademicoServiceHnc;
        this.estudianteServiceHnc = estudianteServiceHnc;
        this.estudianteRepositoryHnc = estudianteRepositoryHnc;
        this.trayectoriaServiceHnc = trayectoriaServiceHnc;
        this.ocrServiceHnc = ocrServiceHnc;
        this.resolutorHnc = resolutorHnc;
    }

    @FXML
    public void initialize() {
        programaCombo.setConverter(rahpConvertidor(RacaProgramaEstudio::getNombre));
        planCombo.setConverter(rahpConvertidor(p -> p.getCodigo() + " - " + p.getNombre()));
        cicloCombo.setConverter(rahpConvertidor(c -> "Ciclo " + c.getNumero() + " - " + c.getNombre()));
        periodoCombo.setConverter(rahpConvertidor(YmclPeriodoAcademico::getNombre));

        programaCombo.getItems().setAll(programaEstudioServiceHnc.racaListar());
        periodoCombo.getItems().setAll(periodoAcademicoServiceHnc.ymclListar());
        situacionesPorCodigoHnc = registroAcademicoServiceHnc.ymclListarSituaciones().stream()
                .collect(Collectors.toMap(YmclSituacionAcademica::getCodigo, Function.identity(), (a, b) -> a));

        programaCombo.valueProperty().addListener((obsHnc, antesHnc, programaHnc) -> {
            planCombo.getItems().clear();
            if (programaHnc != null) {
                planCombo.getItems().setAll(planEstudioServiceHnc.racaListarPorPrograma(programaHnc.getId()));
            }
        });
        planCombo.valueProperty().addListener((obsHnc, antesHnc, planHnc) -> {
            cicloCombo.getItems().clear();
            if (planHnc != null) {
                cicloCombo.getItems().setAll(cicloServiceHnc.racaListarPorPlan(planHnc.getId()));
            }
        });
        cicloCombo.valueProperty().addListener((obsHnc, antesHnc, cicloHnc) -> rahpAlCambiarCiclo(cicloHnc));
        matriculaField.textProperty().addListener((obsHnc, antesHnc, textoHnc) -> rahpActualizarEstadoEstudiante());

        rahpConstruirColumnas();
        rahpActualizarEstadoEstudiante();
    }

    private void rahpConstruirColumnas() {
        TableColumn<RahpFilaCurso, String> colDetectadoHnc = new TableColumn<>("Curso en el reporte");
        colDetectadoHnc.setCellValueFactory(d -> d.getValue().cursoDetectado);
        colDetectadoHnc.setPrefWidth(330);

        TableColumn<RahpFilaCurso, RacaPlanCurso> colPlanHnc = new TableColumn<>("Curso del plan (en este ciclo)");
        colPlanHnc.setCellValueFactory(d -> d.getValue().planCurso);
        colPlanHnc.setCellFactory(ComboBoxTableCell.forTableColumn(
                rahpConvertidor(pc -> pc.getCurso().getCodigo() + " - " + pc.getCurso().getNombre()), planCursosDelCicloHnc));
        colPlanHnc.setOnEditCommit(e -> e.getRowValue().planCurso.set(e.getNewValue()));
        colPlanHnc.setPrefWidth(380);

        TableColumn<RahpFilaCurso, String> colNotaHnc = new TableColumn<>("Nota");
        colNotaHnc.setCellValueFactory(d -> d.getValue().nota);
        colNotaHnc.setCellFactory(TextFieldTableCell.forTableColumn());
        colNotaHnc.setOnEditCommit(e -> e.getRowValue().nota.set(e.getNewValue()));
        colNotaHnc.setPrefWidth(70);

        TableColumn<RahpFilaCurso, YmclSituacionAcademica> colSituacionHnc = new TableColumn<>("Situacion (leida / segun la nota)");
        colSituacionHnc.setCellValueFactory(d -> d.getValue().situacion);
        colSituacionHnc.setCellFactory(ComboBoxTableCell.forTableColumn(rahpConvertidor(YmclSituacionAcademica::getNombre),
                FXCollections.observableArrayList(situacionesPorCodigoHnc.values().stream()
                        .sorted(java.util.Comparator.comparing(YmclSituacionAcademica::getNombre)).toList())));
        colSituacionHnc.setOnEditCommit(e -> {
            e.getRowValue().situacionManual = true;
            e.getRowValue().situacion.set(e.getNewValue());
        });
        colSituacionHnc.setPrefWidth(190);

        tabla.getColumns().addAll(colDetectadoHnc, colPlanHnc, colNotaHnc, colSituacionHnc);
    }

    private YmclSituacionAcademica rahpSituacionPorNota(Double notaHnc) {
        return notaHnc == null ? null : situacionesPorCodigoHnc.get(YmclEscalaLiteral.ymclCodigoPara(notaHnc));
    }

    @FXML
    public void rahpOnElegirImagen() {
        FileChooser chooserHnc = new FileChooser();
        chooserHnc.setTitle("Seleccionar reporte de calificaciones");
        chooserHnc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagenes", "*.png", "*.jpg", "*.jpeg"));
        File archivoHnc = chooserHnc.showOpenDialog(tabla.getScene().getWindow());
        if (archivoHnc == null) {
            return;
        }
        try {
            BufferedImage imagenHnc = ImageIO.read(archivoHnc);
            if (imagenHnc == null) {
                rahpMostrarAdvertencia("El archivo seleccionado no es una imagen valida.");
                return;
            }
            imagenOriginalHnc = imagenHnc;
            rotacionHnc = 0;
            rahpMostrarVistaPrevia();
        } catch (java.io.IOException e) {
            rahpMostrarAdvertencia("No se pudo leer la imagen.");
        }
    }

    @FXML
    public void rahpOnRotar() {
        if (imagenOriginalHnc == null) {
            return;
        }
        rotacionHnc = (rotacionHnc + 90) % 360;
        rahpMostrarVistaPrevia();
    }

    private void rahpMostrarVistaPrevia() {
        previewImageView.setImage(SwingFXUtils.toFXImage(ocrServiceHnc.rahpRotar(imagenOriginalHnc, rotacionHnc), null));
    }

    @FXML
    public void rahpOnReconocer() {
        if (imagenOriginalHnc == null) {
            rahpMostrarAdvertencia("Elija primero la imagen del reporte.");
            return;
        }
        reconocerButton.setDisable(true);
        resultadoArea.setText("Reconociendo texto, un momento...");
        BufferedImage imagenHnc = ocrServiceHnc.rahpRotar(imagenOriginalHnc, rotacionHnc);

        Task<RahpReporteDetectado> tareaHnc = new Task<>() {
            @Override
            protected RahpReporteDetectado call() {
                return RahpReconocedorReporteCalificaciones.rahpReconocer(ocrServiceHnc.rahpReconocerPalabrasTabla(imagenHnc));
            }
        };
        tareaHnc.setOnSucceeded(e -> {
            reconocerButton.setDisable(false);
            rahpAplicarReporte(tareaHnc.getValue());
        });
        tareaHnc.setOnFailed(e -> {
            reconocerButton.setDisable(false);
            resultadoArea.clear();
            rahpMostrarAdvertencia(tareaHnc.getException() instanceof RyeSgfaException se
                    ? se.getMessage() : "No se pudo procesar la imagen.");
        });
        new Thread(tareaHnc, "sgfa-ocr-reporte").start();
    }

    public void rahpPrecargar(BufferedImage imagenHnc, RahpReporteDetectado detectadoHnc, Runnable alGuardarHnc) {
        this.imagenOriginalHnc = imagenHnc;
        this.rotacionHnc = 0;
        this.alGuardarHnc = alGuardarHnc;
        rahpMostrarVistaPrevia();
        rahpAplicarReporte(detectadoHnc);
    }

    private void rahpAplicarReporte(RahpReporteDetectado r) {
        RahpPropuesta p = resolutorHnc.rahpProponer(r);
        promedioReporteHnc = r.promedioReporte();
        matriculaField.setText(rahpVacioSiNulo(p.matricula()));
        paternoField.setText(rahpVacioSiNulo(p.apellidoPaterno()));
        maternoField.setText(rahpVacioSiNulo(p.apellidoMaterno()));
        nombresField.setText(rahpVacioSiNulo(p.nombres()));

        tabla.getItems().setAll(r.cursos().stream()
                .map(c -> RahpFilaCurso.desde(c, this::rahpSituacionPorNota, situacionesPorCodigoHnc.get(c.situacion()))).toList());

        if (p.programa() != null) {
            programaCombo.getItems().stream().filter(x -> x.getId().equals(p.programa().getId())).findFirst()
                    .ifPresent(programaCombo::setValue);
        }
        if (p.plan() != null) {
            planCombo.getItems().stream().filter(x -> x.getId().equals(p.plan().getId())).findFirst()
                    .ifPresent(planCombo::setValue);
        }
        if (p.ciclo() != null) {
            cicloCombo.getItems().stream().filter(x -> x.getId().equals(p.ciclo().getId())).findFirst()
                    .ifPresent(cicloCombo::setValue);
        }
        if (p.periodo() != null) {
            periodoCombo.getItems().stream().filter(x -> x.getId().equals(p.periodo().getId())).findFirst()
                    .ifPresent(periodoCombo::setValue);
        }

        StringBuilder textoHnc = new StringBuilder();
        textoHnc.append(r.cursos().size()).append(" curso(s) leidos, ").append(p.rahpCursosEmparejados())
                .append(" emparejado(s) con el plan. Revise y corrija todo antes de guardar.\n");
        if (r.promedioReporte() != null) {
            textoHnc.append("Promedio ponderado impreso en el reporte: ").append(r.promedioReporte()).append('\n');
        }
        p.avisos().forEach(a -> textoHnc.append("  - ").append(a).append('\n'));
        p.notas().forEach(n -> textoHnc.append("  (i) ").append(n).append('\n'));
        resultadoArea.setText(textoHnc.toString());
    }

    private void rahpAlCambiarCiclo(RacaCiclo cicloHnc) {
        planCursosDelCicloHnc.setAll(cicloHnc == null ? List.of() : planCursoRepositoryHnc.findByCicloIdOrderByOrdenAsc(cicloHnc.getId()));
        for (RahpFilaCurso filaHnc : tabla.getItems()) {
            if (filaHnc.cursoDetectado.get().isBlank()) {
                continue;
            }
            filaHnc.planCurso.set(RahpEmparejadorNombres.rahpMejorCoincidencia(filaHnc.cursoDetectado.get(), planCursosDelCicloHnc,
                    pc -> pc.getCurso().getNombre()).orElse(null));
        }
        tabla.refresh();
    }

    private Optional<YmclEstudiante> rahpBuscarEstudiante(String matriculaHnc) {
        if (matriculaHnc == null || matriculaHnc.isBlank()) {
            return Optional.empty();
        }
        return estudianteRepositoryHnc.findByDni(matriculaHnc.trim())
                .or(() -> estudianteRepositoryHnc.findByCodigo(matriculaHnc.trim()));
    }

    private void rahpActualizarEstadoEstudiante() {
        String matriculaHnc = matriculaField.getText();
        if (matriculaHnc == null || matriculaHnc.isBlank()) {
            estadoEstudianteLabel.setText("Indique el N° de matricula del estudiante.");
            return;
        }
        estadoEstudianteLabel.setText(rahpBuscarEstudiante(matriculaHnc)
                .map(e -> "Estudiante ya registrado: " + e.getApellidoPaterno() + " " + rahpVacioSiNulo(e.getApellidoMaterno())
                        + ", " + e.getNombres() + ". Solo se agregaran las notas a su trayectoria.")
                .orElse("Estudiante nuevo: se registrara usando el N° de matricula como DNI y codigo."));
    }

    @FXML
    public void rahpOnAgregarFila() {
        tabla.getItems().add(new RahpFilaCurso("", null, "", this::rahpSituacionPorNota));
    }

    @FXML
    public void rahpOnGuardar() {
        String matriculaHnc = matriculaField.getText() == null ? "" : matriculaField.getText().trim();
        RacaPlanEstudio planHnc = planCombo.getValue();
        RacaCiclo cicloHnc = cicloCombo.getValue();
        YmclPeriodoAcademico periodoHnc = periodoCombo.getValue();
        RacaProgramaEstudio programaHnc = programaCombo.getValue();
        if (matriculaHnc.isBlank() || programaHnc == null || planHnc == null || cicloHnc == null || periodoHnc == null) {
            rahpMostrarAdvertencia("Complete N° de matricula, Programa, Plan, Ciclo y Periodo.");
            return;
        }

        List<YmclItemNota> itemsHnc = new ArrayList<>();
        List<String> omitidosHnc = new ArrayList<>();
        Set<Long> yaUsadosHnc = new HashSet<>();
        for (RahpFilaCurso filaHnc : tabla.getItems()) {
            String etiquetaHnc = filaHnc.cursoDetectado.get().isBlank() ? "(fila sin nombre)" : filaHnc.cursoDetectado.get();
            RacaPlanCurso planCursoHnc = filaHnc.planCurso.get();
            Double notaHnc = filaHnc.rahpNotaComoNumero();
            if (planCursoHnc == null || notaHnc == null) {
                omitidosHnc.add(etiquetaHnc + (planCursoHnc == null ? ": sin curso del plan asignado" : ": sin nota valida"));
                continue;
            }
            if (notaHnc < 0 || notaHnc > 20) {
                rahpMostrarAdvertencia("La nota de \"" + etiquetaHnc + "\" esta fuera del rango 0-20.");
                return;
            }
            if (!yaUsadosHnc.add(planCursoHnc.getId())) {
                rahpMostrarAdvertencia("El curso \"" + planCursoHnc.getCurso().getNombre()
                        + "\" esta asignado a mas de una fila.");
                return;
            }
            YmclSituacionAcademica situacionHnc = filaHnc.situacion.get() != null ? filaHnc.situacion.get() : rahpSituacionPorNota(notaHnc);
            if (situacionHnc == null) {
                rahpMostrarAdvertencia("Falta la escala literal en la base de datos (situaciones DESTACADO/LOGRADO/...).");
                return;
            }
            itemsHnc.add(new YmclItemNota(planCursoHnc.getId(), notaHnc, situacionHnc.getId(), null));
        }
        if (itemsHnc.isEmpty()) {
            rahpMostrarAdvertencia("No hay ninguna nota lista para guardar (cada fila necesita curso del plan y nota).");
            return;
        }

        Optional<YmclEstudiante> existenteHnc = rahpBuscarEstudiante(matriculaHnc);
        String quienHnc = existenteHnc.isPresent()
                ? "al estudiante ya registrado " + existenteHnc.get().getApellidoPaterno()
                : "a un estudiante NUEVO (" + paternoField.getText() + ")";
        Alert confirmarHnc = new Alert(Alert.AlertType.CONFIRMATION, "Se guardaran " + itemsHnc.size() + " nota(s) " + quienHnc
                + " en " + cicloHnc.getNombre() + ", periodo " + periodoHnc.getNombre() + ". ¿Continuar?",
                ButtonType.OK, ButtonType.CANCEL);
        if (confirmarHnc.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            YmclEstudiante estudianteHnc = existenteHnc.orElseGet(this::rahpCrearEstudiante);
            YmclTrayectoria trayectoriaHnc = trayectoriaServiceHnc.ymclListarPorEstudiante(estudianteHnc.getId()).stream()
                    .filter(t -> t.getPlanEstudio().getId().equals(planHnc.getId()))
                    .findFirst()
                    .orElseGet(() -> trayectoriaServiceHnc.ymclIniciar(estudianteHnc.getId(), programaHnc.getId(), planHnc.getId(), null,
                            "Importado desde Reporte de calificaciones (OCR)"));
            registroAcademicoServiceHnc.ymclGuardarMasivo(trayectoriaHnc.getId(), periodoHnc.getId(), itemsHnc);

            StringBuilder textoHnc = new StringBuilder();
            textoHnc.append("Guardado: ").append(itemsHnc.size()).append(" nota(s) para ")
                    .append(estudianteHnc.getApellidoPaterno()).append(' ')
                    .append(rahpVacioSiNulo(estudianteHnc.getApellidoMaterno()))
                    .append(", ").append(estudianteHnc.getNombres()).append(" (")
                    .append(existenteHnc.isPresent() ? "ya registrado" : "registrado ahora").append(").\n");
            YmclResultadoPromedio promedioHnc = registroAcademicoServiceHnc.ymclCalcularPromedio(trayectoriaHnc.getId(), cicloHnc.getId(),
                    periodoHnc.getId());
            if (promedioHnc.promedio() != null) {
                textoHnc.append(String.format("Promedio ponderado calculado: %.2f", promedioHnc.promedio()));
                if (promedioReporteHnc != null) {
                    textoHnc.append(String.format(" | en el reporte: %.2f", promedioReporteHnc));
                }
                textoHnc.append('\n');
            }
            omitidosHnc.forEach(o -> textoHnc.append("  Omitido - ").append(o).append('\n'));
            resultadoArea.setText(textoHnc.toString());
            rahpActualizarEstadoEstudiante();
            if (alGuardarHnc != null) {
                alGuardarHnc.run();
            }
        } catch (RyeSgfaException e) {
            rahpMostrarAdvertencia(e.getMessage());
        }
    }

    private YmclEstudiante rahpCrearEstudiante() {
        String paternoHnc = paternoField.getText() == null ? "" : paternoField.getText().trim();
        String nombresHnc = nombresField.getText() == null ? "" : nombresField.getText().trim();
        if (paternoHnc.isBlank() || nombresHnc.isBlank()) {
            throw new RyeValidacionException("Estudiante nuevo: faltan el apellido paterno o los nombres.");
        }
        String matriculaHnc = matriculaField.getText().trim();
        String maternoHnc = maternoField.getText() == null || maternoField.getText().isBlank()
                ? null : maternoField.getText().trim();
        return estudianteServiceHnc.ymclCrear(matriculaHnc, matriculaHnc, nombresHnc, paternoHnc, maternoHnc, null, null, null, null, null);
    }

    @FXML
    public void rahpOnCerrar() {
        tabla.fireEvent(new RyeCerrarVistaEvent());
    }

    private static String rahpVacioSiNulo(String textoHnc) {
        return textoHnc == null ? "" : textoHnc;
    }

    private <T> StringConverter<T> rahpConvertidor(Function<T, String> textoDeHnc) {
        return new StringConverter<>() {
            @Override
            public String toString(T objetoHnc) {
                return objetoHnc == null ? "" : textoDeHnc.apply(objetoHnc);
            }

            @Override
            public T fromString(String s) {
                return null;
            }
        };
    }

    private void rahpMostrarAdvertencia(String mensajeHnc) {
        new Alert(Alert.AlertType.WARNING, mensajeHnc, ButtonType.OK).showAndWait();
    }

    public static final class RahpFilaCurso {
        final StringProperty cursoDetectado;
        final ObjectProperty<RacaPlanCurso> planCurso;
        final StringProperty nota;
        final ObjectProperty<YmclSituacionAcademica> situacion = new SimpleObjectProperty<>();
        boolean situacionManual;

        RahpFilaCurso(String cursoDetectadoHnc, RacaPlanCurso planCursoHnc, String notaHnc,
                  Function<Double, YmclSituacionAcademica> segunNotaHnc) {
            this.cursoDetectado = new SimpleStringProperty(cursoDetectadoHnc);
            this.planCurso = new SimpleObjectProperty<>(planCursoHnc);
            this.nota = new SimpleStringProperty(notaHnc);
            this.situacion.set(segunNotaHnc.apply(rahpNotaComoNumero()));
            this.nota.addListener((obsHnc, antesHnc, ahoraHnc) -> {
                if (!situacionManual) {
                    situacion.set(segunNotaHnc.apply(rahpNotaComoNumero()));
                }
            });
        }

        static RahpFilaCurso desde(RahpCursoDetectado detectadoHnc, Function<Double, YmclSituacionAcademica> segunNotaHnc,
                               YmclSituacionAcademica leidaHnc) {
            Double n = detectadoHnc.nota();
            String notaHnc = n == null ? "" : (n % 1 == 0 ? String.valueOf(n.intValue()) : String.valueOf(n));
            RahpFilaCurso filaHnc = new RahpFilaCurso(detectadoHnc.nombre(), null, notaHnc, segunNotaHnc);
            if (leidaHnc != null) {
                filaHnc.situacion.set(leidaHnc);
            }
            return filaHnc;
        }

        Double rahpNotaComoNumero() {
            String textoHnc = nota.get();
            if (textoHnc == null || textoHnc.isBlank()) {
                return null;
            }
            try {
                return Double.parseDouble(textoHnc.trim().replace(',', '.'));
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
}
