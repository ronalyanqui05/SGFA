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

import pe.edu.sgfa.common.exception.SgfaException;
import pe.edu.sgfa.common.exception.ValidacionException;
import pe.edu.sgfa.estudiante.domain.Estudiante;
import pe.edu.sgfa.estudiante.repository.EstudianteRepository;
import pe.edu.sgfa.estudiante.service.EstudianteService;
import pe.edu.sgfa.nota.domain.SituacionAcademica;
import pe.edu.sgfa.nota.service.EscalaLiteral;
import pe.edu.sgfa.nota.service.ItemNota;
import pe.edu.sgfa.nota.service.RegistroAcademicoService;
import pe.edu.sgfa.nota.service.ResultadoPromedio;
import pe.edu.sgfa.ocr.service.EmparejadorNombres;
import pe.edu.sgfa.ocr.service.OcrService;
import pe.edu.sgfa.ocr.service.ReconocedorReporteCalificaciones;
import pe.edu.sgfa.ocr.service.ResolutorReporte;
import pe.edu.sgfa.ocr.service.ResolutorReporte.Propuesta;
import pe.edu.sgfa.ocr.service.ReconocedorReporteCalificaciones.CursoDetectado;
import pe.edu.sgfa.ocr.service.ReconocedorReporteCalificaciones.ReporteDetectado;
import pe.edu.sgfa.periodo.domain.PeriodoAcademico;
import pe.edu.sgfa.periodo.service.PeriodoAcademicoService;
import pe.edu.sgfa.programa.domain.Ciclo;
import pe.edu.sgfa.programa.domain.EstadoPlanEstudio;
import pe.edu.sgfa.programa.domain.PlanCurso;
import pe.edu.sgfa.programa.domain.PlanEstudio;
import pe.edu.sgfa.programa.domain.ProgramaEstudio;
import pe.edu.sgfa.programa.repository.PlanCursoRepository;
import pe.edu.sgfa.programa.service.CicloService;
import pe.edu.sgfa.programa.service.PlanEstudioService;
import pe.edu.sgfa.programa.service.ProgramaEstudioService;
import pe.edu.sgfa.shell.CerrarVistaEvent;
import pe.edu.sgfa.trayectoria.domain.Trayectoria;
import pe.edu.sgfa.trayectoria.service.TrayectoriaService;

@Component
@Scope("prototype")
public class OcrReporteController {

    private final ProgramaEstudioService programaEstudioService;
    private final PlanEstudioService planEstudioService;
    private final CicloService cicloService;
    private final PeriodoAcademicoService periodoAcademicoService;
    private final PlanCursoRepository planCursoRepository;
    private final RegistroAcademicoService registroAcademicoService;
    private final EstudianteService estudianteService;
    private final EstudianteRepository estudianteRepository;
    private final TrayectoriaService trayectoriaService;
    private final OcrService ocrService;
    private final ResolutorReporte resolutor;

    @FXML
    private TextField matriculaField;
    @FXML
    private TextField paternoField;
    @FXML
    private TextField maternoField;
    @FXML
    private TextField nombresField;
    @FXML
    private ComboBox<ProgramaEstudio> programaCombo;
    @FXML
    private ComboBox<PlanEstudio> planCombo;
    @FXML
    private ComboBox<Ciclo> cicloCombo;
    @FXML
    private ComboBox<PeriodoAcademico> periodoCombo;
    @FXML
    private Label estadoEstudianteLabel;
    @FXML
    private ImageView previewImageView;
    @FXML
    private Button reconocerButton;
    @FXML
    private TableView<FilaCurso> tabla;
    @FXML
    private TextArea resultadoArea;

    private final ObservableList<PlanCurso> planCursosDelCiclo = FXCollections.observableArrayList();
    private BufferedImage imagenOriginal;
    private int rotacion;
    private Double promedioReporte;
    private Map<String, SituacionAcademica> situacionesPorCodigo = Map.of();

    private Runnable alGuardar;

