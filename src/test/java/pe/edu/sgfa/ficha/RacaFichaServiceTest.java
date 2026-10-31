package pe.edu.sgfa.ficha;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import net.sf.jasperreports.engine.JasperPrint;

import pe.edu.sgfa.common.exception.RyeAccesoDenegadoException;
import pe.edu.sgfa.estudiante.domain.YmclEstudiante;
import pe.edu.sgfa.estudiante.service.YmclEstudianteService;
import pe.edu.sgfa.ficha.domain.RacaEstadoFicha;
import pe.edu.sgfa.ficha.domain.RacaFichaGenerada;
import pe.edu.sgfa.ficha.service.RacaDatosFicha;
import pe.edu.sgfa.ficha.service.RacaFichaConfirmada;
import pe.edu.sgfa.ficha.service.RacaFichaService;
import pe.edu.sgfa.ficha.service.RacaFilaFichaCiclo;
import pe.edu.sgfa.usuario.domain.RyeRolUsuario;
import pe.edu.sgfa.institucion.service.RacaInstitucionService;
import pe.edu.sgfa.nota.repository.YmclSituacionAcademicaRepository;
import pe.edu.sgfa.nota.service.YmclItemNota;
import pe.edu.sgfa.nota.service.YmclRegistroAcademicoService;
import pe.edu.sgfa.periodo.domain.YmclPeriodoAcademico;
import pe.edu.sgfa.periodo.service.YmclPeriodoAcademicoService;
import pe.edu.sgfa.programa.domain.RacaCiclo;
import pe.edu.sgfa.programa.domain.RacaCurso;
import pe.edu.sgfa.programa.domain.RacaPlanCurso;
import pe.edu.sgfa.programa.domain.RacaPlanEstudio;
import pe.edu.sgfa.programa.domain.RacaProgramaEstudio;
import pe.edu.sgfa.programa.service.RacaCicloService;
import pe.edu.sgfa.programa.service.RacaCursoService;
import pe.edu.sgfa.programa.service.RacaPlanCursoService;
import pe.edu.sgfa.programa.service.RacaPlanEstudioService;
import pe.edu.sgfa.programa.service.RacaProgramaEstudioService;
import pe.edu.sgfa.trayectoria.domain.YmclTrayectoria;
import pe.edu.sgfa.trayectoria.service.YmclTrayectoriaService;
import pe.edu.sgfa.usuario.service.RyeAdminBootstrapResult;
import pe.edu.sgfa.usuario.service.RyeAuthService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RacaFichaServiceTest {

    @BeforeAll
    static void useIsolatedHomeDir() throws IOException {
        Path tempHomeCrr = Files.createTempDirectory("sgfa-ficha-test-home-");
        System.setProperty("sgfa.home-dir", tempHomeCrr.toString());
    }

    @Autowired
    private RacaInstitucionService institucionServiceCrr;
    @Autowired
    private RacaProgramaEstudioService programaEstudioServiceCrr;
    @Autowired
    private RacaPlanEstudioService planEstudioServiceCrr;
    @Autowired
    private RacaCicloService cicloServiceCrr;
    @Autowired
    private RacaCursoService cursoServiceCrr;
    @Autowired
    private RacaPlanCursoService planCursoServiceCrr;
    @Autowired
    private YmclPeriodoAcademicoService periodoAcademicoServiceCrr;
    @Autowired
    private YmclEstudianteService estudianteServiceCrr;
    @Autowired
    private YmclTrayectoriaService trayectoriaServiceCrr;
    @Autowired
    private YmclRegistroAcademicoService registroAcademicoServiceCrr;
    @Autowired
    private YmclSituacionAcademicaRepository situacionAcademicaRepositoryCrr;
    @Autowired
    private RacaFichaService fichaServiceCrr;
    @Autowired
    private RyeAuthService authServiceCrr;
    @Autowired
    private RyeAdminBootstrapResult adminBootstrapResultCrr;
    @Autowired
    private pe.edu.sgfa.usuario.service.RyeUsuarioService usuarioServiceCrr;

    private static RyeAdminBootstrapResult.RyeCredenciales credencialesAdminCrr;

    private YmclTrayectoria trayectoriaCrr;
    private RacaCiclo ciclo1Crr;
    private RacaCiclo ciclo2Crr;
    private RacaCurso cursoRepetidoCrr;
    private RacaCurso cursoSinRegistroCrr;
    private RacaCurso cursoCiclo2Crr;
    private Long aprobadoIdCrr;
    private Long desaprobadoIdCrr;

    @BeforeEach
    void prepararDatosBase() {
        if (credencialesAdminCrr == null) {
            credencialesAdminCrr = adminBootstrapResultCrr.ryeConsumir()
                    .orElseThrow(() -> new IllegalStateException("El admin inicial no se genero"));
        }
        authServiceCrr.ryeCerrarSesion();
        authServiceCrr.ryeAutenticar(credencialesAdminCrr.username(), credencialesAdminCrr.password());

        if (institucionServiceCrr.racaObtenerPrincipal().isEmpty()) {
            institucionServiceCrr.racaGuardar("IESPP Juliaca", "JULI", "Av. Principal 123", null, null);
        }

        RacaProgramaEstudio programaCrr = programaEstudioServiceCrr.racaCrear("FIC" + System.nanoTime(), "Comunicacion", null, 10);
        RacaPlanEstudio planCrr = planEstudioServiceCrr.racaCrear(programaCrr.getId(), "PF" + System.nanoTime(), "Plan ficha", null, null);
        ciclo1Crr = cicloServiceCrr.racaCrear(planCrr.getId(), 1, "Primer ciclo", 1);
        ciclo2Crr = cicloServiceCrr.racaCrear(planCrr.getId(), 2, "Segundo ciclo", 2);

        cursoRepetidoCrr = cursoServiceCrr.racaCrear("REP" + System.nanoTime(), "Curso con recuperacion", 4, null, null);
        cursoSinRegistroCrr = cursoServiceCrr.racaCrear("SIN" + System.nanoTime(), "Curso sin nota", 3, null, null);
        cursoCiclo2Crr = cursoServiceCrr.racaCrear("C2C" + System.nanoTime(), "Curso ciclo dos", 5, null, null);

        planCursoServiceCrr.racaAsignar(planCrr.getId(), ciclo1Crr.getId(), cursoRepetidoCrr.getId(), 1, true);
        planCursoServiceCrr.racaAsignar(planCrr.getId(), ciclo1Crr.getId(), cursoSinRegistroCrr.getId(), 2, true);
        planCursoServiceCrr.racaAsignar(planCrr.getId(), ciclo2Crr.getId(), cursoCiclo2Crr.getId(), 1, true);

        YmclPeriodoAcademico periodoViejoCrr = periodoAcademicoServiceCrr.ymclCrear("FICV" + System.nanoTime(), 2025, "II", null, null);
        YmclPeriodoAcademico periodoNuevoCrr = periodoAcademicoServiceCrr.ymclCrear("FICN" + System.nanoTime(), 2026, "I", null, null);

        YmclEstudiante estudianteCrr = estudianteServiceCrr.ymclCrear("EST" + System.nanoTime(), null, "Maria", "Condori" + System.nanoTime(),
                null, null, null, null, null, null);
        trayectoriaCrr = trayectoriaServiceCrr.ymclIniciar(estudianteCrr.getId(), programaCrr.getId(), planCrr.getId(), null, null);

        aprobadoIdCrr = idSituacion("APROBADO");
        desaprobadoIdCrr = idSituacion("DESAPROBADO");

        Long planCursoRepetidoIdCrr = planCursoIdDe(ciclo1Crr, cursoRepetidoCrr);
        registroAcademicoServiceCrr.ymclGuardarMasivo(trayectoriaCrr.getId(), periodoViejoCrr.getId(),
                List.of(new YmclItemNota(planCursoRepetidoIdCrr, 8.0, desaprobadoIdCrr, "primer intento")));
        registroAcademicoServiceCrr.ymclGuardarMasivo(trayectoriaCrr.getId(), periodoNuevoCrr.getId(),
                List.of(new YmclItemNota(planCursoRepetidoIdCrr, 16.0, aprobadoIdCrr, "recuperacion")));

        Long planCursoCiclo2IdCrr = planCursoIdDe(ciclo2Crr, cursoCiclo2Crr);
        registroAcademicoServiceCrr.ymclGuardarMasivo(trayectoriaCrr.getId(), periodoNuevoCrr.getId(),
                List.of(new YmclItemNota(planCursoCiclo2IdCrr, 18.0, aprobadoIdCrr, null)));

    }

    private Long idSituacion(String codigoCrr) {
        return situacionAcademicaRepositoryCrr.findAll().stream()
                .filter(s -> s.getCodigo().equals(codigoCrr)).findFirst().orElseThrow().getId();
    }

    private Long planCursoIdDe(RacaCiclo cicloCrr, RacaCurso cursoCrr) {
        List<RacaPlanCurso> planCursosCrr = registroAcademicoServiceCrr.ymclListarVigentesPorCiclo(trayectoriaCrr.getId(), cicloCrr.getId())
                .stream().map(f -> f.planCurso()).toList();
        return planCursosCrr.stream()
                .filter(pc -> pc.getCurso().getId().equals(cursoCrr.getId()))
                .findFirst().orElseThrow().getId();
    }

    @Test
    void construirDatosAgrupaPorCicloEnOrden() {
        RacaDatosFicha datosCrr = fichaServiceCrr.racaConstruirDatos(trayectoriaCrr.getId());

        assertThat(datosCrr.ciclos()).hasSize(2);
        assertThat(datosCrr.ciclos().get(0).numero()).isEqualTo(1);
        assertThat(datosCrr.ciclos().get(1).numero()).isEqualTo(2);
        assertThat(datosCrr.estudianteNombreCompleto()).contains("Condori");
    }

    @Test
    void cursoSinNotaApareceSinNota() {
        RacaDatosFicha datosCrr = fichaServiceCrr.racaConstruirDatos(trayectoriaCrr.getId());

        RacaFilaFichaCiclo filaSinRegistroCrr = datosCrr.ciclos().get(0).filas().stream()
                .filter(f -> f.getCursoCodigo().equals(cursoSinRegistroCrr.getCodigo()))
                .findFirst().orElseThrow();

        assertThat(filaSinRegistroCrr.getNotaFinal()).isNull();
    }

    @Test
    void cursoConRecuperacionMuestraSoloLaNotaMasReciente() {
        RacaDatosFicha datosCrr = fichaServiceCrr.racaConstruirDatos(trayectoriaCrr.getId());

        List<RacaFilaFichaCiclo> filasCursoRepetidoCrr = datosCrr.ciclos().get(0).filas().stream()
                .filter(f -> f.getCursoCodigo().equals(cursoRepetidoCrr.getCodigo()))
                .toList();

        assertThat(filasCursoRepetidoCrr).hasSize(1);
        assertThat(filasCursoRepetidoCrr.get(0).getNotaFinal()).isEqualTo(16.0);
        assertThat(filasCursoRepetidoCrr.get(0).getObservacion()).isEqualTo("recuperacion");
    }

    @Test
    void laFechaDeCadaFilaEsLaDeFinDePeriodoNoLaDeHoy() {
        YmclPeriodoAcademico periodoConFechasCrr = periodoAcademicoServiceCrr.ymclCrear("FICF" + System.nanoTime(), 2026, "I",
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 7, 15));
        Long planCursoCiclo2IdCrr = planCursoIdDe(ciclo2Crr, cursoCiclo2Crr);
        registroAcademicoServiceCrr.ymclGuardarMasivo(trayectoriaCrr.getId(), periodoConFechasCrr.getId(),
                List.of(new YmclItemNota(planCursoCiclo2IdCrr, 14.0, aprobadoIdCrr, null)));

        RacaDatosFicha datosCrr = fichaServiceCrr.racaConstruirDatos(trayectoriaCrr.getId());
        RacaFilaFichaCiclo filaCrr = datosCrr.ciclos().get(1).filas().get(0);

        assertThat(filaCrr.getFechaRegistro()).isEqualTo("15/07/2026");
    }

    @Test
    void siLaFilaTieneObservacionNoMuestraFecha() {
        YmclPeriodoAcademico periodoConFechasCrr = periodoAcademicoServiceCrr.ymclCrear("FICO" + System.nanoTime(), 2026, "I",
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 7, 15));
        Long planCursoCiclo2IdCrr = planCursoIdDe(ciclo2Crr, cursoCiclo2Crr);
        registroAcademicoServiceCrr.ymclGuardarMasivo(trayectoriaCrr.getId(), periodoConFechasCrr.getId(),
                List.of(new YmclItemNota(planCursoCiclo2IdCrr, 14.0, aprobadoIdCrr, "nota del docente")));

        RacaDatosFicha datosCrr = fichaServiceCrr.racaConstruirDatos(trayectoriaCrr.getId());
        RacaFilaFichaCiclo filaCrr = datosCrr.ciclos().get(1).filas().get(0);

        assertThat(filaCrr.getObservacion()).isEqualTo("nota del docente");
        assertThat(filaCrr.getFechaRegistro()).isNull();
    }

    @Test
    void promedioDelPrimerCicloExcluyeElCursoSinRegistro() {
        RacaDatosFicha datosCrr = fichaServiceCrr.racaConstruirDatos(trayectoriaCrr.getId());

        var promedioCiclo1Crr = datosCrr.ciclos().get(0).promedio();

        assertThat(promedioCiclo1Crr.totalCreditos()).isEqualTo(cursoRepetidoCrr.getCreditos());
        assertThat(promedioCiclo1Crr.promedio()).isEqualTo(16.0);
    }

    @Test
    void elPieDelCicloTraeLaSumaDePuntajeYSusCreditos() {
        RacaDatosFicha datosCrr = fichaServiceCrr.racaConstruirDatos(trayectoriaCrr.getId());

        RacaFilaFichaCiclo filaCrr = datosCrr.ciclos().get(0).filas().get(0);

        assertThat(filaCrr.getTotalCreditosCiclo()).isEqualTo(cursoRepetidoCrr.getCreditos());
        assertThat(filaCrr.getSumaPonderadaCiclo()).isEqualTo(16.0 * cursoRepetidoCrr.getCreditos());
    }

    @Test
    void laFichaImpresaUsaLaFechaDeHoyYLosRotulosNuevos() {
        String textoCrr = textoDe(fichaServiceCrr.racaPrevisualizar(trayectoriaCrr.getId()));

        assertThat(textoCrr).contains("Fecha de emision: "
                + LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        assertThat(textoCrr).contains("Puntaje", "Secretario Académico", "Director General");
        assertThat(textoCrr).doesNotContain("Situacion", "Ponderado\n", "Firma del responsable", "Sello");
        assertThat(textoCrr).doesNotContain(cursoRepetidoCrr.getCodigo() + " - ");
    }

    private static String textoDe(JasperPrint printCrr) {
        StringBuilder textoCrr = new StringBuilder();
        for (var paginaCrr : printCrr.getPages()) {
            for (var elementoCrr : paginaCrr.getElements()) {
                if (elementoCrr instanceof net.sf.jasperreports.engine.JRPrintText t) {
                    textoCrr.append(t.getFullText()).append('\n');
                }
            }
        }
        return textoCrr.toString();
    }

    @Test
    void previsualizarNoDejaRastroNiConsumeCorrelativo() {
        JasperPrint printCrr = fichaServiceCrr.racaPrevisualizar(trayectoriaCrr.getId());

        assertThat(printCrr).isNotNull();
        assertThat(printCrr.getPages()).isNotEmpty();
        assertThat(fichaServiceCrr.racaListarPorTrayectoria(trayectoriaCrr.getId())).isEmpty();

        fichaServiceCrr.racaPrevisualizar(trayectoriaCrr.getId());
        assertThat(fichaServiceCrr.racaListarPorTrayectoria(trayectoriaCrr.getId())).isEmpty();
    }

    @Test
    void confirmarYGuardarProduceUnPdfValidoYUnaFilaDeHistorial() throws IOException {
        RacaFichaConfirmada confirmadaCrr = fichaServiceCrr.racaConfirmarYGuardar(trayectoriaCrr.getId());
        RacaFichaGenerada registroCrr = confirmadaCrr.registro();

        assertThat(registroCrr.getId()).isNotNull();
        assertThat(registroCrr.getEstado()).isEqualTo(RacaEstadoFicha.GENERADA);
        assertThat(registroCrr.getCorrelativo()).isNotBlank();
        assertThat(registroCrr.getAnio()).isEqualTo(LocalDate.now().getYear());

        Path archivoCrr = Path.of(registroCrr.getArchivoPath());
        assertThat(Files.exists(archivoCrr)).isTrue();
        assertThat(Files.size(archivoCrr)).isGreaterThan(0);
        byte[] contenidoCrr = Files.readAllBytes(archivoCrr);
        String firmaCrr = new String(contenidoCrr, 0, 4, StandardCharsets.US_ASCII);
        assertThat(firmaCrr).isEqualTo("%PDF");

        assertThat(confirmadaCrr.print()).isNotNull();
        assertThat(confirmadaCrr.print().getPages()).isNotEmpty();

        List<RacaFichaGenerada> historialCrr = fichaServiceCrr.racaListarPorTrayectoria(trayectoriaCrr.getId());
        assertThat(historialCrr).hasSize(1);
        assertThat(historialCrr.get(0).getId()).isEqualTo(registroCrr.getId());
    }

    @Test
    void correlativoEsSecuencialDentroDelMismoAnio() {
        RacaFichaGenerada primeraCrr = fichaServiceCrr.racaConfirmarYGuardar(trayectoriaCrr.getId()).registro();
        RacaFichaGenerada segundaCrr = fichaServiceCrr.racaConfirmarYGuardar(trayectoriaCrr.getId()).registro();

        assertThat(segundaCrr.getNumeroSecuencial()).isEqualTo(primeraCrr.getNumeroSecuencial() + 1);
        assertThat(segundaCrr.getAnio()).isEqualTo(primeraCrr.getAnio());
        assertThat(segundaCrr.getCorrelativo()).isNotEqualTo(primeraCrr.getCorrelativo());
    }

    @Test
    void anularCambiaEstadoYRegistraElMotivo() {
        RacaFichaGenerada registroCrr = fichaServiceCrr.racaConfirmarYGuardar(trayectoriaCrr.getId()).registro();

        RacaFichaGenerada anuladaCrr = fichaServiceCrr.racaAnular(registroCrr.getId(), "Duplicada por error");

        assertThat(anuladaCrr.getEstado()).isEqualTo(RacaEstadoFicha.ANULADA);
        assertThat(anuladaCrr.getObservacion()).isEqualTo("Duplicada por error");
    }

    @Test
    void anularRequiereRolAdmin() {
        RacaFichaGenerada registroCrr = fichaServiceCrr.racaConfirmarYGuardar(trayectoriaCrr.getId()).registro();

        String usernameOperadorCrr = "operador" + System.nanoTime();
        usuarioServiceCrr.ryeCrear(usernameOperadorCrr, "clave1234", "Operador de prueba", RyeRolUsuario.OPERADOR);
        authServiceCrr.ryeCerrarSesion();
        authServiceCrr.ryeAutenticar(usernameOperadorCrr, "clave1234");

        assertThatThrownBy(() -> fichaServiceCrr.racaAnular(registroCrr.getId(), "intento no autorizado"))
                .isInstanceOf(RyeAccesoDenegadoException.class);
    }
}
