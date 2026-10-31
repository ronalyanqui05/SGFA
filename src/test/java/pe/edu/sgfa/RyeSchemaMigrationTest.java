package pe.edu.sgfa;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RyeSchemaMigrationTest {

    private static final Set<String> EXPECTED_TABLES_YNQ = Set.of(
            "app_config",
            "instituciones",
            "programas_estudio",
            "planes_estudio",
            "ciclos",
            "cursos",
            "plan_cursos",
            "usuarios",
            "estudiantes",
            "trayectorias_academicas",
            "periodos_academicos",
            "situaciones_academicas",
            "registros_academicos",
            "auditoria",
            "fichas_generadas",
            "documentos_emitidos"
    );

    @BeforeAll
    static void useIsolatedHomeDir() throws IOException {
        Path tempHomeYnq = Files.createTempDirectory("sgfa-schema-test-home-");
        System.setProperty("sgfa.home-dir", tempHomeYnq.toString());
    }

    @Autowired
    private JdbcTemplate jdbcTemplateYnq;

    @Test
    void allDomainTablesExist() {
        List<String> tablesYnq = jdbcTemplateYnq.queryForList(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' "
                        + "AND name <> 'flyway_schema_history'",
                String.class);

        assertThat(tablesYnq).containsExactlyInAnyOrderElementsOf(EXPECTED_TABLES_YNQ);
    }

    @Test
    void situacionesAcademicasCatalogIsSeeded() {
        Integer countYnq = jdbcTemplateYnq.queryForObject(
                "SELECT COUNT(*) FROM situaciones_academicas", Integer.class);

        assertThat(countYnq).isEqualTo(11);
    }

    @Test
    void foreignKeyEnforcementIsEnabled() {
        Integer fkEnabledYnq = jdbcTemplateYnq.queryForObject("PRAGMA foreign_keys", Integer.class);

        assertThat(fkEnabledYnq).isEqualTo(1);
    }
}