    public OcrReporteController(ProgramaEstudioService programaEstudioService, PlanEstudioService planEstudioService,
                                 CicloService cicloService, PeriodoAcademicoService periodoAcademicoService,
                                 PlanCursoRepository planCursoRepository, RegistroAcademicoService registroAcademicoService,
                                 EstudianteService estudianteService, EstudianteRepository estudianteRepository,
                                 TrayectoriaService trayectoriaService, OcrService ocrService, ResolutorReporte resolutor) {
        this.programaEstudioService = programaEstudioService;
        this.planEstudioService = planEstudioService;
        this.cicloService = cicloService;
        this.periodoAcademicoService = periodoAcademicoService;
        this.planCursoRepository = planCursoRepository;
        this.registroAcademicoService = registroAcademicoService;
        this.estudianteService = estudianteService;
        this.estudianteRepository = estudianteRepository;
        this.trayectoriaService = trayectoriaService;
        this.ocrService = ocrService;
        this.resolutor = resolutor;
    }

    @FXML
    public void initialize() {
        programaCombo.setConverter(convertidor(ProgramaEstudio::getNombre));
        planCombo.setConverter(convertidor(p -> p.getCodigo() + " - " + p.getNombre()));
        cicloCombo.setConverter(convertidor(c -> "Ciclo " + c.getNumero() + " - " + c.getNombre()));
        periodoCombo.setConverter(convertidor(PeriodoAcademico::getNombre));

        programaCombo.getItems().setAll(programaEstudioService.listar());
        periodoCombo.getItems().setAll(periodoAcademicoService.listar());
        situacionesPorCodigo = registroAcademicoService.listarSituaciones().stream()
                .collect(Collectors.toMap(SituacionAcademica::getCodigo, Function.identity(), (a, b) -> a));

        programaCombo.valueProperty().addListener((obs, antes, programa) -> {
            planCombo.getItems().clear();
            if (programa != null) {
                planCombo.getItems().setAll(planEstudioService.listarPorPrograma(programa.getId()));
            }
        });
        planCombo.valueProperty().addListener((obs, antes, plan) -> {
            cicloCombo.getItems().clear();
            if (plan != null) {
                cicloCombo.getItems().setAll(cicloService.listarPorPlan(plan.getId()));
            }
        });
        cicloCombo.valueProperty().addListener((obs, antes, ciclo) -> alCambiarCiclo(ciclo));
        matriculaField.textProperty().addListener((obs, antes, txt) -> actualizarEstadoEstudiante());

        construirColumnas();
        actualizarEstadoEstudiante();
    }

    private void construirColumnas() {
        TableColumn<FilaCurso, String> colDtctd = new TableColumn<>("Curso en el reporte");
        colDtctd.setCellValueFactory(d -> d.getValue().cursoDetectado);
        colDtctd.setPrefWidth(330);

        TableColumn<FilaCurso, PlanCurso> colPln = new TableColumn<>("Curso del plan (en este ciclo)");
        colPln.setCellValueFactory(d -> d.getValue().planCurso);
        colPln.setCellFactory(ComboBoxTableCell.forTableColumn(
                convertidor(pc -> pc.getCurso().getCodigo() + " - " + pc.getCurso().getNombre()), planCursosDelCiclo));
        colPln.setOnEditCommit(e -> e.getRowValue().planCurso.set(e.getNewValue()));
        colPln.setPrefWidth(380);

        TableColumn<FilaCurso, String> colNt = new TableColumn<>("Nota");
        colNt.setCellValueFactory(d -> d.getValue().nota);
        colNt.setCellFactory(TextFieldTableCell.forTableColumn());
        colNt.setOnEditCommit(e -> e.getRowValue().nota.set(e.getNewValue()));
        colNt.setPrefWidth(70);

        TableColumn<FilaCurso, SituacionAcademica> colStcn = new TableColumn<>("Situacion (leida / segun la nota)");
        colStcn.setCellValueFactory(d -> d.getValue().situacion);
        colStcn.setCellFactory(ComboBoxTableCell.forTableColumn(convertidor(SituacionAcademica::getNombre),
                FXCollections.observableArrayList(situacionesPorCodigo.values().stream()
                        .sorted(java.util.Comparator.comparing(SituacionAcademica::getNombre)).toList())));
        colStcn.setOnEditCommit(e -> {
            e.getRowValue().situacionManual = true;
            e.getRowValue().situacion.set(e.getNewValue());
        });
        colStcn.setPrefWidth(190);

        tabla.getColumns().addAll(colDtctd, colPln, colNt, colStcn);
    }

