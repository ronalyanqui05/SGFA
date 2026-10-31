package pe.edu.sgfa.importacion.ui;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import org.springframework.stereotype.Component;

import pe.edu.sgfa.common.exception.RyeSgfaException;
import pe.edu.sgfa.common.exception.RyeValidacionException;
import pe.edu.sgfa.importacion.service.CphErrorImportacion;
import pe.edu.sgfa.importacion.service.CphImportacionEstudiantesService;
import pe.edu.sgfa.importacion.service.CphImportacionNotasService;
import pe.edu.sgfa.importacion.service.CphPlantillaExcelService;
import pe.edu.sgfa.importacion.service.CphResultadoImportacion;
import pe.edu.sgfa.shell.RyeCerrarVistaEvent;
import pe.edu.sgfa.shell.RyeNavigationService;

@Component
public class CphImportacionController {

    private final CphImportacionEstudiantesService importacionEstudiantesServicePch;
    private final CphImportacionNotasService importacionNotasServicePch;
    private final CphPlantillaExcelService plantillaExcelServicePch;
    private final RyeNavigationService navigationServicePch;

    @FXML
    private TextArea resultadoEstudiantes;
    @FXML
    private TextArea resultadoNotas;

    public CphImportacionController(CphImportacionEstudiantesService importacionEstudiantesServicePch,
                                  CphImportacionNotasService importacionNotasServicePch,
                                  CphPlantillaExcelService plantillaExcelServicePch, RyeNavigationService navigationServicePch) {
        this.importacionEstudiantesServicePch = importacionEstudiantesServicePch;
        this.importacionNotasServicePch = importacionNotasServicePch;
        this.plantillaExcelServicePch = plantillaExcelServicePch;
        this.navigationServicePch = navigationServicePch;
    }

    @FXML
    public void cphOnImportarReporteOcr() {
        navigationServicePch.ryeAbrirVista("/fxml/ocr/importar-reporte.fxml", "Importar reporte de calificaciones (OCR)");
    }

    @FXML
    public void cphOnImportarReportesLoteOcr() {
        navigationServicePch.ryeAbrirVista("/fxml/ocr/importar-reportes-lote.fxml", "Importar PDF con varios reportes (OCR)");
    }

    @FXML
    public void cphOnImportarActaOcr() {
        navigationServicePch.ryeAbrirVista("/fxml/ocr/importar-acta.fxml", "Importar acta consolidada (OCR)");
    }

    @FXML
    public void cphOnDescargarPlantillaEstudiantes() {
        cphElegirDestino("plantilla_estudiantes.xlsx").ifPresent(destinoPch -> {
            try {
                plantillaExcelServicePch.cphEscribirPlantillaEstudiantes(destinoPch);
                cphMostrarInfo("Plantilla guardada en:\n" + destinoPch);
            } catch (RyeSgfaException e) {
                cphMostrarAdvertencia(e.getMessage());
            }
        });
    }

    @FXML
    public void cphOnImportarEstudiantes() {
        cphElegirOrigen().ifPresent(archivoPch -> {
            try {
                CphResultadoImportacion resultadoPch = importacionEstudiantesServicePch.cphImportar(archivoPch);
                resultadoEstudiantes.setText(cphFormatear(resultadoPch));
            } catch (RyeSgfaException e) {
                cphMostrarAdvertencia(e.getMessage());
            }
        });
    }

    @FXML
    public void cphOnDescargarPlantillaNotas() {
        cphElegirDestino("plantilla_notas.xlsx").ifPresent(destinoPch -> {
            try {
                plantillaExcelServicePch.cphEscribirPlantillaNotas(destinoPch);
                cphMostrarInfo("Plantilla guardada en:\n" + destinoPch);
            } catch (RyeSgfaException e) {
                cphMostrarAdvertencia(e.getMessage());
            }
        });
    }

    @FXML
    public void cphOnImportarNotas() {
        cphElegirOrigen().ifPresent(archivoPch -> {
            try {
                CphResultadoImportacion resultadoPch = importacionNotasServicePch.cphImportar(archivoPch);
                resultadoNotas.setText(cphFormatear(resultadoPch));
            } catch (RyeSgfaException e) {
                cphMostrarAdvertencia(e.getMessage());
            }
        });
    }

    @FXML
    public void cphOnCerrar() {
        resultadoEstudiantes.fireEvent(new RyeCerrarVistaEvent());
    }

    private String cphFormatear(CphResultadoImportacion resultadoPch) {
        StringBuilder textoPch = new StringBuilder();
        textoPch.append("Filas procesadas: ").append(resultadoPch.totalFilas())
                .append(" | Exitosas: ").append(resultadoPch.exitosas())
                .append(" | Con error: ").append(resultadoPch.errores().size())
                .append('\n');
        List<CphErrorImportacion> erroresPch = resultadoPch.errores();
        if (!erroresPch.isEmpty()) {
            textoPch.append('\n');
            for (CphErrorImportacion errorPch : erroresPch) {
                textoPch.append("Fila ").append(errorPch.fila()).append(": ").append(errorPch.mensaje()).append('\n');
            }
        }
        return textoPch.toString();
    }

    private Optional<Path> cphElegirDestino(String nombreSugeridoPch) {
        FileChooser chooserPch = new FileChooser();
        chooserPch.setTitle("Guardar plantilla");
        chooserPch.setInitialFileName(nombreSugeridoPch);
        chooserPch.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        File archivoPch = chooserPch.showSaveDialog(resultadoEstudiantes.getScene().getWindow());
        return Optional.ofNullable(archivoPch).map(File::toPath);
    }

    private Optional<Path> cphElegirOrigen() {
        FileChooser chooserPch = new FileChooser();
        chooserPch.setTitle("Seleccionar archivo Excel");
        chooserPch.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        File archivoPch = chooserPch.showOpenDialog(resultadoEstudiantes.getScene().getWindow());
        return Optional.ofNullable(archivoPch).map(File::toPath);
    }

    private void cphMostrarInfo(String mensajePch) {
        Alert alertaPch = new Alert(Alert.AlertType.INFORMATION, mensajePch, ButtonType.OK);
        alertaPch.setHeaderText(null);
        alertaPch.showAndWait();
    }

    private void cphMostrarAdvertencia(String mensajePch) {
        new Alert(Alert.AlertType.WARNING, mensajePch, ButtonType.OK).showAndWait();
    }
}
