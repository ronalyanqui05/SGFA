package pe.edu.sgfa.reporte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import pe.edu.sgfa.common.exception.RyeAccesoDenegadoException;
import pe.edu.sgfa.common.exception.RyeValidacionException;
import pe.edu.sgfa.institucion.service.RacaInstitucionService;
import pe.edu.sgfa.reporte.domain.CphDocumentoEmitido;
import pe.edu.sgfa.reporte.domain.CphEstadoDocumento;
import pe.edu.sgfa.reporte.domain.CphTipoDocumento;
import pe.edu.sgfa.reporte.service.CphDocumentoOficialService;
import pe.edu.sgfa.reporte.service.CphTablaReporte;
import pe.edu.sgfa.usuario.domain.RyeRolUsuario;
import pe.edu.sgfa.usuario.service.RyeAdminBootstrapResult;
import pe.edu.sgfa.usuario.service.RyeAuthService;
import pe.edu.sgfa.usuario.service.RyeUsuarioService;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CphDocumentoOficialServiceTest {

    @BeforeAll
    static void baseAislada() throws IOException {
        System.setProperty("sgfa.home-dir", Files.createTempDirectory("sgfa-oficial-test-home-").toString());
    }

    @Autowired
    private CphDocumentoOficialService servicioPch;
    @Autowired
    private RacaInstitucionService institucionServicePch;
    @Autowired
    private RyeAuthService authServicePch;
    @Autowired
    private RyeAdminBootstrapResult adminBootstrapResultPch;
    @Autowired
    private RyeUsuarioService usuarioServicePch;

    private static RyeAdminBootstrapResult.RyeCredenciales credencialesPch;
    private static boolean institucionListaPch;

    @BeforeEach
    void preparar() throws IOException {
        if (credencialesPch == null) {
            credencialesPch = adminBootstrapResultPch.ryeConsumir().orElseThrow();
        }
        authServicePch.ryeCerrarSesion();
        authServicePch.ryeAutenticar(credencialesPch.username(), credencialesPch.password());
        if (!institucionListaPch) {
            institucionServicePch.racaGuardar("Escuela de Educación Superior Pedagógica Pública Juliaca", "EESPPJ", "Av. Infancia", null, null);
            Path logoPch = Files.createTempFile("logo-", ".png");
            BufferedImage imgPch = new BufferedImage(120, 140, BufferedImage.TYPE_INT_RGB);
            var g = imgPch.createGraphics();
            g.setColor(new Color(40, 90, 160));
            g.fillRect(0, 0, 120, 140);
            g.dispose();
            ImageIO.write(imgPch, "png", logoPch.toFile());
            institucionServicePch.racaActualizarLogo(logoPch);
            institucionListaPch = true;
        }
    }

    private static CphTablaReporte tabla(String tituloPch, String estudiantePch) {
        return new CphTablaReporte(tituloPch, List.of("Programa: Educación Secundaria", "Periodo: 2026-I", "Emitido: 01/01/2026"), null,
                List.of("Puesto", "Apellidos y nombres", "Promedio"), List.of(5, 30, 8), List.of(true, false, true),
                List.of(List.of("1", estudiantePch, "16.75"), List.of("2", "OTRO ESTUDIANTE", "15.00")), List.of("Nota al pie."), true);
    }

    private static String texto(Path pdfPch) throws IOException {
        try (PDDocument docPch = PDDocument.load(pdfPch.toFile())) {
            return new PDFTextStripper().getText(docPch);
        }
    }

    @Test
    void cadaTipoLlevaSuPropioCorrelativoPorAnio() {
        int anioPch = LocalDate.now().getYear();

        CphDocumentoEmitido acta1Pch = servicioPch.cphEmitir(CphTipoDocumento.ACTA, tabla("ACTA CONSOLIDADA DE NOTAS", "ALVA PEREZ"));
        CphDocumentoEmitido acta2Pch = servicioPch.cphEmitir(CphTipoDocumento.ACTA, tabla("ACTA CONSOLIDADA DE NOTAS", "ALVA PEREZ"));
        CphDocumentoEmitido meritoPch = servicioPch.cphEmitir(CphTipoDocumento.ORDEN_MERITO, tabla("ORDEN DE MERITO", "ALVA PEREZ"));
        CphDocumentoEmitido merito2Pch = servicioPch.cphEmitir(CphTipoDocumento.ORDEN_MERITO, tabla("ORDEN DE MERITO", "ALVA PEREZ"));

        assertThat(acta2Pch.getNumeroSecuencial()).isEqualTo(acta1Pch.getNumeroSecuencial() + 1);
        assertThat(acta1Pch.getCorrelativo()).startsWith("AC-").endsWith("-" + anioPch);
        assertThat(acta2Pch.getCorrelativo()).isEqualTo(String.format("AC-%06d-%d", acta2Pch.getNumeroSecuencial(), anioPch));
        assertThat(meritoPch.getCorrelativo()).startsWith("OM-").endsWith("-" + anioPch);

        assertThat(merito2Pch.getNumeroSecuencial()).isEqualTo(meritoPch.getNumeroSecuencial() + 1);
        assertThat(merito2Pch.getCorrelativo()).isEqualTo(String.format("OM-%06d-%d", merito2Pch.getNumeroSecuencial(), anioPch));
        assertThat(meritoPch.getEstado()).isEqualTo(CphEstadoDocumento.EMITIDO);
    }

    @Test
    void elPdfOficialLlevaLogoNumeroCodigoYLineasDeFirma() throws Exception {
        CphDocumentoEmitido d = servicioPch.cphEmitir(CphTipoDocumento.ORDEN_MERITO, tabla("ORDEN DE MERITO", "QUISPE MAMANI"));

        Path pdfPch = servicioPch.cphArchivo(d);
        assertThat(pdfPch).exists();
        assertThat(pdfPch.getParent().getFileName().toString()).isEqualTo("oficiales");
        String textoPch = texto(pdfPch);
        assertThat(textoPch).contains("Escuela de Educación Superior Pedagógica Pública Juliaca", "ORDEN DE MERITO",
                "N° " + d.getCorrelativo(), "Codigo de verificacion: " + d.getCodigoVerificacion(), "QUISPE MAMANI",
                "Secretario Académico", "Director General", "(Firma y sello)", "Documento oficial", "Nota al pie.");
        assertThat(d.getCodigoVerificacion()).matches("[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}");

        try (PDDocument docPch = PDDocument.load(pdfPch.toFile())) {
            assertThat(docPch.getPage(0).getResources().getXObjectNames()).isNotEmpty();
        }
    }

    @Test
    void elHistorialListaLoMasRecientePrimeroYGuardaLosFiltros() {
        CphDocumentoEmitido a = servicioPch.cphEmitir(CphTipoDocumento.ACTA, tabla("ACTA CONSOLIDADA DE NOTAS", "UNO"));
        CphDocumentoEmitido b = servicioPch.cphEmitir(CphTipoDocumento.ACTA, tabla("ACTA CONSOLIDADA DE NOTAS", "DOS"));

        List<CphDocumentoEmitido> historialPch = servicioPch.cphListar();

        List<String> correlativosPch = historialPch.stream().map(CphDocumentoEmitido::getCorrelativo).toList();
        assertThat(correlativosPch.indexOf(b.getCorrelativo())).isGreaterThanOrEqualTo(0)
                .isLessThan(correlativosPch.indexOf(a.getCorrelativo()));
        assertThat(b.getDescripcion()).isEqualTo("Programa: Educación Secundaria | Periodo: 2026-I");
        assertThat(b.getUsuario()).isNotNull();
    }

    @Test
    void elCodigoDeVerificacionCambiaSiCambiaElContenido() {
        CphDocumentoEmitido a = servicioPch.cphEmitir(CphTipoDocumento.ACTA, tabla("ACTA CONSOLIDADA DE NOTAS", "ALVA"));
        CphDocumentoEmitido b = servicioPch.cphEmitir(CphTipoDocumento.ACTA, tabla("ACTA CONSOLIDADA DE NOTAS", "ALVA"));
        CphDocumentoEmitido otroContenidoPch = servicioPch.cphEmitir(CphTipoDocumento.ACTA, tabla("ACTA CONSOLIDADA DE NOTAS", "OTRA PERSONA"));

        assertThat(a.getCodigoVerificacion()).isNotEqualTo(b.getCodigoVerificacion());
        assertThat(otroContenidoPch.getCodigoVerificacion()).isNotEqualTo(b.getCodigoVerificacion());
    }

    @Test
    void verificarComparaNumeroYCodigoSinImportarGuionesNiMayusculas() {
        CphDocumentoEmitido d = servicioPch.cphEmitir(CphTipoDocumento.ORDEN_MERITO, tabla("ORDEN DE MERITO", "ALVA"));

        assertThat(servicioPch.cphVerificar(d.getCorrelativo(), d.getCodigoVerificacion()).map(CphDocumentoEmitido::getCorrelativo))
                .contains(d.getCorrelativo());
        assertThat(servicioPch.cphVerificar(d.getCorrelativo().toLowerCase(), d.getCodigoVerificacion().toLowerCase().replace("-", " "))
                .map(CphDocumentoEmitido::getCorrelativo)).contains(d.getCorrelativo());
        assertThat(servicioPch.cphVerificar(d.getCorrelativo(), "0000-0000-0000")).isEmpty();
        assertThat(servicioPch.cphVerificar("OM-999999-2026", d.getCodigoVerificacion())).isEmpty();
        assertThat(servicioPch.cphVerificar(null, null)).isEmpty();
    }

    @Test
    void anularConservaElArchivoYExigeMotivo() {
        CphDocumentoEmitido d = servicioPch.cphEmitir(CphTipoDocumento.ACTA, tabla("ACTA CONSOLIDADA DE NOTAS", "ALVA"));

        assertThatThrownBy(() -> servicioPch.cphAnular(d.getId(), " ")).isInstanceOf(RyeValidacionException.class);
        CphDocumentoEmitido anuladoPch = servicioPch.cphAnular(d.getId(), "Emitido con el periodo equivocado");

        assertThat(anuladoPch.getEstado()).isEqualTo(CphEstadoDocumento.ANULADO);
        assertThat(anuladoPch.getObservacion()).isEqualTo("Emitido con el periodo equivocado");
        assertThat(servicioPch.cphArchivo(anuladoPch)).exists();
    }

    @Test
    void soloElAdministradorPuedeAnular() {
        CphDocumentoEmitido d = servicioPch.cphEmitir(CphTipoDocumento.ACTA, tabla("ACTA CONSOLIDADA DE NOTAS", "ALVA"));
        String operadorPch = "operador" + System.nanoTime();
        usuarioServicePch.ryeCrear(operadorPch, "clave1234", "Operador de prueba", RyeRolUsuario.OPERADOR);
        authServicePch.ryeCerrarSesion();
        authServicePch.ryeAutenticar(operadorPch, "clave1234");

        CphDocumentoEmitido delOperadorPch = servicioPch.cphEmitir(CphTipoDocumento.ACTA, tabla("ACTA CONSOLIDADA DE NOTAS", "BETO"));
        assertThat(servicioPch.cphListar()).extracting(CphDocumentoEmitido::getCorrelativo)
                .contains(d.getCorrelativo(), delOperadorPch.getCorrelativo());
        assertThatThrownBy(() -> servicioPch.cphAnular(d.getId(), "intento no autorizado")).isInstanceOf(RyeAccesoDenegadoException.class);
    }
}
