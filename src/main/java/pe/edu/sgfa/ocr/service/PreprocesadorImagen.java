package pe.edu.sgfa.ocr.service;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public final class PreprocesadorImagen {

    private static final int ANCHO_OBJETIVO = 2400;

    private static final int LARGO_MINIMO_LINEA = 50;

    private PreprocesadorImagen() {
    }

    public static BufferedImage paraTabla(BufferedImage orgnl) {
        BufferedImage bs = ampliar(orgnl);
        int anch = bs.getWidth();
        int alt = bs.getHeight();
        int[] grs = aGris(bs);
        boolean[] ngr = umbralAdaptativo(grs, anch, alt);
        borrarLineasLargas(ngr, anch, alt);

        BufferedImage sld = new BufferedImage(anch, alt, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < alt; y++) {
            for (int x = 0; x < anch; x++) {
                sld.getRaster().setSample(x, y, 0, ngr[y * anch + x] ? 0 : 255);
            }
        }
        return sld;
    }

    private static BufferedImage ampliar(BufferedImage orgnl) {
        if (orgnl.getWidth() >= ANCHO_OBJETIVO) {
            return orgnl;
        }
        double fctr = (double) ANCHO_OBJETIVO / orgnl.getWidth();
        int anch = ANCHO_OBJETIVO;
        int alt = (int) Math.round(orgnl.getHeight() * fctr);
        BufferedImage ampld = new BufferedImage(anch, alt, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = ampld.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(orgnl, 0, 0, anch, alt, null);
        g.dispose();
        return ampld;
    }

    private static int[] aGris(BufferedImage imgn) {
        int anch = imgn.getWidth();
        int alt = imgn.getHeight();
        int[] grs = new int[anch * alt];
        for (int y = 0; y < alt; y++) {
            for (int x = 0; x < anch; x++) {
                int rgb = imgn.getRGB(x, y);
                grs[y * anch + x] = (int) (0.299 * ((rgb >> 16) & 0xFF) + 0.587 * ((rgb >> 8) & 0xFF)
                        + 0.114 * (rgb & 0xFF));
            }
        }
        return grs;
    }

    private static boolean[] umbralAdaptativo(int[] grs, int anch, int alt) {
        long[] intgrl = new long[(anch + 1) * (alt + 1)];
        for (int y = 0; y < alt; y++) {
            long smFl = 0;
            for (int x = 0; x < anch; x++) {
                smFl += grs[y * anch + x];
                intgrl[(y + 1) * (anch + 1) + (x + 1)] = intgrl[y * (anch + 1) + (x + 1)] + smFl;
            }
        }
        int rd = Math.max(15, anch / 40);
        boolean[] ngr = new boolean[anch * alt];
        for (int y = 0; y < alt; y++) {
            int y0 = Math.max(0, y - rd);
            int y1 = Math.min(alt, y + rd + 1);
            for (int x = 0; x < anch; x++) {
                int x0 = Math.max(0, x - rd);
                int x1 = Math.min(anch, x + rd + 1);
                long sm = intgrl[y1 * (anch + 1) + x1] - intgrl[y0 * (anch + 1) + x1]
                        - intgrl[y1 * (anch + 1) + x0] + intgrl[y0 * (anch + 1) + x0];
                double prmd = (double) sm / ((long) (x1 - x0) * (y1 - y0));
                ngr[y * anch + x] = grs[y * anch + x] < prmd * 0.82;
            }
        }
        return ngr;
    }

    private static void borrarLineasLargas(boolean[] ngr, int anch, int alt) {
        boolean[] aBrrr = new boolean[ngr.length];
        for (int y = 0; y < alt; y++) {
            int inc = -1;
            for (int x = 0; x <= anch; x++) {
                boolean tnt = x < anch && ngr[y * anch + x];
                if (tnt && inc < 0) {
                    inc = x;
                } else if (!tnt && inc >= 0) {
                    if (x - inc >= LARGO_MINIMO_LINEA) {
                        for (int i = inc; i < x; i++) {
                            aBrrr[y * anch + i] = true;
                        }
                    }
                    inc = -1;
                }
            }
        }
        for (int x = 0; x < anch; x++) {
            int inc = -1;
            for (int y = 0; y <= alt; y++) {
                boolean tnt = y < alt && ngr[y * anch + x];
                if (tnt && inc < 0) {
                    inc = y;
                } else if (!tnt && inc >= 0) {
                    if (y - inc >= LARGO_MINIMO_LINEA) {
                        for (int i = inc; i < y; i++) {
                            aBrrr[i * anch + x] = true;
                        }
                    }
                    inc = -1;
                }
            }
        }
        for (int i = 0; i < ngr.length; i++) {
            if (aBrrr[i]) {
                ngr[i] = false;
            }
        }
    }
}
