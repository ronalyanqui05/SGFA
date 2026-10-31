package pe.edu.sgfa.reporte.ui;

import java.awt.Desktop;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pe.edu.sgfa.common.exception.RyeSgfaException;
import pe.edu.sgfa.ficha.service.RacaTituloCiclo;
import pe.edu.sgfa.periodo.domain.YmclPeriodoAcademico;
import pe.edu.sgfa.programa.domain.RacaCiclo;
import pe.edu.sgfa.programa.domain.RacaPlanEstudio;
import pe.edu.sgfa.programa.domain.RacaProgramaEstudio;
import pe.edu.sgfa.reporte.domain.CphDocumentoEmitido;
import pe.edu.sgfa.reporte.domain.CphTipoDocumento;
import pe.edu.sgfa.reporte.service.CphDocumentoOficialService;
import pe.edu.sgfa.reporte.service.CphExportadorExcel;
import pe.edu.sgfa.reporte.service.CphExportadorPdf;
import pe.edu.sgfa.reporte.service.CphReporteService;
import pe.edu.sgfa.reporte.service.CphReporteService.CphAmbitoMerito;
import pe.edu.sgfa.reporte.service.CphTablaReporte;
import pe.edu.sgfa.shell.RyeCerrarVistaEvent;
import pe.edu.sgfa.shell.RyeNavigationService;

@Component
@Scope("prototype")
public class CphReportesController {

    private enum CphTipo {
        ACTA("Acta consolidada de notas", true, false, CphTipoDocumento.ACTA),
        MERITO("Orden de merito", true, false, CphTipoDocumento.ORDEN_MERITO),
        RESULTADOS("Cuadro de resultados por curso", true, false, null),
        RIESGO("Estudiantes en riesgo y promocion", true, false, null),
        COBERTURA("Cobertura de notas (que falta cargar, por ciclo)", false, false, null),
        PENDIENTES("Pendientes de cargar (detalle por estudiante)", true, true, null),
        HONOR("Cuadro de honor por periodo (1.°, 2.° y 3.° lugar de cada especialidad)", false, false, null, false);

        final String texto;
        final boolean usaCiclo;
        final boolean cicloOpcional;
        final CphTipoDocumento documento;

        final boolean usaPlan;

        CphTipo(String textoPch, boolean usaCicloPch, boolean cicloOpcionalPch, CphTipoDocumento documentoPch) {
            this(textoPch, usaCicloPch, cicloOpcionalPch, documentoPch, true);
        }

        CphTipo(String textoPch, boolean usaCicloPch, boolean cicloOpcionalPch, CphTipoDocumento documentoPch, boolean usaPlanPch) {
            this.usaPlan = usaPlanPch;
            this.texto = textoPch;
            this.usaCiclo = usaCicloPch;
            this.cicloOpcional = cicloOpcionalPch;
            this.documento = documentoPch;
        }
    }

    private static final Map<String, String> COLORES_PCH = Map.ofEntries(
            Map.entry("ALTO", "#c62828"), Map.entry("NO PROMUEVE", "#c62828"), Map.entry("Sin notas", "#c62828"),
            Map.entry("MEDIO", "#c77700"), Map.entry("INCOMPLETO", "#c77700"), Map.entry("POR DEFINIR", "#c77700"),
            Map.entry("Parcial", "#c77700"),
            Map.entry("BAJO", "#1e8e3e"), Map.entry("PROMUEVE", "#1e8e3e"), Map.entry("Quinto superior", "#1e8e3e"),
            Map.entry("Tercio superior", "#1e8e3e"));

    private final CphReporteService reporteServicePch;
    private final CphExportadorExcel exportadorExcelPch;
    private final CphExportadorPdf exportadorPdfPch;
    private final CphDocumentoOficialService documentoOficialServicePch;
    private final RyeNavigationService navigationServicePch;

    @FXML
    private ComboBox<CphTipo> reporteCombo;
    @FXML
    private ComboBox<RacaProgramaEstudio> programaCombo;
    @FXML
    private ComboBox<RacaPlanEstudio> planCombo;
    @FXML
    private ComboBox<RacaCiclo> cicloCombo;
    @FXML
    private CheckBox todosCiclosCheck;
    @FXML
    private ComboBox<YmclPeriodoAcademico> periodoCombo;
    @FXML
    private HBox opcionesMeritoBox;
    @FXML
    private ComboBox<CphAmbitoMerito> ambitoCombo;
    @FXML
    private TextField limiteField;
    @FXML
    private Button generarButton;
    @FXML
    private Button excelButton;
    @FXML
    private Button pdfButton;
    @FXML
    private Button vistaPreviaButton;
    @FXML
    private Button oficialButton;
    @FXML
    private Label tituloLabel;
    @FXML
    private Label estadoLabel;
    @FXML
    private TableView<List<String>> tabla;

