package pe.edu.sgfa.shell;

import javafx.fxml.FXMLLoader;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class SpringFxmlLoaderFactory {

    private final ApplicationContext context;

    public SpringFxmlLoaderFactory(ApplicationContext context) {
        this.context = context;
    }

    public FXMLLoader create(String fxmlClsspthPth) {
        FXMLLoader ldr = new FXMLLoader(getClass().getResource(fxmlClsspthPth));
        ldr.setControllerFactory(context::getBean);
        return ldr;
    }
}
