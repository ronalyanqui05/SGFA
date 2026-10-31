package pe.edu.sgfa.ocr.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import javax.imageio.ImageIO;

import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.Word;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import pe.edu.sgfa.common.exception.RyeSgfaException;

@Component
public class RahpOcrService {

    private static final String IDIOMA_HNC = "spa";
    private static final float RESOLUCION_PDF_DPI_HNC = 300f;

    private final Path tessdataDirHnc;
    private volatile boolean tessdataListoHnc;

    public RahpOcrService(@Value("${sgfa.tessdata-dir}") String tessdataDirHnc) {
        this.tessdataDirHnc = Path.of(tessdataDirHnc);
    }

    public String rahpReconocerTexto(File imagenHnc) {
        return rahpReconocerTexto(rahpLeerImagen(imagenHnc));
    }

    public String rahpReconocerTexto(BufferedImage imagenHnc) {
        rahpAsegurarTessdata();
        try {
            return rahpCrearMotor().doOCR(imagenHnc).strip();
        } catch (TesseractException e) {
            throw new RyeSgfaException("No se pudo reconocer el texto de la imagen.", e);
        }
    }

    public String rahpReconocerTextoPdf(File pdfHnc) {
        rahpAsegurarTessdata();
        ITesseract motorHnc = rahpCrearMotor();
        StringBuilder textoHnc = new StringBuilder();
        try (PDDocument documentoHnc = PDDocument.load(pdfHnc)) {
            PDFRenderer rendererHnc = new PDFRenderer(documentoHnc);
            for (int paginaHnc = 0; paginaHnc < documentoHnc.getNumberOfPages(); paginaHnc++) {
                BufferedImage imagenPaginaHnc = rendererHnc.renderImageWithDPI(paginaHnc, RESOLUCION_PDF_DPI_HNC);
                if (!textoHnc.isEmpty()) {
                    textoHnc.append('\n');
                }
                textoHnc.append(motorHnc.doOCR(imagenPaginaHnc).strip());
            }
        } catch (IOException e) {
            throw new RyeSgfaException("No se pudo leer el PDF.", e);
        } catch (TesseractException e) {
            throw new RyeSgfaException("No se pudo reconocer el texto del PDF.", e);
        }
        return textoHnc.toString().strip();
    }

    public List<RahpPalabraReconocida> rahpReconocerPalabras(File imagenHnc) {
        return rahpReconocerPalabras(rahpLeerImagen(imagenHnc));
    }

    public List<RahpPalabraReconocida> rahpReconocerPalabras(BufferedImage imagenHnc) {
        rahpAsegurarTessdata();
        List<Word> palabrasHnc = rahpCrearMotor().getWords(imagenHnc, ITessAPI.TessPageIteratorLevel.RIL_WORD);
        return rahpAPalabrasReconocidas(palabrasHnc);
    }

    public List<RahpPalabraReconocida> rahpReconocerPalabrasTabla(BufferedImage imagenHnc) {
        return rahpReconocerPalabras(RahpPreprocesadorImagen.rahpParaTabla(imagenHnc));
    }

    public BufferedImage rahpRotar(BufferedImage imagenHnc, int gradosHnc) {
        int g = ((gradosHnc % 360) + 360) % 360;
        if (g == 0) {
            return imagenHnc;
        }
        int anchoHnc = imagenHnc.getWidth();
        int altoHnc = imagenHnc.getHeight();
        boolean intercambiaEjesHnc = g == 90 || g == 270;
        BufferedImage rotadaHnc = new BufferedImage(intercambiaEjesHnc ? altoHnc : anchoHnc, intercambiaEjesHnc ? anchoHnc : altoHnc,
                imagenHnc.getType() == 0 ? BufferedImage.TYPE_INT_RGB : imagenHnc.getType());

        java.awt.geom.AffineTransform transformacionHnc = new java.awt.geom.AffineTransform();
        switch (g) {
            case 90 -> transformacionHnc.translate(altoHnc, 0);
            case 180 -> transformacionHnc.translate(anchoHnc, altoHnc);
            case 270 -> transformacionHnc.translate(0, anchoHnc);
            default -> { }
        }
        transformacionHnc.rotate(Math.toRadians(g));
        java.awt.Graphics2D g2 = rotadaHnc.createGraphics();
        g2.drawImage(imagenHnc, transformacionHnc, null);
        g2.dispose();
        return rotadaHnc;
    }

