package pe.edu.sgfa.importacion.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import pe.edu.sgfa.auditoria.service.AuditoriaService;
import pe.edu.sgfa.common.exception.SgfaException;
import pe.edu.sgfa.common.exception.ValidacionException;
import pe.edu.sgfa.estudiante.domain.Estudiante;
import pe.edu.sgfa.estudiante.repository.EstudianteRepository;
import pe.edu.sgfa.estudiante.service.EstudianteService;
import pe.edu.sgfa.nota.domain.SituacionAcademica;
import pe.edu.sgfa.nota.repository.SituacionAcademicaRepository;
import pe.edu.sgfa.nota.service.ItemNota;
import pe.edu.sgfa.nota.service.RegistroAcademicoService;
import pe.edu.sgfa.periodo.domain.PeriodoAcademico;
import pe.edu.sgfa.periodo.repository.PeriodoAcademicoRepository;
import pe.edu.sgfa.programa.domain.Ciclo;
import pe.edu.sgfa.programa.domain.Curso;
import pe.edu.sgfa.programa.domain.PlanCurso;
import pe.edu.sgfa.programa.domain.PlanEstudio;
import pe.edu.sgfa.programa.domain.ProgramaEstudio;
import pe.edu.sgfa.programa.repository.CicloRepository;
import pe.edu.sgfa.programa.repository.CursoRepository;
import pe.edu.sgfa.programa.repository.PlanCursoRepository;
import pe.edu.sgfa.programa.repository.PlanEstudioRepository;
import pe.edu.sgfa.programa.repository.ProgramaEstudioRepository;
import pe.edu.sgfa.trayectoria.domain.Trayectoria;
import pe.edu.sgfa.trayectoria.service.TrayectoriaService;
import pe.edu.sgfa.usuario.service.SesionActual;

@Service
public class ImportacionNotasService {

    static final int COLUMNAS = 14;

    private final EstudianteRepository estudianteRepository;
    private final EstudianteService estudianteService;
    private final TrayectoriaService trayectoriaService;
    private final ProgramaEstudioRepository programaEstudioRepository;
    private final PlanEstudioRepository planEstudioRepository;
    private final CicloRepository cicloRepository;
    private final CursoRepository cursoRepository;
    private final PlanCursoRepository planCursoRepository;
    private final PeriodoAcademicoRepository periodoAcademicoRepository;
    private final SituacionAcademicaRepository situacionAcademicaRepository;
    private final RegistroAcademicoService registroAcademicoService;
    private final SesionActual sesionActual;
    private final AuditoriaService auditoriaService;

    public ImportacionNotasService(EstudianteRepository estudianteRepository, EstudianteService estudianteService,
                                    TrayectoriaService trayectoriaService,
                                    ProgramaEstudioRepository programaEstudioRepository,
                                    PlanEstudioRepository planEstudioRepository, CicloRepository cicloRepository,
                                    CursoRepository cursoRepository, PlanCursoRepository planCursoRepository,
                                    PeriodoAcademicoRepository periodoAcademicoRepository,
                                    SituacionAcademicaRepository situacionAcademicaRepository,
                                    RegistroAcademicoService registroAcademicoService, SesionActual sesionActual,
                                    AuditoriaService auditoriaService) {
        this.estudianteRepository = estudianteRepository;
        this.estudianteService = estudianteService;
        this.trayectoriaService = trayectoriaService;
        this.programaEstudioRepository = programaEstudioRepository;
        this.planEstudioRepository = planEstudioRepository;
        this.cicloRepository = cicloRepository;
        this.cursoRepository = cursoRepository;
        this.planCursoRepository = planCursoRepository;
        this.periodoAcademicoRepository = periodoAcademicoRepository;
        this.situacionAcademicaRepository = situacionAcademicaRepository;
        this.registroAcademicoService = registroAcademicoService;
        this.sesionActual = sesionActual;
        this.auditoriaService = auditoriaService;
    }

