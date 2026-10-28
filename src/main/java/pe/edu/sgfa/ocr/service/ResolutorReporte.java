package pe.edu.sgfa.ocr.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import pe.edu.sgfa.common.exception.ValidacionException;
import pe.edu.sgfa.estudiante.domain.Estudiante;
import pe.edu.sgfa.estudiante.repository.EstudianteRepository;
import pe.edu.sgfa.estudiante.service.EstudianteService;
import pe.edu.sgfa.nota.domain.SituacionAcademica;
import pe.edu.sgfa.nota.service.EscalaLiteral;
import pe.edu.sgfa.nota.service.ItemNota;
import pe.edu.sgfa.nota.service.RegistroAcademicoService;
import pe.edu.sgfa.nota.service.ResultadoPromedio;
import pe.edu.sgfa.ocr.service.ReconocedorReporteCalificaciones.CursoDetectado;
import pe.edu.sgfa.ocr.service.ReconocedorReporteCalificaciones.ReporteDetectado;
import pe.edu.sgfa.periodo.domain.PeriodoAcademico;
import pe.edu.sgfa.periodo.service.PeriodoAcademicoService;
import pe.edu.sgfa.programa.domain.Ciclo;
import pe.edu.sgfa.programa.domain.EstadoPlanEstudio;
import pe.edu.sgfa.programa.domain.PlanCurso;
import pe.edu.sgfa.programa.domain.PlanEstudio;
import pe.edu.sgfa.programa.domain.ProgramaEstudio;
import pe.edu.sgfa.programa.repository.PlanCursoRepository;
import pe.edu.sgfa.programa.service.CicloService;
import pe.edu.sgfa.programa.service.PlanEstudioService;
import pe.edu.sgfa.programa.service.ProgramaEstudioService;
import pe.edu.sgfa.trayectoria.domain.Trayectoria;
import pe.edu.sgfa.trayectoria.service.TrayectoriaService;

@Service
public class ResolutorReporte {

    private static final double TOLERANCIA_PROMEDIO = 0.015;

    private final ProgramaEstudioService programaEstudioService;
    private final PlanEstudioService planEstudioService;
    private final CicloService cicloService;
    private final PeriodoAcademicoService periodoAcademicoService;
    private final PlanCursoRepository planCursoRepository;
    private final RegistroAcademicoService registroAcademicoService;
    private final EstudianteService estudianteService;
    private final EstudianteRepository estudianteRepository;
    private final TrayectoriaService trayectoriaService;

    public ResolutorReporte(ProgramaEstudioService programaEstudioService, PlanEstudioService planEstudioService,
                             CicloService cicloService, PeriodoAcademicoService periodoAcademicoService,
                             PlanCursoRepository planCursoRepository, RegistroAcademicoService registroAcademicoService,
                             EstudianteService estudianteService, EstudianteRepository estudianteRepository,
                             TrayectoriaService trayectoriaService) {
        this.programaEstudioService = programaEstudioService;
        this.planEstudioService = planEstudioService;
        this.cicloService = cicloService;
        this.periodoAcademicoService = periodoAcademicoService;
        this.planCursoRepository = planCursoRepository;
        this.registroAcademicoService = registroAcademicoService;
        this.estudianteService = estudianteService;
        this.estudianteRepository = estudianteRepository;
        this.trayectoriaService = trayectoriaService;
    }

    public enum Estado {

        LISTO,

        REVISAR
    }

    public record CursoPropuesto(String nombreDetectado, Double nota, String situacionCodigo, PlanCurso planCurso) {
    }