    private BufferedImage rahpLeerImagen(File imagenHnc) {
        try {
            BufferedImage bufferHnc = ImageIO.read(imagenHnc);
            if (bufferHnc == null) {
                throw new RyeSgfaException("El archivo seleccionado no es una imagen valida.");
            }
            return bufferHnc;
        } catch (IOException e) {
            throw new RyeSgfaException("No se pudo leer la imagen.", e);
        }
    }

    private List<RahpPalabraReconocida> rahpAPalabrasReconocidas(List<Word> palabrasHnc) {
        return palabrasHnc.stream()
                .map(w -> new RahpPalabraReconocida(w.getText().strip(), w.getBoundingBox().x, w.getBoundingBox().y,
                        w.getBoundingBox().width, w.getBoundingBox().height))
                .filter(p -> !p.texto().isEmpty())
                .toList();
    }

    public List<List<RahpPalabraReconocida>> rahpReconocerPalabrasPdf(File pdfHnc) {
        rahpAsegurarTessdata();
        ITesseract motorHnc = rahpCrearMotor();
        List<List<RahpPalabraReconocida>> paginasHnc = new java.util.ArrayList<>();
        try (PDDocument documentoHnc = PDDocument.load(pdfHnc)) {
            PDFRenderer rendererHnc = new PDFRenderer(documentoHnc);
            for (int paginaHnc = 0; paginaHnc < documentoHnc.getNumberOfPages(); paginaHnc++) {
                BufferedImage imagenPaginaHnc = rendererHnc.renderImageWithDPI(paginaHnc, RESOLUCION_PDF_DPI_HNC);
                List<Word> palabrasHnc = motorHnc.getWords(imagenPaginaHnc, ITessAPI.TessPageIteratorLevel.RIL_WORD);
                paginasHnc.add(rahpAPalabrasReconocidas(palabrasHnc));
            }
        } catch (IOException e) {
            throw new RyeSgfaException("No se pudo leer el PDF.", e);
        }
        return paginasHnc;
    }

    public String rahpReconocerTexto(Path archivoHnc) {
        String nombreHnc = archivoHnc.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        File fileHnc = archivoHnc.toFile();
        return nombreHnc.endsWith(".pdf") ? rahpReconocerTextoPdf(fileHnc) : rahpReconocerTexto(fileHnc);
    }

    private ITesseract rahpCrearMotor() {
        Tesseract motorHnc = new Tesseract();
        motorHnc.setDatapath(tessdataDirHnc.toString());
        motorHnc.setLanguage(IDIOMA_HNC);
        return motorHnc;
    }

    private void rahpAsegurarTessdata() {
        if (tessdataListoHnc) {
            return;
        }
        synchronized (this) {
            if (tessdataListoHnc) {
                return;
            }
            Path destinoHnc = tessdataDirHnc.resolve(IDIOMA_HNC + ".traineddata");
            if (!Files.exists(destinoHnc)) {
                try (InputStream origenHnc = getClass().getResourceAsStream("/tessdata/" + IDIOMA_HNC + ".traineddata")) {
                    if (origenHnc == null) {
                        throw new RyeSgfaException(
                                "Falta el archivo de idioma de OCR (" + IDIOMA_HNC + ".traineddata) en la instalacion.");
                    }
                    Files.copy(origenHnc, destinoHnc, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new UncheckedIOException(
                            "No se pudo preparar el archivo de idioma de OCR en " + destinoHnc, e);
                }
            }
            tessdataListoHnc = true;
        }
    }
}
