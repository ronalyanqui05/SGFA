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

import pe.edu.sgfa.common.exception.SgfaException;
import pe.edu.sgfa.ficha.service.TituloCiclo;
import pe.edu.sgfa.periodo.domain.PeriodoAcademico;
import pe.edu.sgfa.programa.domain.Ciclo;
import pe.edu.sgfa.programa.domain.PlanEstudio;
import pe.edu.sgfa.programa.domain.ProgramaEstudio;
import pe.edu.sgfa.reporte.domain.DocumentoEmitido;
import pe.edu.sgfa.reporte.domain.TipoDocumento;
import pe.edu.sgfa.reporte.service.DocumentoOficialService;
import pe.edu.sgfa.reporte.service.ExportadorExcel;
import pe.edu.sgfa.reporte.service.ExportadorPdf;
import pe.edu.sgfa.reporte.service.ReporteService;
import pe.edu.sgfa.reporte.service.ReporteService.AmbitoMerito;
import pe.edu.sgfa.reporte.service.TablaReporte;
import pe.edu.sgfa.shell.CerrarVistaEvent;
import pe.edu.sgfa.shell.NavigationService;

@Component
@Scope("prototype")
public class ReportesController {

    private enum Tipo {
        ACTA("Acta consolidada de notas", true, false, TipoDocumento.ACTA),
        MERITO("Orden de merito", true, false, TipoDocumento.ORDEN_MERITO),
        RESULTADOS("Cuadro de resultados por curso", true, false, null),
        RIESGO("Estudiantes en riesgo y promocion", true, false, null),
        COBERTURA("Cobertura de notas (que falta cargar, por ciclo)", false, false, null),
        PENDIENTES("Pendientes de cargar (detalle por estudiante)", true, true, null),
        HONOR("Cuadro de honor por periodo (1.°, 2.° y 3.° lugar de cada especialidad)", false, false, null, false);

        final String texto;
        final boolean usaCiclo;
        final boolean cicloOpcional;
        final TipoDocumento documento;

        final boolean usaPlan;

        Tipo(String texto, boolean usaCiclo, boolean cicloOpcional, TipoDocumento documento) {
            this(texto, usaCiclo, cicloOpcional, documento, true);
        }

        Tipo(String texto, boolean usaCiclo, boolean cicloOpcional, TipoDocumento documento, boolean usaPlan) {
            this.usaPlan = usaPlan;
            this.texto = texto;
            this.usaCiclo = usaCiclo;
            this.cicloOpcional = cicloOpcional;
            this.documento = documento;
        }
    }

    private static final Map<String, String> COLORES = Map.ofEntries(
            Map.entry("ALTO", "#c62828"), Map.entry("NO PROMUEVE", "#c62828"), Map.entry("Sin notas", "#c62828"),
            Map.entry("MEDIO", "#c77700"), Map.entry("INCOMPLETO", "#c77700"), Map.entry("POR DEFINIR", "#c77700"),
            Map.entry("Parcial", "#c77700"),
            Map.entry("BAJO", "#1e8e3e"), Map.entry("PROMUEVE", "#1e8e3e"), Map.entry("Quinto superior", "#1e8e3e"),
            Map.entry("Tercio superior", "#1e8e3e"));

    private final ReporteService reporteService;
    private final ExportadorExcel exportadorExcel;
    private final ExportadorPdf exportadorPdf;
    private final DocumentoOficialService documentoOficialService;
    private final NavigationService navigationService;

    @FXML
    private ComboBox<Tipo> reporteCombo;
    @FXML
    private ComboBox<ProgramaEstudio> programaCombo;
    @FXML
    private ComboBox<PlanEstudio> planCombo;
    @FXML
    private ComboBox<Ciclo> cicloCombo;
    @FXML
    private CheckBox todosCiclosCheck;
    @FXML
    private ComboBox<PeriodoAcademico> periodoCombo;
    @FXML
    private HBox opcionesMeritoBox;
    @FXML
    private ComboBox<AmbitoMerito> ambitoCombo;
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

    private TablaReporte actual;
    private Tipo actualTipo;

    public ReportesController(ReporteService reporteService, ExportadorExcel exportadorExcel, ExportadorPdf exportadorPdf,
                               DocumentoOficialService documentoOficialService, NavigationService navigationService) {
        this.reporteService = reporteService;
        this.exportadorExcel = exportadorExcel;
        this.exportadorPdf = exportadorPdf;
        this.documentoOficialService = documentoOficialService;
        this.navigationService = navigationService;
    }