    public record Propuesta(String matricula, String apellidoPaterno, String apellidoMaterno, String nombres,
                             Estudiante estudiante, ProgramaEstudio programa, PlanEstudio plan, Ciclo ciclo,
                             PeriodoAcademico periodo, List<CursoPropuesto> cursos, Double promedioImpreso,
                             Double promedioCalculado, List<String> avisos, List<String> notas) {

        public Estado estado() {
            return avisos.isEmpty() ? Estado.LISTO : Estado.REVISAR;
        }

        public boolean esNuevo() {
            return estudiante == null;
        }

        public long cursosEmparejados() {
            return cursos.stream().filter(c -> c.planCurso() != null).count();
        }

        Propuesta conAviso(String avs) {
            List<String> nvs = new ArrayList<>(avisos);
            nvs.add(avs);
            return new Propuesta(matricula, apellidoPaterno, apellidoMaterno, nombres, estudiante, programa, plan, ciclo,
                    periodo, cursos, promedioImpreso, promedioCalculado, List.copyOf(nvs), notas);
        }
    }

    public record ResultadoGuardado(Estudiante estudiante, boolean estudianteNuevo, int notasGuardadas,
                                     Double promedioCalculado) {
    }

    public Propuesta proponer(ReporteDetectado r) {
        return proponer(r, null);
    }

    private Propuesta proponer(ReporteDetectado r, PeriodoAcademico prdPorDfct) {
        List<String> avisos = new ArrayList<>(r.advertencias());
        List<String> notas = new ArrayList<>();

        Optional<Estudiante> exstnt = buscarEstudiante(r.matricula());
        String ptrn = r.apellidoPaterno();
        String mtrn = r.apellidoMaterno();
        String nombres = r.nombres();
        if (exstnt.isPresent()) {
            Estudiante e = exstnt.get();

            if (ptrn != null && EmparejadorNombres.similitud(ptrn, e.getApellidoPaterno()) < 0.6) {
                avisos.add("La matricula " + r.matricula() + " es de " + e.getApellidoPaterno() + " "
                        + vacioSiNulo(e.getApellidoMaterno()) + ", pero el reporte dice " + ptrn + ": revise la matricula.");
            }
            ptrn = e.getApellidoPaterno();
            mtrn = e.getApellidoMaterno();
            nombres = e.getNombres();
        } else if (r.matricula() != null) {
            if (ptrn == null || ptrn.isBlank() || nombres == null || nombres.isBlank()) {
                avisos.add("Estudiante nuevo: faltan el apellido paterno o los nombres.");
            }
        }

        ProgramaEstudio programa = null;
        PlanEstudio plan = null;
        Optional<Trayectoria> trayectoria = exstnt.flatMap(e -> trayectoriaService.obtenerActiva(e.getId())
                .or(() -> trayectoriaService.listarPorEstudiante(e.getId()).stream().findFirst()));
        if (trayectoria.isPresent()) {
            programa = trayectoria.get().getPrograma();
            plan = trayectoria.get().getPlanEstudio();
        } else {
            programa = programaPorTexto(r.textoCompleto());
            if (programa == null) {
                avisos.add("No se identifico el programa de estudios.");
            } else {
                List<PlanEstudio> actvs = planEstudioService.listarPorPrograma(programa.getId()).stream()
                        .filter(p -> p.getEstado() == EstadoPlanEstudio.ACTIVO).toList();
                if (actvs.size() == 1) {
                    plan = actvs.get(0);
                } else {
                    avisos.add("El programa " + programa.getNombre() + " tiene "
                            + (actvs.isEmpty() ? "ningun plan activo" : "varios planes activos") + ": elija el plan.");
                }
            }
        }

        Ciclo ciclo = null;
        if (plan != null) {
            List<Ciclo> cclsDelPln = cicloService.listarPorPlan(plan.getId());
            if (r.ciclo() != null) {
                ciclo = cclsDelPln.stream().filter(c -> c.getNumero() == r.ciclo()).findFirst().orElse(null);
                if (ciclo == null) {
                    avisos.add("El plan " + plan.getCodigo() + " no tiene el ciclo " + r.ciclo() + ".");
                }
            } else {

                ciclo = inferirCiclo(cclsDelPln, r.cursos());
                if (ciclo != null) {
                    avisos.remove("No se reconocio el ciclo.");
                    notas.add("El ciclo no se leyo del encabezado: se dedujo de los cursos (" + ciclo.getNombre() + ").");
                }
            }
        }

        PeriodoAcademico periodo = null;
        if (r.periodo() != null) {
            periodo = periodoAcademicoService.listar().stream()
                    .filter(p -> p.getNombre().replaceAll("\\s", "").equalsIgnoreCase(r.periodo())).findFirst().orElse(null);
            if (periodo == null) {
                avisos.add("El periodo " + r.periodo() + " no existe en Catalogos.");
            }
        } else if (prdPorDfct != null) {
            periodo = prdPorDfct;
            avisos.remove("No se reconocio el periodo academico.");
            notas.add("El periodo no se leyo de la hoja: se uso el del resto del lote (" + periodo.getNombre() + ").");
        }

        List<CursoPropuesto> cursos = emparejarCursos(r.cursos(), ciclo, avisos);
        Double clcld = promedioDe(cursos);
        if (r.promedioReporte() != null && clcld != null
                && Math.abs(r.promedioReporte() - clcld) > TOLERANCIA_PROMEDIO) {
            avisos.add(String.format("El promedio impreso (%.2f) no coincide con el calculado (%.2f): probablemente "
                    + "una nota o un curso se leyo mal.", r.promedioReporte(), clcld));
        }
        if (r.promedioReporte() == null && !cursos.isEmpty()) {
            avisos.add("No se leyo el promedio impreso: no hay con que verificar las notas.");
        }
        if (r.matricula() != null && r.matricula().length() == 7 && exstnt.isEmpty()) {
            avisos.add("La matricula leida tiene 7 digitos (" + r.matricula() + "): si se perdio un digito, corrijala.");
        }
        if (cursos.isEmpty() && r.advertencias().stream().noneMatch(a -> a.contains("tabla"))) {
            avisos.add("No se leyo ningun curso.");
        }

        return new Propuesta(r.matricula(), ptrn, mtrn, nombres, exstnt.orElse(null), programa, plan, ciclo,
                periodo, cursos, r.promedioReporte(), clcld, List.copyOf(avisos), List.copyOf(notas));
    }

