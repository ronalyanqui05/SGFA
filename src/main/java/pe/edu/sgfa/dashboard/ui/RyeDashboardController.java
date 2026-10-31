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

import pe.edu.sgfa.dashboard.service.RyeDashboardService;
import pe.edu.sgfa.dashboard.service.RyeResumenDashboard;
import pe.edu.sgfa.institucion.service.RacaInstitucionService;
import pe.edu.sgfa.shell.RyeCerrarVistaEvent;
import pe.edu.sgfa.shell.RyeNavigationService;
import pe.edu.sgfa.shell.RyeThemeService;
import pe.edu.sgfa.shell.RyeVistaHost;
import pe.edu.sgfa.usuario.domain.RyeRolUsuario;
import pe.edu.sgfa.usuario.service.RyeAuthService;
import pe.edu.sgfa.usuario.service.RyeSesionActual;

@Component
public class RyeDashboardController implements RyeVistaHost {

    private final RyeDashboardService dashboardServiceYnq;
    private final RacaInstitucionService institucionServiceYnq;
    private final RyeSesionActual sesionActualYnq;
    private final RyeAuthService authServiceYnq;
    private final RyeNavigationService navigationServiceYnq;
    private final RyeThemeService themeServiceYnq;

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

    private final Deque<Node> vistasAnterioresYnq = new ArrayDeque<>();
    private final Deque<String> titulosAnterioresYnq = new ArrayDeque<>();
    private String nombreInstitucionYnq = "SGFA";
    private String saludoYnq = "";
    private String tituloActualYnq;
    private Button seccionActivaYnq;

    public RyeDashboardController(RyeDashboardService dashboardServiceYnq, RacaInstitucionService institucionServiceYnq,
                                RyeSesionActual sesionActualYnq, RyeAuthService authServiceYnq, RyeNavigationService navigationServiceYnq,
                                RyeThemeService themeServiceYnq, pe.edu.sgfa.shell.RyeIdentidadVisual identidadVisualYnq) {
        this.identidadVisualYnq = identidadVisualYnq;
        this.dashboardServiceYnq = dashboardServiceYnq;
        this.institucionServiceYnq = institucionServiceYnq;
        this.sesionActualYnq = sesionActualYnq;
        this.authServiceYnq = authServiceYnq;
        this.navigationServiceYnq = navigationServiceYnq;
        this.themeServiceYnq = themeServiceYnq;
    }

    private final pe.edu.sgfa.shell.RyeIdentidadVisual identidadVisualYnq;

    private javafx.beans.value.ChangeListener<javafx.scene.image.Image> alCambiarLogoYnq;

    @FXML
    public void initialize() {
        ryeMostrarLogoEnLaMarca(identidadVisualYnq.logoProperty().get());
        alCambiarLogoYnq = (obsYnq, antesYnq, ahoraYnq) -> ryeMostrarLogoEnLaMarca(ahoraYnq);
        identidadVisualYnq.logoProperty().addListener(new javafx.beans.value.WeakChangeListener<>(alCambiarLogoYnq));
        ryeActualizarNombreInstitucion();
        navigationServiceYnq.ryeRegistrarHost(this);
        contenido.addEventHandler(RyeCerrarVistaEvent.CERRAR_VISTA, e -> {
            ryeVolver();
            e.consume();
        });
        ryeMostrarCabeceraInicio();
        ryeActualizarTextoTema();
        themeServiceYnq.oscuroProperty().addListener((obsYnq, antesYnq, ahoraYnq) -> ryeActualizarTextoTema());

        sesionActualYnq.ryeObtenerUsuarioActual().ifPresent(usuarioYnq -> {
            saludoYnq = "Bienvenido, " + usuarioYnq.getNombres() + " (" + usuarioYnq.getRol() + ")";
            sesionLabel.setText(saludoYnq);
            boolean esAdminYnq = usuarioYnq.getRol() == RyeRolUsuario.ADMIN;
            adminTitle.setVisible(esAdminYnq);
            adminTitle.setManaged(esAdminYnq);
            usuariosButton.setVisible(esAdminYnq);
            usuariosButton.setManaged(esAdminYnq);
            institucionButton.setVisible(esAdminYnq);
            institucionButton.setManaged(esAdminYnq);
            backupButton.setVisible(esAdminYnq);
            backupButton.setManaged(esAdminYnq);
            auditoriaButton.setVisible(esAdminYnq);
            auditoriaButton.setManaged(esAdminYnq);
        });

        ryeCargarResumen();
    }

