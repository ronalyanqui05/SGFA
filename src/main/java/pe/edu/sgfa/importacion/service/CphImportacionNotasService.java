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

import pe.edu.sgfa.auditoria.service.RahpAuditoriaService;
import pe.edu.sgfa.common.exception.RyeSgfaException;
import pe.edu.sgfa.common.exception.RyeValidacionException;
import pe.edu.sgfa.estudiante.domain.YmclEstudiante;
import pe.edu.sgfa.estudiante.repository.YmclEstudianteRepository;
import pe.edu.sgfa.estudiante.service.YmclEstudianteService;
import pe.edu.sgfa.nota.domain.YmclSituacionAcademica;
import pe.edu.sgfa.nota.repository.YmclSituacionAcademicaRepository;
import pe.edu.sgfa.nota.service.YmclItemNota;
import pe.edu.sgfa.nota.service.YmclRegistroAcademicoService;
import pe.edu.sgfa.periodo.domain.YmclPeriodoAcademico;
import pe.edu.sgfa.periodo.repository.YmclPeriodoAcademicoRepository;
import pe.edu.sgfa.programa.domain.RacaCiclo;
import pe.edu.sgfa.programa.domain.RacaCurso;
import pe.edu.sgfa.programa.domain.RacaPlanCurso;
import pe.edu.sgfa.programa.domain.RacaPlanEstudio;
import pe.edu.sgfa.programa.domain.RacaProgramaEstudio;
import pe.edu.sgfa.programa.repository.RacaCicloRepository;
import pe.edu.sgfa.programa.repository.RacaCursoRepository;
import pe.edu.sgfa.programa.repository.RacaPlanCursoRepository;
import pe.edu.sgfa.programa.repository.RacaPlanEstudioRepository;
import pe.edu.sgfa.programa.repository.RacaProgramaEstudioRepository;
import pe.edu.sgfa.trayectoria.domain.YmclTrayectoria;
import pe.edu.sgfa.trayectoria.service.YmclTrayectoriaService;
import pe.edu.sgfa.usuario.service.RyeSesionActual;

@Service
public class CphImportacionNotasService {

    static final int COLUMNAS = 14;

    private final YmclEstudianteRepository estudianteRepositoryPch;
    private final YmclEstudianteService estudianteServicePch;
    private final YmclTrayectoriaService trayectoriaServicePch;
    private final RacaProgramaEstudioRepository programaEstudioRepositoryPch;
    private final RacaPlanEstudioRepository planEstudioRepositoryPch;
    private final RacaCicloRepository cicloRepositoryPch;
    private final RacaCursoRepository cursoRepositoryPch;
    private final RacaPlanCursoRepository planCursoRepositoryPch;
    private final YmclPeriodoAcademicoRepository periodoAcademicoRepositoryPch;
    private final YmclSituacionAcademicaRepository situacionAcademicaRepositoryPch;
    private final YmclRegistroAcademicoService registroAcademicoServicePch;
    private final RyeSesionActual sesionActualPch;
    private final RahpAuditoriaService auditoriaServicePch;

    public CphImportacionNotasService(YmclEstudianteRepository estudianteRepositoryPch, YmclEstudianteService estudianteServicePch,
                                    YmclTrayectoriaService trayectoriaServicePch,
                                    RacaProgramaEstudioRepository programaEstudioRepositoryPch,
                                    RacaPlanEstudioRepository planEstudioRepositoryPch, RacaCicloRepository cicloRepositoryPch,
                                    RacaCursoRepository cursoRepositoryPch, RacaPlanCursoRepository planCursoRepositoryPch,
                                    YmclPeriodoAcademicoRepository periodoAcademicoRepositoryPch,
                                    YmclSituacionAcademicaRepository situacionAcademicaRepositoryPch,
                                    YmclRegistroAcademicoService registroAcademicoServicePch, RyeSesionActual sesionActualPch,
                                    RahpAuditoriaService auditoriaServicePch) {
        this.estudianteRepositoryPch = estudianteRepositoryPch;
        this.estudianteServicePch = estudianteServicePch;
        this.trayectoriaServicePch = trayectoriaServicePch;
        this.programaEstudioRepositoryPch = programaEstudioRepositoryPch;
        this.planEstudioRepositoryPch = planEstudioRepositoryPch;
        this.cicloRepositoryPch = cicloRepositoryPch;
        this.cursoRepositoryPch = cursoRepositoryPch;
        this.planCursoRepositoryPch = planCursoRepositoryPch;
        this.periodoAcademicoRepositoryPch = periodoAcademicoRepositoryPch;
        this.situacionAcademicaRepositoryPch = situacionAcademicaRepositoryPch;
        this.registroAcademicoServicePch = registroAcademicoServicePch;
        this.sesionActualPch = sesionActualPch;
        this.auditoriaServicePch = auditoriaServicePch;
    }

