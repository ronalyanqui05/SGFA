package pe.edu.sgfa.shell;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import pe.edu.sgfa.RyeSgfaApplication;
import pe.edu.sgfa.usuario.service.RyeAdminBootstrapResult;

public class RyeSgfaJavaFxApp extends Application {

    private ConfigurableApplicationContext contextYnq;

    @Override
    public void init() {
        contextYnq = new SpringApplicationBuilder(RyeSgfaApplication.class)
                .headless(false)
                .run();
    }

    @Override
    public void start(Stage stageYnq) {
        RyeNavigationService navigationServiceYnq = contextYnq.getBean(RyeNavigationService.class);
        navigationServiceYnq.ryeInicializar(stageYnq);

        ryeMostrarCredencialesInicialesSiCorresponde();

        stageYnq.setTitle("SGFA - Sistema de Gestion de Fichas Academicas");
        navigationServiceYnq.ryeMostrarLogin();
        stageYnq.show();
    }

    @Override
    public void stop() {
        contextYnq.close();
        Platform.exit();
    }

    private void ryeMostrarCredencialesInicialesSiCorresponde() {
        contextYnq.getBean(RyeAdminBootstrapResult.class).ryeConsumir().ifPresent(credencialesYnq -> {
            TextArea areaYnq = new TextArea(
                    "Usuario: " + credencialesYnq.username() + "\n"
                            + "Contrasena: " + credencialesYnq.password() + "\n\n"
                            + "Anote esta contrasena ahora. No volvera a mostrarse.");
            areaYnq.setEditable(false);
            areaYnq.setWrapText(true);
            areaYnq.setPrefRowCount(4);

            Alert alertYnq = new Alert(Alert.AlertType.INFORMATION, "", ButtonType.OK);
            alertYnq.setTitle("Usuario administrador creado");
            alertYnq.setHeaderText("Se genero un usuario administrador inicial");
            alertYnq.getDialogPane().setContent(areaYnq);
            alertYnq.showAndWait();
        });
    }
}
