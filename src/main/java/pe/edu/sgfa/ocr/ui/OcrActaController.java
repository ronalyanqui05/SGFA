package pe.edu.sgfa.ocr.ui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.imageio.ImageIO;

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
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.springframework.stereotype.Component;

import pe.edu.sgfa.common.exception.SgfaException;
import pe.edu.sgfa.common.exception.ValidacionException;
import pe.edu.sgfa.estudiante.domain.Estudiante;
import pe.edu.sgfa.estudiante.repository.EstudianteRepository;
import pe.edu.sgfa.estudiante.service.EstudianteService;
import pe.edu.sgfa.nota.domain.SituacionAcademica;
import pe.edu.sgfa.nota.service.RegistroAcademicoService;
import pe.edu.sgfa.ocr.service.OcrService;
import pe.edu.sgfa.ocr.service.PalabraReconocida;
import pe.edu.sgfa.ocr.service.ReconocedorActa;
import pe.edu.sgfa.periodo.domain.PeriodoAcademico;
import pe.edu.sgfa.periodo.service.PeriodoAcademicoService;
import pe.edu.sgfa.programa.domain.Ciclo;
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
public class OcrActaController {

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

    @FXML
    private ComboBox<ProgramaEstudio> programaCombo;
    @FXML
    private ComboBox<PlanEstudio> planCombo;
    @FXML
    private ComboBox<Ciclo> cicloCombo;
    @FXML
    private ComboBox<PeriodoAcademico> periodoCombo;
    @FXML
    private HBox mapeoCursosBox;
    @FXML
    private ImageView previewImageView;
    @FXML
    private Label paginaLabel;
    @FXML
    private Button reconocerButton;
    @FXML
    private TableView<FilaActaUI> tabla;
    @FXML
    private TextArea resultadoArea;

    private final List<ComboBox<PlanCurso>> combosMapeoCursos = new ArrayList<>();
    private final List<BufferedImage> paginasCargadas = new ArrayList<>();
    private int paginaActual = -1;
    private int rotacionActual;
    private Map<String, SituacionAcademica> situacionesPorCodigo = Map.of();

    public OcrActaController(ProgramaEstudioService programaEstudioService, PlanEstudioService planEstudioService,
                              CicloService cicloService, PeriodoAcademicoService periodoAcademicoService,
                              PlanCursoRepository planCursoRepository, RegistroAcademicoService registroAcademicoService,
                              EstudianteService estudianteService, EstudianteRepository estudianteRepository,
                              TrayectoriaService trayectoriaService, OcrService ocrService) {
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
    }

    @FXML
    public void initialize() {
        programaCombo.setConverter(convertidor(ProgramaEstudio::getNombre));
        planCombo.setConverter(convertidor(PlanEstudio::getCodigo));
        cicloCombo.setConverter(convertidor(c -> "Ciclo " + c.getNumero() + " - " + c.getNombre()));
        periodoCombo.setConverter(convertidor(PeriodoAcademico::getNombre));

        programaCombo.getItems().setAll(programaEstudioService.listar());
        periodoCombo.getItems().setAll(periodoAcademicoService.listar());

        programaCombo.valueProperty().addListener((obs, antes, programa) -> {
            planCombo.getItems().clear();
            cicloCombo.getItems().clear();
            if (programa != null) {
                planCombo.getItems().setAll(planEstudioService.listarPorPrograma(programa.getId()));
            }
        });
        planCombo.valueProperty().addListener((obs, antes, pln) -> {
            cicloCombo.getItems().clear();
            if (pln != null) {
                cicloCombo.getItems().setAll(cicloService.listarPorPlan(pln.getId()));
            }
        });
        cicloCombo.valueProperty().addListener((obs, antes, ccl) -> reconstruirMapeoYColumnas(ccl));

        situacionesPorCodigo = registroAcademicoService.listarSituaciones().stream()
                .collect(java.util.stream.Collectors.toMap(SituacionAcademica::getCodigo,
                        java.util.function.Function.identity(), (a, b) -> a));
    }

    private SituacionAcademica situacionDeTexto(String txt) {
        if (txt.strip().equalsIgnoreCase("T")) {
            return situacionesPorCodigo.get("RETIRADO");
        }
        String cdg = pe.edu.sgfa.ocr.service.LectorSituacion.codigoDe(txt);
        return cdg == null ? null : situacionesPorCodigo.get(cdg);
    }