    private SituacionAcademica situacionPorNota(Double nota) {
        return nota == null ? null : situacionesPorCodigo.get(EscalaLiteral.codigoPara(nota));
    }

    @FXML
    public void onElegirImagen() {
        FileChooser chsr = new FileChooser();
        chsr.setTitle("Seleccionar reporte de calificaciones");
        chsr.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagenes", "*.png", "*.jpg", "*.jpeg"));
        File archv = chsr.showOpenDialog(tabla.getScene().getWindow());
        if (archv == null) {
            return;
        }
        try {
            BufferedImage imgn = ImageIO.read(archv);
            if (imgn == null) {
                mostrarAdvertencia("El archivo seleccionado no es una imagen valida.");
                return;
            }
            imagenOriginal = imgn;
            rotacion = 0;
            mostrarVistaPrevia();
        } catch (java.io.IOException e) {
            mostrarAdvertencia("No se pudo leer la imagen.");
        }
    }

    @FXML
    public void onRotar() {
        if (imagenOriginal == null) {
            return;
        }
        rotacion = (rotacion + 90) % 360;
        mostrarVistaPrevia();
    }

    private void mostrarVistaPrevia() {
        previewImageView.setImage(SwingFXUtils.toFXImage(ocrService.rotar(imagenOriginal, rotacion), null));
    }

    @FXML
    public void onReconocer() {
        if (imagenOriginal == null) {
            mostrarAdvertencia("Elija primero la imagen del reporte.");
            return;
        }
        reconocerButton.setDisable(true);
        resultadoArea.setText("Reconociendo texto, un momento...");
        BufferedImage imgn = ocrService.rotar(imagenOriginal, rotacion);

        Task<ReporteDetectado> tr = new Task<>() {
            @Override
            protected ReporteDetectado call() {
                return ReconocedorReporteCalificaciones.reconocer(ocrService.reconocerPalabrasTabla(imgn));
            }
        };
        tr.setOnSucceeded(e -> {
            reconocerButton.setDisable(false);
            aplicarReporte(tr.getValue());
        });
        tr.setOnFailed(e -> {
            reconocerButton.setDisable(false);
            resultadoArea.clear();
            mostrarAdvertencia(tr.getException() instanceof SgfaException se
                    ? se.getMessage() : "No se pudo procesar la imagen.");
        });
        new Thread(tr, "sgfa-ocr-reporte").start();
    }

    public void precargar(BufferedImage imgn, ReporteDetectado dtctd, Runnable alGuardar) {
        this.imagenOriginal = imgn;
        this.rotacion = 0;
        this.alGuardar = alGuardar;
        mostrarVistaPrevia();
        aplicarReporte(dtctd);
    }

    private void aplicarReporte(ReporteDetectado r) {
        Propuesta p = resolutor.proponer(r);
        promedioReporte = r.promedioReporte();
        matriculaField.setText(vacioSiNulo(p.matricula()));
        paternoField.setText(vacioSiNulo(p.apellidoPaterno()));
        maternoField.setText(vacioSiNulo(p.apellidoMaterno()));
        nombresField.setText(vacioSiNulo(p.nombres()));

        tabla.getItems().setAll(r.cursos().stream()
                .map(c -> FilaCurso.desde(c, this::situacionPorNota, situacionesPorCodigo.get(c.situacion()))).toList());

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

        StringBuilder txt = new StringBuilder();
        txt.append(r.cursos().size()).append(" curso(s) leidos, ").append(p.cursosEmparejados())
                .append(" emparejado(s) con el plan. Revise y corrija todo antes de guardar.\n");
        if (r.promedioReporte() != null) {
            txt.append("Promedio ponderado impreso en el reporte: ").append(r.promedioReporte()).append('\n');
        }
        p.avisos().forEach(a -> txt.append("  - ").append(a).append('\n'));
        p.notas().forEach(n -> txt.append("  (i) ").append(n).append('\n'));
        resultadoArea.setText(txt.toString());
    }

    private void alCambiarCiclo(Ciclo ciclo) {
        planCursosDelCiclo.setAll(ciclo == null ? List.of() : planCursoRepository.findByCicloIdOrderByOrdenAsc(ciclo.getId()));
        for (FilaCurso fl : tabla.getItems()) {
            if (fl.cursoDetectado.get().isBlank()) {
                continue;
            }
            fl.planCurso.set(EmparejadorNombres.mejorCoincidencia(fl.cursoDetectado.get(), planCursosDelCiclo,
                    pc -> pc.getCurso().getNombre()).orElse(null));
        }
        tabla.refresh();
    }

