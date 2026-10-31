package pe.edu.sgfa.ficha.ui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperPrint;
import org.springframework.stereotype.Component;

import pe.edu.sgfa.common.exception.RyeSgfaException;
import pe.edu.sgfa.ficha.service.RacaFichaConfirmada;
import pe.edu.sgfa.ficha.service.RacaFichaService;
import pe.edu.sgfa.shell.RyeCerrarVistaEvent;
import pe.edu.sgfa.shell.RyeVisorPdf;

@Component
public class RacaVistaPreviaFichaController {

    private final RacaFichaService fichaServiceCrr;

    @FXML
    private RyeVisorPdf visor;
    @FXML
    private Button botonConfirmar;

    private Long trayectoriaIdCrr;

    public RacaVistaPreviaFichaController(RacaFichaService fichaServiceCrr) {
        this.fichaServiceCrr = fichaServiceCrr;
    }

    public void setDatos(JasperPrint printCrr, Long trayectoriaIdCrr) {
        this.trayectoriaIdCrr = trayectoriaIdCrr;
        visor.ryeMostrar(racaAPdf(printCrr), "ficha_academica_borrador.pdf");
    }

    private static byte[] racaAPdf(JasperPrint printCrr) {
        try {
            return JasperExportManager.exportReportToPdf(printCrr);
        } catch (JRException e) {
            throw new RyeSgfaException("No se pudo preparar la vista previa de la ficha.", e);
        }
    }

    @FXML
    public void racaOnConfirmar() {
        try {
            RacaFichaConfirmada confirmadaCrr = fichaServiceCrr.racaConfirmarYGuardar(trayectoriaIdCrr);

            Path archivoCrr = Path.of(confirmadaCrr.registro().getArchivoPath());
            byte[] pdfCrr = Files.isRegularFile(archivoCrr) ? Files.readAllBytes(archivoCrr) : racaAPdf(confirmadaCrr.print());
            visor.ryeMostrar(pdfCrr, archivoCrr.getFileName().toString());
            botonConfirmar.setDisable(true);

            Alert alertaCrr = new Alert(Alert.AlertType.INFORMATION,
                    "Ficha oficial generada: N° " + confirmadaCrr.registro().getCorrelativo()
                            + "\nArchivo: " + confirmadaCrr.registro().getArchivoPath());
            alertaCrr.setHeaderText(null);
            alertaCrr.showAndWait();
        } catch (RyeSgfaException e) {
            Alert alertaCrr = new Alert(Alert.AlertType.WARNING, e.getMessage());
            alertaCrr.setHeaderText(null);
            alertaCrr.showAndWait();
        } catch (IOException e) {
            Alert alertaCrr = new Alert(Alert.AlertType.WARNING, "La ficha se genero pero no se pudo mostrar: " + e.getMessage());
            alertaCrr.setHeaderText(null);
            alertaCrr.showAndWait();
        }
    }

    @FXML
    public void racaOnCerrar() {
        visor.ryeLiberar();
        visor.fireEvent(new RyeCerrarVistaEvent());
    }
}