    @FXML
    public void initialize() {
        reporteCombo.getItems().setAll(Tipo.values());
        reporteCombo.setConverter(convertidor(t -> t.texto));
        reporteCombo.getSelectionModel().selectFirst();
        ambitoCombo.getItems().setAll(AmbitoMerito.values());
        ambitoCombo.setConverter(convertidor(a -> switch (a) {
            case CICLO_PERIODO -> "Un ciclo en un periodo";
            case CICLO_VIGENTE -> "Un ciclo (notas vigentes de cualquier periodo)";
            case PLAN_ACUMULADO -> "General del plan (acumulado)";
        }));
        ambitoCombo.getSelectionModel().selectFirst();
        programaCombo.setConverter(convertidor(ProgramaEstudio::getNombre));
        planCombo.setConverter(convertidor(p -> p.getCodigo() + " - " + p.getNombre()));
        cicloCombo.setConverter(convertidor(c -> TituloCiclo.de(c.getNumero())));
        periodoCombo.setConverter(convertidor(PeriodoAcademico::getNombre));

        programaCombo.getItems().setAll(reporteService.listarProgramas());
        periodoCombo.getItems().setAll(reporteService.listarPeriodos());
        periodoCombo.getSelectionModel().selectFirst();

        programaCombo.valueProperty().addListener((obs, antes, programa) -> {
            planCombo.getItems().clear();
            if (programa != null) {
                planCombo.getItems().setAll(reporteService.listarPlanes(programa.getId()));
                planCombo.getSelectionModel().selectFirst();
            }
        });
        planCombo.valueProperty().addListener((obs, antes, pln) -> {
            cicloCombo.getItems().clear();
            if (pln != null) {
                cicloCombo.getItems().setAll(reporteService.listarCiclos(pln.getId()));
                cicloCombo.getSelectionModel().selectFirst();
            }
        });
        reporteCombo.valueProperty().addListener((obs, antes, tp) -> actualizarFiltros());
        ambitoCombo.valueProperty().addListener((obs, antes, ambt) -> actualizarFiltros());
        todosCiclosCheck.selectedProperty().addListener((obs, antes, ahora) -> actualizarFiltros());
        programaCombo.getSelectionModel().selectFirst();
        actualizarFiltros();
        excelButton.setDisable(true);
        pdfButton.setDisable(true);
        vistaPreviaButton.setDisable(true);
        oficialButton.setDisable(true);
        tabla.setPlaceholder(new Label("Elija el reporte y los filtros y pulse \"Generar\"."));
    }

    public void elegirOrdenMerito() {
        reporteCombo.setValue(Tipo.MERITO);
    }

    private boolean necesitaCiclo(Tipo tp) {
        if (tp == null || !tp.usaCiclo) {
            return false;
        }
        return !(tp == Tipo.MERITO && ambitoCombo.getValue() == AmbitoMerito.PLAN_ACUMULADO)
                && !(tp.cicloOpcional && todosCiclosCheck.isSelected());
    }

    private boolean necesitaPeriodo(Tipo tp) {
        return tp != null && !(tp == Tipo.MERITO && ambitoCombo.getValue() != AmbitoMerito.CICLO_PERIODO);
    }

    private void actualizarFiltros() {
        Tipo tp = reporteCombo.getValue();
        boolean mrt = tp == Tipo.MERITO;
        opcionesMeritoBox.setVisible(mrt);
        opcionesMeritoBox.setManaged(mrt);
        boolean opcnl = tp != null && tp.cicloOpcional;
        todosCiclosCheck.setVisible(opcnl);
        todosCiclosCheck.setManaged(opcnl);
        if (!opcnl) {
            todosCiclosCheck.setSelected(false);
        }
        boolean usaPlan = tp == null || tp.usaPlan;
        programaCombo.setDisable(!usaPlan);
        planCombo.setDisable(!usaPlan);
        cicloCombo.setDisable(!necesitaCiclo(tp));
        periodoCombo.setDisable(!necesitaPeriodo(tp));
    }

