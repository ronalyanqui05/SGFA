package pe.edu.sgfa.reporte.ui;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pe.edu.sgfa.common.exception.RyeSgfaException;
import pe.edu.sgfa.reporte.domain.CphDocumentoEmitido;
import pe.edu.sgfa.reporte.domain.CphEstadoDocumento;
import pe.edu.sgfa.reporte.service.CphDocumentoOficialService;
import pe.edu.sgfa.shell.RyeCerrarVistaEvent;
import pe.edu.sgfa.shell.RyeNavigationService;
import pe.edu.sgfa.usuario.domain.RyeRolUsuario;
import pe.edu.sgfa.usuario.service.RyeSesionActual;

@Component
@Scope("prototype")
public class CphDocumentosEmitidosController {

    private static final DateTimeFormatter FECHA_PCH = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CphDocumentoOficialService servicioPch;
    private final RyeSesionActual sesionActualPch;
    private final RyeNavigationService navigationServicePch;

    @FXML
    private TableView<CphDocumentoEmitido> tabla;
    @FXML
    private Button anularButton;
    @FXML
    private Label estadoLabel;

    public CphDocumentosEmitidosController(CphDocumentoOficialService servicioPch, RyeSesionActual sesionActualPch,
                                         RyeNavigationService navigationServicePch) {
        this.servicioPch = servicioPch;
        this.sesionActualPch = sesionActualPch;
        this.navigationServicePch = navigationServicePch;
    }

