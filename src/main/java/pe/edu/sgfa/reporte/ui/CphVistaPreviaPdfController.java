package pe.edu.sgfa.reporte.ui;

import javafx.fxml.FXML;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pe.edu.sgfa.shell.RyeCerrarVistaEvent;
import pe.edu.sgfa.shell.RyeVisorPdf;

@Component
@Scope("prototype")
public class CphVistaPreviaPdfController {

    @FXML
    private RyeVisorPdf visor;

    public void setDatos(byte[] pdfPch, String nombreArchivoPch) {
        visor.ryeMostrar(pdfPch, nombreArchivoPch);
    }

    @FXML
    public void cphOnCerrar() {
        visor.ryeLiberar();
        visor.fireEvent(new RyeCerrarVistaEvent());
    }
}