    public ResultadoImportacion importar(Path archv) {
        sesionActual.requerirAutenticacion();

        List<ErrorImportacion> errrs = new ArrayList<>();
        Map<GrupoClave, List<FilaConItem>> grps = new LinkedHashMap<>();
        int ttlFls = 0;

        try (InputStream entrd = Files.newInputStream(archv);
             Workbook lbr = new XSSFWorkbook(entrd)) {
            Sheet hj = lbr.getSheetAt(0);

            int numeroFila = 1;
            while (true) {
                numeroFila++;
                Row fl = hj.getRow(numeroFila - 1);
                if (LectorExcel.filaVacia(fl, COLUMNAS)) {
                    break;
                }
                ttlFls++;
                try {
                    FilaResuelta rslt = resolverFila(fl);
                    grps.computeIfAbsent(rslt.grupo(), g -> new ArrayList<>())
                            .add(new FilaConItem(numeroFila, rslt.item()));
                } catch (SgfaException | IllegalArgumentException e) {
                    errrs.add(new ErrorImportacion(numeroFila, e.getMessage()));
                }
            }
        } catch (IOException e) {
            throw new SgfaException("No se pudo leer el archivo Excel.", e);
        }

        int extss = 0;
        for (Map.Entry<GrupoClave, List<FilaConItem>> grupo : grps.entrySet()) {
            List<ItemNota> itms = grupo.getValue().stream().map(FilaConItem::item).toList();
            try {
                registroAcademicoService.guardarMasivo(grupo.getKey().trayectoriaId(), grupo.getKey().periodoId(), itms);
                extss += itms.size();
            } catch (SgfaException e) {
                for (FilaConItem flConItm : grupo.getValue()) {
                    errrs.add(new ErrorImportacion(flConItm.numeroFila(), e.getMessage()));
                }
            }
        }

        var actr = sesionActual.obtenerUsuarioActual().orElse(null);
        auditoriaService.registrar(actr, "IMPORTAR_NOTAS", "registros_academicos", null,
                "total=" + ttlFls + ", exitosas=" + extss + ", errores=" + errrs.size());

        return new ResultadoImportacion(ttlFls, extss, errrs);
    }

    private FilaResuelta resolverFila(Row fl) {
        Estudiante estudiante = resolverEstudiante(fl);
        Trayectoria trayectoria = resolverTrayectoria(fl, estudiante);

        Integer nmrCcl = LectorExcel.entero(fl, 8);
        if (nmrCcl == null) {
            throw new IllegalArgumentException("Falta el numero de ciclo.");
        }
        Ciclo ccl = cicloRepository.findByPlanEstudioIdAndNumero(trayectoria.getPlanEstudio().getId(), nmrCcl)
                .orElseThrow(() -> new ValidacionException("El ciclo " + nmrCcl
                        + " no existe en el plan '" + trayectoria.getPlanEstudio().getCodigo() + "'."));

        String cdgCrs = LectorExcel.textoObligatorio(fl, 9, "Curso");
        Curso crs = cursoRepository.findByCodigo(cdgCrs)
                .orElseThrow(() -> new ValidacionException("El curso '" + cdgCrs + "' no existe."));

        PlanCurso plnCrs = planCursoRepository.findByCicloIdAndCursoId(ccl.getId(), crs.getId())
                .orElseThrow(() -> new ValidacionException("El curso '" + cdgCrs
                        + "' no esta asignado al ciclo " + nmrCcl + " en este plan."));

        String nmbrPrd = LectorExcel.textoObligatorio(fl, 10, "Periodo");
        PeriodoAcademico periodo = periodoAcademicoRepository.findByNombre(nmbrPrd)
                .orElseThrow(() -> new ValidacionException("El periodo '" + nmbrPrd + "' no existe."));

        Double nota = LectorExcel.decimal(fl, 11);
        if (nota != null && (nota < 0 || nota > 20)) {
            throw new IllegalArgumentException("La nota debe estar entre 0 y 20.");
        }

        String cdgStcn = LectorExcel.textoObligatorio(fl, 12, "Situacion");
        SituacionAcademica stcn = situacionAcademicaRepository.findByCodigo(cdgStcn)
                .orElseThrow(() -> new ValidacionException("La situacion '" + cdgStcn + "' no existe."));

        String obsrvcn = LectorExcel.texto(fl, 13);

        ItemNota item = new ItemNota(plnCrs.getId(), nota, stcn.getId(), obsrvcn);
        return new FilaResuelta(new GrupoClave(trayectoria.getId(), periodo.getId()), item);
    }

