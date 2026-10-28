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

import pe.edu.sgfa.shell.CerrarVistaEvent;
import pe.edu.sgfa.auditoria.domain.Auditoria;
import pe.edu.sgfa.auditoria.service.AuditoriaService;
import pe.edu.sgfa.usuario.domain.Usuario;
import pe.edu.sgfa.usuario.service.UsuarioService;

@Component
public class AuditoriaController {

    private static final int TAMANO_PAGINA = 50;
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final AuditoriaService auditoriaService;
    private final UsuarioService usuarioService;

    private Map<Long, String> nombresPorUsuarioId = new HashMap<>();
    private int paginaActual = 0;
    private Page<Auditoria> paginaCargada;

    @FXML
    private DatePicker desdePicker;
    @FXML
    private DatePicker hastaPicker;
    @FXML
    private ComboBox<Usuario> usuarioCombo;
    @FXML
    private ComboBox<String> accionCombo;
    @FXML
    private ComboBox<String> entidadCombo;
    @FXML
    private TableView<Auditoria> tabla;
    @FXML
    private TableColumn<Auditoria, String> colFecha;
    @FXML
    private TableColumn<Auditoria, String> colUsuario;
    @FXML
    private TableColumn<Auditoria, String> colAccion;
    @FXML
    private TableColumn<Auditoria, String> colEntidad;
    @FXML
    private TableColumn<Auditoria, String> colEntidadId;
    @FXML
    private TableColumn<Auditoria, String> colDetalle;
    @FXML
    private Label paginaLabel;
    @FXML
    private Button anteriorButton;
    @FXML
    private Button siguienteButton;

    public AuditoriaController(AuditoriaService auditoriaService, UsuarioService usuarioService) {
        this.auditoriaService = auditoriaService;
        this.usuarioService = usuarioService;
    }

    @FXML
    public void initialize() {
        colFecha.setCellValueFactory(d -> new SimpleStringProperty(FORMATO_FECHA.format(d.getValue().getFecha())));
        colUsuario.setCellValueFactory(d -> new SimpleStringProperty(nombreDeUsuario(d.getValue().getUsuarioId())));
        colAccion.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAccion()));
        colEntidad.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getEntidad() == null ? "" : d.getValue().getEntidad()));
        colEntidadId.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getEntidadId() == null ? "" : d.getValue().getEntidadId()));
        colDetalle.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getDetalle() == null ? "" : d.getValue().getDetalle()));

        var usrs = usuarioService.listar();
        nombresPorUsuarioId = new HashMap<>();
        usrs.forEach(u -> nombresPorUsuarioId.put(u.getId(), u.getUsername() + " (" + u.getNombres() + ")"));

        usuarioCombo.setConverter(convertidor(u -> u.getUsername() + " (" + u.getNombres() + ")", "Todos"));
        usuarioCombo.getItems().add(null);
        usuarioCombo.getItems().addAll(usrs);

        accionCombo.setConverter(convertidor(a -> a, "Todas"));
        accionCombo.getItems().add(null);
        accionCombo.getItems().addAll(auditoriaService.listarAcciones());

        entidadCombo.setConverter(convertidor(e -> e, "Todas"));
        entidadCombo.getItems().add(null);
        entidadCombo.getItems().addAll(auditoriaService.listarEntidades());

        buscar(0);
    }

    @FXML
    public void onBuscar() {
        buscar(0);
    }

    @FXML
    public void onLimpiar() {
        desdePicker.setValue(null);
        hastaPicker.setValue(null);
        usuarioCombo.getSelectionModel().selectFirst();
        accionCombo.getSelectionModel().selectFirst();
        entidadCombo.getSelectionModel().selectFirst();
        buscar(0);
    }

    @FXML
    public void onAnterior() {
        if (paginaCargada != null && paginaCargada.hasPrevious()) {
            buscar(paginaActual - 1);
        }
    }

    @FXML
    public void onSiguiente() {
        if (paginaCargada != null && paginaCargada.hasNext()) {
            buscar(paginaActual + 1);
        }
    }

    @FXML
    public void onCerrar() {
        tabla.fireEvent(new CerrarVistaEvent());
    }

    private void buscar(int pgn) {
        var dsd = desdePicker.getValue() == null ? null
                : desdePicker.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant();
        var hst = hastaPicker.getValue() == null ? null
                : hastaPicker.getValue().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().minusNanos(1);
        Long usrId = usuarioCombo.getValue() == null ? null : usuarioCombo.getValue().getId();
        String accn = accionCombo.getValue();
        String entdd = entidadCombo.getValue();

        paginaCargada = auditoriaService.buscar(dsd, hst, usrId, accn, entdd, pgn, TAMANO_PAGINA);
        paginaActual = pgn;
        tabla.getItems().setAll(paginaCargada.getContent());

        int ttlPgns = Math.max(paginaCargada.getTotalPages(), 1);
        paginaLabel.setText("Pagina " + (paginaActual + 1) + " de " + ttlPgns
                + " (" + paginaCargada.getTotalElements() + " registros)");
        anteriorButton.setDisable(!paginaCargada.hasPrevious());
        siguienteButton.setDisable(!paginaCargada.hasNext());
    }

    private String nombreDeUsuario(Long usrId) {
        if (usrId == null) {
            return "(sistema)";
        }
        return nombresPorUsuarioId.getOrDefault(usrId, "Usuario #" + usrId);
    }

    private <T> StringConverter<T> convertidor(java.util.function.Function<T, String> textoDe, String txtPrNl) {
        return new StringConverter<>() {
            @Override
            public String toString(T objt) {
                return objt == null ? txtPrNl : textoDe.apply(objt);
            }

            @Override
            public T fromString(String s) {
                return null;
            }
        };
    }
}