    private CphTablaReporte actualPch;
    private CphTipo actualTipoPch;

    public CphReportesController(CphReporteService reporteServicePch, CphExportadorExcel exportadorExcelPch, CphExportadorPdf exportadorPdfPch,
                               CphDocumentoOficialService documentoOficialServicePch, RyeNavigationService navigationServicePch) {
        this.reporteServicePch = reporteServicePch;
        this.exportadorExcelPch = exportadorExcelPch;
        this.exportadorPdfPch = exportadorPdfPch;
        this.documentoOficialServicePch = documentoOficialServicePch;
        this.navigationServicePch = navigationServicePch;
    }

    @FXML
    public void initialize() {
        reporteCombo.getItems().setAll(CphTipo.values());
        reporteCombo.setConverter(cphConvertidor(t -> t.texto));
        reporteCombo.getSelectionModel().selectFirst();
        ambitoCombo.getItems().setAll(CphAmbitoMerito.values());
        ambitoCombo.setConverter(cphConvertidor(a -> switch (a) {
            case CICLO_PERIODO -> "Un ciclo en un periodo";
            case CICLO_VIGENTE -> "Un ciclo (notas vigentes de cualquier periodo)";
            case PLAN_ACUMULADO -> "General del plan (acumulado)";
        }));
        ambitoCombo.getSelectionModel().selectFirst();
        programaCombo.setConverter(cphConvertidor(RacaProgramaEstudio::getNombre));
        planCombo.setConverter(cphConvertidor(p -> p.getCodigo() + " - " + p.getNombre()));
        cicloCombo.setConverter(cphConvertidor(c -> RacaTituloCiclo.de(c.getNumero())));
        periodoCombo.setConverter(cphConvertidor(YmclPeriodoAcademico::getNombre));

        programaCombo.getItems().setAll(reporteServicePch.cphListarProgramas());
        periodoCombo.getItems().setAll(reporteServicePch.cphListarPeriodos());
        periodoCombo.getSelectionModel().selectFirst();

        programaCombo.valueProperty().addListener((obsPch, antesPch, programaPch) -> {
            planCombo.getItems().clear();
            if (programaPch != null) {
                planCombo.getItems().setAll(reporteServicePch.cphListarPlanes(programaPch.getId()));
                planCombo.getSelectionModel().selectFirst();
            }
        });
        planCombo.valueProperty().addListener((obsPch, antesPch, planPch) -> {
            cicloCombo.getItems().clear();
            if (planPch != null) {
                cicloCombo.getItems().setAll(reporteServicePch.cphListarCiclos(planPch.getId()));
                cicloCombo.getSelectionModel().selectFirst();
            }
        });
        reporteCombo.valueProperty().addListener((obsPch, antesPch, tipoPch) -> cphActualizarFiltros());
        ambitoCombo.valueProperty().addListener((obsPch, antesPch, ambitoPch) -> cphActualizarFiltros());
        todosCiclosCheck.selectedProperty().addListener((obsPch, antesPch, ahoraPch) -> cphActualizarFiltros());
        programaCombo.getSelectionModel().selectFirst();
        cphActualizarFiltros();
        excelButton.setDisable(true);
        pdfButton.setDisable(true);
        vistaPreviaButton.setDisable(true);
        oficialButton.setDisable(true);
        tabla.setPlaceholder(new Label("Elija el reporte y los filtros y pulse \"Generar\"."));
    }

    public void cphElegirOrdenMerito() {
        reporteCombo.setValue(CphTipo.MERITO);
    }

    private boolean cphNecesitaCiclo(CphTipo tipoPch) {
        if (tipoPch == null || !tipoPch.usaCiclo) {
            return false;
        }
        return !(tipoPch == CphTipo.MERITO && ambitoCombo.getValue() == CphAmbitoMerito.PLAN_ACUMULADO)
                && !(tipoPch.cicloOpcional && todosCiclosCheck.isSelected());
    }

    private boolean cphNecesitaPeriodo(CphTipo tipoPch) {
        return tipoPch != null && !(tipoPch == CphTipo.MERITO && ambitoCombo.getValue() != CphAmbitoMerito.CICLO_PERIODO);
    }

