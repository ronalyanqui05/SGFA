package pe.edu.sgfa.ocr.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

class OcrServiceRotarTest {

    private static final int ROJO = 0xFF0000;
    private static final int BLANCO = 0xFFFFFF;

    private static BufferedImage imagen() {
        BufferedImage img = new BufferedImage(4, 2, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 2; y++) {
                img.setRGB(x, y, BLANCO);
            }
        }
        img.setRGB(0, 0, ROJO);
        return img;
    }

    private static int rgb(BufferedImage img, int x, int y) {
        return img.getRGB(x, y) & 0xFFFFFF;
    }

    private OcrService servicio() throws Exception {
        return new OcrService(Files.createTempDirectory("tess-").toString());
    }

    @Test
    void girar90LlevaLaEsquinaSuperiorIzquierdaALaSuperiorDerecha() throws Exception {
        BufferedImage r = servicio().rotar(imagen(), 90);

        assertThat(r.getWidth()).isEqualTo(2);
        assertThat(r.getHeight()).isEqualTo(4);
        assertThat(rgb(r, 1, 0)).isEqualTo(ROJO);
        assertThat(rgb(r, 0, 0)).isEqualTo(BLANCO);
        assertThat(rgb(r, 1, 3)).isEqualTo(BLANCO);
    }

    @Test
    void girar180LlevaLaEsquinaALaInferiorDerecha() throws Exception {
        BufferedImage r = servicio().rotar(imagen(), 180);

        assertThat(r.getWidth()).isEqualTo(4);
        assertThat(r.getHeight()).isEqualTo(2);
        assertThat(rgb(r, 3, 1)).isEqualTo(ROJO);
        assertThat(rgb(r, 0, 0)).isEqualTo(BLANCO);
    }

    @Test
    void girar270LlevaLaEsquinaALaInferiorIzquierda() throws Exception {
        BufferedImage r = servicio().rotar(imagen(), 270);

        assertThat(r.getWidth()).isEqualTo(2);
        assertThat(r.getHeight()).isEqualTo(4);
        assertThat(rgb(r, 0, 3)).isEqualTo(ROJO);
        assertThat(rgb(r, 0, 0)).isEqualTo(BLANCO);
        assertThat(rgb(r, 1, 3)).isEqualTo(BLANCO);
    }

    @Test
    void girar0YMultiplosDe360NoCambianNada() throws Exception {
        BufferedImage orgnl = imagen();

        assertThat(servicio().rotar(orgnl, 0)).isSameAs(orgnl);
        assertThat(servicio().rotar(orgnl, 360)).isSameAs(orgnl);
        assertThat(rgb(servicio().rotar(orgnl, -90), 0, 3)).isEqualTo(ROJO);
    }

    @Test
    void nuncaQuedanFranjasNegras() throws Exception {
        for (int g : new int[] {90, 180, 270}) {
            BufferedImage r = servicio().rotar(imagen(), g);
            long ngrs = 0;
            for (int x = 0; x < r.getWidth(); x++) {
                for (int y = 0; y < r.getHeight(); y++) {
                    if (rgb(r, x, y) == 0) {
                        ngrs++;
                    }
                }
            }
            assertThat(ngrs).as("pixeles negros girando %d", g).isZero();
        }
    }
}