    private Optional<Estudiante> buscarEstudiante(String matricula) {
        if (matricula == null || matricula.isBlank()) {
            return Optional.empty();
        }
        return estudianteRepository.findByDni(matricula.trim())
                .or(() -> estudianteRepository.findByCodigo(matricula.trim()));
    }

    private void actualizarEstadoEstudiante() {
        String matricula = matriculaField.getText();
        if (matricula == null || matricula.isBlank()) {
            estadoEstudianteLabel.setText("Indique el N° de matricula del estudiante.");
            return;
        }
        estadoEstudianteLabel.setText(buscarEstudiante(matricula)
                .map(e -> "Estudiante ya registrado: " + e.getApellidoPaterno() + " " + vacioSiNulo(e.getApellidoMaterno())
                        + ", " + e.getNombres() + ". Solo se agregaran las notas a su trayectoria.")
                .orElse("Estudiante nuevo: se registrara usando el N° de matricula como DNI y codigo."));
    }

    @FXML
    public void onAgregarFila() {
        tabla.getItems().add(new FilaCurso("", null, "", this::situacionPorNota));
    }

    @FXML
    public void onGuardar() {
        String matricula = matriculaField.getText() == null ? "" : matriculaField.getText().trim();
        PlanEstudio plan = planCombo.getValue();
        Ciclo ciclo = cicloCombo.getValue();
        PeriodoAcademico periodo = periodoCombo.getValue();
        ProgramaEstudio programa = programaCombo.getValue();
        if (matricula.isBlank() || programa == null || plan == null || ciclo == null || periodo == null) {
            mostrarAdvertencia("Complete N° de matricula, Programa, Plan, Ciclo y Periodo.");
            return;
        }

        List<ItemNota> itms = new ArrayList<>();
        List<String> omtds = new ArrayList<>();
        Set<Long> yaUsds = new HashSet<>();
        for (FilaCurso fl : tabla.getItems()) {
            String etqt = fl.cursoDetectado.get().isBlank() ? "(fila sin nombre)" : fl.cursoDetectado.get();
            PlanCurso planCurso = fl.planCurso.get();
            Double nota = fl.notaComoNumero();
            if (planCurso == null || nota == null) {
                omtds.add(etqt + (planCurso == null ? ": sin curso del plan asignado" : ": sin nota valida"));
                continue;
            }
            if (nota < 0 || nota > 20) {
                mostrarAdvertencia("La nota de \"" + etqt + "\" esta fuera del rango 0-20.");
                return;
            }
            if (!yaUsds.add(planCurso.getId())) {
                mostrarAdvertencia("El curso \"" + planCurso.getCurso().getNombre()
                        + "\" esta asignado a mas de una fila.");
                return;
            }
            SituacionAcademica situacion = fl.situacion.get() != null ? fl.situacion.get() : situacionPorNota(nota);
            if (situacion == null) {
                mostrarAdvertencia("Falta la escala literal en la base de datos (situaciones DESTACADO/LOGRADO/...).");
                return;
            }
            itms.add(new ItemNota(planCurso.getId(), nota, situacion.getId(), null));
        }
        if (itms.isEmpty()) {
            mostrarAdvertencia("No hay ninguna nota lista para guardar (cada fila necesita curso del plan y nota).");
            return;
        }

        Optional<Estudiante> exstnt = buscarEstudiante(matricula);
        String qn = exstnt.isPresent()
                ? "al estudiante ya registrado " + exstnt.get().getApellidoPaterno()
                : "a un estudiante NUEVO (" + paternoField.getText() + ")";
        Alert cnfrmr = new Alert(Alert.AlertType.CONFIRMATION, "Se guardaran " + itms.size() + " nota(s) " + qn
                + " en " + ciclo.getNombre() + ", periodo " + periodo.getNombre() + ". ¿Continuar?",
                ButtonType.OK, ButtonType.CANCEL);
        if (cnfrmr.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            Estudiante estudiante = exstnt.orElseGet(this::crearEstudiante);
            Trayectoria trayectoria = trayectoriaService.listarPorEstudiante(estudiante.getId()).stream()
                    .filter(t -> t.getPlanEstudio().getId().equals(plan.getId()))
                    .findFirst()
                    .orElseGet(() -> trayectoriaService.iniciar(estudiante.getId(), programa.getId(), plan.getId(), null,
                            "Importado desde Reporte de calificaciones (OCR)"));
            registroAcademicoService.guardarMasivo(trayectoria.getId(), periodo.getId(), itms);

            StringBuilder txt = new StringBuilder();
            txt.append("Guardado: ").append(itms.size()).append(" nota(s) para ")
                    .append(estudiante.getApellidoPaterno()).append(' ')
                    .append(vacioSiNulo(estudiante.getApellidoMaterno()))
                    .append(", ").append(estudiante.getNombres()).append(" (")
                    .append(exstnt.isPresent() ? "ya registrado" : "registrado ahora").append(").\n");
            ResultadoPromedio promedio = registroAcademicoService.calcularPromedio(trayectoria.getId(), ciclo.getId(),
                    periodo.getId());
            if (promedio.promedio() != null) {
                txt.append(String.format("Promedio ponderado calculado: %.2f", promedio.promedio()));
                if (promedioReporte != null) {
                    txt.append(String.format(" | en el reporte: %.2f", promedioReporte));
                }
                txt.append('\n');
            }
            omtds.forEach(o -> txt.append("  Omitido - ").append(o).append('\n'));
            resultadoArea.setText(txt.toString());
            actualizarEstadoEstudiante();
            if (alGuardar != null) {
                alGuardar.run();
            }
        } catch (SgfaException e) {
            mostrarAdvertencia(e.getMessage());
        }
    }