    private void cphActualizarFiltros() {
        CphTipo tipoPch = reporteCombo.getValue();
        boolean meritoPch = tipoPch == CphTipo.MERITO;
        opcionesMeritoBox.setVisible(meritoPch);
        opcionesMeritoBox.setManaged(meritoPch);
        boolean opcionalPch = tipoPch != null && tipoPch.cicloOpcional;
        todosCiclosCheck.setVisible(opcionalPch);
        todosCiclosCheck.setManaged(opcionalPch);
        if (!opcionalPch) {
            todosCiclosCheck.setSelected(false);
        }
        boolean usaPlanPch = tipoPch == null || tipoPch.usaPlan;
        programaCombo.setDisable(!usaPlanPch);
        planCombo.setDisable(!usaPlanPch);
        cicloCombo.setDisable(!cphNecesitaCiclo(tipoPch));
        periodoCombo.setDisable(!cphNecesitaPeriodo(tipoPch));
    }

    @FXML
    public void cphOnGenerar() {
        CphTipo tipoPch = reporteCombo.getValue();
        RacaPlanEstudio planPch = planCombo.getValue();
        YmclPeriodoAcademico periodoPch = periodoCombo.getValue();
        RacaCiclo cicloPch = cicloCombo.getValue();
        boolean todosPch = tipoPch != null && tipoPch.cicloOpcional && todosCiclosCheck.isSelected();
        CphAmbitoMerito ambitoPch = ambitoCombo.getValue();
        if (tipoPch == null || (tipoPch.usaPlan && planPch == null) || (cphNecesitaPeriodo(tipoPch) && periodoPch == null)
                || (cphNecesitaCiclo(tipoPch) && cicloPch == null)) {
            cphMostrarAdvertencia("Elija el reporte" + (tipoPch == null || tipoPch.usaPlan ? ", el programa y el plan" : "")
                    + (cphNecesitaCiclo(tipoPch) ? ", el ciclo" : "")
                    + (cphNecesitaPeriodo(tipoPch) ? " y el periodo." : "."));
            return;
        }
        Integer limitePch = null;
        if (tipoPch == CphTipo.MERITO && !limiteField.getText().isBlank()) {
            try {
                limitePch = Integer.parseInt(limiteField.getText().trim());
            } catch (NumberFormatException e) {
                limitePch = 0;
            }
            if (limitePch < 1) {
                cphMostrarAdvertencia("El numero de puestos a mostrar debe ser un entero positivo (o dejarlo vacio para ver todos).");
                return;
            }
        }
        Integer limitePuestosPch = limitePch;
        generarButton.setDisable(true);
        estadoLabel.setText("Generando...");
        Task<CphTablaReporte> tareaPch = new Task<>() {
            @Override
            protected CphTablaReporte call() {
                Long cicloIdPch = cicloPch == null ? null : cicloPch.getId();
                Long periodoIdPch = periodoPch == null ? null : periodoPch.getId();
                return switch (tipoPch) {
                    case HONOR -> reporteServicePch.cphCuadroDeHonor(periodoIdPch, 3);
                    case ACTA -> reporteServicePch.cphActaConsolidada(planPch.getId(), cicloIdPch, periodoIdPch);
                    case MERITO -> reporteServicePch.cphOrdenMerito(planPch.getId(), ambitoPch, cicloIdPch, periodoIdPch, limitePuestosPch);
                    case RESULTADOS -> reporteServicePch.cphResultadosPorCurso(planPch.getId(), cicloIdPch, periodoIdPch);
                    case RIESGO -> reporteServicePch.cphRiesgoYPromocion(planPch.getId(), cicloIdPch, periodoIdPch);
                    case COBERTURA -> reporteServicePch.cphCoberturaPorCiclo(planPch.getId(), periodoIdPch);
                    case PENDIENTES -> reporteServicePch.cphPendientes(planPch.getId(), periodoIdPch, todosPch ? null : cicloIdPch);
                };
            }
        };
        tareaPch.setOnSucceeded(e -> {
            generarButton.setDisable(false);
            actualTipoPch = tipoPch;
            cphMostrar(tareaPch.getValue());
        });
        tareaPch.setOnFailed(e -> {
            generarButton.setDisable(false);
            estadoLabel.setText("");
            Throwable errorPch = tareaPch.getException();
            cphMostrarAdvertencia(errorPch instanceof RyeSgfaException ? errorPch.getMessage()
                    : "No se pudo generar el reporte: " + errorPch.getMessage());
        });
        Thread hiloPch = new Thread(tareaPch, "sgfa-reporte");
        hiloPch.setDaemon(true);
        hiloPch.start();
    }

