package pe.edu.sgfa.auditoria.ui;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.StringConverter;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import pe.edu.sgfa.shell.RyeCerrarVistaEvent;
import pe.edu.sgfa.auditoria.domain.RahpAuditoria;
import pe.edu.sgfa.auditoria.service.RahpAuditoriaService;
import pe.edu.sgfa.usuario.domain.RyeUsuario;
import pe.edu.sgfa.usuario.service.RyeUsuarioService;

@Component
public class RahpAuditoriaController {

    private static final int TAMANO_PAGINA_HNC = 50;
    private static final DateTimeFormatter FORMATO_FECHA_HNC = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final RahpAuditoriaService auditoriaServiceHnc;
    private final RyeUsuarioService usuarioServiceHnc;

    private Map<Long, String> nombresPorUsuarioIdHnc = new HashMap<>();
    private int paginaActualHnc = 0;
    private Page<RahpAuditoria> paginaCargadaHnc;

    @FXML
    private DatePicker desdePicker;
    @FXML
    private DatePicker hastaPicker;
    @FXML
    private ComboBox<RyeUsuario> usuarioCombo;
    @FXML
    private ComboBox<String> accionCombo;
    @FXML
    private ComboBox<String> entidadCombo;
    @FXML
    private TableView<RahpAuditoria> tabla;
    @FXML
    private TableColumn<RahpAuditoria, String> colFecha;
    @FXML
    private TableColumn<RahpAuditoria, String> colUsuario;
    @FXML
    private TableColumn<RahpAuditoria, String> colAccion;
    @FXML
    private TableColumn<RahpAuditoria, String> colEntidad;
    @FXML
    private TableColumn<RahpAuditoria, String> colEntidadId;
    @FXML
    private TableColumn<RahpAuditoria, String> colDetalle;
    @FXML
    private Label paginaLabel;
    @FXML
    private Button anteriorButton;
    @FXML
    private Button siguienteButton;

    public RahpAuditoriaController(RahpAuditoriaService auditoriaServiceHnc, RyeUsuarioService usuarioServiceHnc) {
        this.auditoriaServiceHnc = auditoriaServiceHnc;
        this.usuarioServiceHnc = usuarioServiceHnc;
    }

    @FXML
    public void initialize() {
        colFecha.setCellValueFactory(d -> new SimpleStringProperty(FORMATO_FECHA_HNC.format(d.getValue().getFecha())));
        colUsuario.setCellValueFactory(d -> new SimpleStringProperty(rahpNombreDeUsuario(d.getValue().getUsuarioId())));
        colAccion.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAccion()));
        colEntidad.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getEntidad() == null ? "" : d.getValue().getEntidad()));
        colEntidadId.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getEntidadId() == null ? "" : d.getValue().getEntidadId()));
        colDetalle.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getDetalle() == null ? "" : d.getValue().getDetalle()));

        var usuariosHnc = usuarioServiceHnc.ryeListar();
        nombresPorUsuarioIdHnc = new HashMap<>();
        usuariosHnc.forEach(u -> nombresPorUsuarioIdHnc.put(u.getId(), u.getUsername() + " (" + u.getNombres() + ")"));

        usuarioCombo.setConverter(rahpConvertidor(u -> u.getUsername() + " (" + u.getNombres() + ")", "Todos"));
        usuarioCombo.getItems().add(null);
        usuarioCombo.getItems().addAll(usuariosHnc);

        accionCombo.setConverter(rahpConvertidor(a -> a, "Todas"));
        accionCombo.getItems().add(null);
        accionCombo.getItems().addAll(auditoriaServiceHnc.rahpListarAcciones());

        entidadCombo.setConverter(rahpConvertidor(e -> e, "Todas"));
        entidadCombo.getItems().add(null);
        entidadCombo.getItems().addAll(auditoriaServiceHnc.rahpListarEntidades());

        rahpBuscar(0);
    }

    @FXML
    public void rahpOnBuscar() {
        rahpBuscar(0);
    }

    @FXML
    public void rahpOnLimpiar() {
        desdePicker.setValue(null);
        hastaPicker.setValue(null);
        usuarioCombo.getSelectionModel().selectFirst();
        accionCombo.getSelectionModel().selectFirst();
        entidadCombo.getSelectionModel().selectFirst();
        rahpBuscar(0);
    }

    @FXML
    public void rahpOnAnterior() {
        if (paginaCargadaHnc != null && paginaCargadaHnc.hasPrevious()) {
            rahpBuscar(paginaActualHnc - 1);
        }
    }

    @FXML
    public void rahpOnSiguiente() {
        if (paginaCargadaHnc != null && paginaCargadaHnc.hasNext()) {
            rahpBuscar(paginaActualHnc + 1);
        }
    }

    @FXML
    public void rahpOnCerrar() {
        tabla.fireEvent(new RyeCerrarVistaEvent());
    }

    private void rahpBuscar(int paginaHnc) {
        var desdeHnc = desdePicker.getValue() == null ? null
                : desdePicker.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant();
        var hastaHnc = hastaPicker.getValue() == null ? null
                : hastaPicker.getValue().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().minusNanos(1);
        Long usuarioIdHnc = usuarioCombo.getValue() == null ? null : usuarioCombo.getValue().getId();
        String accionHnc = accionCombo.getValue();
        String entidadHnc = entidadCombo.getValue();

        paginaCargadaHnc = auditoriaServiceHnc.rahpBuscar(desdeHnc, hastaHnc, usuarioIdHnc, accionHnc, entidadHnc, paginaHnc, TAMANO_PAGINA_HNC);
        paginaActualHnc = paginaHnc;
        tabla.getItems().setAll(paginaCargadaHnc.getContent());

        int totalPaginasHnc = Math.max(paginaCargadaHnc.getTotalPages(), 1);
        paginaLabel.setText("Pagina " + (paginaActualHnc + 1) + " de " + totalPaginasHnc
                + " (" + paginaCargadaHnc.getTotalElements() + " registros)");
        anteriorButton.setDisable(!paginaCargadaHnc.hasPrevious());
        siguienteButton.setDisable(!paginaCargadaHnc.hasNext());
    }

    private String rahpNombreDeUsuario(Long usuarioIdHnc) {
        if (usuarioIdHnc == null) {
            return "(sistema)";
        }
        return nombresPorUsuarioIdHnc.getOrDefault(usuarioIdHnc, "Usuario #" + usuarioIdHnc);
    }

    private <T> StringConverter<T> rahpConvertidor(java.util.function.Function<T, String> textoDeHnc, String textoParaNuloHnc) {
        return new StringConverter<>() {
            @Override
            public String toString(T objetoHnc) {
                return objetoHnc == null ? textoParaNuloHnc : textoDeHnc.apply(objetoHnc);
            }

            @Override
            public T fromString(String s) {
                return null;
            }
        };
    }
}
