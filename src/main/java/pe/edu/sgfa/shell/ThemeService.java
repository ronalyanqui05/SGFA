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
public class ThemeService {

    private static final String CLASE_OSCURO = "dark";

    private final Path archivo;
    private final BooleanProperty oscuro = new SimpleBooleanProperty(false);

    public ThemeService(@Value("${sgfa.config-dir}") String cnfgDir) {
        this.archivo = Path.of(cnfgDir, "tema.txt");
        oscuro.set(leerPreferencia());
        oscuro.addListener((obs, antes, ahora) -> guardarPreferencia(ahora));
    }

    public BooleanProperty oscuroProperty() {
        return oscuro;
    }

    public boolean isOscuro() {
        return oscuro.get();
    }

    public void alternar() {
        oscuro.set(!oscuro.get());
    }

    public void aplicarA(Parent rz) {
        if (!rz.getStyleClass().contains("sgfa-themed")) {
            rz.getStyleClass().add("sgfa-themed");
        }
        actualizarClase(rz, oscuro.get());
        ChangeListener<Boolean> lstnr = (obs, antes, ahora) -> actualizarClase(rz, ahora);
        oscuro.addListener(lstnr);

    }

    private void actualizarClase(Parent rz, boolean actvrOscr) {
        if (actvrOscr) {
            if (!rz.getStyleClass().contains(CLASE_OSCURO)) {
                rz.getStyleClass().add(CLASE_OSCURO);
            }
        } else {
            rz.getStyleClass().remove(CLASE_OSCURO);
        }
    }

    private boolean leerPreferencia() {
        if (!Files.exists(archivo)) {
            return false;
        }
        try {
            return "dark".equals(Files.readString(archivo, StandardCharsets.UTF_8).strip());
        } catch (IOException e) {
            return false;
        }
    }

    private void guardarPreferencia(boolean actvrOscr) {
        try {
            Files.writeString(archivo, actvrOscr ? "dark" : "light", StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la preferencia de tema en " + archivo, e);
        }
    }
}
