package pe.edu.sgfa;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import pe.edu.sgfa.config.AppConfigRepository;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SgfaApplicationContextTests {

    @BeforeAll
    static void useIsolatedHomeDir() throws IOException {
        Path tmpHm = Files.createTempDirectory("sgfa-test-home-");
        System.setProperty("sgfa.home-dir", tmpHm.toString());
    }

    @Autowired
    private AppConfigRepository appConfigRepository;

    @Test
    void contextLoadsAndBaselineMigrationRan() {
        assertThat(appConfigRepository.findById("app_name"))
                .hasValueSatisfying(cfg -> assertThat(cfg.getConfigValue()).isEqualTo("SGFA"));
    }
}
