package pe.edu.sgfa.importacion.service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import pe.edu.sgfa.common.exception.RyeSgfaException;

@Service
public class CphPlantillaExcelService {

    public void cphEscribirPlantillaEstudiantes(Path destinoPch) {
        String[] cabeceraPch = {"Codigo", "DNI", "Apellido Paterno", "Apellido Materno", "Nombres", "Sexo (M/F)",
                "Fecha Nacimiento (AAAA-MM-DD)", "Direccion", "Telefono", "Correo",
                "Programa (codigo EXACTO de Catalogos academicos, opcional)",
                "Plan (codigo EXACTO del plan dentro de ese Programa, opcional, obligatorio si Programa tiene dato)",
                "Fecha Inicio Trayectoria (AAAA-MM-DD, opcional, solo si Programa/Plan tienen dato)"};
        String[] ejemploPch = {"2026001", "70123456", "Quispe", "Mamani", "Ana Maria", "F", "2001-05-14",
                "Jr. Los Andes 123", "951234567", "ana.quispe@correo.com", "EI", "EI-2024", "2024-03-01"};
        cphEscribir(destinoPch, "Estudiantes", cabeceraPch, ejemploPch);
    }

    public void cphEscribirPlantillaNotas(Path destinoPch) {
        String[] cabeceraPch = {"Codigo Estudiante", "DNI", "Apellido Paterno", "Apellido Materno", "Nombres",
                "Programa (codigo EXACTO de Catalogos academicos, dejar vacio si el estudiante ya tiene trayectoria)",
                "Plan (codigo EXACTO del plan dentro de ese Programa, dejar vacio si el estudiante ya tiene trayectoria)",
                "Fecha Inicio Trayectoria (AAAA-MM-DD, opcional, solo si Programa/Plan tienen dato)",
                "Ciclo (numero)", "Curso (codigo)", "Periodo (nombre)", "Nota", "Situacion (codigo)", "Observacion"};
        String[] ejemploPch = {"2026001", "70123456", "Quispe", "Mamani", "Ana Maria", "EI", "EI-2024", "2024-03-01",
                "1", "MAT101", "2026-I", "16", "APROBADO", ""};
        cphEscribir(destinoPch, "Notas", cabeceraPch, ejemploPch);
    }

    private void cphEscribir(Path destinoPch, String nombreHojaPch, String[] cabeceraPch, String[] ejemploPch) {
        try (XSSFWorkbook libroPch = new XSSFWorkbook()) {
            Sheet hojaPch = libroPch.createSheet(nombreHojaPch);

            CellStyle estiloCabeceraPch = libroPch.createCellStyle();
            Font fuenteNegritaPch = libroPch.createFont();
            fuenteNegritaPch.setBold(true);
            estiloCabeceraPch.setFont(fuenteNegritaPch);

            Row filaCabeceraPch = hojaPch.createRow(0);
            for (int i = 0; i < cabeceraPch.length; i++) {
                Cell celdaPch = filaCabeceraPch.createCell(i);
                celdaPch.setCellValue(cabeceraPch[i]);
                celdaPch.setCellStyle(estiloCabeceraPch);
            }

            Row filaEjemploPch = hojaPch.createRow(1);
            for (int i = 0; i < ejemploPch.length; i++) {
                filaEjemploPch.createCell(i).setCellValue(ejemploPch[i]);
            }

            for (int i = 0; i < cabeceraPch.length; i++) {
                hojaPch.autoSizeColumn(i);
            }

            try (OutputStream salidaPch = Files.newOutputStream(destinoPch)) {
                libroPch.write(salidaPch);
            }
        } catch (IOException e) {
            throw new RyeSgfaException("No se pudo generar la plantilla Excel.", e);
        }
    }
}
