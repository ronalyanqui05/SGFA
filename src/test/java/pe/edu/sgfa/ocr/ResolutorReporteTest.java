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

import pe.edu.sgfa.common.exception.ValidacionException;
import pe.edu.sgfa.estudiante.repository.EstudianteRepository;
import pe.edu.sgfa.nota.service.RegistroAcademicoService;
import pe.edu.sgfa.ocr.service.ReconocedorReporteCalificaciones.CursoDetectado;
import pe.edu.sgfa.ocr.service.ReconocedorReporteCalificaciones.ReporteDetectado;
import pe.edu.sgfa.ocr.service.ResolutorReporte;
import pe.edu.sgfa.ocr.service.ResolutorReporte.Estado;
import pe.edu.sgfa.ocr.service.ResolutorReporte.Propuesta;
import pe.edu.sgfa.periodo.service.PeriodoAcademicoService;
import pe.edu.sgfa.programa.domain.Ciclo;
import pe.edu.sgfa.programa.domain.PlanEstudio;
import pe.edu.sgfa.programa.domain.ProgramaEstudio;
import pe.edu.sgfa.programa.service.CicloService;
import pe.edu.sgfa.programa.service.CursoService;
import pe.edu.sgfa.programa.service.PlanCursoService;
import pe.edu.sgfa.programa.service.PlanEstudioService;
import pe.edu.sgfa.programa.service.ProgramaEstudioService;
import pe.edu.sgfa.trayectoria.service.TrayectoriaService;
import pe.edu.sgfa.usuario.service.AdminBootstrapResult;
import pe.edu.sgfa.usuario.service.AuthService;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ResolutorReporteTest {

    @BeforeAll
    static void baseAislada() throws IOException {
        System.setProperty("sgfa.home-dir", Files.createTempDirectory("sgfa-resolutor-test-home-").toString());
    }

    @Autowired
    private pe.edu.sgfa.institucion.service.InstitucionService institucionService;
    @Autowired
    private ResolutorReporte resolutor;
    @Autowired
    private ProgramaEstudioService programaEstudioService;
    @Autowired
    private PlanEstudioService planEstudioService;
    @Autowired
    private CicloService cicloService;
    @Autowired
    private CursoService cursoService;
    @Autowired
    private PlanCursoService planCursoService;
    @Autowired
    private PeriodoAcademicoService periodoAcademicoService;
    @Autowired
    private EstudianteRepository estudianteRepository;
    @Autowired
    private TrayectoriaService trayectoriaService;
    @Autowired
    private RegistroAcademicoService registroAcademicoService;
    @Autowired
    private AuthService authService;
    @Autowired
    private AdminBootstrapResult adminBootstrapResult;

    private static AdminBootstrapResult.Credenciales credenciales;
    private static boolean catalogoCreado;
    private static Ciclo ciclo1;
    private static PlanEstudio plan;

    private static final String TEXTO_PROGRAMA = "Programa de Estudios EDUCACION ASTRONOMIA APLICADA Ciclo CICLO I";

    @BeforeEach
    void preparar() {
        if (credenciales == null) {
            credenciales = adminBootstrapResult.consumir().orElseThrow();
        }
        authService.cerrarSesion();
        authService.autenticar(credenciales.username(), credenciales.password());
        if (institucionService.obtenerPrincipal().isEmpty()) {
            institucionService.guardar("IESPP Juliaca", "JULI", "Av. Principal 123", null, null);
        }
        if (!catalogoCreado) {
            ProgramaEstudio programa = programaEstudioService.crear("AST", "Educacion Astronomia Aplicada", null, 10);
            plan = planEstudioService.crear(programa.getId(), "PA-1", "Plan astronomia", null, null);
            ciclo1 = cicloService.crear(plan.getId(), 1, "Ciclo I", 1);
            var lctr = cursoService.crear("AST-I_CM01", "LECTURA Y ESCRITURA EN LA EDUCACIÓN SUPERIOR", 3, 4, null);
            var dsrrll = cursoService.crear("AST-I_CM02", "DESARROLLO PERSONAL I", 5, 6, null);
            var dsrrllDos = cursoService.crear("AST-I_CM03", "DESARROLLO PERSONAL II", 5, 6, null);
            planCursoService.asignar(plan.getId(), ciclo1.getId(), lctr.getId(), 1, true);
            planCursoService.asignar(plan.getId(), ciclo1.getId(), dsrrll.getId(), 2, true);
            planCursoService.asignar(plan.getId(), ciclo1.getId(), dsrrllDos.getId(), 3, true);
            periodoAcademicoService.crear("2031-I", 2031, null, null, null);
            catalogoCreado = true;
        }
    }

    private static ReporteDetectado reporte(String mtrcl, String ptrn, List<CursoDetectado> crss, Double prmd) {
        return new ReporteDetectado(mtrcl, ptrn, "CONDORCALLO", "Eynar", "2031-I", 1, TEXTO_PROGRAMA, crss,
                prmd, List.of());
    }

    private static List<CursoDetectado> cursosCompletos() {
        return List.of(new CursoDetectado("LECTURA Y ESCRITURA EN LA EDUCACION SUPERIOR", 16.0, "LOGRADO"),
                new CursoDetectado("DESARROLLO PERSONAL I", 14.0, "PROCESO"));
    }

    @Test
    void unReporteCompletoDeEstudianteNuevoQuedaListo() {
        Propuesta p = resolutor.proponer(reporte("70000001", "APAZA", cursosCompletos(), 14.75));

        assertThat(p.avisos()).isEmpty();
        assertThat(p.estado()).isEqualTo(Estado.LISTO);
        assertThat(p.esNuevo()).isTrue();
        assertThat(p.programa().getNombre()).isEqualTo("Educacion Astronomia Aplicada");
        assertThat(p.plan().getId()).isEqualTo(plan.getId());
        assertThat(p.ciclo().getNumero()).isEqualTo(1);
        assertThat(p.periodo().getNombre()).isEqualTo("2031-I");
        assertThat(p.cursosEmparejados()).isEqualTo(2);

        assertThat(p.promedioCalculado()).isEqualTo(14.75);
    }

    @Test
    void unPromedioImpresoQueNoCuadraPideRevision() {

        var crss = List.of(new CursoDetectado("LECTURA Y ESCRITURA EN LA EDUCACION SUPERIOR", 18.0, "LOGRADO"),
                new CursoDetectado("DESARROLLO PERSONAL I", 14.0, "PROCESO"));

        Propuesta p = resolutor.proponer(reporte("70000002", "APAZA", crss, 14.75));

        assertThat(p.estado()).isEqualTo(Estado.REVISAR);
        assertThat(p.avisos()).anyMatch(a -> a.contains("promedio impreso"));
    }

    @Test
    void unCursoQueNoSeIdentificaPideRevision() {
        var crss = List.of(new CursoDetectado("MATERIA QUE NO EXISTE EN EL PLAN", 15.0, "LOGRADO"));

        Propuesta p = resolutor.proponer(reporte("70000003", "APAZA", crss, null));

        assertThat(p.estado()).isEqualTo(Estado.REVISAR);
        assertThat(p.avisos()).anyMatch(a -> a.contains("no se identifico el curso"));
        assertThat(p.cursosEmparejados()).isZero();
    }

    @Test
    void unaSituacionQueContradiceLaNotaPideRevision() {
        var crss = List.of(new CursoDetectado("LECTURA Y ESCRITURA EN LA EDUCACION SUPERIOR", 10.0, "LOGRADO"));

        Propuesta p = resolutor.proponer(reporte("70000004", "APAZA", crss, null));

        assertThat(p.avisos()).anyMatch(a -> a.contains("corresponde a INICIO"));
    }

    @Test
    void unNivelDeDiferenciaEnElBordeNoEsContradiccion() {

        var crss = List.of(new CursoDetectado("LECTURA Y ESCRITURA EN LA EDUCACION SUPERIOR", 14.0, "PROCESO"));

        Propuesta p = resolutor.proponer(reporte("70000011", "APAZA", crss, 14.0));

        assertThat(p.avisos()).isEmpty();
        assertThat(p.estado()).isEqualTo(Estado.LISTO);
    }

    @Test
    void sinPromedioImpresoNoSePuedeVerificarYPideRevision() {
        Propuesta p = resolutor.proponer(reporte("70000012", "APAZA", cursosCompletos(), null));

        assertThat(p.estado()).isEqualTo(Estado.REVISAR);
        assertThat(p.avisos()).anyMatch(a -> a.contains("promedio impreso"));
    }

    @Test
    void unaMatriculaDeSieteDigitosPideRevisarSiSePerdioUnDigito() {
        Propuesta p = resolutor.proponer(reporte("7000013", "APAZA", cursosCompletos(), 14.75));

        assertThat(p.estado()).isEqualTo(Estado.REVISAR);
        assertThat(p.avisos()).anyMatch(a -> a.contains("7 digitos"));
    }

    @Test
    void elPeriodoIlegibleSeCompletaConElDelRestoDelLote() {
        var sinPrd = new ReporteDetectado("70000014", "APAZA", "X", "Eynar", null, 1, TEXTO_PROGRAMA, cursosCompletos(),
                14.75, List.of("No se reconocio el periodo academico."));

        var lt = resolutor.proponerTodos(List.of(reporte("70000015", "APAZA", cursosCompletos(), 14.75),
                reporte("70000016", "APAZA", cursosCompletos(), 14.75), sinPrd));

        assertThat(lt.get(2).periodo().getNombre()).isEqualTo("2031-I");
        assertThat(lt.get(2).estado()).isEqualTo(Estado.LISTO);
        assertThat(lt.get(2).notas()).anyMatch(n -> n.contains("resto del lote"));
    }

    @Test
    void elCicloIlegibleSeDeduceDeLosCursos() {
        var sinCcl = new ReporteDetectado("70000017", "APAZA", "X", "Eynar", "2031-I", null, TEXTO_PROGRAMA,
                cursosCompletos(), 14.75, List.of("No se reconocio el ciclo."));

        Propuesta p = resolutor.proponer(sinCcl);

        assertThat(p.ciclo().getNumero()).isEqualTo(1);
        assertThat(p.estado()).isEqualTo(Estado.LISTO);
        assertThat(p.notas()).anyMatch(n -> n.contains("se dedujo de los cursos"));
    }

    @Test
    void sinPeriodoOSinProgramaNoEsLista() {
        var sinDts = new ReporteDetectado("70000005", "APAZA", "X", "Eynar", "1900-I", 1, "texto sin programa",
                cursosCompletos(), null, List.of());

        Propuesta p = resolutor.proponer(sinDts);

        assertThat(p.estado()).isEqualTo(Estado.REVISAR);
        assertThat(p.avisos()).anyMatch(a -> a.contains("programa")).anyMatch(a -> a.contains("1900-I"));
    }

    @Test
    void guardarRegistraAlEstudianteNuevoYLuegoSoloAgregaNotasAlExistente() {
        Propuesta nv = resolutor.proponer(reporte("70000006", "APAZA", cursosCompletos(), 14.75));
        var rsltd = resolutor.guardar(nv);

        assertThat(rsltd.estudianteNuevo()).isTrue();
        assertThat(rsltd.notasGuardadas()).isEqualTo(2);
        assertThat(rsltd.promedioCalculado()).isEqualTo(14.75);
        assertThat(estudianteRepository.findByDni("70000006")).isPresent();
        var tryctrs = trayectoriaService.listarPorEstudiante(rsltd.estudiante().getId());
        assertThat(tryctrs).hasSize(1);

        var otrCrs = List.of(new CursoDetectado("DESARROLLO PERSONAL II", 17.0, "LOGRADO"));
        Propuesta exstnt = resolutor.proponer(reporte("70000006", "APAZA", otrCrs, null));
        assertThat(exstnt.esNuevo()).isFalse();
        assertThat(exstnt.plan().getId()).isEqualTo(plan.getId());

        var sgnd = resolutor.guardar(exstnt);

        assertThat(sgnd.estudianteNuevo()).isFalse();
        assertThat(trayectoriaService.listarPorEstudiante(rsltd.estudiante().getId())).hasSize(1);
        assertThat(estudianteRepository.findAll().stream().filter(e -> "70000006".equals(e.getDni()))).hasSize(1);
        var fls = registroAcademicoService.listarVigentesPorCiclo(tryctrs.get(0).getId(), ciclo1.getId());
        assertThat(fls).filteredOn(f -> f.registro() != null).hasSize(3);
    }

    @Test
    void siLaMatriculaEsDeOtraPersonaSeAvisa() {
        resolutor.guardar(resolutor.proponer(reporte("70000007", "QUISPE", cursosCompletos(), null)));

        Propuesta p = resolutor.proponer(reporte("70000007", "MAMANI", cursosCompletos(), null));

        assertThat(p.estado()).isEqualTo(Estado.REVISAR);
        assertThat(p.avisos()).anyMatch(a -> a.contains("revise la matricula"));
    }

    @Test
    void dosHojasDelLoteConElMismoEstudianteCicloYPeriodoSeMarcan() {
        var lt = resolutor.proponerTodos(List.of(
                reporte("70000008", "APAZA", cursosCompletos(), null),
                reporte("70000008", "APAZA", cursosCompletos(), null),
                reporte("70000009", "APAZA", cursosCompletos(), 14.75)));

        assertThat(lt.get(0).avisos()).anyMatch(a -> a.contains("Otra hoja del lote"));
        assertThat(lt.get(1).avisos()).anyMatch(a -> a.contains("Otra hoja del lote"));
        assertThat(lt.get(2).estado()).isEqualTo(Estado.LISTO);
    }

    @Test
    void noSeGuardaSinLosDatosImprescindibles() {
        var sinPrd = new ReporteDetectado("70000010", "APAZA", "X", "Eynar", null, 1, TEXTO_PROGRAMA,
                cursosCompletos(), null, List.of());

        assertThatThrownBy(() -> resolutor.guardar(resolutor.proponer(sinPrd)))
                .isInstanceOf(ValidacionException.class);
    }
}