    @FXML
    public void initialize() {
        tabla.getColumns().add(cphTexto("Numero", 150, d -> d.getCorrelativo()));
        tabla.getColumns().add(cphTexto("Documento", 170, CphDocumentoEmitido::getTitulo));
        tabla.getColumns().add(cphTexto("Detalle", 430, d -> d.getDescripcion() == null ? "" : d.getDescripcion()));
        tabla.getColumns().add(cphTexto("Fecha", 90, d -> d.getFechaEmision().format(FECHA_PCH)));
        tabla.getColumns().add(cphTexto("Emitido por", 160, d -> d.getUsuario() == null ? "" : d.getUsuario().getNombres()));

        TableColumn<CphDocumentoEmitido, String> estadoPch = cphTexto("Estado", 90, d -> d.getEstado().name());
        estadoPch.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String valorPch, boolean vacioPch) {
                super.updateItem(valorPch, vacioPch);
                setText(vacioPch ? null : valorPch);
                setStyle(vacioPch ? "" : "-fx-font-weight: bold; -fx-text-fill: "
                        + (CphEstadoDocumento.ANULADO.name().equals(valorPch) ? "#c62828" : "#1e8e3e") + ";");
            }
        });
        tabla.getColumns().add(estadoPch);
        tabla.getColumns().add(cphTexto("Motivo de anulacion", 200, d -> d.getObservacion() == null ? "" : d.getObservacion()));

        boolean adminPch = sesionActualPch.ryeObtenerUsuarioActual().map(u -> u.getRol() == RyeRolUsuario.ADMIN).orElse(false);
        anularButton.setVisible(adminPch);
        anularButton.setManaged(adminPch);
        tabla.setPlaceholder(new Label("Aun no se emitio ningun documento oficial."));
        cphRecargar();
    }

    private TableColumn<CphDocumentoEmitido, String> cphTexto(String tituloPch, double anchoPch,
                                                         java.util.function.Function<CphDocumentoEmitido, String> valorPch) {
        TableColumn<CphDocumentoEmitido, String> colPch = new TableColumn<>(tituloPch);
        colPch.setCellValueFactory(d -> new ReadOnlyStringWrapper(valorPch.apply(d.getValue())));
        colPch.setPrefWidth(anchoPch);
        return colPch;
    }

    private void cphRecargar() {
        tabla.getItems().setAll(servicioPch.cphListar());
        estadoLabel.setText(tabla.getItems().size() + " documento(s) emitido(s).");
    }

    @FXML
    public void cphOnActualizar() {
        cphRecargar();
    }

    @FXML
    public void cphOnAbrir() {
        CphDocumentoEmitido d = tabla.getSelectionModel().getSelectedItem();
        if (d == null) {
            cphAviso("Seleccione un documento de la lista.");
            return;
        }
        File archivoPch = servicioPch.cphArchivo(d).toFile();
        if (!archivoPch.isFile()) {
            cphAviso("No se encontro el archivo del documento (" + archivoPch + "). Puede haberse movido o borrado.");
            return;
        }
        try {
            byte[] pdfPch = java.nio.file.Files.readAllBytes(archivoPch.toPath());
            navigationServicePch.ryeAbrirVista("/fxml/reporte/vista-previa-pdf.fxml", d.getTitulo() + " " + d.getCorrelativo(),
                    controladorPch -> ((CphVistaPreviaPdfController) controladorPch).setDatos(pdfPch, archivoPch.getName()));
        } catch (java.io.IOException e) {
            cphAviso("No se pudo abrir el PDF. Esta en:\n" + archivoPch);
        }
    }

    @FXML
    public void cphOnAnular() {
        CphDocumentoEmitido d = tabla.getSelectionModel().getSelectedItem();
        if (d == null) {
            cphAviso("Seleccione un documento de la lista.");
            return;
        }
        if (d.getEstado() == CphEstadoDocumento.ANULADO) {
            cphAviso("Ese documento ya esta anulado.");
            return;
        }
        TextInputDialog dialogoPch = new TextInputDialog();
        dialogoPch.setTitle("Anular documento");
        dialogoPch.setHeaderText("Anular " + d.getCorrelativo());
        dialogoPch.setContentText("Motivo de la anulacion:");
        Optional<String> motivoPch = dialogoPch.showAndWait();
        if (motivoPch.isEmpty()) {
            return;
        }
        try {
            servicioPch.cphAnular(d.getId(), motivoPch.get());
            cphRecargar();
        } catch (RyeSgfaException e) {
            cphAviso(e.getMessage());
        }
    }

    @FXML
    public void cphOnVerificar() {
        Dialog<ButtonType> dialogoPch = new Dialog<>();
        dialogoPch.setTitle("Verificar documento");
        dialogoPch.setHeaderText("Escriba el numero y el codigo de verificacion impresos en el documento.");
        TextField numeroPch = new TextField();
        numeroPch.setPromptText("Ej. OM-000001-2026");
        TextField codigoPch = new TextField();
        codigoPch.setPromptText("Ej. A1B2-C3D4-E5F6");
        GridPane panelPch = new GridPane();
        panelPch.setHgap(8);
        panelPch.setVgap(8);
        panelPch.addRow(0, new Label("Numero"), numeroPch);
        panelPch.addRow(1, new Label("Codigo"), codigoPch);
        dialogoPch.getDialogPane().setContent(panelPch);
        dialogoPch.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        if (dialogoPch.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        Optional<CphDocumentoEmitido> encontradoPch = servicioPch.cphVerificar(numeroPch.getText(), codigoPch.getText());
        if (encontradoPch.isEmpty()) {
            Alert a = new Alert(Alert.AlertType.ERROR, "Ese numero y codigo NO corresponden a ningun documento emitido por "
                    + "este sistema. El documento puede estar alterado o no ser oficial.", ButtonType.OK);
            a.setHeaderText("No verificado");
            a.showAndWait();
            return;
        }
        CphDocumentoEmitido d = encontradoPch.get();
        boolean vigentePch = d.getEstado() == CphEstadoDocumento.EMITIDO;
        Alert a = new Alert(vigentePch ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING,
                d.getTitulo() + " " + d.getCorrelativo() + "\nEmitido el " + d.getFechaEmision().format(FECHA_PCH)
                        + (d.getUsuario() == null ? "" : " por " + d.getUsuario().getNombres())
                        + (d.getDescripcion() == null ? "" : "\n" + d.getDescripcion())
                        + (vigentePch ? "" : "\n\nESTE DOCUMENTO FUE ANULADO: " + d.getObservacion()), ButtonType.OK);
        a.setHeaderText(vigentePch ? "Documento verificado" : "Documento verificado pero ANULADO");
        a.showAndWait();
    }

    @FXML
    public void cphOnCerrar() {
        tabla.fireEvent(new RyeCerrarVistaEvent());
    }

    private void cphAviso(String mensajePch) {
        new Alert(Alert.AlertType.WARNING, mensajePch, ButtonType.OK).showAndWait();
    }
}
