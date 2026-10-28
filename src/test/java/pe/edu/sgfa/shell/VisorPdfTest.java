package pe.edu.sgfa.shell;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class VisorPdfTest {

    @BeforeAll
    static void iniciarJavaFx() {
        try {
            CompletableFuture<Void> arrnq = new CompletableFuture<>();
            Platform.startup(() -> arrnq.complete(null));
            arrnq.get(20, TimeUnit.SECONDS);
        } catch (IllegalStateException yaIncd) {

        } catch (Exception | Error e) {
            Assumptions.abort("JavaFX no se pudo iniciar en este entorno: " + e);
        }
    }

    private static <T> T enHiloFx(Supplier<T> accn) throws Exception {
        CompletableFuture<T> rsltd = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                rsltd.complete(accn.get());
            } catch (Throwable t) {
                rsltd.completeExceptionally(t);
            }
        });
        return rsltd.get(30, TimeUnit.SECONDS);
    }

    private static byte[] pdfDeTresPaginas() throws Exception {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream sld = new ByteArrayOutputStream()) {
            for (int i = 1; i <= 3; i++) {
                PDPage pgn = new PDPage(PDRectangle.A4);
                doc.addPage(pgn);
                try (PDPageContentStream c = new PDPageContentStream(doc, pgn)) {
                    c.beginText();
                    c.setFont(PDType1Font.HELVETICA_BOLD, 40);
                    c.newLineAtOffset(80, 700);
                    c.showText("PAGINA " + i);
                    c.endText();
                }
            }
            doc.save(sld);
            return sld.toByteArray();
        }
    }

    private static VisorPdf visorEnEscena() throws Exception {
        return enHiloFx(() -> {
            VisorPdf vsr = new VisorPdf();
            Scene escn = new Scene(vsr, 900, 700);
            escn.getRoot().applyCss();
            vsr.layout();
            return vsr;
        });
    }

    @SuppressWarnings("unchecked")
    private static List<StackPane> marcos(VisorPdf vsr) {
        return (List<StackPane>) ReflectionTestUtils.getField(vsr, "marcos");
    }

    private static void esperarPaginas(VisorPdf vsr, int cntdd) throws Exception {
        for (int i = 0; i < 100; i++) {
            Thread.sleep(100);
            boolean lsts = enHiloFx(() -> marcos(vsr).size() == cntdd && marcos(vsr).stream()
                    .allMatch(m -> ((ImageView) m.getChildren().get(0)).getImage() != null));
            if (lsts) {
                return;
            }
        }
        throw new AssertionError("el visor no termino de dibujar las " + cntdd + " paginas");
    }

    private static boolean tieneTintaOscura(ImageView vst) {
        var imgn = vst.getImage();
        var lctr = imgn.getPixelReader();
        for (int y = 0; y < imgn.getHeight(); y += 2) {
            for (int x = 0; x < imgn.getWidth(); x += 2) {
                if (lctr.getColor(x, y).getBrightness() < 0.3) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    void dibujaTodasLasPaginasConSuContenido() throws Exception {
        VisorPdf vsr = visorEnEscena();

        enHiloFx(() -> {
            try {
                vsr.mostrar(pdfDeTresPaginas(), "prueba.pdf");
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            return null;
        });
        esperarPaginas(vsr, 3);

        assertThat(enHiloFx(vsr::paginas)).isEqualTo(3);
        assertThat(enHiloFx(vsr::paginaActual)).isEqualTo(1);

        assertThat(enHiloFx(() -> marcos(vsr).stream().allMatch(m -> tieneTintaOscura((ImageView) m.getChildren().get(0))))).isTrue();

        double zoom = enHiloFx(vsr::zoom);
        assertThat(zoom).isBetween(0.5, 1.2);
        Label ttl = findTotal(vsr);
        assertThat(enHiloFx(ttl::getText)).isEqualTo("de 3");
    }

    private static Label findTotal(VisorPdf vsr) {
        return (Label) ReflectionTestUtils.getField(vsr, "totalLabel");
    }

    @Test
    void acercarYAlejarCambianElTamanoDeLasPaginas() throws Exception {
        VisorPdf vsr = visorEnEscena();
        enHiloFx(() -> {
            try {
                vsr.mostrar(pdfDeTresPaginas(), "prueba.pdf");
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            return null;
        });
        esperarPaginas(vsr, 3);
        double anchIncl = enHiloFx(() -> marcos(vsr).get(0).getPrefWidth());
        double zmIncl = enHiloFx(vsr::zoom);

        enHiloFx(() -> {
            vsr.acercar();
            return null;
        });
        esperarPaginas(vsr, 3);
        assertThat(enHiloFx(vsr::zoom)).isGreaterThan(zmIncl);
        assertThat(enHiloFx(() -> marcos(vsr).get(0).getPrefWidth())).isGreaterThan(anchIncl);

        enHiloFx(() -> {
            vsr.aplicarModo("200%");
            return null;
        });
        esperarPaginas(vsr, 3);
        assertThat(enHiloFx(vsr::zoom)).isEqualTo(2.0);

        assertThat(enHiloFx(() -> marcos(vsr).get(0).getPrefWidth())).isBetween(1580.0, 1595.0);

        enHiloFx(() -> {
            vsr.alejar();
            return null;
        });
        esperarPaginas(vsr, 3);
        assertThat(enHiloFx(vsr::zoom)).isLessThan(2.0);

        enHiloFx(() -> {
            vsr.aplicarModo("Ajustar al ancho");
            return null;
        });
        esperarPaginas(vsr, 3);
        assertThat(enHiloFx(vsr::zoom)).isBetween(0.5, 1.2);
    }

    @Test
    void irAUnaPaginaDesplazaYActualizaElNumero() throws Exception {
        VisorPdf vsr = visorEnEscena();
        enHiloFx(() -> {
            try {
                vsr.mostrar(pdfDeTresPaginas(), "prueba.pdf");
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            return null;
        });
        esperarPaginas(vsr, 3);

        enHiloFx(() -> {
            vsr.irAPagina(3);
            return null;
        });
        Thread.sleep(400);

        assertThat(enHiloFx(vsr::paginaActual)).isEqualTo(3);
        enHiloFx(() -> {
            vsr.irAPagina(99);
            return null;
        });
        Thread.sleep(300);
        assertThat(enHiloFx(vsr::paginaActual)).isEqualTo(3);
        enHiloFx(() -> {
            vsr.irAPagina(1);
            return null;
        });
        Thread.sleep(400);
        assertThat(enHiloFx(vsr::paginaActual)).isEqualTo(1);
    }

    @Test
    void liberarVaciaElVisor() throws Exception {
        VisorPdf vsr = visorEnEscena();
        enHiloFx(() -> {
            try {
                vsr.mostrar(pdfDeTresPaginas(), "prueba.pdf");
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            return null;
        });
        esperarPaginas(vsr, 3);

        enHiloFx(() -> {
            vsr.liberar();
            return null;
        });

        assertThat(enHiloFx(vsr::paginas)).isZero();
        assertThat(enHiloFx(() -> findTotal(vsr).getText())).isEqualTo("de 0");
    }
}
