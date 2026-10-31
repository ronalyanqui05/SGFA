package pe.edu.sgfa.ocr.service;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public final class RahpPreprocesadorImagen {

    private static final int ANCHO_OBJETIVO_HNC = 2400;

    private static final int LARGO_MINIMO_LINEA_HNC = 50;

    private RahpPreprocesadorImagen() {
    }

    public static BufferedImage rahpParaTabla(BufferedImage originalHnc) {
        BufferedImage baseHnc = rahpAmpliar(originalHnc);
        int anchoHnc = baseHnc.getWidth();
        int altoHnc = baseHnc.getHeight();
        int[] grisHnc = rahpAGris(baseHnc);
        boolean[] negroHnc = rahpUmbralAdaptativo(grisHnc, anchoHnc, altoHnc);
        rahpBorrarLineasLargas(negroHnc, anchoHnc, altoHnc);

        BufferedImage salidaHnc = new BufferedImage(anchoHnc, altoHnc, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < altoHnc; y++) {
            for (int x = 0; x < anchoHnc; x++) {
                salidaHnc.getRaster().setSample(x, y, 0, negroHnc[y * anchoHnc + x] ? 0 : 255);
            }
        }
        return salidaHnc;
    }

    private static BufferedImage rahpAmpliar(BufferedImage originalHnc) {
        if (originalHnc.getWidth() >= ANCHO_OBJETIVO_HNC) {
            return originalHnc;
        }
        double factorHnc = (double) ANCHO_OBJETIVO_HNC / originalHnc.getWidth();
        int anchoHnc = ANCHO_OBJETIVO_HNC;
        int altoHnc = (int) Math.round(originalHnc.getHeight() * factorHnc);
        BufferedImage ampliadaHnc = new BufferedImage(anchoHnc, altoHnc, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = ampliadaHnc.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(originalHnc, 0, 0, anchoHnc, altoHnc, null);
        g.dispose();
        return ampliadaHnc;
    }

    private static int[] rahpAGris(BufferedImage imagenHnc) {
        int anchoHnc = imagenHnc.getWidth();
        int altoHnc = imagenHnc.getHeight();
        int[] grisHnc = new int[anchoHnc * altoHnc];
        for (int y = 0; y < altoHnc; y++) {
            for (int x = 0; x < anchoHnc; x++) {
                int rgbHnc = imagenHnc.getRGB(x, y);
                grisHnc[y * anchoHnc + x] = (int) (0.299 * ((rgbHnc >> 16) & 0xFF) + 0.587 * ((rgbHnc >> 8) & 0xFF)
                        + 0.114 * (rgbHnc & 0xFF));
            }
        }
        return grisHnc;
    }

    private static boolean[] rahpUmbralAdaptativo(int[] grisHnc, int anchoHnc, int altoHnc) {
        long[] integralHnc = new long[(anchoHnc + 1) * (altoHnc + 1)];
        for (int y = 0; y < altoHnc; y++) {
            long sumaFilaHnc = 0;
            for (int x = 0; x < anchoHnc; x++) {
                sumaFilaHnc += grisHnc[y * anchoHnc + x];
                integralHnc[(y + 1) * (anchoHnc + 1) + (x + 1)] = integralHnc[y * (anchoHnc + 1) + (x + 1)] + sumaFilaHnc;
            }
        }
        int radioHnc = Math.max(15, anchoHnc / 40);
        boolean[] negroHnc = new boolean[anchoHnc * altoHnc];
        for (int y = 0; y < altoHnc; y++) {
            int y0 = Math.max(0, y - radioHnc);
            int y1 = Math.min(altoHnc, y + radioHnc + 1);
            for (int x = 0; x < anchoHnc; x++) {
                int x0 = Math.max(0, x - radioHnc);
                int x1 = Math.min(anchoHnc, x + radioHnc + 1);
                long sumaHnc = integralHnc[y1 * (anchoHnc + 1) + x1] - integralHnc[y0 * (anchoHnc + 1) + x1]
                        - integralHnc[y1 * (anchoHnc + 1) + x0] + integralHnc[y0 * (anchoHnc + 1) + x0];
                double promedioHnc = (double) sumaHnc / ((long) (x1 - x0) * (y1 - y0));
                negroHnc[y * anchoHnc + x] = grisHnc[y * anchoHnc + x] < promedioHnc * 0.82;
            }
        }
        return negroHnc;
    }

    private static void rahpBorrarLineasLargas(boolean[] negroHnc, int anchoHnc, int altoHnc) {
        boolean[] aBorrarHnc = new boolean[negroHnc.length];
        for (int y = 0; y < altoHnc; y++) {
            int inicioHnc = -1;
            for (int x = 0; x <= anchoHnc; x++) {
                boolean tintaHnc = x < anchoHnc && negroHnc[y * anchoHnc + x];
                if (tintaHnc && inicioHnc < 0) {
                    inicioHnc = x;
                } else if (!tintaHnc && inicioHnc >= 0) {
                    if (x - inicioHnc >= LARGO_MINIMO_LINEA_HNC) {
                        for (int i = inicioHnc; i < x; i++) {
                            aBorrarHnc[y * anchoHnc + i] = true;
                        }
                    }
                    inicioHnc = -1;
                }
            }
        }
        for (int x = 0; x < anchoHnc; x++) {
            int inicioHnc = -1;
            for (int y = 0; y <= altoHnc; y++) {
                boolean tintaHnc = y < altoHnc && negroHnc[y * anchoHnc + x];
                if (tintaHnc && inicioHnc < 0) {
                    inicioHnc = y;
                } else if (!tintaHnc && inicioHnc >= 0) {
                    if (y - inicioHnc >= LARGO_MINIMO_LINEA_HNC) {
                        for (int i = inicioHnc; i < y; i++) {
                            aBorrarHnc[i * anchoHnc + x] = true;
                        }
                    }
                    inicioHnc = -1;
                }
            }
        }
        for (int i = 0; i < negroHnc.length; i++) {
            if (aBorrarHnc[i]) {
                negroHnc[i] = false;
            }
        }
    }
}
