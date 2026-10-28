package pe.edu.sgfa.reporte.ui;

import javafx.fxml.FXML;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pe.edu.sgfa.shell.CerrarVistaEvent;
import pe.edu.sgfa.shell.VisorPdf;

@Component
@Scope("prototype")
public class VistaPreviaPdfController {

    @FXML
    private VisorPdf visor;

    public void setDatos(byte[] pdf, String nmbrArchv) {
        visor.mostrar(pdf, nmbrArchv);
    }

    @FXML
    public void onCerrar() {
        visor.liberar();
        visor.fireEvent(new CerrarVistaEvent());
    }
}