    private void reconstruirMapeoYColumnas(Ciclo ccl) {
        mapeoCursosBox.getChildren().clear();
        combosMapeoCursos.clear();
        tabla.getColumns().clear();
        tabla.getItems().clear();

        if (ccl == null) {
            return;
        }
        List<PlanCurso> plnCrss = planCursoRepository.findByCicloIdOrderByOrdenAsc(ccl.getId());
        if (plnCrss.isEmpty()) {
            return;
        }

        agregarColumnaTexto("N° Orden", FilaActaUI::ordenNroProperty, 60);
        agregarColumnaTexto("N° Matricula", FilaActaUI::matriculaProperty, 90);
        agregarColumnaTexto("Apellido paterno", FilaActaUI::apellidoPaternoProperty, 110);
        agregarColumnaTexto("Apellido materno", FilaActaUI::apellidoMaternoProperty, 110);
        agregarColumnaTexto("Nombres", FilaActaUI::nombresProperty, 130);

        for (int i = 0; i < plnCrss.size(); i++) {
            int indc = i;
            ComboBox<PlanCurso> mp = new ComboBox<>();
            mp.setConverter(convertidor(pc -> pc.getCurso().getCodigo()));
            mp.getItems().setAll(plnCrss);
            mp.getSelectionModel().select(i);
            mp.setPromptText("Columna " + (i + 1));
            combosMapeoCursos.add(mp);
            mapeoCursosBox.getChildren().add(
                    new javafx.scene.layout.VBox(2, new Label("Col. " + (i + 1) + ":"), mp));

            TableColumn<FilaActaUI, SituacionAcademica> colStcn =
                    new TableColumn<>(plnCrss.get(i).getCurso().getCodigo() + " (sit.)");
            colStcn.setCellValueFactory(d -> d.getValue().situacionProperty(indc));
            colStcn.setCellFactory(ComboBoxTableCell.forTableColumn(
                    convertidor(SituacionAcademica::getCodigo),
                    javafx.collections.FXCollections.observableArrayList(registroAcademicoService.listarSituaciones())));
            colStcn.setOnEditCommit(e -> e.getRowValue().situacionProperty(indc).set(e.getNewValue()));
            colStcn.setPrefWidth(90);

            TableColumn<FilaActaUI, String> colNt = new TableColumn<>(plnCrss.get(i).getCurso().getCodigo() + " (nota)");
            colNt.setCellValueFactory(d -> d.getValue().notaProperty(indc));
            colNt.setCellFactory(TextFieldTableCell.forTableColumn());
            colNt.setOnEditCommit(e -> e.getRowValue().notaProperty(indc).set(e.getNewValue()));
            colNt.setPrefWidth(60);

            tabla.getColumns().addAll(colStcn, colNt);
        }
    }

    private void agregarColumnaTexto(String ttl, java.util.function.Function<FilaActaUI, javafx.beans.property.StringProperty> propiedad,
                                      double anch) {
        TableColumn<FilaActaUI, String> clmn = new TableColumn<>(ttl);
        clmn.setCellValueFactory(d -> propiedad.apply(d.getValue()));
        clmn.setCellFactory(TextFieldTableCell.forTableColumn());
        clmn.setOnEditCommit(e -> propiedad.apply(e.getRowValue()).set(e.getNewValue()));
        clmn.setPrefWidth(anch);
        tabla.getColumns().add(clmn);
    }

