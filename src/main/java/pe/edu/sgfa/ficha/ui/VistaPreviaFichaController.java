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

import pe.edu.sgfa.common.exception.SgfaException;
import pe.edu.sgfa.ficha.service.FichaConfirmada;
import pe.edu.sgfa.ficha.service.FichaService;
import pe.edu.sgfa.shell.CerrarVistaEvent;
import pe.edu.sgfa.shell.VisorPdf;

@Component
public class VistaPreviaFichaController {

    private final FichaService fichaService;

    @FXML
    private VisorPdf visor;
    @FXML
    private Button botonConfirmar;

    private Long trayectoriaId;

    public VistaPreviaFichaController(FichaService fichaService) {
        this.fichaService = fichaService;
    }

    public void setDatos(JasperPrint print, Long trayectoriaId) {
        this.trayectoriaId = trayectoriaId;
        visor.mostrar(aPdf(print), "ficha_academica_borrador.pdf");
    }

    private static byte[] aPdf(JasperPrint print) {
        try {
            return JasperExportManager.exportReportToPdf(print);
        } catch (JRException e) {
            throw new SgfaException("No se pudo preparar la vista previa de la ficha.", e);
        }
    }

    @FXML
    public void onConfirmar() {
        try {
            FichaConfirmada cnfrmd = fichaService.confirmarYGuardar(trayectoriaId);

            Path archv = Path.of(cnfrmd.registro().getArchivoPath());
            byte[] pdf = Files.isRegularFile(archv) ? Files.readAllBytes(archv) : aPdf(cnfrmd.print());
            visor.mostrar(pdf, archv.getFileName().toString());
            botonConfirmar.setDisable(true);

            Alert alrt = new Alert(Alert.AlertType.INFORMATION,
                    "Ficha oficial generada: N° " + cnfrmd.registro().getCorrelativo()
                            + "\nArchivo: " + cnfrmd.registro().getArchivoPath());
            alrt.setHeaderText(null);
            alrt.showAndWait();
        } catch (SgfaException e) {
            Alert alrt = new Alert(Alert.AlertType.WARNING, e.getMessage());
            alrt.setHeaderText(null);
            alrt.showAndWait();
        } catch (IOException e) {
            Alert alrt = new Alert(Alert.AlertType.WARNING, "La ficha se genero pero no se pudo mostrar: " + e.getMessage());
            alrt.setHeaderText(null);
            alrt.showAndWait();
        }
    }

    @FXML
    public void onCerrar() {
        visor.liberar();
        visor.fireEvent(new CerrarVistaEvent());
    }
}