    private void ryeMostrarLogoEnLaMarca(javafx.scene.image.Image logoYnq) {
        boolean hayLogoYnq = logoYnq != null;
        marcaImagen.setImage(logoYnq);
        marcaImagen.setVisible(hayLogoYnq);
        marcaImagen.setManaged(hayLogoYnq);
        marcaLetra.setVisible(!hayLogoYnq);
        marcaLetra.setManaged(!hayLogoYnq);
        marcaLogo.getStyleClass().remove("dash-marca-logo-imagen");
        if (hayLogoYnq) {
            marcaLogo.getStyleClass().add("dash-marca-logo-imagen");
        }
    }

    private void ryeCargarResumen() {
        RyeResumenDashboard resumenYnq = dashboardServiceYnq.ryeObtenerResumen();
        tarjetasBox.getChildren().setAll(
                ryeTarjeta("◈", "Estudiantes", String.valueOf(resumenYnq.totalEstudiantes()), 0),
                ryeTarjeta("✓", "Estudiantes activos", String.valueOf(resumenYnq.estudiantesActivos()), 1),
                ryeTarjeta("▤", "Programas", String.valueOf(resumenYnq.totalProgramas()), 2),
                ryeTarjeta("▥", "Planes de estudio", String.valueOf(resumenYnq.totalPlanes()), 3),
                ryeTarjeta("◴", "Periodo actual", resumenYnq.periodoActualNombre() == null ? "-" : resumenYnq.periodoActualNombre(), 4));
    }

    private VBox ryeTarjeta(String iconoYnq, String tituloYnq, String valorYnq, int indiceYnq) {
        Label iconoLabelYnq = new Label(iconoYnq);
        iconoLabelYnq.getStyleClass().add("dash-kpi-icon-glyph");
        StackPane iconoBoxYnq = new StackPane(iconoLabelYnq);
        iconoBoxYnq.getStyleClass().add("dash-kpi-icon");

        Label valorLabelYnq = new Label(valorYnq);
        valorLabelYnq.getStyleClass().add("dash-kpi-value");
        Label tituloLabelYnq = new Label(tituloYnq);
        tituloLabelYnq.getStyleClass().add("dash-kpi-title");

        VBox cajaYnq = new VBox(10, iconoBoxYnq, valorLabelYnq, tituloLabelYnq);
        cajaYnq.getStyleClass().add("dash-card");
        HBox.setHgrow(cajaYnq, Priority.ALWAYS);
        cajaYnq.setMaxWidth(Double.MAX_VALUE);
        cajaYnq.setPrefWidth(160);
        cajaYnq.setOpacity(0);

        Duration retrasoYnq = Duration.millis(90L * indiceYnq);
        FadeTransition fadeYnq = new FadeTransition(Duration.millis(450), cajaYnq);
        fadeYnq.setFromValue(0);
        fadeYnq.setToValue(1);
        fadeYnq.setDelay(retrasoYnq);
        TranslateTransition subirYnq = new TranslateTransition(Duration.millis(450), cajaYnq);
        subirYnq.setFromY(18);
        subirYnq.setToY(0);
        subirYnq.setDelay(retrasoYnq);
        subirYnq.setInterpolator(Interpolator.EASE_OUT);
        fadeYnq.play();
        subirYnq.play();

        if (valorYnq.matches("\\d{1,9}")) {
            int destinoYnq = Integer.parseInt(valorYnq);
            SimpleIntegerProperty contadorYnq = new SimpleIntegerProperty(0);
            contadorYnq.addListener((obsYnq, antesYnq, ahoraYnq) -> valorLabelYnq.setText(String.valueOf(ahoraYnq.intValue())));
            valorLabelYnq.setText("0");
            Timeline conteoYnq = new Timeline(new KeyFrame(Duration.millis(900),
                    new KeyValue(contadorYnq, destinoYnq, Interpolator.EASE_OUT)));
            conteoYnq.setDelay(retrasoYnq);
            conteoYnq.play();
        }
        return cajaYnq;
    }

    @Override
    public void ryeMostrar(Parent vistaYnq, String tituloYnq) {
        StackPane tarjetaYnq = new StackPane(vistaYnq);
        tarjetaYnq.getStyleClass().add("dash-view-card");
        vistasAnterioresYnq.push(contenido.getChildren().get(0));
        titulosAnterioresYnq.push(tituloActualYnq == null ? "" : tituloActualYnq);
        contenido.getChildren().setAll(tarjetaYnq);

        vistaYnq.applyCss();
        tituloActualYnq = tituloYnq;
        institucionLabel.setText(tituloYnq);
        sesionLabel.setText(nombreInstitucionYnq);
        volverButton.setVisible(true);
        volverButton.setManaged(true);
    }