    @FXML
    public void onAgregarPagina() {
        FileChooser chsr = new FileChooser();
        chsr.setTitle("Seleccionar pagina del acta");
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
            paginasCargadas.add(imgn);
            paginaActual = paginasCargadas.size() - 1;
            rotacionActual = 0;
            mostrarVistaPrevia();
        } catch (java.io.IOException e) {
            mostrarAdvertencia("No se pudo leer la imagen.");
        }
    }

    @FXML
    public void onRotar() {
        if (paginaActual < 0) {
            return;
        }
        rotacionActual = (rotacionActual + 90) % 360;
        mostrarVistaPrevia();
    }

    @FXML
    public void onPaginaAnterior() {
        if (paginaActual > 0) {
            paginaActual--;
            rotacionActual = 0;
            mostrarVistaPrevia();
        }
    }

    @FXML
    public void onPaginaSiguiente() {
        if (paginaActual < paginasCargadas.size() - 1) {
            paginaActual++;
            rotacionActual = 0;
            mostrarVistaPrevia();
        }
    }

    private void mostrarVistaPrevia() {
        BufferedImage rtd = ocrService.rotar(paginasCargadas.get(paginaActual), rotacionActual);
        previewImageView.setImage(SwingFXUtils.toFXImage(rtd, null));
        paginaLabel.setText("Pagina " + (paginaActual + 1) + " de " + paginasCargadas.size());
    }

    @FXML
    public void onReconocer() {
        if (paginasCargadas.isEmpty()) {
            mostrarAdvertencia("Agregue al menos una pagina del acta.");
            return;
        }
        if (combosMapeoCursos.isEmpty()) {
            mostrarAdvertencia("Seleccione Programa, Plan y Ciclo primero (para saber que cursos tiene el acta).");
            return;
        }

        reconocerButton.setDisable(true);
        resultadoArea.setText("Reconociendo texto, un momento...");
        int cntddCrss = combosMapeoCursos.size();

        List<BufferedImage> pgnsFnls = List.copyOf(paginasCargadas);

        Task<List<FilaActaUI>> tr = new Task<>() {
            @Override
            protected List<FilaActaUI> call() {
                List<FilaActaUI> fls = new ArrayList<>();
                for (BufferedImage pgn : pgnsFnls) {
                    List<PalabraReconocida> plbrs = ocrService.reconocerPalabras(pgn);
                    List<ReconocedorActa.FilaActaDetectada> dtctds = ReconocedorActa.agrupar(plbrs, cntddCrss);
                    for (var dtctd : dtctds) {
                        fls.add(FilaActaUI.desde(dtctd, OcrActaController.this::situacionDeTexto));
                    }
                }
                return fls;
            }
        };
        tr.setOnSucceeded(e -> {
            tabla.getItems().setAll(tr.getValue());
            reconocerButton.setDisable(false);
            resultadoArea.setText(tr.getValue().size() + " fila(s) detectada(s). Revise y corrija cada "
                    + "celda antes de guardar -- el OCR de esta tabla es un punto de partida, no un resultado final.");
        });
        tr.setOnFailed(e -> {
            reconocerButton.setDisable(false);
            resultadoArea.clear();
            String mnsj = tr.getException() instanceof SgfaException se
                    ? se.getMessage() : "No se pudo procesar las imagenes.";
            mostrarAdvertencia(mnsj);
        });
        new Thread(tr, "sgfa-ocr-acta").start();
    }

    @FXML
    public void onAgregarFila() {
        if (combosMapeoCursos.isEmpty()) {
            mostrarAdvertencia("Seleccione Programa, Plan y Ciclo primero.");
            return;
        }
        tabla.getItems().add(new FilaActaUI(combosMapeoCursos.size()));
    }

    @FXML
    public void onGuardar() {
        ProgramaEstudio programa = programaCombo.getValue();
        PlanEstudio pln = planCombo.getValue();
        PeriodoAcademico periodo = periodoCombo.getValue();
        if (programa == null || pln == null || periodo == null || combosMapeoCursos.isEmpty()) {
            mostrarAdvertencia("Seleccione Programa, Plan, Ciclo y Periodo.");
            return;
        }
        List<Long> plnCrsIdPorClmn = combosMapeoCursos.stream()
                .map(c -> c.getValue() == null ? null : c.getValue().getId())
                .toList();
        if (plnCrsIdPorClmn.contains(null)) {
            mostrarAdvertencia("Falta indicar el curso de alguna columna.");
            return;
        }

        int grdds = 0;
        List<String> errrs = new ArrayList<>();
        for (FilaActaUI fl : tabla.getItems()) {
            if (!fl.tieneDatos()) {
                continue;
            }
            String dscrptr = fl.matriculaProperty().get() + " " + fl.apellidoPaternoProperty().get();
            try {
                Estudiante estudiante = resolverEstudiante(fl);
                Trayectoria trayectoria = resolverTrayectoria(estudiante, programa, pln);
                List<pe.edu.sgfa.nota.service.ItemNota> itms = fl.aItems(plnCrsIdPorClmn);
                registroAcademicoService.guardarMasivo(trayectoria.getId(), periodo.getId(), itms);
                grdds++;
            } catch (SgfaException e) {
                errrs.add(dscrptr + ": " + e.getMessage());
            }
        }

        StringBuilder txt = new StringBuilder();
        txt.append("Guardadas: ").append(grdds).append(" | Con error: ").append(errrs.size()).append('\n');
        for (String errr : errrs) {
            txt.append("  ").append(errr).append('\n');
        }
        resultadoArea.setText(txt.toString());
    }

    private Estudiante resolverEstudiante(FilaActaUI fl) {
        String mtrcl = fl.matriculaProperty().get();
        if (mtrcl == null || mtrcl.isBlank()) {
            throw new ValidacionException("Falta el N° de matricula (se usa como DNI y codigo del estudiante).");
        }
        Optional<Estudiante> exstnt = estudianteRepository.findByDni(mtrcl)
                .or(() -> estudianteRepository.findByCodigo(mtrcl));
        if (exstnt.isPresent()) {
            return exstnt.get();
        }
        String aplldPtrn = fl.apellidoPaternoProperty().get();
        if (aplldPtrn == null || aplldPtrn.isBlank()) {
            throw new ValidacionException("Falta el apellido paterno (estudiante nuevo).");
        }
        String nmbrs = fl.nombresProperty().get();
        return estudianteService.crear(mtrcl, mtrcl,
                nmbrs == null || nmbrs.isBlank() ? aplldPtrn : nmbrs, aplldPtrn,
                fl.apellidoMaternoProperty().get(), null, null, null, null, null);
    }

    private Trayectoria resolverTrayectoria(Estudiante estudiante, ProgramaEstudio programa, PlanEstudio pln) {
        return trayectoriaService.listarPorEstudiante(estudiante.getId()).stream()
                .filter(t -> t.getPlanEstudio().getId().equals(pln.getId()))
                .findFirst()
                .orElseGet(() -> trayectoriaService.iniciar(estudiante.getId(), programa.getId(), pln.getId(), null,
                        "Importado desde Acta Consolidada (OCR)"));
    }

    @FXML
    public void onCerrar() {
        tabla.fireEvent(new CerrarVistaEvent());
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