    private Estudiante crearEstudiante() {
        String ptrn = paternoField.getText() == null ? "" : paternoField.getText().trim();
        String nombres = nombresField.getText() == null ? "" : nombresField.getText().trim();
        if (ptrn.isBlank() || nombres.isBlank()) {
            throw new ValidacionException("Estudiante nuevo: faltan el apellido paterno o los nombres.");
        }
        String matricula = matriculaField.getText().trim();
        String mtrn = maternoField.getText() == null || maternoField.getText().isBlank()
                ? null : maternoField.getText().trim();
        return estudianteService.crear(matricula, matricula, nombres, ptrn, mtrn, null, null, null, null, null);
    }

    @FXML
    public void onCerrar() {
        tabla.fireEvent(new CerrarVistaEvent());
    }

    private static String vacioSiNulo(String txt) {
        return txt == null ? "" : txt;
    }

    private <T> StringConverter<T> convertidor(Function<T, String> txtDe) {
        return new StringConverter<>() {
            @Override
            public String toString(T objt) {
                return objt == null ? "" : txtDe.apply(objt);
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

    public static final class FilaCurso {
        final StringProperty cursoDetectado;
        final ObjectProperty<PlanCurso> planCurso;
        final StringProperty nota;
        final ObjectProperty<SituacionAcademica> situacion = new SimpleObjectProperty<>();
        boolean situacionManual;

        FilaCurso(String cursoDetectado, PlanCurso planCurso, String nota,
                  Function<Double, SituacionAcademica> sgnNt) {
            this.cursoDetectado = new SimpleStringProperty(cursoDetectado);
            this.planCurso = new SimpleObjectProperty<>(planCurso);
            this.nota = new SimpleStringProperty(nota);
            this.situacion.set(sgnNt.apply(notaComoNumero()));
            this.nota.addListener((obs, antes, ahora) -> {
                if (!situacionManual) {
                    situacion.set(sgnNt.apply(notaComoNumero()));
                }
            });
        }

        static FilaCurso desde(CursoDetectado dtctd, Function<Double, SituacionAcademica> sgnNt,
                               SituacionAcademica ld) {
            Double n = dtctd.nota();
            String nota = n == null ? "" : (n % 1 == 0 ? String.valueOf(n.intValue()) : String.valueOf(n));
            FilaCurso fl = new FilaCurso(dtctd.nombre(), null, nota, sgnNt);
            if (ld != null) {
                fl.situacion.set(ld);
            }
            return fl;
        }

        Double notaComoNumero() {
            String txt = nota.get();
            if (txt == null || txt.isBlank()) {
                return null;
            }
            try {
                return Double.parseDouble(txt.trim().replace(',', '.'));
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
}
