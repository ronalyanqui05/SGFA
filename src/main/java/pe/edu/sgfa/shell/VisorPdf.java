package pe.edu.sgfa.shell;

import java.awt.image.BufferedImage;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.effect.DropShadow;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.util.Duration;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.printing.PDFPageable;
import org.apache.pdfbox.rendering.PDFRenderer;

public class VisorPdf extends BorderPane {

    private static final double PX_POR_PT = 96.0 / 72.0;
    private static final double[] NIVELES = {0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 2.0, 3.0, 4.0};
    private static final String AJUSTAR_ANCHO = "Ajustar al ancho";
    private static final String PAGINA_COMPLETA = "Pagina completa";

    private final ScrollPane desplazamiento = new ScrollPane();
    private final VBox paginasBox = new VBox(14);
    private final Label totalLabel = new Label("de 0");
    private final TextField paginaField = new TextField("0");
    private final ComboBox<String> zoomCombo = new ComboBox<>();
    private final Button guardarButton = new Button("Guardar PDF");
    private final Button imprimirButton = new Button("Imprimir");
    private final Button primeraButton = new Button("⏮");
    private final Button anteriorButton = new Button("◀");
    private final Button siguienteButton = new Button("▶");
    private final Button ultimaButton = new Button("⏭");
    private final ExecutorService hilo = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "sgfa-visor-pdf");
        t.setDaemon(true);
        return t;
    });
    private final PauseTransition reajuste = new PauseTransition(Duration.millis(180));

    private PDDocument documento;
    private PDFRenderer renderizador;
    private byte[] bytes;
    private String nombreSugerido = "documento.pdf";
    private final List<StackPane> marcos = new ArrayList<>();
    private double zoom = 1.0;
    private String modoZoom = AJUSTAR_ANCHO;
    private int generacion;
    private int paginaActual = 1;
    private boolean actualizandoCampos;

    public VisorPdf() {
        getStyleClass().add("visor-pdf");
        setTop(barra());
        paginasBox.setAlignment(Pos.TOP_CENTER);
        paginasBox.setPadding(new Insets(16));
        desplazamiento.setContent(paginasBox);
        desplazamiento.setPannable(true);
        desplazamiento.setFitToWidth(false);
        desplazamiento.setStyle("-fx-background-color: #8d98a8;");
        paginasBox.setStyle("-fx-background-color: #8d98a8;");
        setCenter(desplazamiento);

        desplazamiento.viewportBoundsProperty().addListener((obs, antes, ahora) -> {
            if (documento != null && (AJUSTAR_ANCHO.equals(modoZoom) || PAGINA_COMPLETA.equals(modoZoom))
                    && (antes == null || Math.abs(antes.getWidth() - ahora.getWidth()) > 6
                    || Math.abs(antes.getHeight() - ahora.getHeight()) > 6)) {
                reajuste.playFromStart();
            }
        });
        reajuste.setOnFinished(e -> renderizar());
        desplazamiento.vvalueProperty().addListener((obs, antes, ahora) -> actualizarPaginaActual());

        desplazamiento.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, e -> {
            if (e.isControlDown() && e.getDeltaY() != 0) {
                if (e.getDeltaY() > 0) {
                    acercar();
                } else {
                    alejar();
                }
                e.consume();
            }
        });
        sceneProperty().addListener((obs, antes, escena) -> {
            if (escena != null) {
                escena.getAccelerators().put(new KeyCodeCombination(KeyCode.P, KeyCombination.CONTROL_DOWN), this::imprimir);
                escena.getAccelerators().put(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN), this::guardar);
            }
        });
        actualizarEstadoControles();
    }

    private ToolBar barra() {
        guardarButton.setOnAction(e -> guardar());
        imprimirButton.setOnAction(e -> imprimir());
        primeraButton.setOnAction(e -> irAPagina(1));
        anteriorButton.setOnAction(e -> irAPagina(paginaActual - 1));
        siguienteButton.setOnAction(e -> irAPagina(paginaActual + 1));
        ultimaButton.setOnAction(e -> irAPagina(paginas()));
        for (Button b : List.of(primeraButton, anteriorButton, siguienteButton, ultimaButton)) {
            b.setMinWidth(34);
        }
        paginaField.setPrefColumnCount(3);
        paginaField.setAlignment(Pos.CENTER);
        paginaField.setOnAction(e -> {
            try {
                irAPagina(Integer.parseInt(paginaField.getText().trim()));
            } catch (NumberFormatException ex) {
                actualizarPaginaActual();
            }
        });

        Button mns = new Button("−");
        Button mas = new Button("+");
        mns.setMinWidth(34);
        mas.setMinWidth(34);
        mns.setOnAction(e -> alejar());
        mas.setOnAction(e -> acercar());
        zoomCombo.getItems().setAll(AJUSTAR_ANCHO, PAGINA_COMPLETA, "50%", "75%", "100%", "125%", "150%", "200%");
        zoomCombo.setEditable(false);
        zoomCombo.setValue(AJUSTAR_ANCHO);
        zoomCombo.setPrefWidth(150);
        zoomCombo.setOnAction(e -> {
            if (!actualizandoCampos && zoomCombo.getValue() != null) {
                aplicarModo(zoomCombo.getValue());
            }
        });

        ToolBar barra = new ToolBar(guardarButton, imprimirButton, new Separator(), primeraButton, anteriorButton,
                new Label("Pagina"), paginaField, totalLabel, siguienteButton, ultimaButton, new Separator(), mns,
                zoomCombo, mas);
        barra.setPadding(new Insets(6, 10, 6, 10));
        return barra;
    }

    public void mostrar(byte[] pdf, String nombreSugerido) {
        liberar();
        this.bytes = pdf;
        this.nombreSugerido = nombreSugerido == null || nombreSugerido.isBlank() ? "documento.pdf" : nombreSugerido;
        try {
            documento = PDDocument.load(pdf);
            renderizador = new PDFRenderer(documento);
        } catch (IOException e) {
            documento = null;
            avisar("No se pudo abrir el PDF: " + e.getMessage());
            return;
        }
        paginaActual = 1;
        modoZoom = AJUSTAR_ANCHO;
        actualizandoCampos = true;
        zoomCombo.setValue(AJUSTAR_ANCHO);
        actualizandoCampos = false;
        renderizar();
    }

    public void liberar() {
        generacion++;
        synchronized (this) {
            if (documento != null) {
                try {
                    documento.close();
                } catch (IOException ignrd) {

                }
            }
            documento = null;
            renderizador = null;
        }
        paginasBox.getChildren().clear();
        marcos.clear();
        actualizarEstadoControles();
    }

    private double anchoPaginaPt(int indc) {
        PDPage p = documento.getPage(indc);
        PDRectangle cj = p.getCropBox();
        int gr = ((p.getRotation() % 360) + 360) % 360;
        return gr == 90 || gr == 270 ? cj.getHeight() : cj.getWidth();
    }

    private double altoPaginaPt(int indc) {
        PDPage p = documento.getPage(indc);
        PDRectangle cj = p.getCropBox();
        int gr = ((p.getRotation() % 360) + 360) % 360;
        return gr == 90 || gr == 270 ? cj.getWidth() : cj.getHeight();
    }

    private double zoomEfectivo() {
        if (documento == null) {
            return 1.0;
        }
        double anch = desplazamiento.getViewportBounds().getWidth() - 44;
        double alt = desplazamiento.getViewportBounds().getHeight() - 36;
        if (AJUSTAR_ANCHO.equals(modoZoom) && anch > 100) {
            double myr = 0;
            for (int i = 0; i < documento.getNumberOfPages(); i++) {
                myr = Math.max(myr, anchoPaginaPt(i));
            }
            return limitar(anch / (myr * PX_POR_PT));
        }
        if (PAGINA_COMPLETA.equals(modoZoom) && anch > 100 && alt > 100) {
            double myrAnch = 0;
            double myrAlt = 0;
            for (int i = 0; i < documento.getNumberOfPages(); i++) {
                myrAnch = Math.max(myrAnch, anchoPaginaPt(i));
                myrAlt = Math.max(myrAlt, altoPaginaPt(i));
            }
            return limitar(Math.min(anch / (myrAnch * PX_POR_PT), alt / (myrAlt * PX_POR_PT)));
        }
        return AJUSTAR_ANCHO.equals(modoZoom) || PAGINA_COMPLETA.equals(modoZoom) ? 1.0 : zoom;
    }

    private static double limitar(double z) {
        return Math.max(0.25, Math.min(4.0, z));
    }

    private void renderizar() {
        if (documento == null) {
            return;
        }
        int gen = ++generacion;
        double z = zoomEfectivo();
        zoom = z;
        double esclPntll = Math.max(1.0, Screen.getPrimary().getOutputScaleX());
        double vvl = desplazamiento.getVvalue();

        paginasBox.getChildren().clear();
        marcos.clear();
        List<ImageView> vsts = new ArrayList<>();
        for (int i = 0; i < documento.getNumberOfPages(); i++) {
            double anch = anchoPaginaPt(i) * PX_POR_PT * z;
            double alt = altoPaginaPt(i) * PX_POR_PT * z;
            ImageView vst = new ImageView();
            vst.setFitWidth(anch);
            vst.setFitHeight(alt);
            vst.setSmooth(true);
            StackPane mrc = new StackPane(vst);
            mrc.setMinSize(anch, alt);
            mrc.setPrefSize(anch, alt);
            mrc.setMaxSize(anch, alt);
            mrc.setStyle("-fx-background-color: white;");
            mrc.setEffect(new DropShadow(10, 0, 2, Color.rgb(0, 0, 0, 0.35)));
            marcos.add(mrc);
            vsts.add(vst);
            paginasBox.getChildren().add(mrc);
        }
        zoomCombo.setTooltip(new javafx.scene.control.Tooltip("Zoom actual: " + Math.round(z * 100) + "%"));
        actualizarEstadoControles();
        desplazamiento.setVvalue(vvl);

        final PDFRenderer r = renderizador;
        final PDDocument doc = documento;
        hilo.submit(() -> {
            for (int i = 0; i < vsts.size(); i++) {
                if (gen != generacion) {
                    return;
                }
                try {
                    BufferedImage img;
                    synchronized (VisorPdf.this) {
                        if (gen != generacion || doc != documento) {
                            return;
                        }
                        img = r.renderImage(i, (float) (z * PX_POR_PT * esclPntll));
                    }
                    Image fx = SwingFXUtils.toFXImage(img, null);
                    int indc = i;
                    Platform.runLater(() -> {
                        if (gen == generacion) {
                            vsts.get(indc).setImage(fx);
                        }
                    });
                } catch (IOException | RuntimeException e) {
                    return;
                }
            }
        });
    }

    public int paginas() {
        return marcos.size();
    }

    public int paginaActual() {
        return paginaActual;
    }

    public double zoom() {
        return zoom;
    }

    public void acercar() {
        cambiarNivel(true);
    }

    public void alejar() {
        cambiarNivel(false);
    }

    private void cambiarNivel(boolean sbr) {
        if (documento == null) {
            return;
        }
        double actl = zoomEfectivo();
        double nv = actl;
        if (sbr) {
            for (double n : NIVELES) {
                if (n > actl + 0.01) {
                    nv = n;
                    break;
                }
            }
        } else {
            for (int i = NIVELES.length - 1; i >= 0; i--) {
                if (NIVELES[i] < actl - 0.01) {
                    nv = NIVELES[i];
                    break;
                }
            }
        }
        modoZoom = "FIJO";
        zoom = nv;
        actualizandoCampos = true;
        zoomCombo.setValue(Math.round(nv * 100) + "%");
        actualizandoCampos = false;
        renderizar();
    }

    public void aplicarModo(String md) {
        if (AJUSTAR_ANCHO.equals(md) || PAGINA_COMPLETA.equals(md)) {
            modoZoom = md;
        } else {
            try {
                zoom = limitar(Double.parseDouble(md.replace("%", "").trim()) / 100.0);
                modoZoom = "FIJO";
            } catch (NumberFormatException e) {
                return;
            }
        }
        renderizar();
    }

    public void irAPagina(int nmr) {
        if (marcos.isEmpty()) {
            return;
        }
        int dstn = Math.max(1, Math.min(marcos.size(), nmr));
        Platform.runLater(() -> {
            desplazamiento.layout();
            double cntnd = paginasBox.getHeight();
            double vsbl = desplazamiento.getViewportBounds().getHeight();
            double y = marcos.get(dstn - 1).getBoundsInParent().getMinY() - 8;
            double rng = Math.max(1, cntnd - vsbl);
            desplazamiento.setVvalue(Math.max(0, Math.min(1, y / rng)));
            paginaActual = dstn;
            actualizarEstadoControles();
        });
    }

    private void actualizarPaginaActual() {
        if (marcos.isEmpty()) {
            return;
        }
        double rng = Math.max(1, paginasBox.getHeight() - desplazamiento.getViewportBounds().getHeight());
        double arrb = desplazamiento.getVvalue() * rng + desplazamiento.getViewportBounds().getHeight() / 3;
        int actl = 1;
        for (int i = 0; i < marcos.size(); i++) {
            if (marcos.get(i).getBoundsInParent().getMinY() <= arrb) {
                actl = i + 1;
            }
        }
        if (actl != paginaActual) {
            paginaActual = actl;
            actualizarEstadoControles();
        }
    }

    private void actualizarEstadoControles() {
        int ttl = marcos.size();
        actualizandoCampos = true;
        paginaField.setText(String.valueOf(ttl == 0 ? 0 : paginaActual));
        totalLabel.setText("de " + ttl);
        actualizandoCampos = false;
        boolean hay = ttl > 0;
        guardarButton.setDisable(!hay);
        imprimirButton.setDisable(!hay);
        paginaField.setDisable(!hay);
        zoomCombo.setDisable(!hay);
        primeraButton.setDisable(!hay || paginaActual <= 1);
        anteriorButton.setDisable(!hay || paginaActual <= 1);
        siguienteButton.setDisable(!hay || paginaActual >= ttl);
        ultimaButton.setDisable(!hay || paginaActual >= ttl);
    }

    private void guardar() {
        if (bytes == null || getScene() == null) {
            return;
        }
        FileChooser chsr = new FileChooser();
        chsr.setTitle("Guardar una copia del PDF");
        chsr.setInitialFileName(nombreSugerido);
        chsr.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File dstn = chsr.showSaveDialog(getScene().getWindow());
        if (dstn == null) {
            return;
        }
        try {
            Files.write(dstn.toPath(), bytes);
        } catch (IOException e) {
            avisar("No se pudo guardar el archivo: " + e.getMessage());
        }
    }

    private void imprimir() {
        final PDDocument doc = documento;
        if (doc == null) {
            return;
        }
        final String trbj = nombreSugerido;
        Thread t = new Thread(() -> {
            try {
                PrinterJob job = PrinterJob.getPrinterJob();
                job.setJobName(trbj);
                synchronized (VisorPdf.this) {
                    job.setPageable(new PDFPageable(doc));
                }
                if (job.printDialog()) {
                    synchronized (VisorPdf.this) {
                        job.print();
                    }
                }
            } catch (Exception e) {
                Platform.runLater(() -> avisar("No se pudo imprimir: " + e.getMessage()));
            }
        }, "sgfa-imprimir-pdf");
        t.setDaemon(true);
        t.start();
    }

    private void avisar(String mnsj) {
        new Alert(Alert.AlertType.WARNING, mnsj, ButtonType.OK).showAndWait();
    }
}
