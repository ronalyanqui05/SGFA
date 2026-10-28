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

import pe.edu.sgfa.common.exception.AccesoDenegadoException;
import pe.edu.sgfa.estudiante.domain.Estudiante;
import pe.edu.sgfa.estudiante.service.EstudianteService;
import pe.edu.sgfa.ficha.domain.EstadoFicha;
import pe.edu.sgfa.ficha.domain.FichaGenerada;
import pe.edu.sgfa.ficha.service.DatosFicha;
import pe.edu.sgfa.ficha.service.FichaConfirmada;
import pe.edu.sgfa.ficha.service.FichaService;
import pe.edu.sgfa.ficha.service.FilaFichaCiclo;
import pe.edu.sgfa.usuario.domain.RolUsuario;
import pe.edu.sgfa.institucion.service.InstitucionService;
import pe.edu.sgfa.nota.repository.SituacionAcademicaRepository;
import pe.edu.sgfa.nota.service.ItemNota;
import pe.edu.sgfa.nota.service.RegistroAcademicoService;
import pe.edu.sgfa.periodo.domain.PeriodoAcademico;
import pe.edu.sgfa.periodo.service.PeriodoAcademicoService;
import pe.edu.sgfa.programa.domain.Ciclo;
import pe.edu.sgfa.programa.domain.Curso;
import pe.edu.sgfa.programa.domain.PlanCurso;
import pe.edu.sgfa.programa.domain.PlanEstudio;
import pe.edu.sgfa.programa.domain.ProgramaEstudio;
import pe.edu.sgfa.programa.service.CicloService;
import pe.edu.sgfa.programa.service.CursoService;
import pe.edu.sgfa.programa.service.PlanCursoService;
import pe.edu.sgfa.programa.service.PlanEstudioService;
import pe.edu.sgfa.programa.service.ProgramaEstudioService;
import pe.edu.sgfa.trayectoria.domain.Trayectoria;
import pe.edu.sgfa.trayectoria.service.TrayectoriaService;
import pe.edu.sgfa.usuario.service.AdminBootstrapResult;
import pe.edu.sgfa.usuario.service.AuthService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FichaServiceTest {

    @BeforeAll
    static void useIsolatedHomeDir() throws IOException {
        Path tmpHm = Files.createTempDirectory("sgfa-ficha-test-home-");
        System.setProperty("sgfa.home-dir", tmpHm.toString());
    }

    @Autowired
    private InstitucionService institucionService;
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
    private EstudianteService estudianteService;
    @Autowired
    private TrayectoriaService trayectoriaService;
    @Autowired
    private RegistroAcademicoService registroAcademicoService;
    @Autowired
    private SituacionAcademicaRepository situacionAcademicaRepository;
    @Autowired
    private FichaService fichaService;
    @Autowired
    private AuthService authService;
    @Autowired
    private AdminBootstrapResult adminBootstrapResult;
    @Autowired
    private pe.edu.sgfa.usuario.service.UsuarioService usuarioService;

    private static AdminBootstrapResult.Credenciales credencialesAdmin;

    private Trayectoria trayectoria;
    private Ciclo ciclo1;
    private Ciclo ciclo2;
    private Curso cursoRepetido;
    private Curso cursoSinRegistro;
    private Curso cursoCiclo2;
    private Long aprobadoId;
    private Long desaprobadoId;

    @BeforeEach
    void prepararDatosBase() {
        if (credencialesAdmin == null) {
            credencialesAdmin = adminBootstrapResult.consumir()
                    .orElseThrow(() -> new IllegalStateException("El admin inicial no se genero"));
        }
        authService.cerrarSesion();
        authService.autenticar(credencialesAdmin.username(), credencialesAdmin.password());

        if (institucionService.obtenerPrincipal().isEmpty()) {
            institucionService.guardar("IESPP Juliaca", "JULI", "Av. Principal 123", null, null);
        }

        ProgramaEstudio programa = programaEstudioService.crear("FIC" + System.nanoTime(), "Comunicacion", null, 10);
        PlanEstudio pln = planEstudioService.crear(programa.getId(), "PF" + System.nanoTime(), "Plan ficha", null, null);
        ciclo1 = cicloService.crear(pln.getId(), 1, "Primer ciclo", 1);
        ciclo2 = cicloService.crear(pln.getId(), 2, "Segundo ciclo", 2);

        cursoRepetido = cursoService.crear("REP" + System.nanoTime(), "Curso con recuperacion", 4, null, null);
        cursoSinRegistro = cursoService.crear("SIN" + System.nanoTime(), "Curso sin nota", 3, null, null);
        cursoCiclo2 = cursoService.crear("C2C" + System.nanoTime(), "Curso ciclo dos", 5, null, null);

        planCursoService.asignar(pln.getId(), ciclo1.getId(), cursoRepetido.getId(), 1, true);
        planCursoService.asignar(pln.getId(), ciclo1.getId(), cursoSinRegistro.getId(), 2, true);
        planCursoService.asignar(pln.getId(), ciclo2.getId(), cursoCiclo2.getId(), 1, true);

        PeriodoAcademico prdVj = periodoAcademicoService.crear("FICV" + System.nanoTime(), 2025, "II", null, null);
        PeriodoAcademico prdNv = periodoAcademicoService.crear("FICN" + System.nanoTime(), 2026, "I", null, null);

        Estudiante estudiante = estudianteService.crear("EST" + System.nanoTime(), null, "Maria", "Condori" + System.nanoTime(),
                null, null, null, null, null, null);
        trayectoria = trayectoriaService.iniciar(estudiante.getId(), programa.getId(), pln.getId(), null, null);

        aprobadoId = idSituacion("APROBADO");
        desaprobadoId = idSituacion("DESAPROBADO");

        Long plnCrsRptdId = planCursoIdDe(ciclo1, cursoRepetido);
        registroAcademicoService.guardarMasivo(trayectoria.getId(), prdVj.getId(),
                List.of(new ItemNota(plnCrsRptdId, 8.0, desaprobadoId, "primer intento")));
        registroAcademicoService.guardarMasivo(trayectoria.getId(), prdNv.getId(),
                List.of(new ItemNota(plnCrsRptdId, 16.0, aprobadoId, "recuperacion")));

        Long plnCrsCcl2Id = planCursoIdDe(ciclo2, cursoCiclo2);
        registroAcademicoService.guardarMasivo(trayectoria.getId(), prdNv.getId(),
                List.of(new ItemNota(plnCrsCcl2Id, 18.0, aprobadoId, null)));

    }

    private Long idSituacion(String cdg) {
        return situacionAcademicaRepository.findAll().stream()
                .filter(s -> s.getCodigo().equals(cdg)).findFirst().orElseThrow().getId();
    }

    private Long planCursoIdDe(Ciclo ccl, Curso crs) {
        List<PlanCurso> plnCrss = registroAcademicoService.listarVigentesPorCiclo(trayectoria.getId(), ccl.getId())
                .stream().map(f -> f.planCurso()).toList();
        return plnCrss.stream()
                .filter(pc -> pc.getCurso().getId().equals(crs.getId()))
                .findFirst().orElseThrow().getId();
    }

    @Test
    void construirDatosAgrupaPorCicloEnOrden() {
        DatosFicha dts = fichaService.construirDatos(trayectoria.getId());

        assertThat(dts.ciclos()).hasSize(2);
        assertThat(dts.ciclos().get(0).numero()).isEqualTo(1);
        assertThat(dts.ciclos().get(1).numero()).isEqualTo(2);
        assertThat(dts.estudianteNombreCompleto()).contains("Condori");
    }

    @Test
    void cursoSinNotaApareceSinNota() {
        DatosFicha dts = fichaService.construirDatos(trayectoria.getId());

        FilaFichaCiclo flSinRgstr = dts.ciclos().get(0).filas().stream()
                .filter(f -> f.getCursoCodigo().equals(cursoSinRegistro.getCodigo()))
                .findFirst().orElseThrow();

        assertThat(flSinRgstr.getNotaFinal()).isNull();
    }

    @Test
    void cursoConRecuperacionMuestraSoloLaNotaMasReciente() {
        DatosFicha dts = fichaService.construirDatos(trayectoria.getId());

        List<FilaFichaCiclo> flsCrsRptd = dts.ciclos().get(0).filas().stream()
                .filter(f -> f.getCursoCodigo().equals(cursoRepetido.getCodigo()))
                .toList();

        assertThat(flsCrsRptd).hasSize(1);
        assertThat(flsCrsRptd.get(0).getNotaFinal()).isEqualTo(16.0);
        assertThat(flsCrsRptd.get(0).getObservacion()).isEqualTo("recuperacion");
    }

    @Test
    void laFechaDeCadaFilaEsLaDeFinDePeriodoNoLaDeHoy() {
        PeriodoAcademico prdConFchs = periodoAcademicoService.crear("FICF" + System.nanoTime(), 2026, "I",
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 7, 15));
        Long plnCrsCcl2Id = planCursoIdDe(ciclo2, cursoCiclo2);
        registroAcademicoService.guardarMasivo(trayectoria.getId(), prdConFchs.getId(),
                List.of(new ItemNota(plnCrsCcl2Id, 14.0, aprobadoId, null)));

        DatosFicha dts = fichaService.construirDatos(trayectoria.getId());
        FilaFichaCiclo fl = dts.ciclos().get(1).filas().get(0);

        assertThat(fl.getFechaRegistro()).isEqualTo("15/07/2026");
    }

    @Test
    void siLaFilaTieneObservacionNoMuestraFecha() {
        PeriodoAcademico prdConFchs = periodoAcademicoService.crear("FICO" + System.nanoTime(), 2026, "I",
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 7, 15));
        Long plnCrsCcl2Id = planCursoIdDe(ciclo2, cursoCiclo2);
        registroAcademicoService.guardarMasivo(trayectoria.getId(), prdConFchs.getId(),
                List.of(new ItemNota(plnCrsCcl2Id, 14.0, aprobadoId, "nota del docente")));

        DatosFicha dts = fichaService.construirDatos(trayectoria.getId());
        FilaFichaCiclo fl = dts.ciclos().get(1).filas().get(0);

        assertThat(fl.getObservacion()).isEqualTo("nota del docente");
        assertThat(fl.getFechaRegistro()).isNull();
    }

    @Test
    void promedioDelPrimerCicloExcluyeElCursoSinRegistro() {
        DatosFicha dts = fichaService.construirDatos(trayectoria.getId());

        var prmdCcl1 = dts.ciclos().get(0).promedio();

        assertThat(prmdCcl1.totalCreditos()).isEqualTo(cursoRepetido.getCreditos());
        assertThat(prmdCcl1.promedio()).isEqualTo(16.0);
    }

    @Test
    void elPieDelCicloTrae_laSumaDePuntajeYSusCreditos() {
        DatosFicha dts = fichaService.construirDatos(trayectoria.getId());

        FilaFichaCiclo fl = dts.ciclos().get(0).filas().get(0);

        assertThat(fl.getTotalCreditosCiclo()).isEqualTo(cursoRepetido.getCreditos());
        assertThat(fl.getSumaPonderadaCiclo()).isEqualTo(16.0 * cursoRepetido.getCreditos());
    }

    @Test
    void laFichaImpresaUsaLaFechaDeHoyYLosRotulosNuevos() {
        String txt = textoDe(fichaService.previsualizar(trayectoria.getId()));

        assertThat(txt).contains("Fecha de emision: "
                + LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        assertThat(txt).contains("Puntaje", "Secretario Académico", "Director General");
        assertThat(txt).doesNotContain("Situacion", "Ponderado\n", "Firma del responsable", "Sello");
        assertThat(txt).doesNotContain(cursoRepetido.getCodigo() + " - ");
    }

    private static String textoDe(JasperPrint print) {
        StringBuilder txt = new StringBuilder();
        for (var pgn : print.getPages()) {
            for (var elmnt : pgn.getElements()) {
                if (elmnt instanceof net.sf.jasperreports.engine.JRPrintText t) {
                    txt.append(t.getFullText()).append('\n');
                }
            }
        }
        return txt.toString();
    }

    @Test
    void previsualizarNoDejaRastroNiConsumeCorrelativo() {
        JasperPrint print = fichaService.previsualizar(trayectoria.getId());

        assertThat(print).isNotNull();
        assertThat(print.getPages()).isNotEmpty();
        assertThat(fichaService.listarPorTrayectoria(trayectoria.getId())).isEmpty();

        fichaService.previsualizar(trayectoria.getId());
        assertThat(fichaService.listarPorTrayectoria(trayectoria.getId())).isEmpty();
    }

    @Test
    void confirmarYGuardarProduceUnPdfValidoYUnaFilaDeHistorial() throws IOException {
        FichaConfirmada cnfrmd = fichaService.confirmarYGuardar(trayectoria.getId());
        FichaGenerada registro = cnfrmd.registro();

        assertThat(registro.getId()).isNotNull();
        assertThat(registro.getEstado()).isEqualTo(EstadoFicha.GENERADA);
        assertThat(registro.getCorrelativo()).isNotBlank();
        assertThat(registro.getAnio()).isEqualTo(LocalDate.now().getYear());

        Path archv = Path.of(registro.getArchivoPath());
        assertThat(Files.exists(archv)).isTrue();
        assertThat(Files.size(archv)).isGreaterThan(0);
        byte[] cntnd = Files.readAllBytes(archv);
        String frm = new String(cntnd, 0, 4, StandardCharsets.US_ASCII);
        assertThat(frm).isEqualTo("%PDF");

        assertThat(cnfrmd.print()).isNotNull();
        assertThat(cnfrmd.print().getPages()).isNotEmpty();

        List<FichaGenerada> hstrl = fichaService.listarPorTrayectoria(trayectoria.getId());
        assertThat(hstrl).hasSize(1);
        assertThat(hstrl.get(0).getId()).isEqualTo(registro.getId());
    }

    @Test
    void correlativoEsSecuencialDentroDelMismoAnio() {
        FichaGenerada prmr = fichaService.confirmarYGuardar(trayectoria.getId()).registro();
        FichaGenerada sgnd = fichaService.confirmarYGuardar(trayectoria.getId()).registro();

        assertThat(sgnd.getNumeroSecuencial()).isEqualTo(prmr.getNumeroSecuencial() + 1);
        assertThat(sgnd.getAnio()).isEqualTo(prmr.getAnio());
        assertThat(sgnd.getCorrelativo()).isNotEqualTo(prmr.getCorrelativo());
    }

    @Test
    void anularCambiaEstadoYRegistraElMotivo() {
        FichaGenerada registro = fichaService.confirmarYGuardar(trayectoria.getId()).registro();

        FichaGenerada anld = fichaService.anular(registro.getId(), "Duplicada por error");

        assertThat(anld.getEstado()).isEqualTo(EstadoFicha.ANULADA);
        assertThat(anld.getObservacion()).isEqualTo("Duplicada por error");
    }

    @Test
    void anularRequiereRolAdmin() {
        FichaGenerada registro = fichaService.confirmarYGuardar(trayectoria.getId()).registro();

        String usrnmOprdr = "operador" + System.nanoTime();
        usuarioService.crear(usrnmOprdr, "clave1234", "Operador de prueba", RolUsuario.OPERADOR);
        authService.cerrarSesion();
        authService.autenticar(usrnmOprdr, "clave1234");

        assertThatThrownBy(() -> fichaService.anular(registro.getId(), "intento no autorizado"))
                .isInstanceOf(AccesoDenegadoException.class);
    }
}
