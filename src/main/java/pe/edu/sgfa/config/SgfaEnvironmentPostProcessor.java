package pe.edu.sgfa.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

public class SgfaEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment envrnmnt, SpringApplication applctn) {
        String ovrrd = envrnmnt.getProperty("sgfa.home-dir");
        var hmDir = AppPaths.resolveHomeDir(ovrrd);
        AppPaths.sembrarDatosIniciales(hmDir, carpetaDatosIniciales());
        var pths = AppPaths.ensureDirectories(hmDir);

        Map<String, Object> prps = new LinkedHashMap<>();
        prps.put("sgfa.home-dir", pths.home().toString());
        prps.put("sgfa.data-dir", pths.data().toString());
        prps.put("sgfa.backups-dir", pths.backups().toString());
        prps.put("sgfa.documents-dir", pths.documents().toString());
        prps.put("sgfa.logs-dir", pths.logs().toString());
        prps.put("sgfa.config-dir", pths.config().toString());
        prps.put("sgfa.tessdata-dir", pths.tessdata().toString());
        prps.put("spring.datasource.url",
                "jdbc:sqlite:" + pths.data().resolve("sgfa.db")
                        + "?journal_mode=WAL&busy_timeout=5000&foreign_keys=on");

        envrnmnt.getPropertySources().addFirst(new MapPropertySource("sgfaResolvedPaths", prps));
    }

    private static java.nio.file.Path carpetaDatosIniciales() {
        try {
            var orgn = SgfaEnvironmentPostProcessor.class.getProtectionDomain().getCodeSource();
            if (orgn == null) {
                return null;
            }
            java.nio.file.Path jar = java.nio.file.Path.of(orgn.getLocation().toURI());
            return java.nio.file.Files.isRegularFile(jar) ? jar.resolveSibling("datos-iniciales") : null;
        } catch (Exception e) {
            return null;
        }
    }
}
