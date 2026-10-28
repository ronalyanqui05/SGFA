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

import pe.edu.sgfa.auditoria.service.AuditoriaService;
import pe.edu.sgfa.common.exception.SgfaException;
import pe.edu.sgfa.common.exception.ValidacionException;
import pe.edu.sgfa.estudiante.domain.Estudiante;
import pe.edu.sgfa.estudiante.domain.Sexo;
import pe.edu.sgfa.estudiante.service.EstudianteService;
import pe.edu.sgfa.programa.domain.PlanEstudio;
import pe.edu.sgfa.programa.domain.ProgramaEstudio;
import pe.edu.sgfa.programa.repository.PlanEstudioRepository;
import pe.edu.sgfa.programa.repository.ProgramaEstudioRepository;
import pe.edu.sgfa.trayectoria.service.TrayectoriaService;
import pe.edu.sgfa.usuario.service.SesionActual;

@Service
public class ImportacionEstudiantesService {

    static final int COLUMNAS = 13;

    private final EstudianteService estudianteService;
    private final TrayectoriaService trayectoriaService;
    private final ProgramaEstudioRepository programaEstudioRepository;
    private final PlanEstudioRepository planEstudioRepository;
    private final SesionActual sesionActual;
    private final AuditoriaService auditoriaService;

    public ImportacionEstudiantesService(EstudianteService estudianteService, TrayectoriaService trayectoriaService,
                                          ProgramaEstudioRepository programaEstudioRepository,
                                          PlanEstudioRepository planEstudioRepository, SesionActual sesionActual,
                                          AuditoriaService auditoriaService) {
        this.estudianteService = estudianteService;
        this.trayectoriaService = trayectoriaService;
        this.programaEstudioRepository = programaEstudioRepository;
        this.planEstudioRepository = planEstudioRepository;
        this.sesionActual = sesionActual;
        this.auditoriaService = auditoriaService;
    }

    public ResultadoImportacion importar(Path archv) {
        sesionActual.requerirAutenticacion();

        List<ErrorImportacion> errrs = new ArrayList<>();
        int extss = 0;
        int ttlFls = 0;

        try (InputStream entrd = Files.newInputStream(archv);
             Workbook lbr = new XSSFWorkbook(entrd)) {
            Sheet hj = lbr.getSheetAt(0);

            int nmrFl = 1;
            while (true) {
                nmrFl++;
                Row fl = hj.getRow(nmrFl - 1);
                if (LectorExcel.filaVacia(fl, COLUMNAS)) {
                    break;
                }
                ttlFls++;
                try {
                    procesarFila(fl);
                    extss++;
                } catch (SgfaException | IllegalArgumentException e) {
                    errrs.add(new ErrorImportacion(nmrFl, e.getMessage()));
                }
            }
        } catch (IOException e) {
            throw new SgfaException("No se pudo leer el archivo Excel.", e);
        }

        var actr = sesionActual.obtenerUsuarioActual().orElse(null);
        auditoriaService.registrar(actr, "IMPORTAR_ESTUDIANTES", "estudiantes", null,
                "total=" + ttlFls + ", exitosas=" + extss + ", errores=" + errrs.size());

        return new ResultadoImportacion(ttlFls, extss, errrs);
    }

    private void procesarFila(Row fl) {
        String cdg = LectorExcel.texto(fl, 0);
        String dni = LectorExcel.texto(fl, 1);
        String aplldPtrn = LectorExcel.texto(fl, 2);
        String aplldMtrn = LectorExcel.texto(fl, 3);
        String nmbrs = LectorExcel.texto(fl, 4);
        Sexo sx = parsearSexo(LectorExcel.texto(fl, 5));
        LocalDate fchNcmnt = LectorExcel.fecha(fl, 6);
        String drccn = LectorExcel.texto(fl, 7);
        String tlfn = LectorExcel.texto(fl, 8);
        String crr = LectorExcel.texto(fl, 9);

        Estudiante estudiante = estudianteService.crear(cdg, dni, nmbrs, aplldPtrn, aplldMtrn,
                fchNcmnt, sx, drccn, tlfn, crr);

        matricularSiCorresponde(fl, estudiante);
    }

    private void matricularSiCorresponde(Row fl, Estudiante estudiante) {
        String cdgPrgrm = LectorExcel.texto(fl, 10);
        String cdgPln = LectorExcel.texto(fl, 11);
        if (cdgPrgrm == null && cdgPln == null) {
            return;
        }

        String cdgPrgrmEfctv = LectorExcel.textoObligatorio(fl, 10, "Programa");
        String cdgPlnEfctv = LectorExcel.textoObligatorio(fl, 11, "Plan");
        LocalDate fchInc = LectorExcel.fecha(fl, 12);

        ProgramaEstudio programa = programaEstudioRepository.findByCodigo(cdgPrgrmEfctv)
                .orElseThrow(() -> new ValidacionException("El programa '" + cdgPrgrmEfctv + "' no existe."));
        PlanEstudio pln = planEstudioRepository.findByProgramaIdAndCodigo(programa.getId(), cdgPlnEfctv)
                .orElseThrow(() -> new ValidacionException("El plan '" + cdgPlnEfctv
                        + "' no existe en el programa '" + cdgPrgrmEfctv + "'."));

        trayectoriaService.iniciar(estudiante.getId(), programa.getId(), pln.getId(), fchInc,
                "Importado desde Excel");
    }

    private Sexo parsearSexo(String texto) {
        String nrmlzd = LectorExcel.normalizarMayusculas(texto);
        if (nrmlzd == null) {
            return null;
        }
        return switch (nrmlzd) {
            case "M", "MASCULINO" -> Sexo.M;
            case "F", "FEMENINO" -> Sexo.F;
            default -> throw new IllegalArgumentException("Sexo \"" + texto + "\" invalido (use M o F).");
        };
    }
}
