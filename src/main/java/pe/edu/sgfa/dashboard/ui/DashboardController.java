package pe.edu.sgfa.dashboard.ui;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;

import pe.edu.sgfa.dashboard.service.DashboardService;
import pe.edu.sgfa.dashboard.service.ResumenDashboard;
import pe.edu.sgfa.institucion.service.InstitucionService;
import pe.edu.sgfa.shell.CerrarVistaEvent;
import pe.edu.sgfa.shell.NavigationService;
import pe.edu.sgfa.shell.ThemeService;
import pe.edu.sgfa.shell.VistaHost;
import pe.edu.sgfa.usuario.domain.RolUsuario;
import pe.edu.sgfa.usuario.service.AuthService;
import pe.edu.sgfa.usuario.service.SesionActual;

@Component
public class DashboardController implements VistaHost {

    private final DashboardService dashboardService;
    private final InstitucionService institucionService;
    private final SesionActual sesionActual;
    private final AuthService authService;
    private final NavigationService navigationService;
    private final ThemeService themeService;

    @FXML
    private Label institucionLabel;
    @FXML
    private Label sesionLabel;
    @FXML
    private HBox tarjetasBox;
    @FXML
    private StackPane contenido;
    @FXML
    private VBox inicioBox;
    @FXML
    private Button volverButton;
    @FXML
    private Button estudiantesButton;
    @FXML
    private Button catalogosButton;
    @FXML
    private Button importarButton;
    @FXML
    private Label adminTitle;
    @FXML
    private Button usuariosButton;
    @FXML
    private Button institucionButton;
    @FXML
    private Button meritoButton;
    @FXML
    private Button reportesButton;
    @FXML
    private javafx.scene.layout.StackPane marcaLogo;
    @FXML
    private Label marcaLetra;
    @FXML
    private javafx.scene.image.ImageView marcaImagen;
    @FXML
    private Button backupButton;
    @FXML
    private Button auditoriaButton;
    @FXML
    private Button temaButton;

    private final Deque<Node> vistasAnteriores = new ArrayDeque<>();
    private final Deque<String> titulosAnteriores = new ArrayDeque<>();
    private String nombreInstitucion = "SGFA";
    private String saludo = "";
    private String tituloActual;
    private Button seccionActiva;

    public DashboardController(DashboardService dashboardService, InstitucionService institucionService,
                                SesionActual sesionActual, AuthService authService, NavigationService navigationService,
                                ThemeService themeService, pe.edu.sgfa.shell.IdentidadVisual identidadVisual) {
        this.identidadVisual = identidadVisual;
        this.dashboardService = dashboardService;
        this.institucionService = institucionService;
        this.sesionActual = sesionActual;
        this.authService = authService;
        this.navigationService = navigationService;
        this.themeService = themeService;
    }

    private final pe.edu.sgfa.shell.IdentidadVisual identidadVisual;

    private javafx.beans.value.ChangeListener<javafx.scene.image.Image> alCambiarLogo;

    @FXML
    public void initialize() {
        mostrarLogoEnLaMarca(identidadVisual.logoProperty().get());
        alCambiarLogo = (obs, antes, ahora) -> mostrarLogoEnLaMarca(ahora);
        identidadVisual.logoProperty().addListener(new javafx.beans.value.WeakChangeListener<>(alCambiarLogo));
        actualizarNombreInstitucion();
        navigationService.registrarHost(this);
        contenido.addEventHandler(CerrarVistaEvent.CERRAR_VISTA, e -> {
            volver();
            e.consume();
        });
        mostrarCabeceraInicio();
        actualizarTextoTema();
        themeService.oscuroProperty().addListener((obs, antes, ahora) -> actualizarTextoTema());

        sesionActual.obtenerUsuarioActual().ifPresent(usuario -> {
            saludo = "Bienvenido, " + usuario.getNombres() + " (" + usuario.getRol() + ")";
            sesionLabel.setText(saludo);
            boolean esAdmn = usuario.getRol() == RolUsuario.ADMIN;
            adminTitle.setVisible(esAdmn);
            adminTitle.setManaged(esAdmn);
            usuariosButton.setVisible(esAdmn);
            usuariosButton.setManaged(esAdmn);
            institucionButton.setVisible(esAdmn);
            institucionButton.setManaged(esAdmn);
            backupButton.setVisible(esAdmn);
            backupButton.setManaged(esAdmn);
            auditoriaButton.setVisible(esAdmn);
            auditoriaButton.setManaged(esAdmn);
        });

        cargarResumen();
    }

