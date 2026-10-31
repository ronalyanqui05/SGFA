package pe.edu.sgfa.shell;

import javafx.fxml.FXMLLoader;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class RyeSpringFxmlLoaderFactory {

    private final ApplicationContext contextYnq;

    public RyeSpringFxmlLoaderFactory(ApplicationContext contextYnq) {
        this.contextYnq = contextYnq;
    }

    public FXMLLoader ryeCreate(String fxmlClasspathPathYnq) {
        FXMLLoader loaderYnq = new FXMLLoader(getClass().getResource(fxmlClasspathPathYnq));
        loaderYnq.setControllerFactory(contextYnq::getBean);
        return loaderYnq;
    }
}
