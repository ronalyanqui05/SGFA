package pe.edu.sgfa.ocr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import pe.edu.sgfa.common.exception.RyeValidacionException;
import pe.edu.sgfa.estudiante.repository.YmclEstudianteRepository;
import pe.edu.sgfa.nota.service.YmclRegistroAcademicoService;
import pe.edu.sgfa.ocr.service.RahpReconocedorReporteCalificaciones.RahpCursoDetectado;
import pe.edu.sgfa.ocr.service.RahpReconocedorReporteCalificaciones.RahpReporteDetectado;
import pe.edu.sgfa.ocr.service.RahpResolutorReporte;
import pe.edu.sgfa.ocr.service.RahpResolutorReporte.RahpEstado;
import pe.edu.sgfa.ocr.service.RahpResolutorReporte.RahpPropuesta;
import pe.edu.sgfa.periodo.service.YmclPeriodoAcademicoService;
import pe.edu.sgfa.programa.domain.RacaCiclo;
import pe.edu.sgfa.programa.domain.RacaPlanEstudio;
import pe.edu.sgfa.programa.domain.RacaProgramaEstudio;
import pe.edu.sgfa.programa.service.RacaCicloService;
import pe.edu.sgfa.programa.service.RacaCursoService;
import pe.edu.sgfa.programa.service.RacaPlanCursoService;
import pe.edu.sgfa.programa.service.RacaPlanEstudioService;
import pe.edu.sgfa.programa.service.RacaProgramaEstudioService;
import pe.edu.sgfa.trayectoria.service.YmclTrayectoriaService;
import pe.edu.sgfa.usuario.service.RyeAdminBootstrapResult;
import pe.edu.sgfa.usuario.service.RyeAuthService;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RahpResolutorReporteTest {

    @BeforeAll
    static void baseAislada() throws IOException {
        System.setProperty("sgfa.home-dir", Files.createTempDirectory("sgfa-resolutor-test-home-").toString());
    }

    @Autowired
    private pe.edu.sgfa.institucion.service.RacaInstitucionService institucionServiceHnc;
    @Autowired
    private RahpResolutorReporte resolutorHnc;
    @Autowired
    private RacaProgramaEstudioService programaEstudioServiceHnc;
    @Autowired
    private RacaPlanEstudioService planEstudioServiceHnc;
    @Autowired
    private RacaCicloService cicloServiceHnc;
    @Autowired
    private RacaCursoService cursoServiceHnc;
    @Autowired
    private RacaPlanCursoService planCursoServiceHnc;
    @Autowired
    private YmclPeriodoAcademicoService periodoAcademicoServiceHnc;
    @Autowired
    private YmclEstudianteRepository estudianteRepositoryHnc;
    @Autowired
    private YmclTrayectoriaService trayectoriaServiceHnc;
    @Autowired
    private YmclRegistroAcademicoService registroAcademicoServiceHnc;
    @Autowired
    private RyeAuthService authServiceHnc;
    @Autowired
    private RyeAdminBootstrapResult adminBootstrapResultHnc;

    private static RyeAdminBootstrapResult.RyeCredenciales credencialesHnc;
    private static boolean catalogoCreadoHnc;
    private static RacaCiclo ciclo1Hnc;
    private static RacaPlanEstudio planHnc;

    private static final String TEXTO_PROGRAMA_HNC = "Programa de Estudios EDUCACION ASTRONOMIA APLICADA Ciclo CICLO I";

    @BeforeEach
    void preparar() {
        if (credencialesHnc == null) {
            credencialesHnc = adminBootstrapResultHnc.ryeConsumir().orElseThrow();
        }
        authServiceHnc.ryeCerrarSesion();
        authServiceHnc.ryeAutenticar(credencialesHnc.username(), credencialesHnc.password());
        if (institucionServiceHnc.racaObtenerPrincipal().isEmpty()) {
            institucionServiceHnc.racaGuardar("IESPP Juliaca", "JULI", "Av. Principal 123", null, null);
        }
        if (!catalogoCreadoHnc) {
            RacaProgramaEstudio programaHnc = programaEstudioServiceHnc.racaCrear("AST", "Educacion Astronomia Aplicada", null, 10);
            planHnc = planEstudioServiceHnc.racaCrear(programaHnc.getId(), "PA-1", "Plan astronomia", null, null);
            ciclo1Hnc = cicloServiceHnc.racaCrear(planHnc.getId(), 1, "Ciclo I", 1);
            var lecturaHnc = cursoServiceHnc.racaCrear("AST-I_CM01", "LECTURA Y ESCRITURA EN LA EDUCACIÓN SUPERIOR", 3, 4, null);
            var desarrolloHnc = cursoServiceHnc.racaCrear("AST-I_CM02", "DESARROLLO PERSONAL I", 5, 6, null);
            var desarrolloDosHnc = cursoServiceHnc.racaCrear("AST-I_CM03", "DESARROLLO PERSONAL II", 5, 6, null);
            planCursoServiceHnc.racaAsignar(planHnc.getId(), ciclo1Hnc.getId(), lecturaHnc.getId(), 1, true);
            planCursoServiceHnc.racaAsignar(planHnc.getId(), ciclo1Hnc.getId(), desarrolloHnc.getId(), 2, true);
            planCursoServiceHnc.racaAsignar(planHnc.getId(), ciclo1Hnc.getId(), desarrolloDosHnc.getId(), 3, true);
            periodoAcademicoServiceHnc.ymclCrear("2031-I", 2031, null, null, null);
            catalogoCreadoHnc = true;
        }
    }

    private static RahpReporteDetectado reporte(String matriculaHnc, String paternoHnc, List<RahpCursoDetectado> cursosHnc, Double promedioHnc) {
        return new RahpReporteDetectado(matriculaHnc, paternoHnc, "CONDORCALLO", "Eynar", "2031-I", 1, TEXTO_PROGRAMA_HNC, cursosHnc,
                promedioHnc, List.of());
    }

    private static List<RahpCursoDetectado> cursosCompletos() {
        return List.of(new RahpCursoDetectado("LECTURA Y ESCRITURA EN LA EDUCACION SUPERIOR", 16.0, "LOGRADO"),
                new RahpCursoDetectado("DESARROLLO PERSONAL I", 14.0, "PROCESO"));
    }

    @Test
    void unReporteCompletoDeEstudianteNuevoQuedaListo() {
        RahpPropuesta p = resolutorHnc.rahpProponer(reporte("70000001", "APAZA", cursosCompletos(), 14.75));

        assertThat(p.avisos()).isEmpty();
        assertThat(p.rahpEstado()).isEqualTo(RahpEstado.LISTO);
        assertThat(p.rahpEsNuevo()).isTrue();
        assertThat(p.programa().getNombre()).isEqualTo("Educacion Astronomia Aplicada");
        assertThat(p.plan().getId()).isEqualTo(planHnc.getId());
        assertThat(p.ciclo().getNumero()).isEqualTo(1);
        assertThat(p.periodo().getNombre()).isEqualTo("2031-I");
        assertThat(p.rahpCursosEmparejados()).isEqualTo(2);

        assertThat(p.promedioCalculado()).isEqualTo(14.75);
    }

    @Test
    void unPromedioImpresoQueNoCuadraPideRevision() {

        var cursosHnc = List.of(new RahpCursoDetectado("LECTURA Y ESCRITURA EN LA EDUCACION SUPERIOR", 18.0, "LOGRADO"),
                new RahpCursoDetectado("DESARROLLO PERSONAL I", 14.0, "PROCESO"));

        RahpPropuesta p = resolutorHnc.rahpProponer(reporte("70000002", "APAZA", cursosHnc, 14.75));

        assertThat(p.rahpEstado()).isEqualTo(RahpEstado.REVISAR);
        assertThat(p.avisos()).anyMatch(a -> a.contains("promedio impreso"));
    }

    @Test
    void unCursoQueNoSeIdentificaPideRevision() {
        var cursosHnc = List.of(new RahpCursoDetectado("MATERIA QUE NO EXISTE EN EL PLAN", 15.0, "LOGRADO"));

        RahpPropuesta p = resolutorHnc.rahpProponer(reporte("70000003", "APAZA", cursosHnc, null));

        assertThat(p.rahpEstado()).isEqualTo(RahpEstado.REVISAR);
        assertThat(p.avisos()).anyMatch(a -> a.contains("no se identifico el curso"));
        assertThat(p.rahpCursosEmparejados()).isZero();
    }

    @Test
    void unaSituacionQueContradiceLaNotaPideRevision() {
        var cursosHnc = List.of(new RahpCursoDetectado("LECTURA Y ESCRITURA EN LA EDUCACION SUPERIOR", 10.0, "LOGRADO"));

        RahpPropuesta p = resolutorHnc.rahpProponer(reporte("70000004", "APAZA", cursosHnc, null));

        assertThat(p.avisos()).anyMatch(a -> a.contains("corresponde a INICIO"));
    }

    @Test
    void unNivelDeDiferenciaEnElBordeNoEsContradiccion() {

        var cursosHnc = List.of(new RahpCursoDetectado("LECTURA Y ESCRITURA EN LA EDUCACION SUPERIOR", 14.0, "PROCESO"));

        RahpPropuesta p = resolutorHnc.rahpProponer(reporte("70000011", "APAZA", cursosHnc, 14.0));

        assertThat(p.avisos()).isEmpty();
        assertThat(p.rahpEstado()).isEqualTo(RahpEstado.LISTO);
    }

    @Test
    void sinPromedioImpresoNoSePuedeVerificarYPideRevision() {
        RahpPropuesta p = resolutorHnc.rahpProponer(reporte("70000012", "APAZA", cursosCompletos(), null));

        assertThat(p.rahpEstado()).isEqualTo(RahpEstado.REVISAR);
        assertThat(p.avisos()).anyMatch(a -> a.contains("promedio impreso"));
    }

    @Test
    void unaMatriculaDeSieteDigitosPideRevisarSiSePerdioUnDigito() {
        RahpPropuesta p = resolutorHnc.rahpProponer(reporte("7000013", "APAZA", cursosCompletos(), 14.75));

        assertThat(p.rahpEstado()).isEqualTo(RahpEstado.REVISAR);
        assertThat(p.avisos()).anyMatch(a -> a.contains("7 digitos"));
    }

    @Test
    void elPeriodoIlegibleSeCompletaConElDelRestoDelLote() {
        var sinPeriodoHnc = new RahpReporteDetectado("70000014", "APAZA", "X", "Eynar", null, 1, TEXTO_PROGRAMA_HNC, cursosCompletos(),
                14.75, List.of("No se reconocio el periodo academico."));

        var loteHnc = resolutorHnc.rahpProponerTodos(List.of(reporte("70000015", "APAZA", cursosCompletos(), 14.75),
                reporte("70000016", "APAZA", cursosCompletos(), 14.75), sinPeriodoHnc));

        assertThat(loteHnc.get(2).periodo().getNombre()).isEqualTo("2031-I");
        assertThat(loteHnc.get(2).rahpEstado()).isEqualTo(RahpEstado.LISTO);
        assertThat(loteHnc.get(2).notas()).anyMatch(n -> n.contains("resto del lote"));
    }

    @Test
    void elCicloIlegibleSeDeduceDeLosCursos() {
        var sinCicloHnc = new RahpReporteDetectado("70000017", "APAZA", "X", "Eynar", "2031-I", null, TEXTO_PROGRAMA_HNC,
                cursosCompletos(), 14.75, List.of("No se reconocio el ciclo."));

        RahpPropuesta p = resolutorHnc.rahpProponer(sinCicloHnc);

        assertThat(p.ciclo().getNumero()).isEqualTo(1);
        assertThat(p.rahpEstado()).isEqualTo(RahpEstado.LISTO);
        assertThat(p.notas()).anyMatch(n -> n.contains("se dedujo de los cursos"));
    }

    @Test
    void sinPeriodoOSinProgramaNoEsLista() {
        var sinDatosHnc = new RahpReporteDetectado("70000005", "APAZA", "X", "Eynar", "1900-I", 1, "texto sin programa",
                cursosCompletos(), null, List.of());

        RahpPropuesta p = resolutorHnc.rahpProponer(sinDatosHnc);

        assertThat(p.rahpEstado()).isEqualTo(RahpEstado.REVISAR);
        assertThat(p.avisos()).anyMatch(a -> a.contains("programa")).anyMatch(a -> a.contains("1900-I"));
    }

    @Test
    void guardarRegistraAlEstudianteNuevoYLuegoSoloAgregaNotasAlExistente() {
        RahpPropuesta nuevaHnc = resolutorHnc.rahpProponer(reporte("70000006", "APAZA", cursosCompletos(), 14.75));
        var resultadoHnc = resolutorHnc.rahpGuardar(nuevaHnc);

        assertThat(resultadoHnc.estudianteNuevo()).isTrue();
        assertThat(resultadoHnc.notasGuardadas()).isEqualTo(2);
        assertThat(resultadoHnc.promedioCalculado()).isEqualTo(14.75);
        assertThat(estudianteRepositoryHnc.findByDni("70000006")).isPresent();
        var trayectoriasHnc = trayectoriaServiceHnc.ymclListarPorEstudiante(resultadoHnc.estudiante().getId());
        assertThat(trayectoriasHnc).hasSize(1);

        var otroCursoHnc = List.of(new RahpCursoDetectado("DESARROLLO PERSONAL II", 17.0, "LOGRADO"));
        RahpPropuesta existenteHnc = resolutorHnc.rahpProponer(reporte("70000006", "APAZA", otroCursoHnc, null));
        assertThat(existenteHnc.rahpEsNuevo()).isFalse();
        assertThat(existenteHnc.plan().getId()).isEqualTo(planHnc.getId());

        var segundoHnc = resolutorHnc.rahpGuardar(existenteHnc);

        assertThat(segundoHnc.estudianteNuevo()).isFalse();
        assertThat(trayectoriaServiceHnc.ymclListarPorEstudiante(resultadoHnc.estudiante().getId())).hasSize(1);
        assertThat(estudianteRepositoryHnc.findAll().stream().filter(e -> "70000006".equals(e.getDni()))).hasSize(1);
        var filasHnc = registroAcademicoServiceHnc.ymclListarVigentesPorCiclo(trayectoriasHnc.get(0).getId(), ciclo1Hnc.getId());
        assertThat(filasHnc).filteredOn(f -> f.registro() != null).hasSize(3);
    }

    @Test
    void siLaMatriculaEsDeOtraPersonaSeAvisa() {
        resolutorHnc.rahpGuardar(resolutorHnc.rahpProponer(reporte("70000007", "QUISPE", cursosCompletos(), null)));

        RahpPropuesta p = resolutorHnc.rahpProponer(reporte("70000007", "MAMANI", cursosCompletos(), null));

        assertThat(p.rahpEstado()).isEqualTo(RahpEstado.REVISAR);
        assertThat(p.avisos()).anyMatch(a -> a.contains("revise la matricula"));
    }

    @Test
    void dosHojasDelLoteConElMismoEstudianteCicloYPeriodoSeMarcan() {
        var loteHnc = resolutorHnc.rahpProponerTodos(List.of(
                reporte("70000008", "APAZA", cursosCompletos(), null),
                reporte("70000008", "APAZA", cursosCompletos(), null),
                reporte("70000009", "APAZA", cursosCompletos(), 14.75)));

        assertThat(loteHnc.get(0).avisos()).anyMatch(a -> a.contains("Otra hoja del lote"));
        assertThat(loteHnc.get(1).avisos()).anyMatch(a -> a.contains("Otra hoja del lote"));
        assertThat(loteHnc.get(2).rahpEstado()).isEqualTo(RahpEstado.LISTO);
    }

    @Test
    void noSeGuardaSinLosDatosImprescindibles() {
        var sinPeriodoHnc = new RahpReporteDetectado("70000010", "APAZA", "X", "Eynar", null, 1, TEXTO_PROGRAMA_HNC,
                cursosCompletos(), null, List.of());

        assertThatThrownBy(() -> resolutorHnc.rahpGuardar(resolutorHnc.rahpProponer(sinPeriodoHnc)))
                .isInstanceOf(RyeValidacionException.class);
    }
}