    private void mostrarLogoEnLaMarca(javafx.scene.image.Image logo) {
        boolean hayLg = logo != null;
        marcaImagen.setImage(logo);
        marcaImagen.setVisible(hayLg);
        marcaImagen.setManaged(hayLg);
        marcaLetra.setVisible(!hayLg);
        marcaLetra.setManaged(!hayLg);
        marcaLogo.getStyleClass().remove("dash-marca-logo-imagen");
        if (hayLg) {
            marcaLogo.getStyleClass().add("dash-marca-logo-imagen");
        }
    }

    private void cargarResumen() {
        ResumenDashboard rsmn = dashboardService.obtenerResumen();
        tarjetasBox.getChildren().setAll(
                tarjeta("◈", "Estudiantes", String.valueOf(rsmn.totalEstudiantes()), 0),
                tarjeta("✓", "Estudiantes activos", String.valueOf(rsmn.estudiantesActivos()), 1),
                tarjeta("▤", "Programas", String.valueOf(rsmn.totalProgramas()), 2),
                tarjeta("▥", "Planes de estudio", String.valueOf(rsmn.totalPlanes()), 3),
                tarjeta("◴", "Periodo actual", rsmn.periodoActualNombre() == null ? "-" : rsmn.periodoActualNombre(), 4));
    }

    private VBox tarjeta(String icn, String ttl, String vlr, int indc) {
        Label icnLbl = new Label(icn);
        icnLbl.getStyleClass().add("dash-kpi-icon-glyph");
        StackPane icnBox = new StackPane(icnLbl);
        icnBox.getStyleClass().add("dash-kpi-icon");

        Label vlrLbl = new Label(vlr);
        vlrLbl.getStyleClass().add("dash-kpi-value");
        Label ttlLbl = new Label(ttl);
        ttlLbl.getStyleClass().add("dash-kpi-title");

        VBox cj = new VBox(10, icnBox, vlrLbl, ttlLbl);
        cj.getStyleClass().add("dash-card");
        HBox.setHgrow(cj, Priority.ALWAYS);
        cj.setMaxWidth(Double.MAX_VALUE);
        cj.setPrefWidth(160);
        cj.setOpacity(0);

        Duration rtrs = Duration.millis(90L * indc);
        FadeTransition fd = new FadeTransition(Duration.millis(450), cj);
        fd.setFromValue(0);
        fd.setToValue(1);
        fd.setDelay(rtrs);
        TranslateTransition sbr = new TranslateTransition(Duration.millis(450), cj);
        sbr.setFromY(18);
        sbr.setToY(0);
        sbr.setDelay(rtrs);
        sbr.setInterpolator(Interpolator.EASE_OUT);
        fd.play();
        sbr.play();

        if (vlr.matches("\\d{1,9}")) {
            int dstn = Integer.parseInt(vlr);
            SimpleIntegerProperty cntdr = new SimpleIntegerProperty(0);
            cntdr.addListener((obs, antes, ahora) -> vlrLbl.setText(String.valueOf(ahora.intValue())));
            vlrLbl.setText("0");
            Timeline cnt = new Timeline(new KeyFrame(Duration.millis(900),
                    new KeyValue(cntdr, dstn, Interpolator.EASE_OUT)));
            cnt.setDelay(rtrs);
            cnt.play();
        }
        return cj;
    }

    @Override
    public void mostrar(Parent vst, String ttl) {
        StackPane tarjeta = new StackPane(vst);
        tarjeta.getStyleClass().add("dash-view-card");
        vistasAnteriores.push(contenido.getChildren().get(0));
        titulosAnteriores.push(tituloActual == null ? "" : tituloActual);
        contenido.getChildren().setAll(tarjeta);

        vst.applyCss();
        tituloActual = ttl;
        institucionLabel.setText(ttl);
        sesionLabel.setText(nombreInstitucion);
        volverButton.setVisible(true);
        volverButton.setManaged(true);
    }