    private Ciclo inferirCiclo(List<Ciclo> ccls, List<CursoDetectado> dtctds) {
        if (dtctds.isEmpty()) {
            return null;
        }
        Ciclo mjr = null;
        long mjrPntj = 0;
        long sgnd = 0;
        for (Ciclo ciclo : ccls) {
            List<PlanCurso> delCcl = planCursoRepository.findByCicloIdOrderByOrdenAsc(ciclo.getId());
            long pntj = dtctds.stream()
                    .filter(c -> EmparejadorNombres.mejorCoincidencia(c.nombre(), delCcl, pc -> pc.getCurso().getNombre())
                            .isPresent()).count();
            if (pntj > mjrPntj) {
                sgnd = mjrPntj;
                mjrPntj = pntj;
                mjr = ciclo;
            } else if (pntj > sgnd) {
                sgnd = pntj;
            }
        }
        boolean cnvncnt = mjrPntj >= Math.max(2, (dtctds.size() + 1) / 2) && mjrPntj > sgnd;
        return cnvncnt ? mjr : null;
    }

    public List<Propuesta> proponerTodos(List<ReporteDetectado> rprts) {
        List<Propuesta> prpsts = new ArrayList<>(rprts.stream().map(this::proponer).toList());

        Map<Long, Long> frcnc = prpsts.stream().map(Propuesta::periodo).filter(java.util.Objects::nonNull)
                .collect(Collectors.groupingBy(PeriodoAcademico::getId, Collectors.counting()));
        var cmn = frcnc.entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);
        if (cmn != null && cmn.getValue() >= 2 && cmn.getValue() * 10 >= prpsts.size() * 6L) {
            PeriodoAcademico prdCmn = prpsts.stream().map(Propuesta::periodo).filter(java.util.Objects::nonNull)
                    .filter(p -> p.getId().equals(cmn.getKey())).findFirst().orElseThrow();
            for (int i = 0; i < prpsts.size(); i++) {
                if (prpsts.get(i).periodo() == null && rprts.get(i).periodo() == null) {
                    prpsts.set(i, proponer(rprts.get(i), prdCmn));
                }
            }
        }
        Map<String, List<Integer>> porClv = new HashMap<>();
        for (int i = 0; i < prpsts.size(); i++) {
            Propuesta p = prpsts.get(i);
            if (p.matricula() != null && p.ciclo() != null && p.periodo() != null) {
                porClv.computeIfAbsent(p.matricula() + "|" + p.ciclo().getId() + "|" + p.periodo().getId(),
                        k -> new ArrayList<>()).add(i);
            }
        }
        for (List<Integer> indcs : porClv.values()) {
            if (indcs.size() > 1) {
                for (int i : indcs) {
                    prpsts.set(i, prpsts.get(i).conAviso("Otra hoja del lote trae el mismo estudiante, ciclo y periodo."));
                }
            }
        }
        return prpsts;
    }

    private List<CursoPropuesto> emparejarCursos(List<CursoDetectado> dtctds, Ciclo ciclo, List<String> avisos) {
        List<PlanCurso> delCcl = ciclo == null ? List.of() : planCursoRepository.findByCicloIdOrderByOrdenAsc(ciclo.getId());
        Set<Long> usds = new HashSet<>();
        List<CursoPropuesto> rsltd = new ArrayList<>();
        for (CursoDetectado c : dtctds) {
            PlanCurso planCurso = delCcl.isEmpty() ? null
                    : EmparejadorNombres.mejorCoincidencia(c.nombre(), delCcl, pc -> pc.getCurso().getNombre()).orElse(null);
            if (planCurso != null && !usds.add(planCurso.getId())) {
                avisos.add("\"" + c.nombre() + "\" se emparejo con un curso que ya tiene otra fila.");
                planCurso = null;
            } else if (planCurso == null && ciclo != null) {
                avisos.add("\"" + c.nombre() + "\": no se identifico el curso del plan.");
            }
            if (c.nota() == null) {
                avisos.add("\"" + c.nombre() + "\": sin nota.");
            } else if (c.nota() < 0 || c.nota() > 20) {
                avisos.add("\"" + c.nombre() + "\": nota fuera de 0-20 (" + c.nota() + ").");
            } else if (c.situacion() != null && contradice(c.situacion(), EscalaLiteral.codigoPara(c.nota()))) {
                avisos.add("\"" + c.nombre() + "\": el reporte dice " + c.situacion() + " pero la nota " + c.nota()
                        + " corresponde a " + EscalaLiteral.codigoPara(c.nota()) + ".");
            }
            String situacion = c.situacion() != null ? c.situacion()
                    : (c.nota() == null ? null : EscalaLiteral.codigoPara(c.nota()));
            rsltd.add(new CursoPropuesto(c.nombre(), c.nota(), situacion, planCurso));
        }
        return rsltd;
    }

    private static final List<String> NIVELES = List.of(EscalaLiteral.PREVIO_INICIO, EscalaLiteral.INICIO,
            EscalaLiteral.PROCESO, EscalaLiteral.LOGRADO, EscalaLiteral.DESTACADO);

    private static boolean contradice(String ld, String sgnNt) {
        int a = NIVELES.indexOf(ld);
        int b = NIVELES.indexOf(sgnNt);
        return a >= 0 && b >= 0 && Math.abs(a - b) > 1;
    }

    private static Double promedioDe(List<CursoPropuesto> cursos) {
        double sm = 0;
        int crdts = 0;
        for (CursoPropuesto c : cursos) {
            if (c.planCurso() != null && c.nota() != null) {
                int cr = c.planCurso().getCurso().getCreditos();
                sm += c.nota() * cr;
                crdts += cr;
            }
        }
        return crdts == 0 ? null : sm / crdts;
    }

    private ProgramaEstudio programaPorTexto(String txt) {
        ProgramaEstudio mjr = null;
        double mjrPntj = 0;
        for (ProgramaEstudio programa : programaEstudioService.listar()) {
            double pntj = EmparejadorNombres.cobertura(programa.getNombre(), txt);
            if (pntj > mjrPntj) {
                mjrPntj = pntj;
                mjr = programa;
            }
        }
        return mjrPntj >= 0.8 ? mjr : null;
    }

    private Optional<Estudiante> buscarEstudiante(String matricula) {
        if (matricula == null || matricula.isBlank()) {
            return Optional.empty();
        }
        return estudianteRepository.findByDni(matricula.trim()).or(() -> estudianteRepository.findByCodigo(matricula.trim()));
    }

    public ResultadoGuardado guardar(Propuesta p) {
        if (p.matricula() == null || p.matricula().isBlank() || p.programa() == null || p.plan() == null
                || p.ciclo() == null || p.periodo() == null) {
            throw new ValidacionException("Faltan datos para guardar (matricula, programa, plan, ciclo o periodo).");
        }
        Map<String, SituacionAcademica> stcns = registroAcademicoService.listarSituaciones().stream()
                .collect(Collectors.toMap(SituacionAcademica::getCodigo, Function.identity(), (a, b) -> a));
        List<ItemNota> itms = new ArrayList<>();
        for (CursoPropuesto c : p.cursos()) {
            if (c.planCurso() == null || c.nota() == null) {
                continue;
            }
            SituacionAcademica situacion = stcns.get(c.situacionCodigo() != null ? c.situacionCodigo()
                    : EscalaLiteral.codigoPara(c.nota()));
            if (situacion == null) {
                throw new ValidacionException("Falta la escala literal en la base de datos (situaciones DESTACADO/LOGRADO/...).");
            }
            itms.add(new ItemNota(c.planCurso().getId(), c.nota(), situacion.getId(), null));
        }
        if (itms.isEmpty()) {
            throw new ValidacionException("No hay ninguna nota emparejada con un curso del plan.");
        }

        boolean nv = p.estudiante() == null && buscarEstudiante(p.matricula()).isEmpty();
        Estudiante estudiante = p.estudiante() != null ? p.estudiante()
                : buscarEstudiante(p.matricula()).orElseGet(() -> crearEstudiante(p));
        Trayectoria trayectoria = trayectoriaService.listarPorEstudiante(estudiante.getId()).stream()
                .filter(t -> t.getPlanEstudio().getId().equals(p.plan().getId()))
                .findFirst()
                .orElseGet(() -> trayectoriaService.iniciar(estudiante.getId(), p.programa().getId(), p.plan().getId(),
                        null, "Importado desde Reporte de calificaciones (OCR)"));
        registroAcademicoService.guardarMasivo(trayectoria.getId(), p.periodo().getId(), itms);

        ResultadoPromedio promedio = registroAcademicoService.calcularPromedio(trayectoria.getId(), p.ciclo().getId(),
                p.periodo().getId());
        return new ResultadoGuardado(estudiante, nv, itms.size(), promedio.promedio());
    }

    private Estudiante crearEstudiante(Propuesta p) {
        if (p.apellidoPaterno() == null || p.apellidoPaterno().isBlank() || p.nombres() == null || p.nombres().isBlank()) {
            throw new ValidacionException("Estudiante nuevo: faltan el apellido paterno o los nombres.");
        }
        String mtrn = p.apellidoMaterno() == null || p.apellidoMaterno().isBlank() ? null : p.apellidoMaterno().trim();
        return estudianteService.crear(p.matricula().trim(), p.matricula().trim(), p.nombres().trim(),
                p.apellidoPaterno().trim(), mtrn, null, null, null, null, null);
    }

    private static String vacioSiNulo(String txt) {
        return txt == null ? "" : txt;
    }
}
