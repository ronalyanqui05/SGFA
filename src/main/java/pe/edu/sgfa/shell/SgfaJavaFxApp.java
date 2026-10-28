package pe.edu.sgfa.shell;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import pe.edu.sgfa.SgfaApplication;
import pe.edu.sgfa.usuario.service.AdminBootstrapResult;

public class SgfaJavaFxApp extends Application {

    private ConfigurableApplicationContext context;

    @Override
    public void init() {
        context = new SpringApplicationBuilder(SgfaApplication.class)
                .headless(false)
                .run();
    }

    @Override
    public void start(Stage stage) {
        NavigationService nvgtnSrvc = context.getBean(NavigationService.class);
        nvgtnSrvc.inicializar(stage);

        mostrarCredencialesInicialesSiCorresponde();

        stage.setTitle("SGFA - Sistema de Gestion de Fichas Academicas");
        nvgtnSrvc.mostrarLogin();
        stage.show();
    }

    @Override
    public void stop() {
        context.close();
        Platform.exit();
    }

    private void mostrarCredencialesInicialesSiCorresponde() {
        context.getBean(AdminBootstrapResult.class).consumir().ifPresent(credenciales -> {
            TextArea ar = new TextArea(
                    "Usuario: " + credenciales.username() + "\n"
                            + "Contrasena: " + credenciales.password() + "\n\n"
                            + "Anote esta contrasena ahora. No volvera a mostrarse.");
            ar.setEditable(false);
            ar.setWrapText(true);
            ar.setPrefRowCount(4);

            Alert alrt = new Alert(Alert.AlertType.INFORMATION, "", ButtonType.OK);
            alrt.setTitle("Usuario administrador creado");
            alrt.setHeaderText("Se genero un usuario administrador inicial");
            alrt.getDialogPane().setContent(ar);
            alrt.showAndWait();
        });
    }
}
