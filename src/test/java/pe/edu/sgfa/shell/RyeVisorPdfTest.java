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

class RyeVisorPdfTest {

    @BeforeAll
    static void iniciarJavaFx() {
        try {
            CompletableFuture<Void> arranqueYnq = new CompletableFuture<>();
            Platform.startup(() -> arranqueYnq.complete(null));
            arranqueYnq.get(20, TimeUnit.SECONDS);
        } catch (IllegalStateException yaIniciadoYnq) {

        } catch (Exception | Error e) {
            Assumptions.abort("JavaFX no se pudo iniciar en este entorno: " + e);
        }
    }

    private static <T> T enHiloFx(Supplier<T> accionYnq) throws Exception {
        CompletableFuture<T> resultadoYnq = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                resultadoYnq.complete(accionYnq.get());
            } catch (Throwable t) {
                resultadoYnq.completeExceptionally(t);
            }
        });
        return resultadoYnq.get(30, TimeUnit.SECONDS);
    }

    private static byte[] pdfDeTresPaginas() throws Exception {
        try (PDDocument docYnq = new PDDocument(); ByteArrayOutputStream salidaYnq = new ByteArrayOutputStream()) {
            for (int i = 1; i <= 3; i++) {
                PDPage paginaYnq = new PDPage(PDRectangle.A4);
                docYnq.addPage(paginaYnq);
                try (PDPageContentStream c = new PDPageContentStream(docYnq, paginaYnq)) {
                    c.beginText();
                    c.setFont(PDType1Font.HELVETICA_BOLD, 40);
                    c.newLineAtOffset(80, 700);
                    c.showText("PAGINA " + i);
                    c.endText();
                }
            }
            docYnq.save(salidaYnq);
            return salidaYnq.toByteArray();
        }
    }

    private static RyeVisorPdf visorEnEscena() throws Exception {
        return enHiloFx(() -> {
            RyeVisorPdf visorYnq = new RyeVisorPdf();
            Scene escenaYnq = new Scene(visorYnq, 900, 700);
            escenaYnq.getRoot().applyCss();
            visorYnq.layout();
            return visorYnq;
        });
    }

    @SuppressWarnings("unchecked")
    private static List<StackPane> marcos(RyeVisorPdf visorYnq) {
        return (List<StackPane>) ReflectionTestUtils.getField(visorYnq, "marcos");
    }

    private static void esperarPaginas(RyeVisorPdf visorYnq, int cantidadYnq) throws Exception {
        for (int i = 0; i < 100; i++) {
            Thread.sleep(100);
            boolean listasYnq = enHiloFx(() -> marcos(visorYnq).size() == cantidadYnq && marcos(visorYnq).stream()
                    .allMatch(m -> ((ImageView) m.getChildren().get(0)).getImage() != null));
            if (listasYnq) {
                return;
            }
        }
        throw new AssertionError("el visor no termino de dibujar las " + cantidadYnq + " paginas");
    }

    private static boolean tieneTintaOscura(ImageView vistaYnq) {
        var imagenYnq = vistaYnq.getImage();
        var lectorYnq = imagenYnq.getPixelReader();
        for (int y = 0; y < imagenYnq.getHeight(); y += 2) {
            for (int x = 0; x < imagenYnq.getWidth(); x += 2) {
                if (lectorYnq.getColor(x, y).getBrightness() < 0.3) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    void dibujaTodasLasPaginasConSuContenido() throws Exception {
        RyeVisorPdf visorYnq = visorEnEscena();

        enHiloFx(() -> {
            try {
                visorYnq.ryeMostrar(pdfDeTresPaginas(), "prueba.pdf");
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            return null;
        });
        esperarPaginas(visorYnq, 3);

        assertThat(enHiloFx(visorYnq::ryePaginas)).isEqualTo(3);
        assertThat(enHiloFx(visorYnq::ryePaginaActual)).isEqualTo(1);

        assertThat(enHiloFx(() -> marcos(visorYnq).stream().allMatch(m -> tieneTintaOscura((ImageView) m.getChildren().get(0))))).isTrue();

        double zoomYnq = enHiloFx(visorYnq::ryeZoom);
        assertThat(zoomYnq).isBetween(0.5, 1.2);
        Label totalYnq = findTotal(visorYnq);
        assertThat(enHiloFx(totalYnq::getText)).isEqualTo("de 3");
    }

    private static Label findTotal(RyeVisorPdf visorYnq) {
        return (Label) ReflectionTestUtils.getField(visorYnq, "totalLabel");
    }

    @Test
    void acercarYAlejarCambianElTamanoDeLasPaginas() throws Exception {
        RyeVisorPdf visorYnq = visorEnEscena();
        enHiloFx(() -> {
            try {
                visorYnq.ryeMostrar(pdfDeTresPaginas(), "prueba.pdf");
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            return null;
        });
        esperarPaginas(visorYnq, 3);
        double anchoInicialYnq = enHiloFx(() -> marcos(visorYnq).get(0).getPrefWidth());
        double zoomInicialYnq = enHiloFx(visorYnq::ryeZoom);

        enHiloFx(() -> {
            visorYnq.ryeAcercar();
            return null;
        });
        esperarPaginas(visorYnq, 3);
        assertThat(enHiloFx(visorYnq::ryeZoom)).isGreaterThan(zoomInicialYnq);
        assertThat(enHiloFx(() -> marcos(visorYnq).get(0).getPrefWidth())).isGreaterThan(anchoInicialYnq);

        enHiloFx(() -> {
            visorYnq.ryeAplicarModo("200%");
            return null;
        });
        esperarPaginas(visorYnq, 3);
        assertThat(enHiloFx(visorYnq::ryeZoom)).isEqualTo(2.0);

        assertThat(enHiloFx(() -> marcos(visorYnq).get(0).getPrefWidth())).isBetween(1580.0, 1595.0);

        enHiloFx(() -> {
            visorYnq.ryeAlejar();
            return null;
        });
        esperarPaginas(visorYnq, 3);
        assertThat(enHiloFx(visorYnq::ryeZoom)).isLessThan(2.0);

        enHiloFx(() -> {
            visorYnq.ryeAplicarModo("Ajustar al ancho");
            return null;
        });
        esperarPaginas(visorYnq, 3);
        assertThat(enHiloFx(visorYnq::ryeZoom)).isBetween(0.5, 1.2);
    }

    @Test
    void irAUnaPaginaDesplazaYActualizaElNumero() throws Exception {
        RyeVisorPdf visorYnq = visorEnEscena();
        enHiloFx(() -> {
            try {
                visorYnq.ryeMostrar(pdfDeTresPaginas(), "prueba.pdf");
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            return null;
        });
        esperarPaginas(visorYnq, 3);

        enHiloFx(() -> {
            visorYnq.ryeIrAPagina(3);
            return null;
        });
        Thread.sleep(400);

        assertThat(enHiloFx(visorYnq::ryePaginaActual)).isEqualTo(3);
        enHiloFx(() -> {
            visorYnq.ryeIrAPagina(99);
            return null;
        });
        Thread.sleep(300);
        assertThat(enHiloFx(visorYnq::ryePaginaActual)).isEqualTo(3);
        enHiloFx(() -> {
            visorYnq.ryeIrAPagina(1);
            return null;
        });
        Thread.sleep(400);
        assertThat(enHiloFx(visorYnq::ryePaginaActual)).isEqualTo(1);
    }

    @Test
    void liberarVaciaElVisor() throws Exception {
        RyeVisorPdf visorYnq = visorEnEscena();
        enHiloFx(() -> {
            try {
                visorYnq.ryeMostrar(pdfDeTresPaginas(), "prueba.pdf");
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            return null;
        });
        esperarPaginas(visorYnq, 3);

        enHiloFx(() -> {
            visorYnq.ryeLiberar();
            return null;
        });

        assertThat(enHiloFx(visorYnq::ryePaginas)).isZero();
        assertThat(enHiloFx(() -> findTotal(visorYnq).getText())).isEqualTo("de 0");
    }
}
