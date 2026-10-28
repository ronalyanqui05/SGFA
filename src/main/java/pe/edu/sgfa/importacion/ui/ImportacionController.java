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

import pe.edu.sgfa.common.exception.SgfaException;
import pe.edu.sgfa.common.exception.ValidacionException;
import pe.edu.sgfa.importacion.service.ErrorImportacion;
import pe.edu.sgfa.importacion.service.ImportacionEstudiantesService;
import pe.edu.sgfa.importacion.service.ImportacionNotasService;
import pe.edu.sgfa.importacion.service.PlantillaExcelService;
import pe.edu.sgfa.importacion.service.ResultadoImportacion;
import pe.edu.sgfa.shell.CerrarVistaEvent;
import pe.edu.sgfa.shell.NavigationService;

@Component
public class ImportacionController {

    private final ImportacionEstudiantesService importacionEstudiantesService;
    private final ImportacionNotasService importacionNotasService;
    private final PlantillaExcelService plantillaExcelService;
    private final NavigationService navigationService;

    @FXML
    private TextArea resultadoEstudiantes;
    @FXML
    private TextArea resultadoNotas;

    public ImportacionController(ImportacionEstudiantesService importacionEstudiantesService,
                                  ImportacionNotasService importacionNotasService,
                                  PlantillaExcelService plantillaExcelService, NavigationService navigationService) {
        this.importacionEstudiantesService = importacionEstudiantesService;
        this.importacionNotasService = importacionNotasService;
        this.plantillaExcelService = plantillaExcelService;
        this.navigationService = navigationService;
    }

    @FXML
    public void onImportarReporteOcr() {
        navigationService.abrirVista("/fxml/ocr/importar-reporte.fxml", "Importar reporte de calificaciones (OCR)");
    }

    @FXML
    public void onImportarReportesLoteOcr() {
        navigationService.abrirVista("/fxml/ocr/importar-reportes-lote.fxml", "Importar PDF con varios reportes (OCR)");
    }

    @FXML
    public void onImportarActaOcr() {
        navigationService.abrirVista("/fxml/ocr/importar-acta.fxml", "Importar acta consolidada (OCR)");
    }

    @FXML
    public void onDescargarPlantillaEstudiantes() {
        elegirDestino("plantilla_estudiantes.xlsx").ifPresent(destino -> {
            try {
                plantillaExcelService.escribirPlantillaEstudiantes(destino);
                mostrarInfo("Plantilla guardada en:\n" + destino);
            } catch (SgfaException e) {
                mostrarAdvertencia(e.getMessage());
            }
        });
    }

    @FXML
    public void onImportarEstudiantes() {
        elegirOrigen().ifPresent(archv -> {
            try {
                ResultadoImportacion rsltd = importacionEstudiantesService.importar(archv);
                resultadoEstudiantes.setText(formatear(rsltd));
            } catch (SgfaException e) {
                mostrarAdvertencia(e.getMessage());
            }
        });
    }

    @FXML
    public void onDescargarPlantillaNotas() {
        elegirDestino("plantilla_notas.xlsx").ifPresent(destino -> {
            try {
                plantillaExcelService.escribirPlantillaNotas(destino);
                mostrarInfo("Plantilla guardada en:\n" + destino);
            } catch (SgfaException e) {
                mostrarAdvertencia(e.getMessage());
            }
        });
    }

    @FXML
    public void onImportarNotas() {
        elegirOrigen().ifPresent(archv -> {
            try {
                ResultadoImportacion rsltd = importacionNotasService.importar(archv);
                resultadoNotas.setText(formatear(rsltd));
            } catch (SgfaException e) {
                mostrarAdvertencia(e.getMessage());
            }
        });
    }

    @FXML
    public void onCerrar() {
        resultadoEstudiantes.fireEvent(new CerrarVistaEvent());
    }

    private String formatear(ResultadoImportacion rsltd) {
        StringBuilder txt = new StringBuilder();
        txt.append("Filas procesadas: ").append(rsltd.totalFilas())
                .append(" | Exitosas: ").append(rsltd.exitosas())
                .append(" | Con error: ").append(rsltd.errores().size())
                .append('\n');
        List<ErrorImportacion> errores = rsltd.errores();
        if (!errores.isEmpty()) {
            txt.append('\n');
            for (ErrorImportacion errr : errores) {
                txt.append("Fila ").append(errr.fila()).append(": ").append(errr.mensaje()).append('\n');
            }
        }
        return txt.toString();
    }

    private Optional<Path> elegirDestino(String nmbrSgrd) {
        FileChooser chsr = new FileChooser();
        chsr.setTitle("Guardar plantilla");
        chsr.setInitialFileName(nmbrSgrd);
        chsr.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        File archv = chsr.showSaveDialog(resultadoEstudiantes.getScene().getWindow());
        return Optional.ofNullable(archv).map(File::toPath);
    }

    private Optional<Path> elegirOrigen() {
        FileChooser chsr = new FileChooser();
        chsr.setTitle("Seleccionar archivo Excel");
        chsr.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        File archv = chsr.showOpenDialog(resultadoEstudiantes.getScene().getWindow());
        return Optional.ofNullable(archv).map(File::toPath);
    }

    private void mostrarInfo(String mensaje) {
        Alert alrt = new Alert(Alert.AlertType.INFORMATION, mensaje, ButtonType.OK);
        alrt.setHeaderText(null);
        alrt.showAndWait();
    }

    private void mostrarAdvertencia(String mensaje) {
        new Alert(Alert.AlertType.WARNING, mensaje, ButtonType.OK).showAndWait();
    }
}
