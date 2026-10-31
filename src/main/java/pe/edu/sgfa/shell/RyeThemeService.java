package pe.edu.sgfa.shell;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.Parent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RyeThemeService {

    private static final String CLASE_OSCURO_YNQ = "dark";

    private final Path archivoYnq;
    private final BooleanProperty oscuroYnq = new SimpleBooleanProperty(false);

    public RyeThemeService(@Value("${sgfa.config-dir}") String configDirYnq) {
        this.archivoYnq = Path.of(configDirYnq, "tema.txt");
        oscuroYnq.set(ryeLeerPreferencia());
        oscuroYnq.addListener((obsYnq, antesYnq, ahoraYnq) -> ryeGuardarPreferencia(ahoraYnq));
    }

    public BooleanProperty oscuroProperty() {
        return oscuroYnq;
    }

    public boolean isOscuro() {
        return oscuroYnq.get();
    }

    public void ryeAlternar() {
        oscuroYnq.set(!oscuroYnq.get());
    }

    public void ryeAplicarA(Parent raizYnq) {
        if (!raizYnq.getStyleClass().contains("sgfa-themed")) {
            raizYnq.getStyleClass().add("sgfa-themed");
        }
        ryeActualizarClase(raizYnq, oscuroYnq.get());
        ChangeListener<Boolean> listenerYnq = (obsYnq, antesYnq, ahoraYnq) -> ryeActualizarClase(raizYnq, ahoraYnq);
        oscuroYnq.addListener(listenerYnq);

    }

    private void ryeActualizarClase(Parent raizYnq, boolean activarOscuroYnq) {
        if (activarOscuroYnq) {
            if (!raizYnq.getStyleClass().contains(CLASE_OSCURO_YNQ)) {
                raizYnq.getStyleClass().add(CLASE_OSCURO_YNQ);
            }
        } else {
            raizYnq.getStyleClass().remove(CLASE_OSCURO_YNQ);
        }
    }

    private boolean ryeLeerPreferencia() {
        if (!Files.exists(archivoYnq)) {
            return false;
        }
        try {
            return "dark".equals(Files.readString(archivoYnq, StandardCharsets.UTF_8).strip());
        } catch (IOException e) {
            return false;
        }
    }

    private void ryeGuardarPreferencia(boolean activarOscuroYnq) {
        try {
            Files.writeString(archivoYnq, activarOscuroYnq ? "dark" : "light", StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la preferencia de tema en " + archivoYnq, e);
        }
    }
}