    @Override
    public void volver() {
        if (vistasAnteriores.isEmpty()) {
            return;
        }
        contenido.getChildren().setAll(vistasAnteriores.pop());
        tituloActual = titulosAnteriores.pop();
        if (vistasAnteriores.isEmpty()) {
            irAInicio();
            actualizarNombreInstitucion();
            cargarResumen();
            mostrarCabeceraInicio();
        } else {
            institucionLabel.setText(tituloActual);
        }
    }

    @FXML
    public void onVolver() {
        volver();
    }

    private void irAInicio() {
        vistasAnteriores.clear();
        titulosAnteriores.clear();
        tituloActual = null;
        contenido.getChildren().setAll(inicioBox);
        marcarSeccion(null);
        volverButton.setVisible(false);
        volverButton.setManaged(false);
    }

    private void mostrarCabeceraInicio() {
        institucionLabel.setText(nombreInstitucion);
        sesionLabel.setText(saludo);
        volverButton.setVisible(false);
        volverButton.setManaged(false);
    }

    private void actualizarNombreInstitucion() {
        nombreInstitucion = institucionService.obtenerPrincipal().map(i -> i.getNombre()).orElse("SGFA");
    }

    private void marcarSeccion(Button btn) {
        if (seccionActiva != null) {
            seccionActiva.getStyleClass().remove("dash-nav-button-active");
        }
        seccionActiva = btn;
        if (btn != null) {
            btn.getStyleClass().add("dash-nav-button-active");
        }
    }

    private void abrirSeccion(Button btn, String fxml, String ttl) {
        irAInicio();
        marcarSeccion(btn);
        navigationService.abrirVista(fxml, ttl);
    }

    @FXML
    public void onLogout() {
        vistasAnteriores.clear();
        titulosAnteriores.clear();
        authService.cerrarSesion();
        navigationService.mostrarLogin();
    }

    @FXML
    public void onGestionarUsuarios() {
        abrirSeccion(usuariosButton, "/fxml/usuario/usuarios.fxml", "Gestion de usuarios");
    }

    @FXML
    public void onConfigurarInstitucion() {
        abrirSeccion(institucionButton, "/fxml/institucion/institucion.fxml", "Configuracion institucional");
    }

    @FXML
    public void onCatalogosAcademicos() {
        abrirSeccion(catalogosButton, "/fxml/programa/catalogos.fxml", "Catalogos academicos");
    }

    @FXML
    public void onEstudiantes() {
        abrirSeccion(estudiantesButton, "/fxml/estudiante/estudiantes.fxml", "Estudiantes");
    }

    @FXML
    public void onImportarExcel() {
        abrirSeccion(importarButton, "/fxml/importacion/importacion.fxml", "Importar desde Excel");
    }

    @FXML
    public void onOrdenMerito() {

        irAInicio();
        marcarSeccion(meritoButton);
        navigationService.abrirVista("/fxml/reporte/reportes.fxml", "Orden de merito",
                controlador -> ((pe.edu.sgfa.reporte.ui.ReportesController) controlador).elegirOrdenMerito());
    }

    @FXML
    public void onReportes() {
        abrirSeccion(reportesButton, "/fxml/reporte/reportes.fxml", "Reportes");
    }

    @FXML
    public void onRespaldo() {
        abrirSeccion(backupButton, "/fxml/backup/backup.fxml", "Respaldo y restauracion");
    }

    @FXML
    public void onAuditoria() {
        abrirSeccion(auditoriaButton, "/fxml/auditoria/auditoria.fxml", "Auditoria");
    }

    @FXML
    public void onAlternarTema() {
        themeService.alternar();
    }

    private void actualizarTextoTema() {
        temaButton.setText(themeService.isOscuro() ? "☀   Modo claro" : "🌙   Modo oscuro");
    }
}
