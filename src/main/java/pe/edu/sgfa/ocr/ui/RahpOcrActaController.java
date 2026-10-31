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

import pe.edu.sgfa.common.exception.RyeSgfaException;
import pe.edu.sgfa.common.exception.RyeValidacionException;
import pe.edu.sgfa.estudiante.domain.YmclEstudiante;
import pe.edu.sgfa.estudiante.repository.YmclEstudianteRepository;
import pe.edu.sgfa.estudiante.service.YmclEstudianteService;
import pe.edu.sgfa.nota.domain.YmclSituacionAcademica;
import pe.edu.sgfa.nota.service.YmclRegistroAcademicoService;
import pe.edu.sgfa.ocr.service.RahpOcrService;
import pe.edu.sgfa.ocr.service.RahpPalabraReconocida;
import pe.edu.sgfa.ocr.service.RahpReconocedorActa;
import pe.edu.sgfa.periodo.domain.YmclPeriodoAcademico;
import pe.edu.sgfa.periodo.service.YmclPeriodoAcademicoService;
import pe.edu.sgfa.programa.domain.RacaCiclo;
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
public class RahpOcrActaController {

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

    @FXML
    private ComboBox<RacaProgramaEstudio> programaCombo;
    @FXML
    private ComboBox<RacaPlanEstudio> planCombo;
    @FXML
    private ComboBox<RacaCiclo> cicloCombo;
    @FXML
    private ComboBox<YmclPeriodoAcademico> periodoCombo;
    @FXML
    private HBox mapeoCursosBox;
    @FXML
    private ImageView previewImageView;
    @FXML
    private Label paginaLabel;
    @FXML
    private Button reconocerButton;
    @FXML
    private TableView<RahpFilaActaUI> tabla;
    @FXML
    private TextArea resultadoArea;

    private final List<ComboBox<RacaPlanCurso>> combosMapeoCursosHnc = new ArrayList<>();
    private final List<BufferedImage> paginasCargadasHnc = new ArrayList<>();
    private int paginaActualHnc = -1;
    private int rotacionActualHnc;
    private Map<String, YmclSituacionAcademica> situacionesPorCodigoHnc = Map.of();

    public RahpOcrActaController(RacaProgramaEstudioService programaEstudioServiceHnc, RacaPlanEstudioService planEstudioServiceHnc,
                              RacaCicloService cicloServiceHnc, YmclPeriodoAcademicoService periodoAcademicoServiceHnc,
                              RacaPlanCursoRepository planCursoRepositoryHnc, YmclRegistroAcademicoService registroAcademicoServiceHnc,
                              YmclEstudianteService estudianteServiceHnc, YmclEstudianteRepository estudianteRepositoryHnc,
                              YmclTrayectoriaService trayectoriaServiceHnc, RahpOcrService ocrServiceHnc) {
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
    }

