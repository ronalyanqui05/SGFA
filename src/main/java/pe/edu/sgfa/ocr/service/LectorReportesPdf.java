package pe.edu.sgfa.ocr.service;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;

import pe.edu.sgfa.common.exception.SgfaException;
import pe.edu.sgfa.ocr.service.ReconocedorReporteCalificaciones.ReporteDetectado;

@Component
public class LectorReportesPdf {

    private static final float DPI = 300f;

    private static final int ANCHO_VISTA = 1600;
    private static final int[] GIROS = {0, 90, 270, 180};

    private final OcrService ocrService;

    public LectorReportesPdf(OcrService ocrService) {
        this.ocrService = ocrService;
    }

    public interface Progreso {
        void hoja(int actl, int ttl);

        boolean cancelado();
    }

    public record HojaLeida(int numero, ReporteDetectado reporte, int giroGrados, byte[] vista) {

        public BufferedImage imagen() {
            try {
                return ImageIO.read(new ByteArrayInputStream(vista));
            } catch (IOException e) {
                throw new SgfaException("No se pudo mostrar la hoja " + numero + ".", e);
            }
        }
    }

    public List<HojaLeida> leer(File pdf, Progreso prgrs) {
        List<HojaLeida> hjs = new ArrayList<>();
        try (PDDocument dcmnt = PDDocument.load(pdf)) {
            PDFRenderer rndrr = new PDFRenderer(dcmnt);
            int ttl = dcmnt.getNumberOfPages();
            for (int i = 0; i < ttl; i++) {
                if (prgrs != null) {
                    if (prgrs.cancelado()) {
                        break;
                    }
                    prgrs.hoja(i + 1, ttl);
                }
                BufferedImage pgn = rndrr.renderImageWithDPI(i, DPI);
                hjs.add(leerHoja(i + 1, pgn));
            }
        } catch (IOException e) {
            throw new SgfaException("No se pudo leer el PDF.", e);
        }
        return hjs;
    }

    HojaLeida leerHoja(int numero, BufferedImage pgn) {
        ReporteDetectado mjr = null;
        BufferedImage mjrImgn = pgn;
        int mjrGr = 0;
        int mjrPntj = -1;
        for (int gr : GIROS) {
            BufferedImage orntd = ocrService.rotar(pgn, gr);
            ReporteDetectado reporte = ReconocedorReporteCalificaciones.reconocer(ocrService.reconocerPalabrasTabla(orntd));
            int puntaje = puntaje(reporte);
            if (puntaje > mjrPntj) {
                mjr = reporte;
                mjrImgn = orntd;
                mjrGr = gr;
                mjrPntj = puntaje;
            }
            if (reporte.matricula() != null && !reporte.cursos().isEmpty()) {
                break;
            }
        }
        return new HojaLeida(numero, mjr, mjrGr, jpegReducido(mjrImgn));
    }

    private static int puntaje(ReporteDetectado r) {
        return (r.matricula() != null ? 3 : 0) + (r.nombres() != null ? 2 : 0) + (r.periodo() != null ? 1 : 0)
                + (r.ciclo() != null ? 1 : 0) + r.cursos().size() * 2;
    }

    private static byte[] jpegReducido(BufferedImage imagen) {
        BufferedImage rdcd = imagen;
        if (imagen.getWidth() > ANCHO_VISTA) {
            int alt = (int) Math.round(imagen.getHeight() * (double) ANCHO_VISTA / imagen.getWidth());
            rdcd = new BufferedImage(ANCHO_VISTA, alt, BufferedImage.TYPE_INT_RGB);
            var g = rdcd.createGraphics();
            g.drawImage(imagen.getScaledInstance(ANCHO_VISTA, alt, Image.SCALE_SMOOTH), 0, 0, null);
            g.dispose();
        } else if (imagen.getType() != BufferedImage.TYPE_INT_RGB) {
            rdcd = new BufferedImage(imagen.getWidth(), imagen.getHeight(), BufferedImage.TYPE_INT_RGB);
            var g = rdcd.createGraphics();
            g.drawImage(imagen, 0, 0, null);
            g.dispose();
        }
        try (ByteArrayOutputStream sld = new ByteArrayOutputStream()) {
            ImageIO.write(rdcd, "jpg", sld);
            return sld.toByteArray();
        } catch (IOException e) {
            throw new SgfaException("No se pudo guardar la vista de la hoja.", e);
        }
    }
}
