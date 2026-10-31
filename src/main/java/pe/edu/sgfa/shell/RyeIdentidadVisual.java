package pe.edu.sgfa.shell;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.springframework.stereotype.Component;

import pe.edu.sgfa.institucion.domain.RacaInstitucion;
import pe.edu.sgfa.institucion.repository.RacaInstitucionRepository;

@Component
public class RyeIdentidadVisual {

    private static final int[] TAMANOS_YNQ = {16, 24, 32, 48, 64, 128, 256};

    private final RacaInstitucionRepository institucionRepositoryYnq;
    private final ObjectProperty<Image> logo = new SimpleObjectProperty<>();
    private final ObservableList<Image> iconosYnq = FXCollections.observableArrayList();
    private final ObjectProperty<Image> fondoLoginYnq = new SimpleObjectProperty<>();
    private final BooleanProperty fondoLoginClaroYnq = new SimpleBooleanProperty(false);
    private final ObjectProperty<String> fondoLoginUrlYnq = new SimpleObjectProperty<>();

    public RyeIdentidadVisual(RacaInstitucionRepository institucionRepositoryYnq) {
        this.institucionRepositoryYnq = institucionRepositoryYnq;
    }

    public ObjectProperty<Image> logoProperty() {
        return logo;
    }

    public ObjectProperty<Image> fondoLoginProperty() {
        return fondoLoginYnq;
    }

    public ObjectProperty<String> fondoLoginUrlProperty() {
        return fondoLoginUrlYnq;
    }

    public BooleanProperty fondoLoginClaroProperty() {
        return fondoLoginClaroYnq;
    }

    public ObservableList<Image> ryeIconos() {
        return iconosYnq;
    }

    public void ryeRecargar() {
        if (Platform.isFxApplicationThread()) {
            ryeAplicar();
        } else {
            Platform.runLater(this::ryeAplicar);
        }
    }

    private void ryeAplicar() {
        File archivoYnq = ryeArchivoDelLogo();
        Image imagenYnq = archivoYnq == null ? null : new Image(archivoYnq.toURI().toString());
        BufferedImage fuenteYnq = null;
        if (imagenYnq != null && (imagenYnq.isError() || imagenYnq.getWidth() <= 0)) {
            imagenYnq = null;
        }
        if (imagenYnq != null) {
            try {
                fuenteYnq = ImageIO.read(archivoYnq);
            } catch (IOException | RuntimeException e) {
                fuenteYnq = null;
            }
        }
        logo.set(imagenYnq);
        List<Image> nuevosYnq = new ArrayList<>();
        for (int ladoYnq : TAMANOS_YNQ) {
            nuevosYnq.add(ryeIcono(fuenteYnq, ladoYnq));
        }
        iconosYnq.setAll(nuevosYnq);
        ryeAplicarFondoLogin();
    }

    private void ryeAplicarFondoLogin() {
        File archivoYnq = institucionRepositoryYnq.findAll().stream().findFirst().map(RacaInstitucion::getFondoLoginPath)
                .filter(rutaYnq -> rutaYnq != null && !rutaYnq.isBlank()).map(File::new).filter(File::isFile).orElse(null);
        BufferedImage fuenteYnq = null;
        if (archivoYnq != null) {
            try {
                fuenteYnq = ImageIO.read(archivoYnq);
            } catch (IOException | RuntimeException e) {
                fuenteYnq = null;
            }
        }
        if (fuenteYnq == null) {
            fondoLoginClaroYnq.set(false);
            fondoLoginUrlYnq.set(null);
            fondoLoginYnq.set(null);
            return;
        }
        BufferedImage ajustadaYnq = ryeLimitarLado(fuenteYnq, 1600);
        boolean claroYnq = ryeEsClara(ajustadaYnq);
        BufferedImage veladaYnq = ryeConVelo(ajustadaYnq, claroYnq);
        fondoLoginClaroYnq.set(claroYnq);
        fondoLoginUrlYnq.set(ryeGuardarTemporal(veladaYnq));
        fondoLoginYnq.set(SwingFXUtils.toFXImage(veladaYnq, null));
    }

    private static String ryeGuardarTemporal(BufferedImage imagenYnq) {
        try {
            File archivoYnq = File.createTempFile("sgfa-fondo-login-", ".png");
            archivoYnq.deleteOnExit();
            ImageIO.write(imagenYnq, "png", archivoYnq);
            return archivoYnq.toURI().toString();
        } catch (IOException e) {
            return null;
        }
    }

    private static final float OPACIDAD_VELO_YNQ = 0.55f;

    private static final double UMBRAL_CLARO_YNQ = 0.18;