    @FXML
    public void initialize() {
        programaCombo.setConverter(rahpConvertidor(RacaProgramaEstudio::getNombre));
        planCombo.setConverter(rahpConvertidor(RacaPlanEstudio::getCodigo));
        cicloCombo.setConverter(rahpConvertidor(c -> "Ciclo " + c.getNumero() + " - " + c.getNombre()));
        periodoCombo.setConverter(rahpConvertidor(YmclPeriodoAcademico::getNombre));

        programaCombo.getItems().setAll(programaEstudioServiceHnc.racaListar());
        periodoCombo.getItems().setAll(periodoAcademicoServiceHnc.ymclListar());

        programaCombo.valueProperty().addListener((obsHnc, antesHnc, programaHnc) -> {
            planCombo.getItems().clear();
            cicloCombo.getItems().clear();
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
        cicloCombo.valueProperty().addListener((obsHnc, antesHnc, cicloHnc) -> rahpReconstruirMapeoYColumnas(cicloHnc));

        situacionesPorCodigoHnc = registroAcademicoServiceHnc.ymclListarSituaciones().stream()
                .collect(java.util.stream.Collectors.toMap(YmclSituacionAcademica::getCodigo,
                        java.util.function.Function.identity(), (a, b) -> a));
    }

    private YmclSituacionAcademica rahpSituacionDeTexto(String textoHnc) {
        if (textoHnc.strip().equalsIgnoreCase("T")) {
            return situacionesPorCodigoHnc.get("RETIRADO");
        }
        String codigoHnc = pe.edu.sgfa.ocr.service.RahpLectorSituacion.rahpCodigoDe(textoHnc);
        return codigoHnc == null ? null : situacionesPorCodigoHnc.get(codigoHnc);
    }

    private void rahpReconstruirMapeoYColumnas(RacaCiclo cicloHnc) {
        mapeoCursosBox.getChildren().clear();
        combosMapeoCursosHnc.clear();
        tabla.getColumns().clear();
        tabla.getItems().clear();

        if (cicloHnc == null) {
            return;
        }
        List<RacaPlanCurso> planCursosHnc = planCursoRepositoryHnc.findByCicloIdOrderByOrdenAsc(cicloHnc.getId());
        if (planCursosHnc.isEmpty()) {
            return;
        }

        rahpAgregarColumnaTexto("N° Orden", RahpFilaActaUI::ordenNroProperty, 60);
        rahpAgregarColumnaTexto("N° Matricula", RahpFilaActaUI::matriculaProperty, 90);
        rahpAgregarColumnaTexto("Apellido paterno", RahpFilaActaUI::apellidoPaternoProperty, 110);
        rahpAgregarColumnaTexto("Apellido materno", RahpFilaActaUI::apellidoMaternoProperty, 110);
        rahpAgregarColumnaTexto("Nombres", RahpFilaActaUI::nombresProperty, 130);

        for (int i = 0; i < planCursosHnc.size(); i++) {
            int indiceHnc = i;
            ComboBox<RacaPlanCurso> mapeoHnc = new ComboBox<>();
            mapeoHnc.setConverter(rahpConvertidor(pc -> pc.getCurso().getCodigo()));
            mapeoHnc.getItems().setAll(planCursosHnc);
            mapeoHnc.getSelectionModel().select(i);
            mapeoHnc.setPromptText("Columna " + (i + 1));
            combosMapeoCursosHnc.add(mapeoHnc);
            mapeoCursosBox.getChildren().add(
                    new javafx.scene.layout.VBox(2, new Label("Col. " + (i + 1) + ":"), mapeoHnc));

            TableColumn<RahpFilaActaUI, YmclSituacionAcademica> colSituacionHnc =
                    new TableColumn<>(planCursosHnc.get(i).getCurso().getCodigo() + " (sit.)");
            colSituacionHnc.setCellValueFactory(d -> d.getValue().situacionProperty(indiceHnc));
            colSituacionHnc.setCellFactory(ComboBoxTableCell.forTableColumn(
                    rahpConvertidor(YmclSituacionAcademica::getCodigo),
                    javafx.collections.FXCollections.observableArrayList(registroAcademicoServiceHnc.ymclListarSituaciones())));
            colSituacionHnc.setOnEditCommit(e -> e.getRowValue().situacionProperty(indiceHnc).set(e.getNewValue()));
            colSituacionHnc.setPrefWidth(90);

            TableColumn<RahpFilaActaUI, String> colNotaHnc = new TableColumn<>(planCursosHnc.get(i).getCurso().getCodigo() + " (nota)");
            colNotaHnc.setCellValueFactory(d -> d.getValue().notaProperty(indiceHnc));
            colNotaHnc.setCellFactory(TextFieldTableCell.forTableColumn());
            colNotaHnc.setOnEditCommit(e -> e.getRowValue().notaProperty(indiceHnc).set(e.getNewValue()));
            colNotaHnc.setPrefWidth(60);

            tabla.getColumns().addAll(colSituacionHnc, colNotaHnc);
        }
    }

    private void rahpAgregarColumnaTexto(String tituloHnc, java.util.function.Function<RahpFilaActaUI, javafx.beans.property.StringProperty> propiedadHnc,
                                      double anchoHnc) {
        TableColumn<RahpFilaActaUI, String> columnaHnc = new TableColumn<>(tituloHnc);
        columnaHnc.setCellValueFactory(d -> propiedadHnc.apply(d.getValue()));
        columnaHnc.setCellFactory(TextFieldTableCell.forTableColumn());
        columnaHnc.setOnEditCommit(e -> propiedadHnc.apply(e.getRowValue()).set(e.getNewValue()));
        columnaHnc.setPrefWidth(anchoHnc);
        tabla.getColumns().add(columnaHnc);
    }

    @FXML
    public void rahpOnAgregarPagina() {
        FileChooser chooserHnc = new FileChooser();
        chooserHnc.setTitle("Seleccionar pagina del acta");
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
            paginasCargadasHnc.add(imagenHnc);
            paginaActualHnc = paginasCargadasHnc.size() - 1;
            rotacionActualHnc = 0;
            rahpMostrarVistaPrevia();
        } catch (java.io.IOException e) {
            rahpMostrarAdvertencia("No se pudo leer la imagen.");
        }
    }

    @FXML
    public void rahpOnRotar() {
        if (paginaActualHnc < 0) {
            return;
        }
        rotacionActualHnc = (rotacionActualHnc + 90) % 360;
        rahpMostrarVistaPrevia();
    }

    @FXML
    public void rahpOnPaginaAnterior() {
        if (paginaActualHnc > 0) {
            paginaActualHnc--;
            rotacionActualHnc = 0;
            rahpMostrarVistaPrevia();
        }
    }

    @FXML
    public void rahpOnPaginaSiguiente() {
        if (paginaActualHnc < paginasCargadasHnc.size() - 1) {
            paginaActualHnc++;
            rotacionActualHnc = 0;
            rahpMostrarVistaPrevia();
        }
    }

    private void rahpMostrarVistaPrevia() {
        BufferedImage rotadaHnc = ocrServiceHnc.rahpRotar(paginasCargadasHnc.get(paginaActualHnc), rotacionActualHnc);
        previewImageView.setImage(SwingFXUtils.toFXImage(rotadaHnc, null));
        paginaLabel.setText("Pagina " + (paginaActualHnc + 1) + " de " + paginasCargadasHnc.size());
    }

    @FXML
    public void rahpOnReconocer() {
        if (paginasCargadasHnc.isEmpty()) {
            rahpMostrarAdvertencia("Agregue al menos una pagina del acta.");
            return;
        }
        if (combosMapeoCursosHnc.isEmpty()) {
            rahpMostrarAdvertencia("Seleccione Programa, Plan y Ciclo primero (para saber que cursos tiene el acta).");
            return;
        }

        reconocerButton.setDisable(true);
        resultadoArea.setText("Reconociendo texto, un momento...");
        int cantidadCursosHnc = combosMapeoCursosHnc.size();

        List<BufferedImage> paginasFinalesHnc = List.copyOf(paginasCargadasHnc);

        Task<List<RahpFilaActaUI>> tareaHnc = new Task<>() {
            @Override
            protected List<RahpFilaActaUI> call() {
                List<RahpFilaActaUI> filasHnc = new ArrayList<>();
                for (BufferedImage paginaHnc : paginasFinalesHnc) {
                    List<RahpPalabraReconocida> palabrasHnc = ocrServiceHnc.rahpReconocerPalabras(paginaHnc);
                    List<RahpReconocedorActa.RahpFilaActaDetectada> detectadasHnc = RahpReconocedorActa.rahpAgrupar(palabrasHnc, cantidadCursosHnc);
                    for (var detectadaHnc : detectadasHnc) {
                        filasHnc.add(RahpFilaActaUI.desde(detectadaHnc, RahpOcrActaController.this::rahpSituacionDeTexto));
                    }
                }
                return filasHnc;
            }
        };
        tareaHnc.setOnSucceeded(e -> {
            tabla.getItems().setAll(tareaHnc.getValue());
            reconocerButton.setDisable(false);
            resultadoArea.setText(tareaHnc.getValue().size() + " fila(s) detectada(s). Revise y corrija cada "
                    + "celda antes de guardar -- el OCR de esta tabla es un punto de partida, no un resultado final.");
        });
        tareaHnc.setOnFailed(e -> {
            reconocerButton.setDisable(false);
            resultadoArea.clear();
            String mensajeHnc = tareaHnc.getException() instanceof RyeSgfaException se
                    ? se.getMessage() : "No se pudo procesar las imagenes.";
            rahpMostrarAdvertencia(mensajeHnc);
        });
        new Thread(tareaHnc, "sgfa-ocr-acta").start();
    }

    @FXML
    public void rahpOnAgregarFila() {
        if (combosMapeoCursosHnc.isEmpty()) {
            rahpMostrarAdvertencia("Seleccione Programa, Plan y Ciclo primero.");
            return;
        }
        tabla.getItems().add(new RahpFilaActaUI(combosMapeoCursosHnc.size()));
    }

    @FXML
    public void rahpOnGuardar() {
        RacaProgramaEstudio programaHnc = programaCombo.getValue();
        RacaPlanEstudio planHnc = planCombo.getValue();
        YmclPeriodoAcademico periodoHnc = periodoCombo.getValue();
        if (programaHnc == null || planHnc == null || periodoHnc == null || combosMapeoCursosHnc.isEmpty()) {
            rahpMostrarAdvertencia("Seleccione Programa, Plan, Ciclo y Periodo.");
            return;
        }
        List<Long> planCursoIdPorColumnaHnc = combosMapeoCursosHnc.stream()
                .map(c -> c.getValue() == null ? null : c.getValue().getId())
                .toList();
        if (planCursoIdPorColumnaHnc.contains(null)) {
            rahpMostrarAdvertencia("Falta indicar el curso de alguna columna.");
            return;
        }

        int guardadasHnc = 0;
        List<String> erroresHnc = new ArrayList<>();
        for (RahpFilaActaUI filaHnc : tabla.getItems()) {
            if (!filaHnc.rahpTieneDatos()) {
                continue;
            }
            String descriptorHnc = filaHnc.matriculaProperty().get() + " " + filaHnc.apellidoPaternoProperty().get();
            try {
                YmclEstudiante estudianteHnc = rahpResolverEstudiante(filaHnc);
                YmclTrayectoria trayectoriaHnc = rahpResolverTrayectoria(estudianteHnc, programaHnc, planHnc);
                List<pe.edu.sgfa.nota.service.YmclItemNota> itemsHnc = filaHnc.rahpAItems(planCursoIdPorColumnaHnc);
                registroAcademicoServiceHnc.ymclGuardarMasivo(trayectoriaHnc.getId(), periodoHnc.getId(), itemsHnc);
                guardadasHnc++;
            } catch (RyeSgfaException e) {
                erroresHnc.add(descriptorHnc + ": " + e.getMessage());
            }
        }

        StringBuilder textoHnc = new StringBuilder();
        textoHnc.append("Guardadas: ").append(guardadasHnc).append(" | Con error: ").append(erroresHnc.size()).append('\n');
        for (String errorHnc : erroresHnc) {
            textoHnc.append("  ").append(errorHnc).append('\n');
        }
        resultadoArea.setText(textoHnc.toString());
    }

    private YmclEstudiante rahpResolverEstudiante(RahpFilaActaUI filaHnc) {
        String matriculaHnc = filaHnc.matriculaProperty().get();
        if (matriculaHnc == null || matriculaHnc.isBlank()) {
            throw new RyeValidacionException("Falta el N° de matricula (se usa como DNI y codigo del estudiante).");
        }
        Optional<YmclEstudiante> existenteHnc = estudianteRepositoryHnc.findByDni(matriculaHnc)
                .or(() -> estudianteRepositoryHnc.findByCodigo(matriculaHnc));
        if (existenteHnc.isPresent()) {
            return existenteHnc.get();
        }
        String apellidoPaternoHnc = filaHnc.apellidoPaternoProperty().get();
        if (apellidoPaternoHnc == null || apellidoPaternoHnc.isBlank()) {
            throw new RyeValidacionException("Falta el apellido paterno (estudiante nuevo).");
        }
        String nombresHnc = filaHnc.nombresProperty().get();
        return estudianteServiceHnc.ymclCrear(matriculaHnc, matriculaHnc,
                nombresHnc == null || nombresHnc.isBlank() ? apellidoPaternoHnc : nombresHnc, apellidoPaternoHnc,
                filaHnc.apellidoMaternoProperty().get(), null, null, null, null, null);
    }

    private YmclTrayectoria rahpResolverTrayectoria(YmclEstudiante estudianteHnc, RacaProgramaEstudio programaHnc, RacaPlanEstudio planHnc) {
        return trayectoriaServiceHnc.ymclListarPorEstudiante(estudianteHnc.getId()).stream()
                .filter(t -> t.getPlanEstudio().getId().equals(planHnc.getId()))
                .findFirst()
                .orElseGet(() -> trayectoriaServiceHnc.ymclIniciar(estudianteHnc.getId(), programaHnc.getId(), planHnc.getId(), null,
                        "Importado desde Acta Consolidada (OCR)"));
    }

    @FXML
    public void rahpOnCerrar() {
        tabla.fireEvent(new RyeCerrarVistaEvent());
    }

    private <T> StringConverter<T> rahpConvertidor(java.util.function.Function<T, String> textoDeHnc) {
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
}