    private void cphMostrar(CphTablaReporte t) {
        actualPch = t;
        tabla.getColumns().clear();
        tabla.getItems().clear();
        tituloLabel.setText(t.titulo() + "   |   " + String.join("   |   ", t.subtitulo()));

        int indicePch = 0;
        if (t.cphTieneGrupos()) {
            for (CphTablaReporte.CphGrupo g : t.grupos()) {
                if (g.texto().isEmpty()) {
                    tabla.getColumns().add(cphColumna(t, indicePch));
                    indicePch++;
                    continue;
                }
                TableColumn<List<String>, ?> padrePch = new TableColumn<>(g.texto());
                for (int k = 0; k < g.columnas(); k++) {
                    padrePch.getColumns().add(cphColumna(t, indicePch++));
                }
                tabla.getColumns().add(padrePch);
            }
        } else {
            for (int i = 0; i < t.columnas().size(); i++) {
                tabla.getColumns().add(cphColumna(t, i));
            }
        }
        tabla.getItems().setAll(t.filas());
        boolean hayDatosPch = !t.filas().isEmpty();
        excelButton.setDisable(!hayDatosPch);
        pdfButton.setDisable(!hayDatosPch);
        vistaPreviaButton.setDisable(!hayDatosPch);
        oficialButton.setDisable(!hayDatosPch || actualTipoPch == null || actualTipoPch.documento == null);
        estadoLabel.setText(hayDatosPch ? t.filas().size() + " fila(s). " + String.join(" ", t.pie())
                : "No hay datos para estos filtros: aun no se cargaron notas de este ciclo y periodo.");
        tabla.setPlaceholder(new Label("Sin datos para estos filtros."));
    }

