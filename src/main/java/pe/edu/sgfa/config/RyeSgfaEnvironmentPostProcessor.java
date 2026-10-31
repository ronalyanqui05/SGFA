package pe.edu.sgfa.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

public class RyeSgfaEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environmentYnq, SpringApplication applicationYnq) {
        String overrideYnq = environmentYnq.getProperty("sgfa.home-dir");
        var homeDirYnq = RyeAppPaths.ryeResolveHomeDir(overrideYnq);
        RyeAppPaths.ryeSembrarDatosIniciales(homeDirYnq, ryeCarpetaDatosIniciales());
        var pathsYnq = RyeAppPaths.ryeEnsureDirectories(homeDirYnq);

        Map<String, Object> propsYnq = new LinkedHashMap<>();
        propsYnq.put("sgfa.home-dir", pathsYnq.home().toString());
        propsYnq.put("sgfa.data-dir", pathsYnq.data().toString());
        propsYnq.put("sgfa.backups-dir", pathsYnq.backups().toString());
        propsYnq.put("sgfa.documents-dir", pathsYnq.documents().toString());
        propsYnq.put("sgfa.logs-dir", pathsYnq.logs().toString());
        propsYnq.put("sgfa.config-dir", pathsYnq.config().toString());
        propsYnq.put("sgfa.tessdata-dir", pathsYnq.tessdata().toString());
        propsYnq.put("spring.datasource.url",
                "jdbc:sqlite:" + pathsYnq.data().resolve("sgfa.db")
                        + "?journal_mode=WAL&busy_timeout=5000&foreign_keys=on");

        environmentYnq.getPropertySources().addFirst(new MapPropertySource("sgfaResolvedPaths", propsYnq));
    }

    private static java.nio.file.Path ryeCarpetaDatosIniciales() {
        try {
            var origenYnq = RyeSgfaEnvironmentPostProcessor.class.getProtectionDomain().getCodeSource();
            if (origenYnq == null) {
                return null;
            }
            java.nio.file.Path jarYnq = java.nio.file.Path.of(origenYnq.getLocation().toURI());
            return java.nio.file.Files.isRegularFile(jarYnq) ? jarYnq.resolveSibling("datos-iniciales") : null;
        } catch (Exception e) {
            return null;
        }
    }
}