    @Override
    public void ryeVolver() {
        if (vistasAnterioresYnq.isEmpty()) {
            return;
        }
        contenido.getChildren().setAll(vistasAnterioresYnq.pop());
        tituloActualYnq = titulosAnterioresYnq.pop();
        if (vistasAnterioresYnq.isEmpty()) {
            ryeIrAInicio();
            ryeActualizarNombreInstitucion();
            ryeCargarResumen();
            ryeMostrarCabeceraInicio();
        } else {
            institucionLabel.setText(tituloActualYnq);
        }
    }

    @FXML
    public void ryeOnVolver() {
        ryeVolver();
    }

    private void ryeIrAInicio() {
        vistasAnterioresYnq.clear();
        titulosAnterioresYnq.clear();
        tituloActualYnq = null;
        contenido.getChildren().setAll(inicioBox);
        ryeMarcarSeccion(null);
        volverButton.setVisible(false);
        volverButton.setManaged(false);
    }

    private void ryeMostrarCabeceraInicio() {
        institucionLabel.setText(nombreInstitucionYnq);
        sesionLabel.setText(saludoYnq);
        volverButton.setVisible(false);
        volverButton.setManaged(false);
    }

    private void ryeActualizarNombreInstitucion() {
        nombreInstitucionYnq = institucionServiceYnq.racaObtenerPrincipal().map(i -> i.getNombre()).orElse("SGFA");
    }

    private void ryeMarcarSeccion(Button botonYnq) {
        if (seccionActivaYnq != null) {
            seccionActivaYnq.getStyleClass().remove("dash-nav-button-active");
        }
        seccionActivaYnq = botonYnq;
        if (botonYnq != null) {
            botonYnq.getStyleClass().add("dash-nav-button-active");
        }
    }

    private void ryeAbrirSeccion(Button botonYnq, String fxmlYnq, String tituloYnq) {
        ryeIrAInicio();
        ryeMarcarSeccion(botonYnq);
        navigationServiceYnq.ryeAbrirVista(fxmlYnq, tituloYnq);
    }

    @FXML
    public void ryeOnLogout() {
        vistasAnterioresYnq.clear();
        titulosAnterioresYnq.clear();
        authServiceYnq.ryeCerrarSesion();
        navigationServiceYnq.ryeMostrarLogin();
    }

    @FXML
    public void ryeOnGestionarUsuarios() {
        ryeAbrirSeccion(usuariosButton, "/fxml/usuario/usuarios.fxml", "Gestion de usuarios");
    }

    @FXML
    public void ryeOnConfigurarInstitucion() {
        ryeAbrirSeccion(institucionButton, "/fxml/institucion/institucion.fxml", "Configuracion institucional");
    }

    @FXML
    public void ryeOnCatalogosAcademicos() {
        ryeAbrirSeccion(catalogosButton, "/fxml/programa/catalogos.fxml", "Catalogos academicos");
    }

    @FXML
    public void ryeOnEstudiantes() {
        ryeAbrirSeccion(estudiantesButton, "/fxml/estudiante/estudiantes.fxml", "Estudiantes");
    }

    @FXML
    public void ryeOnImportarExcel() {
        ryeAbrirSeccion(importarButton, "/fxml/importacion/importacion.fxml", "Importar desde Excel");
    }

    @FXML
    public void ryeOnOrdenMerito() {

        ryeIrAInicio();
        ryeMarcarSeccion(meritoButton);
        navigationServiceYnq.ryeAbrirVista("/fxml/reporte/reportes.fxml", "Orden de merito",
                controladorYnq -> ((pe.edu.sgfa.reporte.ui.CphReportesController) controladorYnq).cphElegirOrdenMerito());
    }

    @FXML
    public void ryeOnReportes() {
        ryeAbrirSeccion(reportesButton, "/fxml/reporte/reportes.fxml", "Reportes");
    }

    @FXML
    public void ryeOnRespaldo() {
        ryeAbrirSeccion(backupButton, "/fxml/backup/backup.fxml", "Respaldo y restauracion");
    }

    @FXML
    public void ryeOnAuditoria() {
        ryeAbrirSeccion(auditoriaButton, "/fxml/auditoria/auditoria.fxml", "Auditoria");
    }

    @FXML
    public void ryeOnAlternarTema() {
        themeServiceYnq.ryeAlternar();
    }

    private void ryeActualizarTextoTema() {
        temaButton.setText(themeServiceYnq.isOscuro() ? "☀   Modo claro" : "🌙   Modo oscuro");
    }
}
