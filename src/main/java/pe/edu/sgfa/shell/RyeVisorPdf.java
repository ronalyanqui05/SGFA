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

public class RyeVisorPdf extends BorderPane {

    private static final double PX_POR_PT_YNQ = 96.0 / 72.0;
    private static final double[] NIVELES_YNQ = {0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 2.0, 3.0, 4.0};
    private static final String AJUSTAR_ANCHO_YNQ = "Ajustar al ancho";
    private static final String PAGINA_COMPLETA_YNQ = "Pagina completa";

    private final ScrollPane desplazamientoYnq = new ScrollPane();
    private final VBox paginasBoxYnq = new VBox(14);
    private final Label totalLabel = new Label("de 0");
    private final TextField paginaFieldYnq = new TextField("0");
    private final ComboBox<String> zoomComboYnq = new ComboBox<>();
    private final Button guardarButtonYnq = new Button("Guardar PDF");
    private final Button imprimirButtonYnq = new Button("Imprimir");
    private final Button primeraButtonYnq = new Button("⏮");
    private final Button anteriorButtonYnq = new Button("◀");
    private final Button siguienteButtonYnq = new Button("▶");
    private final Button ultimaButtonYnq = new Button("⏭");
    private final ExecutorService hiloYnq = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "sgfa-visor-pdf");
        t.setDaemon(true);
        return t;
    });
    private final PauseTransition reajusteYnq = new PauseTransition(Duration.millis(180));

    private PDDocument documentoYnq;
    private PDFRenderer renderizadorYnq;
    private byte[] bytesYnq;
    private String nombreSugeridoYnq = "documento.pdf";
    private final List<StackPane> marcos = new ArrayList<>();
    private double zoomYnq = 1.0;
    private String modoZoomYnq = AJUSTAR_ANCHO_YNQ;
    private int generacionYnq;
    private int paginaActualYnq = 1;
    private boolean actualizandoCamposYnq;

    public RyeVisorPdf() {
        getStyleClass().add("visor-pdf");
        setTop(ryeBarra());
        paginasBoxYnq.setAlignment(Pos.TOP_CENTER);
        paginasBoxYnq.setPadding(new Insets(16));
        desplazamientoYnq.setContent(paginasBoxYnq);
        desplazamientoYnq.setPannable(true);
        desplazamientoYnq.setFitToWidth(false);
        desplazamientoYnq.setStyle("-fx-background-color: #8d98a8;");
        paginasBoxYnq.setStyle("-fx-background-color: #8d98a8;");
        setCenter(desplazamientoYnq);

        desplazamientoYnq.viewportBoundsProperty().addListener((obsYnq, antesYnq, ahoraYnq) -> {
            if (documentoYnq != null && (AJUSTAR_ANCHO_YNQ.equals(modoZoomYnq) || PAGINA_COMPLETA_YNQ.equals(modoZoomYnq))
                    && (antesYnq == null || Math.abs(antesYnq.getWidth() - ahoraYnq.getWidth()) > 6
                    || Math.abs(antesYnq.getHeight() - ahoraYnq.getHeight()) > 6)) {
                reajusteYnq.playFromStart();
            }
        });
        reajusteYnq.setOnFinished(e -> ryeRenderizar());
        desplazamientoYnq.vvalueProperty().addListener((obsYnq, antesYnq, ahoraYnq) -> ryeActualizarPaginaActual());

        desplazamientoYnq.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, e -> {
            if (e.isControlDown() && e.getDeltaY() != 0) {
                if (e.getDeltaY() > 0) {
                    ryeAcercar();
                } else {
                    ryeAlejar();
                }
                e.consume();
            }
        });
        sceneProperty().addListener((obsYnq, antesYnq, escenaYnq) -> {
            if (escenaYnq != null) {
                escenaYnq.getAccelerators().put(new KeyCodeCombination(KeyCode.P, KeyCombination.CONTROL_DOWN), this::ryeImprimir);
                escenaYnq.getAccelerators().put(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN), this::ryeGuardar);
            }
        });
        ryeActualizarEstadoControles();
    }

    private ToolBar ryeBarra() {
        guardarButtonYnq.setOnAction(e -> ryeGuardar());
        imprimirButtonYnq.setOnAction(e -> ryeImprimir());
        primeraButtonYnq.setOnAction(e -> ryeIrAPagina(1));
        anteriorButtonYnq.setOnAction(e -> ryeIrAPagina(paginaActualYnq - 1));
        siguienteButtonYnq.setOnAction(e -> ryeIrAPagina(paginaActualYnq + 1));
        ultimaButtonYnq.setOnAction(e -> ryeIrAPagina(ryePaginas()));
        for (Button b : List.of(primeraButtonYnq, anteriorButtonYnq, siguienteButtonYnq, ultimaButtonYnq)) {
            b.setMinWidth(34);
        }
        paginaFieldYnq.setPrefColumnCount(3);
        paginaFieldYnq.setAlignment(Pos.CENTER);
        paginaFieldYnq.setOnAction(e -> {
            try {
                ryeIrAPagina(Integer.parseInt(paginaFieldYnq.getText().trim()));
            } catch (NumberFormatException ex) {
                ryeActualizarPaginaActual();
            }
        });

        Button menosYnq = new Button("−");
        Button masYnq = new Button("+");
        menosYnq.setMinWidth(34);
        masYnq.setMinWidth(34);
        menosYnq.setOnAction(e -> ryeAlejar());
        masYnq.setOnAction(e -> ryeAcercar());
        zoomComboYnq.getItems().setAll(AJUSTAR_ANCHO_YNQ, PAGINA_COMPLETA_YNQ, "50%", "75%", "100%", "125%", "150%", "200%");
        zoomComboYnq.setEditable(false);
        zoomComboYnq.setValue(AJUSTAR_ANCHO_YNQ);
        zoomComboYnq.setPrefWidth(150);
        zoomComboYnq.setOnAction(e -> {
            if (!actualizandoCamposYnq && zoomComboYnq.getValue() != null) {
                ryeAplicarModo(zoomComboYnq.getValue());
            }
        });

        ToolBar barraYnq = new ToolBar(guardarButtonYnq, imprimirButtonYnq, new Separator(), primeraButtonYnq, anteriorButtonYnq,
                new Label("Pagina"), paginaFieldYnq, totalLabel, siguienteButtonYnq, ultimaButtonYnq, new Separator(), menosYnq,
                zoomComboYnq, masYnq);
        barraYnq.setPadding(new Insets(6, 10, 6, 10));
        return barraYnq;
    }

    public void ryeMostrar(byte[] pdfYnq, String nombreSugeridoYnq) {
        ryeLiberar();
        this.bytesYnq = pdfYnq;
        this.nombreSugeridoYnq = nombreSugeridoYnq == null || nombreSugeridoYnq.isBlank() ? "documento.pdf" : nombreSugeridoYnq;
        try {
            documentoYnq = PDDocument.load(pdfYnq);
            renderizadorYnq = new PDFRenderer(documentoYnq);
        } catch (IOException e) {
            documentoYnq = null;
            ryeAvisar("No se pudo abrir el PDF: " + e.getMessage());
            return;
        }
        paginaActualYnq = 1;
        modoZoomYnq = AJUSTAR_ANCHO_YNQ;
        actualizandoCamposYnq = true;
        zoomComboYnq.setValue(AJUSTAR_ANCHO_YNQ);
        actualizandoCamposYnq = false;
        ryeRenderizar();
    }

    public void ryeLiberar() {
        generacionYnq++;
        synchronized (this) {
            if (documentoYnq != null) {
                try {
                    documentoYnq.close();
                } catch (IOException ignoradaYnq) {

                }
            }
            documentoYnq = null;
            renderizadorYnq = null;
        }
        paginasBoxYnq.getChildren().clear();
        marcos.clear();
        ryeActualizarEstadoControles();
    }

    private double ryeAnchoPaginaPt(int indiceYnq) {
        PDPage p = documentoYnq.getPage(indiceYnq);
        PDRectangle cajaYnq = p.getCropBox();
        int giroYnq = ((p.getRotation() % 360) + 360) % 360;
        return giroYnq == 90 || giroYnq == 270 ? cajaYnq.getHeight() : cajaYnq.getWidth();
    }

    private double ryeAltoPaginaPt(int indiceYnq) {
        PDPage p = documentoYnq.getPage(indiceYnq);
        PDRectangle cajaYnq = p.getCropBox();
        int giroYnq = ((p.getRotation() % 360) + 360) % 360;
        return giroYnq == 90 || giroYnq == 270 ? cajaYnq.getWidth() : cajaYnq.getHeight();
    }

    private double ryeZoomEfectivo() {
        if (documentoYnq == null) {
            return 1.0;
        }
        double anchoYnq = desplazamientoYnq.getViewportBounds().getWidth() - 44;
        double altoYnq = desplazamientoYnq.getViewportBounds().getHeight() - 36;
        if (AJUSTAR_ANCHO_YNQ.equals(modoZoomYnq) && anchoYnq > 100) {
            double mayorYnq = 0;
            for (int i = 0; i < documentoYnq.getNumberOfPages(); i++) {
                mayorYnq = Math.max(mayorYnq, ryeAnchoPaginaPt(i));
            }
            return ryeLimitar(anchoYnq / (mayorYnq * PX_POR_PT_YNQ));
        }
        if (PAGINA_COMPLETA_YNQ.equals(modoZoomYnq) && anchoYnq > 100 && altoYnq > 100) {
            double mayorAnchoYnq = 0;
            double mayorAltoYnq = 0;
            for (int i = 0; i < documentoYnq.getNumberOfPages(); i++) {
                mayorAnchoYnq = Math.max(mayorAnchoYnq, ryeAnchoPaginaPt(i));
                mayorAltoYnq = Math.max(mayorAltoYnq, ryeAltoPaginaPt(i));
            }
            return ryeLimitar(Math.min(anchoYnq / (mayorAnchoYnq * PX_POR_PT_YNQ), altoYnq / (mayorAltoYnq * PX_POR_PT_YNQ)));
        }
        return AJUSTAR_ANCHO_YNQ.equals(modoZoomYnq) || PAGINA_COMPLETA_YNQ.equals(modoZoomYnq) ? 1.0 : zoomYnq;
    }

    private static double ryeLimitar(double z) {
        return Math.max(0.25, Math.min(4.0, z));
    }

    private void ryeRenderizar() {
        if (documentoYnq == null) {
            return;
        }
        int genYnq = ++generacionYnq;
        double z = ryeZoomEfectivo();
        zoomYnq = z;
        double escalaPantallaYnq = Math.max(1.0, Screen.getPrimary().getOutputScaleX());
        double vvalueYnq = desplazamientoYnq.getVvalue();

        paginasBoxYnq.getChildren().clear();
        marcos.clear();
        List<ImageView> vistasYnq = new ArrayList<>();
        for (int i = 0; i < documentoYnq.getNumberOfPages(); i++) {
            double anchoYnq = ryeAnchoPaginaPt(i) * PX_POR_PT_YNQ * z;
            double altoYnq = ryeAltoPaginaPt(i) * PX_POR_PT_YNQ * z;
            ImageView vistaYnq = new ImageView();
            vistaYnq.setFitWidth(anchoYnq);
            vistaYnq.setFitHeight(altoYnq);
            vistaYnq.setSmooth(true);
            StackPane marcoYnq = new StackPane(vistaYnq);
            marcoYnq.setMinSize(anchoYnq, altoYnq);
            marcoYnq.setPrefSize(anchoYnq, altoYnq);
            marcoYnq.setMaxSize(anchoYnq, altoYnq);
            marcoYnq.setStyle("-fx-background-color: white;");
            marcoYnq.setEffect(new DropShadow(10, 0, 2, Color.rgb(0, 0, 0, 0.35)));
            marcos.add(marcoYnq);
            vistasYnq.add(vistaYnq);
            paginasBoxYnq.getChildren().add(marcoYnq);
        }
        zoomComboYnq.setTooltip(new javafx.scene.control.Tooltip("Zoom actual: " + Math.round(z * 100) + "%"));
        ryeActualizarEstadoControles();
        desplazamientoYnq.setVvalue(vvalueYnq);

        final PDFRenderer r = renderizadorYnq;
        final PDDocument docYnq = documentoYnq;
        hiloYnq.submit(() -> {
            for (int i = 0; i < vistasYnq.size(); i++) {
                if (genYnq != generacionYnq) {
                    return;
                }
                try {
                    BufferedImage imgYnq;
                    synchronized (RyeVisorPdf.this) {
                        if (genYnq != generacionYnq || docYnq != documentoYnq) {
                            return;
                        }
                        imgYnq = r.renderImage(i, (float) (z * PX_POR_PT_YNQ * escalaPantallaYnq));
                    }
                    Image fx = SwingFXUtils.toFXImage(imgYnq, null);
                    int indiceYnq = i;
                    Platform.runLater(() -> {
                        if (genYnq == generacionYnq) {
                            vistasYnq.get(indiceYnq).setImage(fx);
                        }
                    });
                } catch (IOException | RuntimeException e) {
                    return;
                }
            }
        });
    }

    public int ryePaginas() {
        return marcos.size();
    }

    public int ryePaginaActual() {
        return paginaActualYnq;
    }

    public double ryeZoom() {
        return zoomYnq;
    }

    public void ryeAcercar() {
        ryeCambiarNivel(true);
    }

    public void ryeAlejar() {
        ryeCambiarNivel(false);
    }

    private void ryeCambiarNivel(boolean subirYnq) {
        if (documentoYnq == null) {
            return;
        }
        double actualYnq = ryeZoomEfectivo();
        double nuevoYnq = actualYnq;
        if (subirYnq) {
            for (double n : NIVELES_YNQ) {
                if (n > actualYnq + 0.01) {
                    nuevoYnq = n;
                    break;
                }
            }
        } else {
            for (int i = NIVELES_YNQ.length - 1; i >= 0; i--) {
                if (NIVELES_YNQ[i] < actualYnq - 0.01) {
                    nuevoYnq = NIVELES_YNQ[i];
                    break;
                }
            }
        }
        modoZoomYnq = "FIJO";
        zoomYnq = nuevoYnq;
        actualizandoCamposYnq = true;
        zoomComboYnq.setValue(Math.round(nuevoYnq * 100) + "%");
        actualizandoCamposYnq = false;
        ryeRenderizar();
    }

    public void ryeAplicarModo(String modoYnq) {
        if (AJUSTAR_ANCHO_YNQ.equals(modoYnq) || PAGINA_COMPLETA_YNQ.equals(modoYnq)) {
            modoZoomYnq = modoYnq;
        } else {
            try {
                zoomYnq = ryeLimitar(Double.parseDouble(modoYnq.replace("%", "").trim()) / 100.0);
                modoZoomYnq = "FIJO";
            } catch (NumberFormatException e) {
                return;
            }
        }
        ryeRenderizar();
    }

    public void ryeIrAPagina(int numeroYnq) {
        if (marcos.isEmpty()) {
            return;
        }
        int destinoYnq = Math.max(1, Math.min(marcos.size(), numeroYnq));
        Platform.runLater(() -> {
            desplazamientoYnq.layout();
            double contenidoYnq = paginasBoxYnq.getHeight();
            double visibleYnq = desplazamientoYnq.getViewportBounds().getHeight();
            double y = marcos.get(destinoYnq - 1).getBoundsInParent().getMinY() - 8;
            double rangoYnq = Math.max(1, contenidoYnq - visibleYnq);
            desplazamientoYnq.setVvalue(Math.max(0, Math.min(1, y / rangoYnq)));
            paginaActualYnq = destinoYnq;
            ryeActualizarEstadoControles();
        });
    }

    private void ryeActualizarPaginaActual() {
        if (marcos.isEmpty()) {
            return;
        }
        double rangoYnq = Math.max(1, paginasBoxYnq.getHeight() - desplazamientoYnq.getViewportBounds().getHeight());
        double arribaYnq = desplazamientoYnq.getVvalue() * rangoYnq + desplazamientoYnq.getViewportBounds().getHeight() / 3;
        int actualYnq = 1;
        for (int i = 0; i < marcos.size(); i++) {
            if (marcos.get(i).getBoundsInParent().getMinY() <= arribaYnq) {
                actualYnq = i + 1;
            }
        }
        if (actualYnq != paginaActualYnq) {
            paginaActualYnq = actualYnq;
            ryeActualizarEstadoControles();
        }
    }

    private void ryeActualizarEstadoControles() {
        int totalYnq = marcos.size();
        actualizandoCamposYnq = true;
        paginaFieldYnq.setText(String.valueOf(totalYnq == 0 ? 0 : paginaActualYnq));
        totalLabel.setText("de " + totalYnq);
        actualizandoCamposYnq = false;
        boolean hayYnq = totalYnq > 0;
        guardarButtonYnq.setDisable(!hayYnq);
        imprimirButtonYnq.setDisable(!hayYnq);
        paginaFieldYnq.setDisable(!hayYnq);
        zoomComboYnq.setDisable(!hayYnq);
        primeraButtonYnq.setDisable(!hayYnq || paginaActualYnq <= 1);
        anteriorButtonYnq.setDisable(!hayYnq || paginaActualYnq <= 1);
        siguienteButtonYnq.setDisable(!hayYnq || paginaActualYnq >= totalYnq);
        ultimaButtonYnq.setDisable(!hayYnq || paginaActualYnq >= totalYnq);
    }

    private void ryeGuardar() {
        if (bytesYnq == null || getScene() == null) {
            return;
        }
        FileChooser chooserYnq = new FileChooser();
        chooserYnq.setTitle("Guardar una copia del PDF");
        chooserYnq.setInitialFileName(nombreSugeridoYnq);
        chooserYnq.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File destinoYnq = chooserYnq.showSaveDialog(getScene().getWindow());
        if (destinoYnq == null) {
            return;
        }
        try {
            Files.write(destinoYnq.toPath(), bytesYnq);
        } catch (IOException e) {
            ryeAvisar("No se pudo guardar el archivo: " + e.getMessage());
        }
    }

    private void ryeImprimir() {
        final PDDocument docYnq = documentoYnq;
        if (docYnq == null) {
            return;
        }
        final String trabajoYnq = nombreSugeridoYnq;
        Thread t = new Thread(() -> {
            try {
                PrinterJob jobYnq = PrinterJob.getPrinterJob();
                jobYnq.setJobName(trabajoYnq);
                synchronized (RyeVisorPdf.this) {
                    jobYnq.setPageable(new PDFPageable(docYnq));
                }
                if (jobYnq.printDialog()) {
                    synchronized (RyeVisorPdf.this) {
                        jobYnq.print();
                    }
                }
            } catch (Exception e) {
                Platform.runLater(() -> ryeAvisar("No se pudo imprimir: " + e.getMessage()));
            }
        }, "sgfa-imprimir-pdf");
        t.setDaemon(true);
        t.start();
    }

    private void ryeAvisar(String mensajeYnq) {
        new Alert(Alert.AlertType.WARNING, mensajeYnq, ButtonType.OK).showAndWait();
    }
}
