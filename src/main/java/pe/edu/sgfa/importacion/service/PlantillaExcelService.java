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

import pe.edu.sgfa.common.exception.SgfaException;

@Service
public class PlantillaExcelService {

    public void escribirPlantillaEstudiantes(Path dstn) {
        String[] cbcr = {"Codigo", "DNI", "Apellido Paterno", "Apellido Materno", "Nombres", "Sexo (M/F)",
                "Fecha Nacimiento (AAAA-MM-DD)", "Direccion", "Telefono", "Correo",
                "Programa (codigo EXACTO de Catalogos academicos, opcional)",
                "Plan (codigo EXACTO del plan dentro de ese Programa, opcional, obligatorio si Programa tiene dato)",
                "Fecha Inicio Trayectoria (AAAA-MM-DD, opcional, solo si Programa/Plan tienen dato)"};
        String[] ejmpl = {"2026001", "70123456", "Quispe", "Mamani", "Ana Maria", "F", "2001-05-14",
                "Jr. Los Andes 123", "951234567", "ana.quispe@correo.com", "EI", "EI-2024", "2024-03-01"};
        escribir(dstn, "Estudiantes", cbcr, ejmpl);
    }

    public void escribirPlantillaNotas(Path dstn) {
        String[] cbcr = {"Codigo Estudiante", "DNI", "Apellido Paterno", "Apellido Materno", "Nombres",
                "Programa (codigo EXACTO de Catalogos academicos, dejar vacio si el estudiante ya tiene trayectoria)",
                "Plan (codigo EXACTO del plan dentro de ese Programa, dejar vacio si el estudiante ya tiene trayectoria)",
                "Fecha Inicio Trayectoria (AAAA-MM-DD, opcional, solo si Programa/Plan tienen dato)",
                "Ciclo (numero)", "Curso (codigo)", "Periodo (nombre)", "Nota", "Situacion (codigo)", "Observacion"};
        String[] ejmpl = {"2026001", "70123456", "Quispe", "Mamani", "Ana Maria", "EI", "EI-2024", "2024-03-01",
                "1", "MAT101", "2026-I", "16", "APROBADO", ""};
        escribir(dstn, "Notas", cbcr, ejmpl);
    }

    private void escribir(Path dstn, String nmbrHj, String[] cbcr, String[] ejmpl) {
        try (XSSFWorkbook lbr = new XSSFWorkbook()) {
            Sheet hj = lbr.createSheet(nmbrHj);

            CellStyle estlCbcr = lbr.createCellStyle();
            Font fntNgrt = lbr.createFont();
            fntNgrt.setBold(true);
            estlCbcr.setFont(fntNgrt);

            Row flCbcr = hj.createRow(0);
            for (int i = 0; i < cbcr.length; i++) {
                Cell cld = flCbcr.createCell(i);
                cld.setCellValue(cbcr[i]);
                cld.setCellStyle(estlCbcr);
            }

            Row flEjmpl = hj.createRow(1);
            for (int i = 0; i < ejmpl.length; i++) {
                flEjmpl.createCell(i).setCellValue(ejmpl[i]);
            }

            for (int i = 0; i < cbcr.length; i++) {
                hj.autoSizeColumn(i);
            }

            try (OutputStream sld = Files.newOutputStream(dstn)) {
                lbr.write(sld);
            }
        } catch (IOException e) {
            throw new SgfaException("No se pudo generar la plantilla Excel.", e);
        }
    }
}
