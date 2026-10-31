package pe.edu.sgfa.ocr.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

class RahpOcrServiceRotarTest {

    private static final int ROJO_HNC = 0xFF0000;
    private static final int BLANCO_HNC = 0xFFFFFF;

    private static BufferedImage imagen() {
        BufferedImage imgHnc = new BufferedImage(4, 2, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 2; y++) {
                imgHnc.setRGB(x, y, BLANCO_HNC);
            }
        }
        imgHnc.setRGB(0, 0, ROJO_HNC);
        return imgHnc;
    }

    private static int rgb(BufferedImage imgHnc, int x, int y) {
        return imgHnc.getRGB(x, y) & 0xFFFFFF;
    }

    private RahpOcrService servicio() throws Exception {
        return new RahpOcrService(Files.createTempDirectory("tess-").toString());
    }

    @Test
    void girar90LlevaLaEsquinaSuperiorIzquierdaALaSuperiorDerecha() throws Exception {
        BufferedImage r = servicio().rahpRotar(imagen(), 90);

        assertThat(r.getWidth()).isEqualTo(2);
        assertThat(r.getHeight()).isEqualTo(4);
        assertThat(rgb(r, 1, 0)).isEqualTo(ROJO_HNC);
        assertThat(rgb(r, 0, 0)).isEqualTo(BLANCO_HNC);
        assertThat(rgb(r, 1, 3)).isEqualTo(BLANCO_HNC);
    }

    @Test
    void girar180LlevaLaEsquinaALaInferiorDerecha() throws Exception {
        BufferedImage r = servicio().rahpRotar(imagen(), 180);

        assertThat(r.getWidth()).isEqualTo(4);
        assertThat(r.getHeight()).isEqualTo(2);
        assertThat(rgb(r, 3, 1)).isEqualTo(ROJO_HNC);
        assertThat(rgb(r, 0, 0)).isEqualTo(BLANCO_HNC);
    }

    @Test
    void girar270LlevaLaEsquinaALaInferiorIzquierda() throws Exception {
        BufferedImage r = servicio().rahpRotar(imagen(), 270);

        assertThat(r.getWidth()).isEqualTo(2);
        assertThat(r.getHeight()).isEqualTo(4);
        assertThat(rgb(r, 0, 3)).isEqualTo(ROJO_HNC);
        assertThat(rgb(r, 0, 0)).isEqualTo(BLANCO_HNC);
        assertThat(rgb(r, 1, 3)).isEqualTo(BLANCO_HNC);
    }

    @Test
    void girar0YMultiplosDe360NoCambianNada() throws Exception {
        BufferedImage originalHnc = imagen();

        assertThat(servicio().rahpRotar(originalHnc, 0)).isSameAs(originalHnc);
        assertThat(servicio().rahpRotar(originalHnc, 360)).isSameAs(originalHnc);
        assertThat(rgb(servicio().rahpRotar(originalHnc, -90), 0, 3)).isEqualTo(ROJO_HNC);
    }

    @Test
    void nuncaQuedanFranjasNegras() throws Exception {
        for (int g : new int[] {90, 180, 270}) {
            BufferedImage r = servicio().rahpRotar(imagen(), g);
            long negrosHnc = 0;
            for (int x = 0; x < r.getWidth(); x++) {
                for (int y = 0; y < r.getHeight(); y++) {
                    if (rgb(r, x, y) == 0) {
                        negrosHnc++;
                    }
                }
            }
            assertThat(negrosHnc).as("pixeles negros girando %d", g).isZero();
        }
    }
}