    private TableColumn<List<String>, String> cphColumna(CphTablaReporte t, int indicePch) {
        TableColumn<List<String>, String> colPch = new TableColumn<>(t.columnas().get(indicePch));
        colPch.setCellValueFactory(d -> new ReadOnlyStringWrapper(indicePch < d.getValue().size() ? d.getValue().get(indicePch) : ""));
        colPch.setPrefWidth(Math.max(40, t.anchos().get(indicePch) * 9));
        boolean numericaPch = t.numericas().get(indicePch);
        colPch.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String valorPch, boolean vacioPch) {
                super.updateItem(valorPch, vacioPch);
                setText(vacioPch ? null : valorPch);
                setAlignment(numericaPch ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                String colorPch = vacioPch ? null : COLORES_PCH.get(valorPch);
                boolean seccionPch = !vacioPch && actualPch != null && actualPch.cphEsSeccion(getIndex());
                setStyle(seccionPch ? "-fx-font-weight: bold; -fx-background-color: rgba(120,140,180,0.25);"
                        : colorPch == null ? "" : "-fx-font-weight: bold; -fx-text-fill: " + colorPch + ";");
            }
        });
        colPch.setSortable(false);
        return colPch;
    }

    @FXML
    public void cphOnExportarExcel() {
        cphExportar("xlsx", "Excel", exportadorExcelPch::cphEscribir);
    }

    @FXML
    public void cphOnExportarPdf() {
        cphExportar("pdf", "PDF", exportadorPdfPch::cphEscribir);
    }

    private interface CphEscritor {
        void cphEscribir(CphTablaReporte tablaPch, String institucionPch, java.nio.file.Path destinoPch);
    }

    private void cphExportar(String extensionPch, String descripcionPch, CphEscritor escritorPch) {
        if (actualPch == null) {
            return;
        }
        FileChooser chooserPch = new FileChooser();
        chooserPch.setTitle("Guardar reporte (" + descripcionPch + ")");
        chooserPch.setInitialFileName(cphNombreSugerido(extensionPch));
        chooserPch.getExtensionFilters().add(new FileChooser.ExtensionFilter(descripcionPch, "*." + extensionPch));
        File archivoPch = chooserPch.showSaveDialog(tabla.getScene().getWindow());
        if (archivoPch == null) {
            return;
        }
        try {
            escritorPch.cphEscribir(actualPch, reporteServicePch.cphNombreInstitucion(), archivoPch.toPath());
            Alert infoPch = new Alert(Alert.AlertType.INFORMATION, "Reporte guardado en:\n" + archivoPch, ButtonType.OK);
            infoPch.setHeaderText(null);
            infoPch.showAndWait();
            cphAbrir(archivoPch);
        } catch (RyeSgfaException e) {
            cphMostrarAdvertencia(e.getMessage());
        }
    }

    @FXML
    public void cphOnEmitirOficial() {
        if (actualPch == null || actualTipoPch == null || actualTipoPch.documento == null) {
            return;
        }
        Alert confirmarPch = new Alert(Alert.AlertType.CONFIRMATION, "Se emitira \"" + actualPch.titulo() + "\" como DOCUMENTO OFICIAL: "
                + "recibira un numero correlativo, un codigo de verificacion y quedara guardado en el historial tal como "
                + "esta ahora (no se puede modificar, solo anular). Imprimalo y firmelo.\n\n¿Emitir?", ButtonType.OK, ButtonType.CANCEL);
        confirmarPch.setHeaderText("Emitir version oficial");
        if (confirmarPch.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        try {
            CphDocumentoEmitido documentoPch = documentoOficialServicePch.cphEmitir(actualTipoPch.documento, actualPch);
            Alert infoPch = new Alert(Alert.AlertType.INFORMATION, "Documento emitido: " + documentoPch.getCorrelativo()
                    + "\nCodigo de verificacion: " + documentoPch.getCodigoVerificacion()
                    + "\n\nQueda guardado en \"Documentos emitidos\" para reimprimirlo cuando se necesite.", ButtonType.OK);
            infoPch.setHeaderText(null);
            infoPch.showAndWait();
            cphVerPdf(java.nio.file.Files.readAllBytes(documentoOficialServicePch.cphArchivo(documentoPch)),
                    documentoOficialServicePch.cphArchivo(documentoPch).getFileName().toString(), documentoPch.getCorrelativo());
        } catch (RyeSgfaException | java.io.IOException e) {
            cphMostrarAdvertencia(e.getMessage());
        }
    }

    @FXML
    public void cphOnVistaPrevia() {
        if (actualPch == null) {
            return;
        }
        try {
            java.nio.file.Path temporalPch = java.nio.file.Files.createTempFile("sgfa-vista-previa-", ".pdf");
            try {
                exportadorPdfPch.cphEscribir(actualPch, reporteServicePch.cphNombreInstitucion(), temporalPch);
                cphVerPdf(java.nio.file.Files.readAllBytes(temporalPch), cphNombreSugerido("pdf"), "Vista previa - " + actualPch.titulo());
            } finally {
                java.nio.file.Files.deleteIfExists(temporalPch);
            }
        } catch (RyeSgfaException | java.io.IOException e) {
            cphMostrarAdvertencia(e.getMessage());
        }
    }

    private void cphVerPdf(byte[] pdfPch, String nombrePch, String tituloPch) {
        navigationServicePch.ryeAbrirVista("/fxml/reporte/vista-previa-pdf.fxml", tituloPch,
                controladorPch -> ((CphVistaPreviaPdfController) controladorPch).setDatos(pdfPch, nombrePch));
    }

    @FXML
    public void cphOnDocumentosEmitidos() {
        navigationServicePch.ryeAbrirVista("/fxml/reporte/documentos.fxml", "Documentos emitidos");
    }

    private void cphAbrir(File archivoPch) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(archivoPch);
            }
        } catch (Exception ignoradaPch) {

        }
    }

    private String cphNombreSugerido(String extensionPch) {
        String basePch = actualPch.titulo().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
        YmclPeriodoAcademico periodoPch = periodoCombo.getValue();
        return basePch + (periodoPch == null || periodoCombo.isDisabled() ? "" : "_" + periodoPch.getNombre()) + "." + extensionPch;
    }

    @FXML
    public void cphOnCerrar() {
        tabla.fireEvent(new RyeCerrarVistaEvent());
    }

    private void cphMostrarAdvertencia(String mensajePch) {
        new Alert(Alert.AlertType.WARNING, mensajePch, ButtonType.OK).showAndWait();
    }

    private <T> StringConverter<T> cphConvertidor(Function<T, String> textoDePch) {
        return new StringConverter<>() {
            @Override
            public String toString(T objetoPch) {
                return objetoPch == null ? "" : textoDePch.apply(objetoPch);
            }

            @Override
            public T fromString(String s) {
                return null;
            }
        };
    }
}