    @FXML
    public void onGenerar() {
        Tipo tp = reporteCombo.getValue();
        PlanEstudio pln = planCombo.getValue();
        PeriodoAcademico periodo = periodoCombo.getValue();
        Ciclo ccl = cicloCombo.getValue();
        boolean tds = tp != null && tp.cicloOpcional && todosCiclosCheck.isSelected();
        AmbitoMerito ambt = ambitoCombo.getValue();
        if (tp == null || (tp.usaPlan && pln == null) || (necesitaPeriodo(tp) && periodo == null)
                || (necesitaCiclo(tp) && ccl == null)) {
            mostrarAdvertencia("Elija el reporte" + (tp == null || tp.usaPlan ? ", el programa y el plan" : "")
                    + (necesitaCiclo(tp) ? ", el ciclo" : "")
                    + (necesitaPeriodo(tp) ? " y el periodo." : "."));
            return;
        }
        Integer lmt = null;
        if (tp == Tipo.MERITO && !limiteField.getText().isBlank()) {
            try {
                lmt = Integer.parseInt(limiteField.getText().trim());
            } catch (NumberFormatException e) {
                lmt = 0;
            }
            if (lmt < 1) {
                mostrarAdvertencia("El numero de puestos a mostrar debe ser un entero positivo (o dejarlo vacio para ver todos).");
                return;
            }
        }
        Integer lmtPsts = lmt;
        generarButton.setDisable(true);
        estadoLabel.setText("Generando...");
        Task<TablaReporte> tr = new Task<>() {
            @Override
            protected TablaReporte call() {
                Long cclId = ccl == null ? null : ccl.getId();
                Long prdId = periodo == null ? null : periodo.getId();
                return switch (tp) {
                    case HONOR -> reporteService.cuadroDeHonor(prdId, 3);
                    case ACTA -> reporteService.actaConsolidada(pln.getId(), cclId, prdId);
                    case MERITO -> reporteService.ordenMerito(pln.getId(), ambt, cclId, prdId, lmtPsts);
                    case RESULTADOS -> reporteService.resultadosPorCurso(pln.getId(), cclId, prdId);
                    case RIESGO -> reporteService.riesgoYPromocion(pln.getId(), cclId, prdId);
                    case COBERTURA -> reporteService.coberturaPorCiclo(pln.getId(), prdId);
                    case PENDIENTES -> reporteService.pendientes(pln.getId(), prdId, tds ? null : cclId);
                };
            }
        };
        tr.setOnSucceeded(e -> {
            generarButton.setDisable(false);
            actualTipo = tp;
            mostrar(tr.getValue());
        });
        tr.setOnFailed(e -> {
            generarButton.setDisable(false);
            estadoLabel.setText("");
            Throwable errr = tr.getException();
            mostrarAdvertencia(errr instanceof SgfaException ? errr.getMessage()
                    : "No se pudo generar el reporte: " + errr.getMessage());
        });
        Thread hl = new Thread(tr, "sgfa-reporte");
        hl.setDaemon(true);
        hl.start();
    }

    private void mostrar(TablaReporte t) {
        actual = t;
        tabla.getColumns().clear();
        tabla.getItems().clear();
        tituloLabel.setText(t.titulo() + "   |   " + String.join("   |   ", t.subtitulo()));

        int indc = 0;
        if (t.tieneGrupos()) {
            for (TablaReporte.Grupo g : t.grupos()) {
                if (g.texto().isEmpty()) {
                    tabla.getColumns().add(columna(t, indc));
                    indc++;
                    continue;
                }
                TableColumn<List<String>, ?> pdr = new TableColumn<>(g.texto());
                for (int k = 0; k < g.columnas(); k++) {
                    pdr.getColumns().add(columna(t, indc++));
                }
                tabla.getColumns().add(pdr);
            }
        } else {
            for (int i = 0; i < t.columnas().size(); i++) {
                tabla.getColumns().add(columna(t, i));
            }
        }
        tabla.getItems().setAll(t.filas());
        boolean hayDts = !t.filas().isEmpty();
        excelButton.setDisable(!hayDts);
        pdfButton.setDisable(!hayDts);
        vistaPreviaButton.setDisable(!hayDts);
        oficialButton.setDisable(!hayDts || actualTipo == null || actualTipo.documento == null);
        estadoLabel.setText(hayDts ? t.filas().size() + " fila(s). " + String.join(" ", t.pie())
                : "No hay datos para estos filtros: aun no se cargaron notas de este ciclo y periodo.");
        tabla.setPlaceholder(new Label("Sin datos para estos filtros."));
    }