    private Estudiante resolverEstudiante(Row fl) {
        String cdg = LectorExcel.texto(fl, 0);
        String dni = LectorExcel.texto(fl, 1);

        if (cdg != null) {
            var exstnt = estudianteRepository.findByCodigo(cdg);
            if (exstnt.isPresent()) {
                return exstnt.get();
            }
        }
        if (dni != null) {
            var exstnt = estudianteRepository.findByDni(dni);
            if (exstnt.isPresent()) {
                return exstnt.get();
            }
        }
        if (cdg == null && dni == null) {
            throw new IllegalArgumentException("Falta el codigo o el DNI del estudiante.");
        }

        String aplldPtrn = LectorExcel.textoObligatorio(fl, 2, "Apellido Paterno (estudiante nuevo)");
        String aplldMtrn = LectorExcel.texto(fl, 3);
        String nmbrs = LectorExcel.textoObligatorio(fl, 4, "Nombres (estudiante nuevo)");

        return estudianteService.crear(cdg, dni, nmbrs, aplldPtrn, aplldMtrn, null, null, null,
                null, null);
    }

    private Trayectoria resolverTrayectoria(Row fl, Estudiante estudiante) {
        String cdgPrgrm = LectorExcel.texto(fl, 5);
        String cdgPln = LectorExcel.texto(fl, 6);

        if (cdgPrgrm == null && cdgPln == null) {
            return trayectoriaService.obtenerActiva(estudiante.getId())
                    .orElseThrow(() -> new ValidacionException(
                            "El estudiante no tiene una trayectoria activa; indique Programa y Plan para iniciar una."));
        }

        String cdgPrgrmEfctv = LectorExcel.textoObligatorio(fl, 5, "Programa");
        String cdgPlnEfctv = LectorExcel.textoObligatorio(fl, 6, "Plan");

        ProgramaEstudio programa = programaEstudioRepository.findByCodigo(cdgPrgrmEfctv)
                .orElseThrow(() -> new ValidacionException("El programa '" + cdgPrgrmEfctv + "' no existe."));
        PlanEstudio pln = planEstudioRepository.findByProgramaIdAndCodigo(programa.getId(), cdgPlnEfctv)
                .orElseThrow(() -> new ValidacionException("El plan '" + cdgPlnEfctv
                        + "' no existe en el programa '" + cdgPrgrmEfctv + "'."));

        LocalDate fchInc = LectorExcel.fecha(fl, 7);

        return trayectoriaService.obtenerActiva(estudiante.getId())
                .map(activa -> {
                    if (!activa.getPlanEstudio().getId().equals(pln.getId())) {
                        throw new ValidacionException("El estudiante ya tiene una trayectoria activa en el plan '"
                                + activa.getPlanEstudio().getCodigo() + "', distinto al indicado en la fila.");
                    }
                    return activa;
                })
                .orElseGet(() -> trayectoriaService.iniciar(estudiante.getId(), programa.getId(), pln.getId(),
                        fchInc, "Importado desde Excel"));
    }

    private record GrupoClave(Long trayectoriaId, Long periodoId) {
    }

    private record FilaResuelta(GrupoClave grupo, ItemNota item) {
    }

    private record FilaConItem(int numeroFila, ItemNota item) {
    }
}
