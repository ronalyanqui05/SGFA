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

import pe.edu.sgfa.common.exception.SgfaException;
import pe.edu.sgfa.reporte.domain.DocumentoEmitido;
import pe.edu.sgfa.reporte.domain.EstadoDocumento;
import pe.edu.sgfa.reporte.service.DocumentoOficialService;
import pe.edu.sgfa.shell.CerrarVistaEvent;
import pe.edu.sgfa.shell.NavigationService;
import pe.edu.sgfa.usuario.domain.RolUsuario;
import pe.edu.sgfa.usuario.service.SesionActual;

@Component
@Scope("prototype")
public class DocumentosEmitidosController {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final DocumentoOficialService servicio;
    private final SesionActual sesionActual;
    private final NavigationService navigationService;

    @FXML
    private TableView<DocumentoEmitido> tabla;
    @FXML
    private Button anularButton;
    @FXML
    private Label estadoLabel;

    public DocumentosEmitidosController(DocumentoOficialService servicio, SesionActual sesionActual,
                                         NavigationService navigationService) {
        this.servicio = servicio;
        this.sesionActual = sesionActual;
        this.navigationService = navigationService;
    }

    @FXML
    public void initialize() {
        tabla.getColumns().add(texto("Numero", 150, d -> d.getCorrelativo()));
        tabla.getColumns().add(texto("Documento", 170, DocumentoEmitido::getTitulo));
        tabla.getColumns().add(texto("Detalle", 430, d -> d.getDescripcion() == null ? "" : d.getDescripcion()));
        tabla.getColumns().add(texto("Fecha", 90, d -> d.getFechaEmision().format(FECHA)));
        tabla.getColumns().add(texto("Emitido por", 160, d -> d.getUsuario() == null ? "" : d.getUsuario().getNombres()));

        TableColumn<DocumentoEmitido, String> estd = texto("Estado", 90, d -> d.getEstado().name());
        estd.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String vlr, boolean vc) {
                super.updateItem(vlr, vc);
                setText(vc ? null : vlr);
                setStyle(vc ? "" : "-fx-font-weight: bold; -fx-text-fill: "
                        + (EstadoDocumento.ANULADO.name().equals(vlr) ? "#c62828" : "#1e8e3e") + ";");
            }
        });
        tabla.getColumns().add(estd);
        tabla.getColumns().add(texto("Motivo de anulacion", 200, d -> d.getObservacion() == null ? "" : d.getObservacion()));

        boolean admn = sesionActual.obtenerUsuarioActual().map(u -> u.getRol() == RolUsuario.ADMIN).orElse(false);
        anularButton.setVisible(admn);
        anularButton.setManaged(admn);
        tabla.setPlaceholder(new Label("Aun no se emitio ningun documento oficial."));
        recargar();
    }

    private TableColumn<DocumentoEmitido, String> texto(String ttl, double anch,
                                                         java.util.function.Function<DocumentoEmitido, String> vlr) {
        TableColumn<DocumentoEmitido, String> col = new TableColumn<>(ttl);
        col.setCellValueFactory(d -> new ReadOnlyStringWrapper(vlr.apply(d.getValue())));
        col.setPrefWidth(anch);
        return col;
    }

    private void recargar() {
        tabla.getItems().setAll(servicio.listar());
        estadoLabel.setText(tabla.getItems().size() + " documento(s) emitido(s).");
    }

    @FXML
    public void onActualizar() {
        recargar();
    }

    @FXML
    public void onAbrir() {
        DocumentoEmitido d = tabla.getSelectionModel().getSelectedItem();
        if (d == null) {
            aviso("Seleccione un documento de la lista.");
            return;
        }
        File archivo = servicio.archivo(d).toFile();
        if (!archivo.isFile()) {
            aviso("No se encontro el archivo del documento (" + archivo + "). Puede haberse movido o borrado.");
            return;
        }
        try {
            byte[] pdf = java.nio.file.Files.readAllBytes(archivo.toPath());
            navigationService.abrirVista("/fxml/reporte/vista-previa-pdf.fxml", d.getTitulo() + " " + d.getCorrelativo(),
                    controlador -> ((VistaPreviaPdfController) controlador).setDatos(pdf, archivo.getName()));
        } catch (java.io.IOException e) {
            aviso("No se pudo abrir el PDF. Esta en:\n" + archivo);
        }
    }

    @FXML
    public void onAnular() {
        DocumentoEmitido d = tabla.getSelectionModel().getSelectedItem();
        if (d == null) {
            aviso("Seleccione un documento de la lista.");
            return;
        }
        if (d.getEstado() == EstadoDocumento.ANULADO) {
            aviso("Ese documento ya esta anulado.");
            return;
        }
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Anular documento");
        dlg.setHeaderText("Anular " + d.getCorrelativo());
        dlg.setContentText("Motivo de la anulacion:");
        Optional<String> mtv = dlg.showAndWait();
        if (mtv.isEmpty()) {
            return;
        }
        try {
            servicio.anular(d.getId(), mtv.get());
            recargar();
        } catch (SgfaException e) {
            aviso(e.getMessage());
        }
    }

    @FXML
    public void onVerificar() {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Verificar documento");
        dlg.setHeaderText("Escriba el numero y el codigo de verificacion impresos en el documento.");
        TextField nmr = new TextField();
        nmr.setPromptText("Ej. OM-000001-2026");
        TextField cdg = new TextField();
        cdg.setPromptText("Ej. A1B2-C3D4-E5F6");
        GridPane pnl = new GridPane();
        pnl.setHgap(8);
        pnl.setVgap(8);
        pnl.addRow(0, new Label("Numero"), nmr);
        pnl.addRow(1, new Label("Codigo"), cdg);
        dlg.getDialogPane().setContent(pnl);
        dlg.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        if (dlg.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        Optional<DocumentoEmitido> encntrd = servicio.verificar(nmr.getText(), cdg.getText());
        if (encntrd.isEmpty()) {
            Alert a = new Alert(Alert.AlertType.ERROR, "Ese numero y codigo NO corresponden a ningun documento emitido por "
                    + "este sistema. El documento puede estar alterado o no ser oficial.", ButtonType.OK);
            a.setHeaderText("No verificado");
            a.showAndWait();
            return;
        }
        DocumentoEmitido d = encntrd.get();
        boolean vgnt = d.getEstado() == EstadoDocumento.EMITIDO;
        Alert a = new Alert(vgnt ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING,
                d.getTitulo() + " " + d.getCorrelativo() + "\nEmitido el " + d.getFechaEmision().format(FECHA)
                        + (d.getUsuario() == null ? "" : " por " + d.getUsuario().getNombres())
                        + (d.getDescripcion() == null ? "" : "\n" + d.getDescripcion())
                        + (vgnt ? "" : "\n\nESTE DOCUMENTO FUE ANULADO: " + d.getObservacion()), ButtonType.OK);
        a.setHeaderText(vgnt ? "Documento verificado" : "Documento verificado pero ANULADO");
        a.showAndWait();
    }

    @FXML
    public void onCerrar() {
        tabla.fireEvent(new CerrarVistaEvent());
    }

    private void aviso(String mnsj) {
        new Alert(Alert.AlertType.WARNING, mnsj, ButtonType.OK).showAndWait();
    }
}
