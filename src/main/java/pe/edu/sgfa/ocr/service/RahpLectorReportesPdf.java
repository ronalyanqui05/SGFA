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

import pe.edu.sgfa.common.exception.RyeSgfaException;
import pe.edu.sgfa.ocr.service.RahpReconocedorReporteCalificaciones.RahpReporteDetectado;

@Component
public class RahpLectorReportesPdf {

    private static final float DPI_HNC = 300f;

    private static final int ANCHO_VISTA_HNC = 1600;
    private static final int[] GIROS_HNC = {0, 90, 270, 180};

    private final RahpOcrService ocrServiceHnc;

    public RahpLectorReportesPdf(RahpOcrService ocrServiceHnc) {
        this.ocrServiceHnc = ocrServiceHnc;
    }

    public interface RahpProgreso {
        void hoja(int actualHnc, int totalHnc);

        boolean cancelado();
    }

    public record RahpHojaLeida(int numero, RahpReporteDetectado reporte, int giroGrados, byte[] vista) {

        public BufferedImage rahpImagen() {
            try {
                return ImageIO.read(new ByteArrayInputStream(vista));
            } catch (IOException e) {
                throw new RyeSgfaException("No se pudo mostrar la hoja " + numero + ".", e);
            }
        }
    }

    public List<RahpHojaLeida> rahpLeer(File pdfHnc, RahpProgreso progresoHnc) {
        List<RahpHojaLeida> hojasHnc = new ArrayList<>();
        try (PDDocument documentoHnc = PDDocument.load(pdfHnc)) {
            PDFRenderer rendererHnc = new PDFRenderer(documentoHnc);
            int totalHnc = documentoHnc.getNumberOfPages();
            for (int i = 0; i < totalHnc; i++) {
                if (progresoHnc != null) {
                    if (progresoHnc.cancelado()) {
                        break;
                    }
                    progresoHnc.hoja(i + 1, totalHnc);
                }
                BufferedImage paginaHnc = rendererHnc.renderImageWithDPI(i, DPI_HNC);
                hojasHnc.add(rahpLeerHoja(i + 1, paginaHnc));
            }
        } catch (IOException e) {
            throw new RyeSgfaException("No se pudo leer el PDF.", e);
        }
        return hojasHnc;
    }

    RahpHojaLeida rahpLeerHoja(int numeroHnc, BufferedImage paginaHnc) {
        RahpReporteDetectado mejorHnc = null;
        BufferedImage mejorImagenHnc = paginaHnc;
        int mejorGiroHnc = 0;
        int mejorPuntajeHnc = -1;
        for (int giroHnc : GIROS_HNC) {
            BufferedImage orientadaHnc = ocrServiceHnc.rahpRotar(paginaHnc, giroHnc);
            RahpReporteDetectado reporteHnc = RahpReconocedorReporteCalificaciones.rahpReconocer(ocrServiceHnc.rahpReconocerPalabrasTabla(orientadaHnc));
            int puntajeHnc = rahpPuntaje(reporteHnc);
            if (puntajeHnc > mejorPuntajeHnc) {
                mejorHnc = reporteHnc;
                mejorImagenHnc = orientadaHnc;
                mejorGiroHnc = giroHnc;
                mejorPuntajeHnc = puntajeHnc;
            }
            if (reporteHnc.matricula() != null && !reporteHnc.cursos().isEmpty()) {
                break;
            }
        }
        return new RahpHojaLeida(numeroHnc, mejorHnc, mejorGiroHnc, rahpJpegReducido(mejorImagenHnc));
    }

    private static int rahpPuntaje(RahpReporteDetectado r) {
        return (r.matricula() != null ? 3 : 0) + (r.nombres() != null ? 2 : 0) + (r.periodo() != null ? 1 : 0)
                + (r.ciclo() != null ? 1 : 0) + r.cursos().size() * 2;
    }

    private static byte[] rahpJpegReducido(BufferedImage imagenHnc) {
        BufferedImage reducidaHnc = imagenHnc;
        if (imagenHnc.getWidth() > ANCHO_VISTA_HNC) {
            int altoHnc = (int) Math.round(imagenHnc.getHeight() * (double) ANCHO_VISTA_HNC / imagenHnc.getWidth());
            reducidaHnc = new BufferedImage(ANCHO_VISTA_HNC, altoHnc, BufferedImage.TYPE_INT_RGB);
            var g = reducidaHnc.createGraphics();
            g.drawImage(imagenHnc.getScaledInstance(ANCHO_VISTA_HNC, altoHnc, Image.SCALE_SMOOTH), 0, 0, null);
            g.dispose();
        } else if (imagenHnc.getType() != BufferedImage.TYPE_INT_RGB) {
            reducidaHnc = new BufferedImage(imagenHnc.getWidth(), imagenHnc.getHeight(), BufferedImage.TYPE_INT_RGB);
            var g = reducidaHnc.createGraphics();
            g.drawImage(imagenHnc, 0, 0, null);
            g.dispose();
        }
        try (ByteArrayOutputStream salidaHnc = new ByteArrayOutputStream()) {
            ImageIO.write(reducidaHnc, "jpg", salidaHnc);
            return salidaHnc.toByteArray();
        } catch (IOException e) {
            throw new RyeSgfaException("No se pudo guardar la vista de la hoja.", e);
        }
    }
}
