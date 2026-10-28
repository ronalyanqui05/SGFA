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

import pe.edu.sgfa.institucion.domain.Institucion;
import pe.edu.sgfa.institucion.repository.InstitucionRepository;

@Component
public class IdentidadVisual {

    private static final int[] TAMANOS = {16, 24, 32, 48, 64, 128, 256};

    private final InstitucionRepository institucionRepository;
    private final ObjectProperty<Image> logo = new SimpleObjectProperty<>();
    private final ObservableList<Image> iconos = FXCollections.observableArrayList();
    private final ObjectProperty<Image> fondoLogin = new SimpleObjectProperty<>();
    private final BooleanProperty fondoLoginClaro = new SimpleBooleanProperty(false);
    private final ObjectProperty<String> fondoLoginUrl = new SimpleObjectProperty<>();

    public IdentidadVisual(InstitucionRepository institucionRepository) {
        this.institucionRepository = institucionRepository;
    }

    public ObjectProperty<Image> logoProperty() {
        return logo;
    }

    public ObjectProperty<Image> fondoLoginProperty() {
        return fondoLogin;
    }

    public ObjectProperty<String> fondoLoginUrlProperty() {
        return fondoLoginUrl;
    }

    public BooleanProperty fondoLoginClaroProperty() {
        return fondoLoginClaro;
    }

    public ObservableList<Image> iconos() {
        return iconos;
    }

    public void recargar() {
        if (Platform.isFxApplicationThread()) {
            aplicar();
        } else {
            Platform.runLater(this::aplicar);
        }
    }

    private void aplicar() {
        File archv = archivoDelLogo();
        Image imgn = archv == null ? null : new Image(archv.toURI().toString());
        BufferedImage fnt = null;
        if (imgn != null && (imgn.isError() || imgn.getWidth() <= 0)) {
            imgn = null;
        }
        if (imgn != null) {
            try {
                fnt = ImageIO.read(archv);
            } catch (IOException | RuntimeException e) {
                fnt = null;
            }
        }
        logo.set(imgn);
        List<Image> nvs = new ArrayList<>();
        for (int ld : TAMANOS) {
            nvs.add(icono(fnt, ld));
        }
        iconos.setAll(nvs);
        aplicarFondoLogin();
    }

    private void aplicarFondoLogin() {
        File archv = institucionRepository.findAll().stream().findFirst().map(Institucion::getFondoLoginPath)
                .filter(ruta -> ruta != null && !ruta.isBlank()).map(File::new).filter(File::isFile).orElse(null);
        BufferedImage fnt = null;
        if (archv != null) {
            try {
                fnt = ImageIO.read(archv);
            } catch (IOException | RuntimeException e) {
                fnt = null;
            }
        }
        if (fnt == null) {
            fondoLoginClaro.set(false);
            fondoLoginUrl.set(null);
            fondoLogin.set(null);
            return;
        }
        BufferedImage ajstd = limitarLado(fnt, 1600);
        boolean clr = esClara(ajstd);
        BufferedImage vld = conVelo(ajstd, clr);
        fondoLoginClaro.set(clr);
        fondoLoginUrl.set(guardarTemporal(vld));
        fondoLogin.set(SwingFXUtils.toFXImage(vld, null));
    }

    private static String guardarTemporal(BufferedImage imgn) {
        try {
            File archv = File.createTempFile("sgfa-fondo-login-", ".png");
            archv.deleteOnExit();
            ImageIO.write(imgn, "png", archv);
            return archv.toURI().toString();
        } catch (IOException e) {
            return null;
        }
    }

    private static final float OPACIDAD_VELO = 0.55f;

    private static final double UMBRAL_CLARO = 0.18;

