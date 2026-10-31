package pe.edu.sgfa.importacion.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import pe.edu.sgfa.auditoria.service.RahpAuditoriaService;
import pe.edu.sgfa.common.exception.RyeSgfaException;
import pe.edu.sgfa.common.exception.RyeValidacionException;
import pe.edu.sgfa.estudiante.domain.YmclEstudiante;
import pe.edu.sgfa.estudiante.domain.YmclSexo;
import pe.edu.sgfa.estudiante.service.YmclEstudianteService;
import pe.edu.sgfa.programa.domain.RacaPlanEstudio;
import pe.edu.sgfa.programa.domain.RacaProgramaEstudio;
import pe.edu.sgfa.programa.repository.RacaPlanEstudioRepository;
import pe.edu.sgfa.programa.repository.RacaProgramaEstudioRepository;
import pe.edu.sgfa.trayectoria.service.YmclTrayectoriaService;
import pe.edu.sgfa.usuario.service.RyeSesionActual;

@Service
public class CphImportacionEstudiantesService {

    static final int COLUMNAS = 13;

    private final YmclEstudianteService estudianteServicePch;
    private final YmclTrayectoriaService trayectoriaServicePch;
    private final RacaProgramaEstudioRepository programaEstudioRepositoryPch;
    private final RacaPlanEstudioRepository planEstudioRepositoryPch;
    private final RyeSesionActual sesionActualPch;
    private final RahpAuditoriaService auditoriaServicePch;

    public CphImportacionEstudiantesService(YmclEstudianteService estudianteServicePch, YmclTrayectoriaService trayectoriaServicePch,
                                          RacaProgramaEstudioRepository programaEstudioRepositoryPch,
                                          RacaPlanEstudioRepository planEstudioRepositoryPch, RyeSesionActual sesionActualPch,
                                          RahpAuditoriaService auditoriaServicePch) {
        this.estudianteServicePch = estudianteServicePch;
        this.trayectoriaServicePch = trayectoriaServicePch;
        this.programaEstudioRepositoryPch = programaEstudioRepositoryPch;
        this.planEstudioRepositoryPch = planEstudioRepositoryPch;
        this.sesionActualPch = sesionActualPch;
        this.auditoriaServicePch = auditoriaServicePch;
    }

    public CphResultadoImportacion cphImportar(Path archivoPch) {
        sesionActualPch.ryeRequerirAutenticacion();

        List<CphErrorImportacion> erroresPch = new ArrayList<>();
        int exitosasPch = 0;
        int totalFilasPch = 0;

        try (InputStream entradaPch = Files.newInputStream(archivoPch);
             Workbook libroPch = new XSSFWorkbook(entradaPch)) {
            Sheet hojaPch = libroPch.getSheetAt(0);

            int numeroFilaPch = 1;
            while (true) {
                numeroFilaPch++;
                Row filaPch = hojaPch.getRow(numeroFilaPch - 1);
                if (CphLectorExcel.cphFilaVacia(filaPch, COLUMNAS)) {
                    break;
                }
                totalFilasPch++;
                try {
                    cphProcesarFila(filaPch);
                    exitosasPch++;
                } catch (RyeSgfaException | IllegalArgumentException e) {
                    erroresPch.add(new CphErrorImportacion(numeroFilaPch, e.getMessage()));
                }
            }
        } catch (IOException e) {
            throw new RyeSgfaException("No se pudo leer el archivo Excel.", e);
        }

        var actorPch = sesionActualPch.ryeObtenerUsuarioActual().orElse(null);
        auditoriaServicePch.rahpRegistrar(actorPch, "IMPORTAR_ESTUDIANTES", "estudiantes", null,
                "total=" + totalFilasPch + ", exitosas=" + exitosasPch + ", errores=" + erroresPch.size());

        return new CphResultadoImportacion(totalFilasPch, exitosasPch, erroresPch);
    }

    private void cphProcesarFila(Row filaPch) {
        String codigoPch = CphLectorExcel.cphTexto(filaPch, 0);
        String dniPch = CphLectorExcel.cphTexto(filaPch, 1);
        String apellidoPaternoPch = CphLectorExcel.cphTexto(filaPch, 2);
        String apellidoMaternoPch = CphLectorExcel.cphTexto(filaPch, 3);
        String nombresPch = CphLectorExcel.cphTexto(filaPch, 4);
        YmclSexo sexoPch = cphParsearSexo(CphLectorExcel.cphTexto(filaPch, 5));
        LocalDate fechaNacimientoPch = CphLectorExcel.fecha(filaPch, 6);
        String direccionPch = CphLectorExcel.cphTexto(filaPch, 7);
        String telefonoPch = CphLectorExcel.cphTexto(filaPch, 8);
        String correoPch = CphLectorExcel.cphTexto(filaPch, 9);

        YmclEstudiante estudiantePch = estudianteServicePch.ymclCrear(codigoPch, dniPch, nombresPch, apellidoPaternoPch, apellidoMaternoPch,
                fechaNacimientoPch, sexoPch, direccionPch, telefonoPch, correoPch);

        cphMatricularSiCorresponde(filaPch, estudiantePch);
    }

    private void cphMatricularSiCorresponde(Row filaPch, YmclEstudiante estudiantePch) {
        String codigoProgramaPch = CphLectorExcel.cphTexto(filaPch, 10);
        String codigoPlanPch = CphLectorExcel.cphTexto(filaPch, 11);
        if (codigoProgramaPch == null && codigoPlanPch == null) {
            return;
        }

        String codigoProgramaEfectivoPch = CphLectorExcel.cphTextoObligatorio(filaPch, 10, "Programa");
        String codigoPlanEfectivoPch = CphLectorExcel.cphTextoObligatorio(filaPch, 11, "Plan");
        LocalDate fechaInicioPch = CphLectorExcel.fecha(filaPch, 12);

        RacaProgramaEstudio programaPch = programaEstudioRepositoryPch.findByCodigo(codigoProgramaEfectivoPch)
                .orElseThrow(() -> new RyeValidacionException("El programa '" + codigoProgramaEfectivoPch + "' no existe."));
        RacaPlanEstudio planPch = planEstudioRepositoryPch.findByProgramaIdAndCodigo(programaPch.getId(), codigoPlanEfectivoPch)
                .orElseThrow(() -> new RyeValidacionException("El plan '" + codigoPlanEfectivoPch
                        + "' no existe en el programa '" + codigoProgramaEfectivoPch + "'."));

        trayectoriaServicePch.ymclIniciar(estudiantePch.getId(), programaPch.getId(), planPch.getId(), fechaInicioPch,
                "Importado desde Excel");
    }

    private YmclSexo cphParsearSexo(String textoPch) {
        String normalizadoPch = CphLectorExcel.cphNormalizarMayusculas(textoPch);
        if (normalizadoPch == null) {
            return null;
        }
        return switch (normalizadoPch) {
            case "M", "MASCULINO" -> YmclSexo.M;
            case "F", "FEMENINO" -> YmclSexo.F;
            default -> throw new IllegalArgumentException("Sexo \"" + textoPch + "\" invalido (use M o F).");
        };
    }
}
