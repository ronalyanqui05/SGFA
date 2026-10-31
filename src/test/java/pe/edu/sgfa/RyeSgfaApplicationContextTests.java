package pe.edu.sgfa;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import pe.edu.sgfa.config.RyeAppConfigRepository;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RyeSgfaApplicationContextTests {

    @BeforeAll
    static void useIsolatedHomeDir() throws IOException {
        Path tempHomeYnq = Files.createTempDirectory("sgfa-test-home-");
        System.setProperty("sgfa.home-dir", tempHomeYnq.toString());
    }

    @Autowired
    private RyeAppConfigRepository appConfigRepositoryYnq;

    @Test
    void contextLoadsAndBaselineMigrationRan() {
        assertThat(appConfigRepositoryYnq.findById("app_name"))
                .hasValueSatisfying(cfgYnq -> assertThat(cfgYnq.getConfigValue()).isEqualTo("SGFA"));
    }
}