    static double luminanciaMedia(BufferedImage imgn) {
        int ps = Math.max(1, Math.min(imgn.getWidth(), imgn.getHeight()) / 64);
        double sm = 0;
        long n = 0;
        for (int y = 0; y < imgn.getHeight(); y += ps) {
            for (int x = 0; x < imgn.getWidth(); x += ps) {
                int argbr = imgn.getRGB(x, y);
                double a = ((argbr >>> 24) & 0xff) / 255.0;
                double r = lineal(((argbr >> 16) & 0xff) / 255.0);
                double g = lineal(((argbr >> 8) & 0xff) / 255.0);
                double b = lineal((argbr & 0xff) / 255.0);
                double l = 0.2126 * r + 0.7152 * g + 0.0722 * b;
                sm += a * l + (1 - a);
                n++;
            }
        }
        return n == 0 ? 0 : sm / n;
    }

    private static double lineal(double c) {
        return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }

    static boolean esClara(BufferedImage imgn) {
        return luminanciaMedia(imgn) > UMBRAL_CLARO;
    }

    static BufferedImage conVelo(BufferedImage imgn, boolean clr) {
        BufferedImage sld = new BufferedImage(imgn.getWidth(), imgn.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sld.createGraphics();
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, sld.getWidth(), sld.getHeight());
        g.drawImage(imgn, 0, 0, null);
        g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, OPACIDAD_VELO));
        g.setColor(clr ? java.awt.Color.WHITE : new java.awt.Color(0x0b, 0x12, 0x20));
        g.fillRect(0, 0, sld.getWidth(), sld.getHeight());
        g.dispose();
        return sld;
    }

    private static BufferedImage limitarLado(BufferedImage orgn, int mxm) {
        int myr = Math.max(orgn.getWidth(), orgn.getHeight());
        if (myr <= mxm) {
            return orgn;
        }
        double e = (double) mxm / myr;
        return reducir(orgn, Math.max(1, (int) Math.round(orgn.getWidth() * e)), Math.max(1, (int) Math.round(orgn.getHeight() * e)));
    }

    private File archivoDelLogo() {
        return institucionRepository.findAll().stream().findFirst().map(Institucion::getLogoPath)
                .filter(ruta -> ruta != null && !ruta.isBlank()).map(File::new).filter(File::isFile).orElse(null);
    }

    static Image icono(BufferedImage logo, int ld) {
        BufferedImage lnz = new BufferedImage(ld, ld, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = lnz.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        if (logo != null) {
            double escl = Math.min((double) ld / logo.getWidth(), (double) ld / logo.getHeight());
            int anch = Math.max(1, (int) Math.round(logo.getWidth() * escl));
            int alt = Math.max(1, (int) Math.round(logo.getHeight() * escl));
            g.drawImage(reducir(logo, anch, alt), (ld - anch) / 2, (ld - alt) / 2, null);
        } else {
            g.setColor(new java.awt.Color(0x2f, 0x6f, 0xd6));
            g.fillRoundRect(0, 0, ld, ld, (int) (ld * 0.56), (int) (ld * 0.56));
            g.setColor(java.awt.Color.WHITE);
            java.awt.Font fnt = new java.awt.Font("Segoe UI", java.awt.Font.BOLD, Math.max(6, (int) (ld * 0.62)));
            g.setFont(fnt);
            java.awt.FontMetrics medidas = g.getFontMetrics();
            g.drawString("S", (ld - medidas.stringWidth("S")) / 2,
                    (ld - medidas.getHeight()) / 2 + medidas.getAscent());
        }
        g.dispose();
        return SwingFXUtils.toFXImage(lnz, null);
    }

    private static BufferedImage reducir(BufferedImage orgn, int anch, int alt) {
        BufferedImage actl = orgn;
        int w = orgn.getWidth();
        int h = orgn.getHeight();
        while (w / 2 >= anch && h / 2 >= alt) {
            w /= 2;
            h /= 2;
            actl = pasar(actl, w, h);
        }
        return actl.getWidth() == anch && actl.getHeight() == alt ? actl : pasar(actl, anch, alt);
    }

    private static BufferedImage pasar(BufferedImage orgn, int anch, int alt) {
        BufferedImage dstn = new BufferedImage(anch, alt, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dstn.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(orgn, 0, 0, anch, alt, null);
        g.dispose();
        return dstn;
    }
}
