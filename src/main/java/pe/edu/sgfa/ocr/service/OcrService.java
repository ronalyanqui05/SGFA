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

import pe.edu.sgfa.common.exception.SgfaException;

@Component
public class OcrService {

    private static final String IDIOMA = "spa";
    private static final float RESOLUCION_PDF_DPI = 300f;

    private final Path tessdataDir;
    private volatile boolean tessdataListo;

    public OcrService(@Value("${sgfa.tessdata-dir}") String tessdataDir) {
        this.tessdataDir = Path.of(tessdataDir);
    }

    public String reconocerTexto(File imgn) {
        return reconocerTexto(leerImagen(imgn));
    }

    public String reconocerTexto(BufferedImage imgn) {
        asegurarTessdata();
        try {
            return crearMotor().doOCR(imgn).strip();
        } catch (TesseractException e) {
            throw new SgfaException("No se pudo reconocer el texto de la imagen.", e);
        }
    }

    public String reconocerTextoPdf(File pdf) {
        asegurarTessdata();
        ITesseract mtr = crearMotor();
        StringBuilder texto = new StringBuilder();
        try (PDDocument dcmnt = PDDocument.load(pdf)) {
            PDFRenderer rndrr = new PDFRenderer(dcmnt);
            for (int pgn = 0; pgn < dcmnt.getNumberOfPages(); pgn++) {
                BufferedImage imgnPgn = rndrr.renderImageWithDPI(pgn, RESOLUCION_PDF_DPI);
                if (!texto.isEmpty()) {
                    texto.append('\n');
                }
                texto.append(mtr.doOCR(imgnPgn).strip());
            }
        } catch (IOException e) {
            throw new SgfaException("No se pudo leer el PDF.", e);
        } catch (TesseractException e) {
            throw new SgfaException("No se pudo reconocer el texto del PDF.", e);
        }
        return texto.toString().strip();
    }

    public List<PalabraReconocida> reconocerPalabras(File imgn) {
        return reconocerPalabras(leerImagen(imgn));
    }

    public List<PalabraReconocida> reconocerPalabras(BufferedImage imgn) {
        asegurarTessdata();
        List<Word> plbrs = crearMotor().getWords(imgn, ITessAPI.TessPageIteratorLevel.RIL_WORD);
        return aPalabrasReconocidas(plbrs);
    }

    public List<PalabraReconocida> reconocerPalabrasTabla(BufferedImage imgn) {
        return reconocerPalabras(PreprocesadorImagen.paraTabla(imgn));
    }

    public BufferedImage rotar(BufferedImage imgn, int grds) {
        int g = ((grds % 360) + 360) % 360;
        if (g == 0) {
            return imgn;
        }
        int anch = imgn.getWidth();
        int alt = imgn.getHeight();
        boolean intrcmbEjs = g == 90 || g == 270;
        BufferedImage rtd = new BufferedImage(intrcmbEjs ? alt : anch, intrcmbEjs ? anch : alt,
                imgn.getType() == 0 ? BufferedImage.TYPE_INT_RGB : imgn.getType());

        java.awt.geom.AffineTransform transformacion = new java.awt.geom.AffineTransform();
        switch (g) {
            case 90 -> transformacion.translate(alt, 0);
            case 180 -> transformacion.translate(anch, alt);
            case 270 -> transformacion.translate(0, anch);
            default -> { }
        }
        transformacion.rotate(Math.toRadians(g));
        java.awt.Graphics2D g2 = rtd.createGraphics();
        g2.drawImage(imgn, transformacion, null);
        g2.dispose();
        return rtd;
    }

    private BufferedImage leerImagen(File imgn) {
        try {
            BufferedImage bffr = ImageIO.read(imgn);
            if (bffr == null) {
                throw new SgfaException("El archivo seleccionado no es una imagen valida.");
            }
            return bffr;
        } catch (IOException e) {
            throw new SgfaException("No se pudo leer la imagen.", e);
        }
    }

    private List<PalabraReconocida> aPalabrasReconocidas(List<Word> plbrs) {
        return plbrs.stream()
                .map(w -> new PalabraReconocida(w.getText().strip(), w.getBoundingBox().x, w.getBoundingBox().y,
                        w.getBoundingBox().width, w.getBoundingBox().height))
                .filter(p -> !p.texto().isEmpty())
                .toList();
    }

    public List<List<PalabraReconocida>> reconocerPalabrasPdf(File pdf) {
        asegurarTessdata();
        ITesseract mtr = crearMotor();
        List<List<PalabraReconocida>> pgns = new java.util.ArrayList<>();
        try (PDDocument dcmnt = PDDocument.load(pdf)) {
            PDFRenderer rndrr = new PDFRenderer(dcmnt);
            for (int pgn = 0; pgn < dcmnt.getNumberOfPages(); pgn++) {
                BufferedImage imgnPgn = rndrr.renderImageWithDPI(pgn, RESOLUCION_PDF_DPI);
                List<Word> plbrs = mtr.getWords(imgnPgn, ITessAPI.TessPageIteratorLevel.RIL_WORD);
                pgns.add(aPalabrasReconocidas(plbrs));
            }
        } catch (IOException e) {
            throw new SgfaException("No se pudo leer el PDF.", e);
        }
        return pgns;
    }

    public String reconocerTexto(Path archv) {
        String nmbr = archv.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        File file = archv.toFile();
        return nmbr.endsWith(".pdf") ? reconocerTextoPdf(file) : reconocerTexto(file);
    }

    private ITesseract crearMotor() {
        Tesseract mtr = new Tesseract();
        mtr.setDatapath(tessdataDir.toString());
        mtr.setLanguage(IDIOMA);
        return mtr;
    }

    private void asegurarTessdata() {
        if (tessdataListo) {
            return;
        }
        synchronized (this) {
            if (tessdataListo) {
                return;
            }
            Path dstn = tessdataDir.resolve(IDIOMA + ".traineddata");
            if (!Files.exists(dstn)) {
                try (InputStream orgn = getClass().getResourceAsStream("/tessdata/" + IDIOMA + ".traineddata")) {
                    if (orgn == null) {
                        throw new SgfaException(
                                "Falta el archivo de idioma de OCR (" + IDIOMA + ".traineddata) en la instalacion.");
                    }
                    Files.copy(orgn, dstn, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new UncheckedIOException(
                            "No se pudo preparar el archivo de idioma de OCR en " + dstn, e);
                }
            }
            tessdataListo = true;
        }
    }
}