    public CphResultadoImportacion cphImportar(Path archivoPch) {
        sesionActualPch.ryeRequerirAutenticacion();

        List<CphErrorImportacion> erroresPch = new ArrayList<>();
        Map<CphGrupoClave, List<CphFilaConItem>> gruposPch = new LinkedHashMap<>();
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
                    CphFilaResuelta resueltaPch = cphResolverFila(filaPch);
                    gruposPch.computeIfAbsent(resueltaPch.grupo(), g -> new ArrayList<>())
                            .add(new CphFilaConItem(numeroFilaPch, resueltaPch.item()));
                } catch (RyeSgfaException | IllegalArgumentException e) {
                    erroresPch.add(new CphErrorImportacion(numeroFilaPch, e.getMessage()));
                }
            }
        } catch (IOException e) {
            throw new RyeSgfaException("No se pudo leer el archivo Excel.", e);
        }

        int exitosasPch = 0;
        for (Map.Entry<CphGrupoClave, List<CphFilaConItem>> grupoPch : gruposPch.entrySet()) {
            List<YmclItemNota> itemsPch = grupoPch.getValue().stream().map(CphFilaConItem::item).toList();
            try {
                registroAcademicoServicePch.ymclGuardarMasivo(grupoPch.getKey().trayectoriaId(), grupoPch.getKey().periodoId(), itemsPch);
                exitosasPch += itemsPch.size();
            } catch (RyeSgfaException e) {
                for (CphFilaConItem filaConItemPch : grupoPch.getValue()) {
                    erroresPch.add(new CphErrorImportacion(filaConItemPch.numeroFila(), e.getMessage()));
                }
            }
        }

        var actorPch = sesionActualPch.ryeObtenerUsuarioActual().orElse(null);
        auditoriaServicePch.rahpRegistrar(actorPch, "IMPORTAR_NOTAS", "registros_academicos", null,
                "total=" + totalFilasPch + ", exitosas=" + exitosasPch + ", errores=" + erroresPch.size());

        return new CphResultadoImportacion(totalFilasPch, exitosasPch, erroresPch);
    }

    private CphFilaResuelta cphResolverFila(Row filaPch) {
        YmclEstudiante estudiantePch = cphResolverEstudiante(filaPch);
        YmclTrayectoria trayectoriaPch = cphResolverTrayectoria(filaPch, estudiantePch);

        Integer numeroCicloPch = CphLectorExcel.cphEntero(filaPch, 8);
        if (numeroCicloPch == null) {
            throw new IllegalArgumentException("Falta el numero de ciclo.");
        }
        RacaCiclo cicloPch = cicloRepositoryPch.findByPlanEstudioIdAndNumero(trayectoriaPch.getPlanEstudio().getId(), numeroCicloPch)
                .orElseThrow(() -> new RyeValidacionException("El ciclo " + numeroCicloPch
                        + " no existe en el plan '" + trayectoriaPch.getPlanEstudio().getCodigo() + "'."));

        String codigoCursoPch = CphLectorExcel.cphTextoObligatorio(filaPch, 9, "Curso");
        RacaCurso cursoPch = cursoRepositoryPch.findByCodigo(codigoCursoPch)
                .orElseThrow(() -> new RyeValidacionException("El curso '" + codigoCursoPch + "' no existe."));

        RacaPlanCurso planCursoPch = planCursoRepositoryPch.findByCicloIdAndCursoId(cicloPch.getId(), cursoPch.getId())
                .orElseThrow(() -> new RyeValidacionException("El curso '" + codigoCursoPch
                        + "' no esta asignado al ciclo " + numeroCicloPch + " en este plan."));

        String nombrePeriodoPch = CphLectorExcel.cphTextoObligatorio(filaPch, 10, "Periodo");
        YmclPeriodoAcademico periodoPch = periodoAcademicoRepositoryPch.findByNombre(nombrePeriodoPch)
                .orElseThrow(() -> new RyeValidacionException("El periodo '" + nombrePeriodoPch + "' no existe."));

        Double notaPch = CphLectorExcel.cphDecimal(filaPch, 11);
        if (notaPch != null && (notaPch < 0 || notaPch > 20)) {
            throw new IllegalArgumentException("La nota debe estar entre 0 y 20.");
        }

        String codigoSituacionPch = CphLectorExcel.cphTextoObligatorio(filaPch, 12, "Situacion");
        YmclSituacionAcademica situacionPch = situacionAcademicaRepositoryPch.findByCodigo(codigoSituacionPch)
                .orElseThrow(() -> new RyeValidacionException("La situacion '" + codigoSituacionPch + "' no existe."));

        String observacionPch = CphLectorExcel.cphTexto(filaPch, 13);

        YmclItemNota itemPch = new YmclItemNota(planCursoPch.getId(), notaPch, situacionPch.getId(), observacionPch);
        return new CphFilaResuelta(new CphGrupoClave(trayectoriaPch.getId(), periodoPch.getId()), itemPch);
    }

    private YmclEstudiante cphResolverEstudiante(Row filaPch) {
        String codigoPch = CphLectorExcel.cphTexto(filaPch, 0);
        String dniPch = CphLectorExcel.cphTexto(filaPch, 1);

        if (codigoPch != null) {
            var existentePch = estudianteRepositoryPch.findByCodigo(codigoPch);
            if (existentePch.isPresent()) {
                return existentePch.get();
            }
        }
        if (dniPch != null) {
            var existentePch = estudianteRepositoryPch.findByDni(dniPch);
            if (existentePch.isPresent()) {
                return existentePch.get();
            }
        }
        if (codigoPch == null && dniPch == null) {
            throw new IllegalArgumentException("Falta el codigo o el DNI del estudiante.");
        }

        String apellidoPaternoPch = CphLectorExcel.cphTextoObligatorio(filaPch, 2, "Apellido Paterno (estudiante nuevo)");
        String apellidoMaternoPch = CphLectorExcel.cphTexto(filaPch, 3);
        String nombresPch = CphLectorExcel.cphTextoObligatorio(filaPch, 4, "Nombres (estudiante nuevo)");

        return estudianteServicePch.ymclCrear(codigoPch, dniPch, nombresPch, apellidoPaternoPch, apellidoMaternoPch, null, null, null,
                null, null);
    }

    private YmclTrayectoria cphResolverTrayectoria(Row filaPch, YmclEstudiante estudiantePch) {
        String codigoProgramaPch = CphLectorExcel.cphTexto(filaPch, 5);
        String codigoPlanPch = CphLectorExcel.cphTexto(filaPch, 6);

        if (codigoProgramaPch == null && codigoPlanPch == null) {
            return trayectoriaServicePch.ymclObtenerActiva(estudiantePch.getId())
                    .orElseThrow(() -> new RyeValidacionException(
                            "El estudiante no tiene una trayectoria activa; indique Programa y Plan para iniciar una."));
        }

        String codigoProgramaEfectivoPch = CphLectorExcel.cphTextoObligatorio(filaPch, 5, "Programa");
        String codigoPlanEfectivoPch = CphLectorExcel.cphTextoObligatorio(filaPch, 6, "Plan");

        RacaProgramaEstudio programaPch = programaEstudioRepositoryPch.findByCodigo(codigoProgramaEfectivoPch)
                .orElseThrow(() -> new RyeValidacionException("El programa '" + codigoProgramaEfectivoPch + "' no existe."));
        RacaPlanEstudio planPch = planEstudioRepositoryPch.findByProgramaIdAndCodigo(programaPch.getId(), codigoPlanEfectivoPch)
                .orElseThrow(() -> new RyeValidacionException("El plan '" + codigoPlanEfectivoPch
                        + "' no existe en el programa '" + codigoProgramaEfectivoPch + "'."));

        LocalDate fechaInicioPch = CphLectorExcel.fecha(filaPch, 7);

        return trayectoriaServicePch.ymclObtenerActiva(estudiantePch.getId())
                .map(activaPch -> {
                    if (!activaPch.getPlanEstudio().getId().equals(planPch.getId())) {
                        throw new RyeValidacionException("El estudiante ya tiene una trayectoria activa en el plan '"
                                + activaPch.getPlanEstudio().getCodigo() + "', distinto al indicado en la fila.");
                    }
                    return activaPch;
                })
                .orElseGet(() -> trayectoriaServicePch.ymclIniciar(estudiantePch.getId(), programaPch.getId(), planPch.getId(),
                        fechaInicioPch, "Importado desde Excel"));
    }

    private record CphGrupoClave(Long trayectoriaId, Long periodoId) {
    }

    private record CphFilaResuelta(CphGrupoClave grupo, YmclItemNota item) {
    }

    private record CphFilaConItem(int numeroFila, YmclItemNota item) {
    }
}
