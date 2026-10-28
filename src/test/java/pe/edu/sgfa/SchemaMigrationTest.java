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
class SchemaMigrationTest {

    private static final Set<String> EXPECTED_TABLES = Set.of(
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
        Path tmpHm = Files.createTempDirectory("sgfa-schema-test-home-");
        System.setProperty("sgfa.home-dir", tmpHm.toString());
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void allDomainTablesExist() {
        List<String> tbls = jdbcTemplate.queryForList(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' "
                        + "AND name <> 'flyway_schema_history'",
                String.class);

        assertThat(tbls).containsExactlyInAnyOrderElementsOf(EXPECTED_TABLES);
    }

    @Test
    void situacionesAcademicasCatalogIsSeeded() {
        Integer cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM situaciones_academicas", Integer.class);

        assertThat(cnt).isEqualTo(11);
    }

    @Test
    void foreignKeyEnforcementIsEnabled() {
        Integer fkEnbld = jdbcTemplate.queryForObject("PRAGMA foreign_keys", Integer.class);

        assertThat(fkEnbld).isEqualTo(1);
    }
}