    private TableColumn<List<String>, String> columna(TablaReporte t, int indc) {
        TableColumn<List<String>, String> col = new TableColumn<>(t.columnas().get(indc));
        col.setCellValueFactory(d -> new ReadOnlyStringWrapper(indc < d.getValue().size() ? d.getValue().get(indc) : ""));
        col.setPrefWidth(Math.max(40, t.anchos().get(indc) * 9));
        boolean nmrc = t.numericas().get(indc);
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String vlr, boolean vc) {
                super.updateItem(vlr, vc);
                setText(vc ? null : vlr);
                setAlignment(nmrc ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                String clr = vc ? null : COLORES.get(vlr);
                boolean sccn = !vc && actual != null && actual.esSeccion(getIndex());
                setStyle(sccn ? "-fx-font-weight: bold; -fx-background-color: rgba(120,140,180,0.25);"
                        : clr == null ? "" : "-fx-font-weight: bold; -fx-text-fill: " + clr + ";");
            }
        });
        col.setSortable(false);
        return col;
    }

    @FXML
    public void onExportarExcel() {
        exportar("xlsx", "Excel", exportadorExcel::escribir);
    }

    @FXML
    public void onExportarPdf() {
        exportar("pdf", "PDF", exportadorPdf::escribir);
    }

    private interface Escritor {
        void escribir(TablaReporte tabla, String insttcn, java.nio.file.Path destino);
    }

    private void exportar(String extnsn, String dscrpcn, Escritor escrtr) {
        if (actual == null) {
            return;
        }
        FileChooser chsr = new FileChooser();
        chsr.setTitle("Guardar reporte (" + dscrpcn + ")");
        chsr.setInitialFileName(nombreSugerido(extnsn));
        chsr.getExtensionFilters().add(new FileChooser.ExtensionFilter(dscrpcn, "*." + extnsn));
        File archivo = chsr.showSaveDialog(tabla.getScene().getWindow());
        if (archivo == null) {
            return;
        }
        try {
            escrtr.escribir(actual, reporteService.nombreInstitucion(), archivo.toPath());
            Alert inf = new Alert(Alert.AlertType.INFORMATION, "Reporte guardado en:\n" + archivo, ButtonType.OK);
            inf.setHeaderText(null);
            inf.showAndWait();
            abrir(archivo);
        } catch (SgfaException e) {
            mostrarAdvertencia(e.getMessage());
        }
    }

    @FXML
    public void onEmitirOficial() {
        if (actual == null || actualTipo == null || actualTipo.documento == null) {
            return;
        }
        Alert cnfrmr = new Alert(Alert.AlertType.CONFIRMATION, "Se emitira \"" + actual.titulo() + "\" como DOCUMENTO OFICIAL: "
                + "recibira un numero correlativo, un codigo de verificacion y quedara guardado en el historial tal como "
                + "esta ahora (no se puede modificar, solo anular). Imprimalo y firmelo.\n\n¿Emitir?", ButtonType.OK, ButtonType.CANCEL);
        cnfrmr.setHeaderText("Emitir version oficial");
        if (cnfrmr.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        try {
            DocumentoEmitido documento = documentoOficialService.emitir(actualTipo.documento, actual);
            Alert inf = new Alert(Alert.AlertType.INFORMATION, "Documento emitido: " + documento.getCorrelativo()
                    + "\nCodigo de verificacion: " + documento.getCodigoVerificacion()
                    + "\n\nQueda guardado en \"Documentos emitidos\" para reimprimirlo cuando se necesite.", ButtonType.OK);
            inf.setHeaderText(null);
            inf.showAndWait();
            verPdf(java.nio.file.Files.readAllBytes(documentoOficialService.archivo(documento)),
                    documentoOficialService.archivo(documento).getFileName().toString(), documento.getCorrelativo());
        } catch (SgfaException | java.io.IOException e) {
            mostrarAdvertencia(e.getMessage());
        }
    }

    @FXML
    public void onVistaPrevia() {
        if (actual == null) {
            return;
        }
        try {
            java.nio.file.Path temporal = java.nio.file.Files.createTempFile("sgfa-vista-previa-", ".pdf");
            try {
                exportadorPdf.escribir(actual, reporteService.nombreInstitucion(), temporal);
                verPdf(java.nio.file.Files.readAllBytes(temporal), nombreSugerido("pdf"), "Vista previa - " + actual.titulo());
            } finally {
                java.nio.file.Files.deleteIfExists(temporal);
            }
        } catch (SgfaException | java.io.IOException e) {
            mostrarAdvertencia(e.getMessage());
        }
    }

    private void verPdf(byte[] pdf, String nmbr, String titulo) {
        navigationService.abrirVista("/fxml/reporte/vista-previa-pdf.fxml", titulo,
                controlador -> ((VistaPreviaPdfController) controlador).setDatos(pdf, nmbr));
    }

    @FXML
    public void onDocumentosEmitidos() {
        navigationService.abrirVista("/fxml/reporte/documentos.fxml", "Documentos emitidos");
    }

    private void abrir(File archivo) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(archivo);
            }
        } catch (Exception ignrd) {

        }
    }

    private String nombreSugerido(String extnsn) {
        String bs = actual.titulo().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
        PeriodoAcademico periodo = periodoCombo.getValue();
        return bs + (periodo == null || periodoCombo.isDisabled() ? "" : "_" + periodo.getNombre()) + "." + extnsn;
    }

    @FXML
    public void onCerrar() {
        tabla.fireEvent(new CerrarVistaEvent());
    }

    private void mostrarAdvertencia(String mnsj) {
        new Alert(Alert.AlertType.WARNING, mnsj, ButtonType.OK).showAndWait();
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
}
