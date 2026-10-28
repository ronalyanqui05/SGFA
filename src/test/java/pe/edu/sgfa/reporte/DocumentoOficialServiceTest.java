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

import pe.edu.sgfa.common.exception.AccesoDenegadoException;
import pe.edu.sgfa.common.exception.ValidacionException;
import pe.edu.sgfa.institucion.service.InstitucionService;
import pe.edu.sgfa.reporte.domain.DocumentoEmitido;
import pe.edu.sgfa.reporte.domain.EstadoDocumento;
import pe.edu.sgfa.reporte.domain.TipoDocumento;
import pe.edu.sgfa.reporte.service.DocumentoOficialService;
import pe.edu.sgfa.reporte.service.TablaReporte;
import pe.edu.sgfa.usuario.domain.RolUsuario;
import pe.edu.sgfa.usuario.service.AdminBootstrapResult;
import pe.edu.sgfa.usuario.service.AuthService;
import pe.edu.sgfa.usuario.service.UsuarioService;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DocumentoOficialServiceTest {

    @BeforeAll
    static void baseAislada() throws IOException {
        System.setProperty("sgfa.home-dir", Files.createTempDirectory("sgfa-oficial-test-home-").toString());
    }

    @Autowired
    private DocumentoOficialService servicio;
    @Autowired
    private InstitucionService institucionService;
    @Autowired
    private AuthService authService;
    @Autowired
    private AdminBootstrapResult adminBootstrapResult;
    @Autowired
    private UsuarioService usuarioService;

    private static AdminBootstrapResult.Credenciales credenciales;
    private static boolean institucionLista;

    @BeforeEach
    void preparar() throws IOException {
        if (credenciales == null) {
            credenciales = adminBootstrapResult.consumir().orElseThrow();
        }
        authService.cerrarSesion();
        authService.autenticar(credenciales.username(), credenciales.password());
        if (!institucionLista) {
            institucionService.guardar("Escuela de Educación Superior Pedagógica Pública Juliaca", "EESPPJ", "Av. Infancia", null, null);
            Path lg = Files.createTempFile("logo-", ".png");
            BufferedImage img = new BufferedImage(120, 140, BufferedImage.TYPE_INT_RGB);
            var g = img.createGraphics();
            g.setColor(new Color(40, 90, 160));
            g.fillRect(0, 0, 120, 140);
            g.dispose();
            ImageIO.write(img, "png", lg.toFile());
            institucionService.actualizarLogo(lg);
            institucionLista = true;
        }
    }

    private static TablaReporte tabla(String ttl, String estdnt) {
        return new TablaReporte(ttl, List.of("Programa: Educación Secundaria", "Periodo: 2026-I", "Emitido: 01/01/2026"), null,
                List.of("Puesto", "Apellidos y nombres", "Promedio"), List.of(5, 30, 8), List.of(true, false, true),
                List.of(List.of("1", estdnt, "16.75"), List.of("2", "OTRO ESTUDIANTE", "15.00")), List.of("Nota al pie."), true);
    }

    private static String texto(Path pdf) throws IOException {
        try (PDDocument doc = PDDocument.load(pdf.toFile())) {
            return new PDFTextStripper().getText(doc);
        }
    }

    @Test
    void cadaTipoLlevaSuPropioCorrelativoPorAnio() {
        int an = LocalDate.now().getYear();

        DocumentoEmitido act1 = servicio.emitir(TipoDocumento.ACTA, tabla("ACTA CONSOLIDADA DE NOTAS", "ALVA PEREZ"));
        DocumentoEmitido act2 = servicio.emitir(TipoDocumento.ACTA, tabla("ACTA CONSOLIDADA DE NOTAS", "ALVA PEREZ"));
        DocumentoEmitido mrt = servicio.emitir(TipoDocumento.ORDEN_MERITO, tabla("ORDEN DE MERITO", "ALVA PEREZ"));
        DocumentoEmitido mrt2 = servicio.emitir(TipoDocumento.ORDEN_MERITO, tabla("ORDEN DE MERITO", "ALVA PEREZ"));

        assertThat(act2.getNumeroSecuencial()).isEqualTo(act1.getNumeroSecuencial() + 1);
        assertThat(act1.getCorrelativo()).startsWith("AC-").endsWith("-" + an);
        assertThat(act2.getCorrelativo()).isEqualTo(String.format("AC-%06d-%d", act2.getNumeroSecuencial(), an));
        assertThat(mrt.getCorrelativo()).startsWith("OM-").endsWith("-" + an);

        assertThat(mrt2.getNumeroSecuencial()).isEqualTo(mrt.getNumeroSecuencial() + 1);
        assertThat(mrt2.getCorrelativo()).isEqualTo(String.format("OM-%06d-%d", mrt2.getNumeroSecuencial(), an));
        assertThat(mrt.getEstado()).isEqualTo(EstadoDocumento.EMITIDO);
    }

    @Test
    void elPdfOficialLlevaLogoNumeroCodigoYLineasDeFirma() throws Exception {
        DocumentoEmitido d = servicio.emitir(TipoDocumento.ORDEN_MERITO, tabla("ORDEN DE MERITO", "QUISPE MAMANI"));

        Path pdf = servicio.archivo(d);
        assertThat(pdf).exists();
        assertThat(pdf.getParent().getFileName().toString()).isEqualTo("oficiales");
        String texto = texto(pdf);
        assertThat(texto).contains("Escuela de Educación Superior Pedagógica Pública Juliaca", "ORDEN DE MERITO",
                "N° " + d.getCorrelativo(), "Codigo de verificacion: " + d.getCodigoVerificacion(), "QUISPE MAMANI",
                "Secretario Académico", "Director General", "(Firma y sello)", "Documento oficial", "Nota al pie.");
        assertThat(d.getCodigoVerificacion()).matches("[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}");

        try (PDDocument doc = PDDocument.load(pdf.toFile())) {
            assertThat(doc.getPage(0).getResources().getXObjectNames()).isNotEmpty();
        }
    }

    @Test
    void elHistorialListaLoMasRecientePrimeroYGuardaLosFiltros() {
        DocumentoEmitido a = servicio.emitir(TipoDocumento.ACTA, tabla("ACTA CONSOLIDADA DE NOTAS", "UNO"));
        DocumentoEmitido b = servicio.emitir(TipoDocumento.ACTA, tabla("ACTA CONSOLIDADA DE NOTAS", "DOS"));

        List<DocumentoEmitido> hstrl = servicio.listar();

        List<String> crrltvs = hstrl.stream().map(DocumentoEmitido::getCorrelativo).toList();
        assertThat(crrltvs.indexOf(b.getCorrelativo())).isGreaterThanOrEqualTo(0)
                .isLessThan(crrltvs.indexOf(a.getCorrelativo()));
        assertThat(b.getDescripcion()).isEqualTo("Programa: Educación Secundaria | Periodo: 2026-I");
        assertThat(b.getUsuario()).isNotNull();
    }

    @Test
    void elCodigoDeVerificacionCambiaSiCambiaElContenido() {
        DocumentoEmitido a = servicio.emitir(TipoDocumento.ACTA, tabla("ACTA CONSOLIDADA DE NOTAS", "ALVA"));
        DocumentoEmitido b = servicio.emitir(TipoDocumento.ACTA, tabla("ACTA CONSOLIDADA DE NOTAS", "ALVA"));
        DocumentoEmitido otrCntnd = servicio.emitir(TipoDocumento.ACTA, tabla("ACTA CONSOLIDADA DE NOTAS", "OTRA PERSONA"));

        assertThat(a.getCodigoVerificacion()).isNotEqualTo(b.getCodigoVerificacion());
        assertThat(otrCntnd.getCodigoVerificacion()).isNotEqualTo(b.getCodigoVerificacion());
    }

    @Test
    void verificarComparaNumeroYCodigoSinImportarGuionesNiMayusculas() {
        DocumentoEmitido d = servicio.emitir(TipoDocumento.ORDEN_MERITO, tabla("ORDEN DE MERITO", "ALVA"));

        assertThat(servicio.verificar(d.getCorrelativo(), d.getCodigoVerificacion()).map(DocumentoEmitido::getCorrelativo))
                .contains(d.getCorrelativo());
        assertThat(servicio.verificar(d.getCorrelativo().toLowerCase(), d.getCodigoVerificacion().toLowerCase().replace("-", " "))
                .map(DocumentoEmitido::getCorrelativo)).contains(d.getCorrelativo());
        assertThat(servicio.verificar(d.getCorrelativo(), "0000-0000-0000")).isEmpty();
        assertThat(servicio.verificar("OM-999999-2026", d.getCodigoVerificacion())).isEmpty();
        assertThat(servicio.verificar(null, null)).isEmpty();
    }

    @Test
    void anularConservaElArchivoYExigeMotivo() {
        DocumentoEmitido d = servicio.emitir(TipoDocumento.ACTA, tabla("ACTA CONSOLIDADA DE NOTAS", "ALVA"));

        assertThatThrownBy(() -> servicio.anular(d.getId(), " ")).isInstanceOf(ValidacionException.class);
        DocumentoEmitido anld = servicio.anular(d.getId(), "Emitido con el periodo equivocado");

        assertThat(anld.getEstado()).isEqualTo(EstadoDocumento.ANULADO);
        assertThat(anld.getObservacion()).isEqualTo("Emitido con el periodo equivocado");
        assertThat(servicio.archivo(anld)).exists();
    }

    @Test
    void soloElAdministradorPuedeAnular() {
        DocumentoEmitido d = servicio.emitir(TipoDocumento.ACTA, tabla("ACTA CONSOLIDADA DE NOTAS", "ALVA"));
        String oprdr = "operador" + System.nanoTime();
        usuarioService.crear(oprdr, "clave1234", "Operador de prueba", RolUsuario.OPERADOR);
        authService.cerrarSesion();
        authService.autenticar(oprdr, "clave1234");

        DocumentoEmitido delOprdr = servicio.emitir(TipoDocumento.ACTA, tabla("ACTA CONSOLIDADA DE NOTAS", "BETO"));
        assertThat(servicio.listar()).extracting(DocumentoEmitido::getCorrelativo)
                .contains(d.getCorrelativo(), delOprdr.getCorrelativo());
        assertThatThrownBy(() -> servicio.anular(d.getId(), "intento no autorizado")).isInstanceOf(AccesoDenegadoException.class);
    }
}