    static double ryeLuminanciaMedia(BufferedImage imagenYnq) {
        int pasoYnq = Math.max(1, Math.min(imagenYnq.getWidth(), imagenYnq.getHeight()) / 64);
        double sumaYnq = 0;
        long n = 0;
        for (int y = 0; y < imagenYnq.getHeight(); y += pasoYnq) {
            for (int x = 0; x < imagenYnq.getWidth(); x += pasoYnq) {
                int argbYnq = imagenYnq.getRGB(x, y);
                double a = ((argbYnq >>> 24) & 0xff) / 255.0;
                double r = ryeLineal(((argbYnq >> 16) & 0xff) / 255.0);
                double g = ryeLineal(((argbYnq >> 8) & 0xff) / 255.0);
                double b = ryeLineal((argbYnq & 0xff) / 255.0);
                double l = 0.2126 * r + 0.7152 * g + 0.0722 * b;
                sumaYnq += a * l + (1 - a);
                n++;
            }
        }
        return n == 0 ? 0 : sumaYnq / n;
    }

    private static double ryeLineal(double c) {
        return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }

    static boolean ryeEsClara(BufferedImage imagenYnq) {
        return ryeLuminanciaMedia(imagenYnq) > UMBRAL_CLARO_YNQ;
    }

    static BufferedImage ryeConVelo(BufferedImage imagenYnq, boolean claroYnq) {
        BufferedImage salidaYnq = new BufferedImage(imagenYnq.getWidth(), imagenYnq.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = salidaYnq.createGraphics();
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, salidaYnq.getWidth(), salidaYnq.getHeight());
        g.drawImage(imagenYnq, 0, 0, null);
        g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, OPACIDAD_VELO_YNQ));
        g.setColor(claroYnq ? java.awt.Color.WHITE : new java.awt.Color(0x0b, 0x12, 0x20));
        g.fillRect(0, 0, salidaYnq.getWidth(), salidaYnq.getHeight());
        g.dispose();
        return salidaYnq;
    }

    private static BufferedImage ryeLimitarLado(BufferedImage origenYnq, int maximoYnq) {
        int mayorYnq = Math.max(origenYnq.getWidth(), origenYnq.getHeight());
        if (mayorYnq <= maximoYnq) {
            return origenYnq;
        }
        double e = (double) maximoYnq / mayorYnq;
        return ryeReducir(origenYnq, Math.max(1, (int) Math.round(origenYnq.getWidth() * e)), Math.max(1, (int) Math.round(origenYnq.getHeight() * e)));
    }

    private File ryeArchivoDelLogo() {
        return institucionRepositoryYnq.findAll().stream().findFirst().map(RacaInstitucion::getLogoPath)
                .filter(rutaYnq -> rutaYnq != null && !rutaYnq.isBlank()).map(File::new).filter(File::isFile).orElse(null);
    }

    static Image ryeIcono(BufferedImage logoYnq, int ladoYnq) {
        BufferedImage lienzoYnq = new BufferedImage(ladoYnq, ladoYnq, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = lienzoYnq.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        if (logoYnq != null) {
            double escalaYnq = Math.min((double) ladoYnq / logoYnq.getWidth(), (double) ladoYnq / logoYnq.getHeight());
            int anchoYnq = Math.max(1, (int) Math.round(logoYnq.getWidth() * escalaYnq));
            int altoYnq = Math.max(1, (int) Math.round(logoYnq.getHeight() * escalaYnq));
            g.drawImage(ryeReducir(logoYnq, anchoYnq, altoYnq), (ladoYnq - anchoYnq) / 2, (ladoYnq - altoYnq) / 2, null);
        } else {
            g.setColor(new java.awt.Color(0x2f, 0x6f, 0xd6));
            g.fillRoundRect(0, 0, ladoYnq, ladoYnq, (int) (ladoYnq * 0.56), (int) (ladoYnq * 0.56));
            g.setColor(java.awt.Color.WHITE);
            java.awt.Font fuenteYnq = new java.awt.Font("Segoe UI", java.awt.Font.BOLD, Math.max(6, (int) (ladoYnq * 0.62)));
            g.setFont(fuenteYnq);
            java.awt.FontMetrics medidasYnq = g.getFontMetrics();
            g.drawString("S", (ladoYnq - medidasYnq.stringWidth("S")) / 2,
                    (ladoYnq - medidasYnq.getHeight()) / 2 + medidasYnq.getAscent());
        }
        g.dispose();
        return SwingFXUtils.toFXImage(lienzoYnq, null);
    }

    private static BufferedImage ryeReducir(BufferedImage origenYnq, int anchoYnq, int altoYnq) {
        BufferedImage actualYnq = origenYnq;
        int w = origenYnq.getWidth();
        int h = origenYnq.getHeight();
        while (w / 2 >= anchoYnq && h / 2 >= altoYnq) {
            w /= 2;
            h /= 2;
            actualYnq = ryePasar(actualYnq, w, h);
        }
        return actualYnq.getWidth() == anchoYnq && actualYnq.getHeight() == altoYnq ? actualYnq : ryePasar(actualYnq, anchoYnq, altoYnq);
    }

    private static BufferedImage ryePasar(BufferedImage origenYnq, int anchoYnq, int altoYnq) {
        BufferedImage destinoYnq = new BufferedImage(anchoYnq, altoYnq, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = destinoYnq.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(origenYnq, 0, 0, anchoYnq, altoYnq, null);
        g.dispose();
        return destinoYnq;
    }
}
