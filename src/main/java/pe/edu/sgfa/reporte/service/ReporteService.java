package pe.edu.sgfa.reporte.service;

import java.text.Collator;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.sgfa.common.exception.EntidadNoEncontradaException;
import pe.edu.sgfa.common.exception.ValidacionException;
import pe.edu.sgfa.estudiante.domain.Estudiante;
import pe.edu.sgfa.ficha.service.TituloCiclo;
import pe.edu.sgfa.institucion.service.InstitucionService;
import pe.edu.sgfa.nota.domain.RegistroAcademico;
import pe.edu.sgfa.nota.repository.RegistroAcademicoRepository;
import pe.edu.sgfa.nota.service.EscalaLiteral;
import pe.edu.sgfa.periodo.domain.PeriodoAcademico;
import pe.edu.sgfa.periodo.repository.PeriodoAcademicoRepository;
import pe.edu.sgfa.programa.domain.Ciclo;
import pe.edu.sgfa.programa.domain.PlanCurso;
import pe.edu.sgfa.programa.domain.PlanEstudio;
import pe.edu.sgfa.programa.domain.ProgramaEstudio;
import pe.edu.sgfa.programa.repository.CicloRepository;
import pe.edu.sgfa.programa.repository.PlanCursoRepository;
import pe.edu.sgfa.programa.repository.PlanEstudioRepository;
import pe.edu.sgfa.programa.repository.ProgramaEstudioRepository;
import pe.edu.sgfa.reporte.service.TablaReporte.Grupo;
import pe.edu.sgfa.trayectoria.domain.EstadoTrayectoria;
import pe.edu.sgfa.trayectoria.domain.Trayectoria;
import pe.edu.sgfa.trayectoria.repository.TrayectoriaRepository;
import pe.edu.sgfa.usuario.service.SesionActual;

@Service
@Transactional(readOnly = true)
public class ReporteService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ProgramaEstudioRepository programaRepository;
    private final PlanEstudioRepository planRepository;
    private final CicloRepository cicloRepository;
    private final PeriodoAcademicoRepository periodoRepository;
    private final PlanCursoRepository planCursoRepository;
    private final RegistroAcademicoRepository registroRepository;
    private final TrayectoriaRepository trayectoriaRepository;
    private final InstitucionService institucionService;
    private final SesionActual sesionActual;
    private final Collator collator = Collator.getInstance(Locale.forLanguageTag("es"));

    public ReporteService(ProgramaEstudioRepository programaRepository, PlanEstudioRepository planRepository,
                           CicloRepository cicloRepository, PeriodoAcademicoRepository periodoRepository,
                           PlanCursoRepository planCursoRepository, RegistroAcademicoRepository registroRepository,
                           TrayectoriaRepository trayectoriaRepository, InstitucionService institucionService,
                           SesionActual sesionActual) {
        this.programaRepository = programaRepository;
        this.planRepository = planRepository;
        this.cicloRepository = cicloRepository;
        this.periodoRepository = periodoRepository;
        this.planCursoRepository = planCursoRepository;
        this.registroRepository = registroRepository;
        this.trayectoriaRepository = trayectoriaRepository;
        this.institucionService = institucionService;
        this.sesionActual = sesionActual;
        this.collator.setStrength(Collator.SECONDARY);
    }

    public List<ProgramaEstudio> listarProgramas() {
        sesionActual.requerirAutenticacion();
        return programaRepository.findAll().stream().sorted(Comparator.comparing(ProgramaEstudio::getNombre)).toList();
    }

    public List<PlanEstudio> listarPlanes(Long prgrmId) {
        sesionActual.requerirAutenticacion();
        return planRepository.findByProgramaId(prgrmId);
    }

    public List<Ciclo> listarCiclos(Long plnId) {
        sesionActual.requerirAutenticacion();
        return cicloRepository.findByPlanEstudioIdOrderByOrdenAsc(plnId);
    }

    public List<PeriodoAcademico> listarPeriodos() {
        sesionActual.requerirAutenticacion();
        return periodoRepository.findAll().stream()
                .sorted(Comparator.comparingInt(PeriodoAcademico::getAnio).thenComparing(PeriodoAcademico::getNombre).reversed())
                .toList();
    }

    public String nombreInstitucion() {
        return institucionService.obtenerPrincipal().map(i -> i.getNombre()).orElse("");
    }

    public TablaReporte actaConsolidada(Long plnId, Long cclId, Long prdId) {
        Contexto c = contexto(plnId, cclId, prdId);
        Map<Estudiante, Map<Long, RegistroAcademico>> porEstdnt = notasPorEstudiante(c);

        List<String> clmns = new ArrayList<>(List.of("N°", "N° Matricula", "Apellido paterno", "Apellido materno", "Nombres"));
        List<Integer> anchs = new ArrayList<>(List.of(4, 10, 14, 14, 18));
        List<Boolean> nmrcs = new ArrayList<>(List.of(true, false, false, false, false));
        List<Grupo> grps = new ArrayList<>(List.of(new Grupo("", 1), new Grupo("", 1), new Grupo("", 1), new Grupo("", 1),
                new Grupo("", 1)));
        for (PlanCurso pc : c.cursos()) {
            grps.add(new Grupo(pc.getCurso().getNombre() + " (" + pc.getCurso().getCreditos() + " cred.)", 3));
            clmns.addAll(List.of("Sit.", "Nota", "PTJ"));
            anchs.addAll(List.of(4, 5, 6));
            nmrcs.addAll(List.of(false, true, true));
        }
        grps.add(new Grupo("TOTAL", 3));
        clmns.addAll(List.of("Cred.", "PTJ", "Prom."));
        anchs.addAll(List.of(5, 6, 6));
        nmrcs.addAll(List.of(true, true, true));

        List<List<String>> fls = new ArrayList<>();
        double[] smNtPorCrs = new double[c.cursos().size()];
        int[] cntddPorCrs = new int[c.cursos().size()];
        int nmr = 1;
        for (var entrd : estudiantesOrdenados(porEstdnt)) {
            Estudiante e = entrd.getKey();
            List<String> fl = new ArrayList<>(List.of(String.valueOf(nmr++), matricula(e),
                    nz(e.getApellidoPaterno()), nz(e.getApellidoMaterno()), nz(e.getNombres())));
            double ptjTtl = 0;
            int crdTtl = 0;
            for (int i = 0; i < c.cursos().size(); i++) {
                PlanCurso pc = c.cursos().get(i);
                RegistroAcademico r = entrd.getValue().get(pc.getId());
                if (r == null) {
                    fl.addAll(List.of("", "", ""));
                    continue;
                }
                fl.add(letra(r.getSituacion().getCodigo(), r.getSituacion().getNombre()));
                if (r.getNotaFinal() == null) {
                    fl.addAll(List.of("", ""));
                    continue;
                }
                double ptj = r.getNotaFinal() * pc.getCurso().getCreditos();
                fl.add(num(r.getNotaFinal()));
                fl.add(num(ptj));
                smNtPorCrs[i] += r.getNotaFinal();
                cntddPorCrs[i]++;
                if (r.getSituacion().isCuentaParaPromedio()) {
                    ptjTtl += ptj;
                    crdTtl += pc.getCurso().getCreditos();
                }
            }
            fl.add(crdTtl == 0 ? "" : String.valueOf(crdTtl));
            fl.add(crdTtl == 0 ? "" : num(ptjTtl));
            fl.add(crdTtl == 0 ? "-" : dos(ptjTtl / crdTtl));
            fls.add(fl);
        }
        if (!fls.isEmpty()) {
            List<String> promedio = new ArrayList<>(List.of("", "", "PROMEDIO DEL CURSO", "", ""));
            for (int i = 0; i < c.cursos().size(); i++) {
                promedio.addAll(List.of("", cntddPorCrs[i] == 0 ? "-" : dos(smNtPorCrs[i] / cntddPorCrs[i]), ""));
            }
            promedio.addAll(List.of("", "", ""));
            fls.add(promedio);
        }

        List<String> sbttl = encabezado(c);
        sbttl.add("Estudiantes: " + porEstdnt.size());
        return new TablaReporte("ACTA CONSOLIDADA DE NOTAS", sbttl, grps, clmns, anchs, nmrcs, fls,
                List.of("Sit.: D Destacado, L Logrado, E En proceso, I Inicio, P Previo al inicio, T Retirado. "
                        + "PTJ = nota x creditos. Prom. = promedio ponderado del ciclo."), true);
    }

    public TablaReporte resultadosPorCurso(Long plnId, Long cclId, Long prdId) {
        Contexto c = contexto(plnId, cclId, prdId);
        Map<Long, List<RegistroAcademico>> porCrs = c.registros().stream()
                .collect(Collectors.groupingBy(r -> r.getPlanCurso().getId()));

        List<String> clmns = List.of("N°", "Curso", "Cred.", "Evaluados", "Destacado", "Logrado", "En proceso", "Inicio",
                "Previo", "Aprobados", "Desaprobados", "% Aprob.", "Promedio", "Min.", "Max.");
        List<Integer> anchs = List.of(3, 30, 4, 6, 6, 6, 6, 5, 5, 6, 7, 6, 6, 4, 4);
        List<Boolean> nmrcs = List.of(true, false, true, true, true, true, true, true, true, true, true, true, true, true, true);

        List<List<String>> fls = new ArrayList<>();
        int[] ttls = new int[9];
        int n = 1;
        for (PlanCurso pc : c.cursos()) {
            List<RegistroAcademico> rgs = porCrs.getOrDefault(pc.getId(), List.of());
            int[] cnt = new int[9];
            double sm = 0;
            Double min = null;
            Double max = null;
            for (RegistroAcademico r : rgs) {
                cnt[0]++;
                switch (r.getSituacion().getCodigo()) {
                    case EscalaLiteral.DESTACADO -> cnt[1]++;
                    case EscalaLiteral.LOGRADO -> cnt[2]++;
                    case EscalaLiteral.PROCESO -> cnt[3]++;
                    case EscalaLiteral.INICIO -> cnt[4]++;
                    case EscalaLiteral.PREVIO_INICIO -> cnt[5]++;
                    default -> { }
                }
                Double nota = r.getNotaFinal();
                if (nota != null) {
                    cnt[8]++;
                    sm += nota;
                    min = min == null ? nota : Math.min(min, nota);
                    max = max == null ? nota : Math.max(max, nota);
                    if (ReglasAcademicas.aprobada(nota)) {
                        cnt[6]++;
                    } else {
                        cnt[7]++;
                    }
                }
            }
            for (int i = 0; i < ttls.length; i++) {
                ttls[i] += cnt[i];
            }
            fls.add(List.of(String.valueOf(n++), pc.getCurso().getNombre(), String.valueOf(pc.getCurso().getCreditos()),
                    String.valueOf(cnt[0]), String.valueOf(cnt[1]), String.valueOf(cnt[2]), String.valueOf(cnt[3]),
                    String.valueOf(cnt[4]), String.valueOf(cnt[5]), String.valueOf(cnt[6]), String.valueOf(cnt[7]),
                    cnt[8] == 0 ? "-" : porcentaje(cnt[6], cnt[8]), cnt[8] == 0 ? "-" : dos(sm / cnt[8]),
                    min == null ? "-" : num(min), max == null ? "-" : num(max)));
        }
        fls.add(List.of("", "TOTAL", "", String.valueOf(ttls[0]), String.valueOf(ttls[1]), String.valueOf(ttls[2]),
                String.valueOf(ttls[3]), String.valueOf(ttls[4]), String.valueOf(ttls[5]), String.valueOf(ttls[6]),
                String.valueOf(ttls[7]), ttls[8] == 0 ? "-" : porcentaje(ttls[6], ttls[8]), "", "", ""));

        return new TablaReporte("CUADRO DE RESULTADOS POR CURSO", encabezado(c), null, clmns, anchs, nmrcs, fls,
                List.of("Aprobado = nota " + (int) ReglasAcademicas.NOTA_MINIMA_APROBATORIA + " o mas. "
                        + "Evaluados = estudiantes con registro en el curso."), false);
    }

    public TablaReporte riesgoYPromocion(Long plnId, Long cclId, Long prdId) {
        Contexto c = contexto(plnId, cclId, prdId);
        Map<Estudiante, Map<Long, RegistroAcademico>> porEstdnt = notasPorEstudiante(c);
        int crdtsCcl = c.cursos().stream().mapToInt(pc -> pc.getCurso().getCreditos()).sum();

        record Fila(int orden, Estudiante e, List<String> celdas) {
        }
        List<Fila> fls = new ArrayList<>();
        for (var entrd : estudiantesOrdenados(porEstdnt)) {
            Map<Long, RegistroAcademico> nts = entrd.getValue();
            int aprbds = 0;
            int fltnts = 0;
            int evlds = 0;
            double ptj = 0;
            int crdConNt = 0;
            List<String> dsprbds = new ArrayList<>();
            List<String> sinNt = new ArrayList<>();
            for (PlanCurso pc : c.cursos()) {
                RegistroAcademico r = nts.get(pc.getId());
                Double nota = r == null ? null : r.getNotaFinal();
                if (nota == null) {
                    fltnts += pc.getCurso().getCreditos();
                    sinNt.add(pc.getCurso().getNombre());
                    continue;
                }
                evlds++;
                if (r.getSituacion().isCuentaParaPromedio()) {
                    ptj += nota * pc.getCurso().getCreditos();
                    crdConNt += pc.getCurso().getCreditos();
                }
                if (ReglasAcademicas.aprobada(nota)) {
                    aprbds += pc.getCurso().getCreditos();
                } else {
                    dsprbds.add(pc.getCurso().getNombre() + " (" + num(nota) + ")");
                }
            }
            double pct = crdtsCcl == 0 ? 0 : (double) aprbds / crdtsCcl;
            double ptncl = crdtsCcl == 0 ? 0 : (double) (aprbds + fltnts) / crdtsCcl;
            String prmcn;
            String rsg;
            int orden;
            List<String> dtll = new ArrayList<>();
            if (pct >= ReglasAcademicas.PORCENTAJE_PROMOCION) {
                prmcn = "PROMUEVE";
                rsg = dsprbds.isEmpty() ? "BAJO" : "MEDIO";
                orden = dsprbds.isEmpty() ? 3 : 2;
            } else if (ptncl < ReglasAcademicas.PORCENTAJE_PROMOCION) {
                prmcn = "NO PROMUEVE";
                rsg = "ALTO";
                orden = 0;
                dtll.add("Aun aprobando lo que falta no llega al 75% de creditos");
            } else {
                prmcn = "POR DEFINIR";
                rsg = "INCOMPLETO";
                orden = 1;
            }
            if (!dsprbds.isEmpty()) {
                dtll.add("Desaprobado en: " + String.join(", ", dsprbds));
            }
            if (!sinNt.isEmpty()) {
                dtll.add("Faltan notas: " + String.join(", ", sinNt));
            }
            fls.add(new Fila(orden, entrd.getKey(), List.of("", matricula(entrd.getKey()), nombreCompleto(entrd.getKey()),
                    evlds + "/" + c.cursos().size(), String.valueOf(crdtsCcl), String.valueOf(aprbds),
                    crdtsCcl == 0 ? "-" : porcentaje(aprbds, crdtsCcl),
                    crdConNt == 0 ? "-" : dos(ptj / crdConNt), prmcn, rsg, String.join(". ", dtll))));
        }
        fls.sort(Comparator.comparingInt(Fila::orden).thenComparing(f -> nombreCompleto(f.e()), collator));
        List<List<String>> sld = new ArrayList<>();
        int n = 1;
        for (Fila f : fls) {
            List<String> celdas = new ArrayList<>(f.celdas());
            celdas.set(0, String.valueOf(n++));
            sld.add(celdas);
        }
        List<String> sbttl = encabezado(c);
        long alts = fls.stream().filter(f -> f.orden() == 0).count();
        long incmplts = fls.stream().filter(f -> f.orden() == 1).count();
        sbttl.add("Estudiantes: " + fls.size() + " | Riesgo alto: " + alts + " | Incompletos: " + incmplts);
        return new TablaReporte("ESTUDIANTES EN RIESGO Y PROMOCION", sbttl, null,
                List.of("N°", "N° Matricula", "Apellidos y nombres", "Evaluados", "Cred. ciclo", "Cred. aprob.", "% aprob.",
                        "Promedio", "Promocion", "Riesgo", "Detalle"),
                List.of(3, 9, 24, 6, 5, 5, 5, 6, 9, 8, 36),
                List.of(true, false, false, true, true, true, true, true, false, false, false), sld,
                List.of("Se aprueba un curso con nota " + (int) ReglasAcademicas.NOTA_MINIMA_APROBATORIA + " o mas. "
                        + "Se promueve con el 75% o mas de los creditos del ciclo aprobados. "
                        + "ALTO = no llega al 75% ni aprobando lo que falta; INCOMPLETO = faltan notas para decidir; "
                        + "MEDIO = promueve pero con cursos desaprobados; BAJO = todo en orden. Reporte interno."), false);
    }

    public TablaReporte coberturaPorCiclo(Long plnId, Long prdId) {
        PlanEstudio plan = plan(plnId);
        PeriodoAcademico periodo = periodo(prdId);
        sesionActual.requerirAutenticacion();
        Map<Long, List<RegistroAcademico>> porCcl = registroRepository.findParaReporte(plnId, prdId).stream()
                .collect(Collectors.groupingBy(r -> r.getPlanCurso().getCiclo().getId()));

        List<List<String>> fls = new ArrayList<>();
        for (Ciclo ciclo : cicloRepository.findByPlanEstudioIdOrderByOrdenAsc(plnId)) {
            int cursos = planCursoRepository.findByCicloIdOrderByOrdenAsc(ciclo.getId()).size();
            List<RegistroAcademico> rgs = porCcl.getOrDefault(ciclo.getId(), List.of());
            long estdnts = rgs.stream().map(r -> r.getTrayectoria().getId()).distinct().count();
            long esprds = estdnts * cursos;
            long crgds = rgs.stream().filter(r -> r.getNotaFinal() != null).count();
            long fltn = Math.max(0, esprds - crgds);
            String estd = rgs.isEmpty() ? "Sin notas" : (fltn == 0 ? "Completo" : "Parcial");
            fls.add(List.of(TituloCiclo.de(ciclo.getNumero()), String.valueOf(cursos), String.valueOf(estdnts),
                    String.valueOf(esprds), String.valueOf(crgds), String.valueOf(fltn),
                    esprds == 0 ? "-" : porcentaje(crgds, esprds), estd));
        }
        return new TablaReporte("COBERTURA DE NOTAS POR CICLO", List.of("Programa: " + plan.getPrograma().getNombre(),
                "Plan: " + plan.getCodigo() + " - " + plan.getNombre(), "Periodo: " + periodo.getNombre(),
                "Emitido: " + LocalDate.now().format(FECHA)), null,
                List.of("Ciclo", "Cursos", "Estudiantes con notas", "Notas esperadas", "Notas cargadas", "Faltan", "% cobertura", "Estado"),
                List.of(14, 6, 10, 9, 9, 7, 8, 9), List.of(false, true, true, true, true, true, true, false), fls,
                List.of("Esperadas = estudiantes que ya tienen alguna nota del ciclo x cursos del ciclo. "
                        + "Un ciclo \"Sin notas\" puede ser que aun no se cargo o que nadie lo cursa en el periodo."), false);
    }

    public TablaReporte pendientes(Long plnId, Long prdId, Long cclId) {
        PlanEstudio plan = plan(plnId);
        PeriodoAcademico periodo = periodo(prdId);
        sesionActual.requerirAutenticacion();
        List<RegistroAcademico> registros = registroRepository.findParaReporte(plnId, prdId);

        List<Ciclo> ccls = cicloRepository.findByPlanEstudioIdOrderByOrdenAsc(plnId).stream()
                .filter(ci -> cclId == null || ci.getId().equals(cclId)).toList();
        record Pendiente(int ciclo, Estudiante e, List<String> celdas) {
        }
        List<Pendiente> pendientes = new ArrayList<>();
        for (Ciclo ciclo : ccls) {
            List<PlanCurso> cursos = planCursoRepository.findByCicloIdOrderByOrdenAsc(ciclo.getId());
            Map<Estudiante, List<RegistroAcademico>> porEstdnt = registros.stream()
                    .filter(r -> r.getPlanCurso().getCiclo().getId().equals(ciclo.getId()))
                    .collect(Collectors.groupingBy(r -> r.getTrayectoria().getEstudiante()));
            for (var entrd : porEstdnt.entrySet()) {
                Set<Long> conNt = entrd.getValue().stream().filter(r -> r.getNotaFinal() != null)
                        .map(r -> r.getPlanCurso().getId()).collect(Collectors.toSet());
                List<String> fltn = cursos.stream().filter(pc -> !conNt.contains(pc.getId()))
                        .map(pc -> pc.getCurso().getNombre()).toList();
                if (!fltn.isEmpty()) {
                    pendientes.add(new Pendiente(ciclo.getNumero(), entrd.getKey(), List.of("", matricula(entrd.getKey()),
                            nombreCompleto(entrd.getKey()), TituloCiclo.de(ciclo.getNumero()),
                            conNt.size() + "/" + cursos.size(), String.join("; ", fltn))));
                }
            }
        }
        pendientes.sort(Comparator.comparingInt(Pendiente::ciclo).thenComparing(p -> nombreCompleto(p.e()), collator));

        List<List<String>> fls = new ArrayList<>();
        int n = 1;
        for (Pendiente p : pendientes) {
            List<String> celdas = new ArrayList<>(p.celdas());
            celdas.set(0, String.valueOf(n++));
            fls.add(celdas);
        }
        long incmplts = fls.size();
        long sinNngn = 0;
        if (cclId == null) {
            Set<Long> conRgstrs = registros.stream().map(r -> r.getTrayectoria().getId()).collect(Collectors.toSet());
            List<Trayectoria> sinNts = trayectoriaRepository.findByPlanEstudioIdAndEstado(plnId, EstadoTrayectoria.ACTIVA).stream()
                    .filter(t -> !conRgstrs.contains(t.getId()))
                    .sorted(Comparator.comparing((Trayectoria t) -> nombreCompleto(t.getEstudiante()), collator)).toList();
            for (Trayectoria t : sinNts) {
                fls.add(List.of(String.valueOf(n++), matricula(t.getEstudiante()), nombreCompleto(t.getEstudiante()), "-", "0/-",
                        "Sin ninguna nota en el periodo"));
            }
            sinNngn = sinNts.size();
        }
        List<String> sbttl = new ArrayList<>(List.of("Programa: " + plan.getPrograma().getNombre(),
                "Plan: " + plan.getCodigo() + " - " + plan.getNombre(), "Periodo: " + periodo.getNombre()));
        if (cclId != null && !ccls.isEmpty()) {
            sbttl.add("Ciclo: " + TituloCiclo.de(ccls.get(0).getNumero()));
        }
        sbttl.add("Con notas incompletas: " + incmplts + (cclId == null ? " | Activos sin ninguna nota: " + sinNngn : ""));
        return new TablaReporte("PENDIENTES DE CARGAR", sbttl, null,
                List.of("N°", "N° Matricula", "Apellidos y nombres", "Ciclo", "Cursos con nota", "Cursos sin nota"),
                List.of(3, 9, 24, 12, 8, 50), List.of(true, false, false, false, true, false), fls,
                List.of("Incluye a quienes ya tienen alguna nota del ciclo pero les faltan cursos y, al ver todos los ciclos, "
                        + "a los estudiantes activos del plan sin ninguna nota en el periodo."), false);
    }

    public enum AmbitoMerito {

        CICLO_PERIODO,

        CICLO_VIGENTE,

        PLAN_ACUMULADO
    }

    public TablaReporte ordenMerito(Long plnId, AmbitoMerito ambt, Long cclId, Long prdId, Integer lmtPsts) {
        sesionActual.requerirAutenticacion();
        PlanEstudio plan = plan(plnId);
        Ciclo ciclo = null;
        PeriodoAcademico periodo = null;
        if (ambt != AmbitoMerito.PLAN_ACUMULADO) {
            ciclo = cicloRepository.findById(cclId).filter(ci -> ci.getPlanEstudio().getId().equals(plnId))
                    .orElseThrow(() -> new EntidadNoEncontradaException("Ciclo no encontrado en el plan: id=" + cclId));
        }
        if (ambt == AmbitoMerito.CICLO_PERIODO) {
            periodo = periodo(prdId);
        }
        List<PlanCurso> crssDelCcl = ciclo == null ? List.of() : planCursoRepository.findByCicloIdOrderByOrdenAsc(ciclo.getId());
        final Long cclFltr = ciclo == null ? null : ciclo.getId();

        List<RegistroAcademico> registros;
        if (ambt == AmbitoMerito.CICLO_PERIODO) {
            registros = registroRepository.findParaReporte(plnId, periodo.getId()).stream()
                    .filter(r -> r.getPlanCurso().getCiclo().getId().equals(cclFltr)).toList();
        } else {

            Map<String, RegistroAcademico> vgnts = new HashMap<>();
            for (RegistroAcademico r : registroRepository.findTodosDelPlan(plnId)) {
                if (cclFltr != null && !r.getPlanCurso().getCiclo().getId().equals(cclFltr)) {
                    continue;
                }
                vgnts.merge(r.getTrayectoria().getId() + "|" + r.getPlanCurso().getId(), r,
                        (a, b) -> a.getId() > b.getId() ? a : b);
            }
            registros = List.copyOf(vgnts.values());
        }

        Set<Long> actvs = trayectoriaRepository.findByPlanEstudioIdAndEstado(plnId, EstadoTrayectoria.ACTIVA).stream()
                .map(Trayectoria::getId).collect(Collectors.toSet());
        Map<Estudiante, List<RegistroAcademico>> porEstdnt = registros.stream()
                .filter(r -> actvs.contains(r.getTrayectoria().getId()))
                .collect(Collectors.groupingBy(r -> r.getTrayectoria().getEstudiante()));

        record Entrada(Estudiante e, double promedio, int creditos, String observacion) {
        }
        List<Entrada> entrds = new ArrayList<>();
        for (var entrd : porEstdnt.entrySet()) {
            double ptj = 0;
            int creditos = 0;
            List<String> dsprbds = new ArrayList<>();
            Set<Long> conNt = new java.util.HashSet<>();
            for (RegistroAcademico r : entrd.getValue()) {
                Double nota = r.getNotaFinal();
                if (nota == null) {
                    continue;
                }
                conNt.add(r.getPlanCurso().getId());
                if (r.getSituacion().isCuentaParaPromedio()) {
                    ptj += nota * r.getPlanCurso().getCurso().getCreditos();
                    creditos += r.getPlanCurso().getCurso().getCreditos();
                }
                if (!ReglasAcademicas.aprobada(nota)) {
                    dsprbds.add(r.getPlanCurso().getCurso().getNombre() + " (" + num(nota) + ")");
                }
            }
            if (creditos == 0) {
                continue;
            }
            List<String> obsrvcns = new ArrayList<>();
            if (!dsprbds.isEmpty()) {
                obsrvcns.add((dsprbds.size() == 1 ? "1 curso desaprobado: " : dsprbds.size() + " cursos desaprobados: ")
                        + String.join(", ", dsprbds));
            }
            long fltn = crssDelCcl.stream().filter(pc -> !conNt.contains(pc.getId())).count();
            if (fltn > 0) {
                obsrvcns.add("Faltan notas de " + fltn + (fltn == 1 ? " curso" : " cursos"));
            }
            entrds.add(new Entrada(entrd.getKey(), Math.round(ptj / creditos * 100) / 100.0, creditos,
                    String.join(". ", obsrvcns)));
        }
        entrds.sort(Comparator.comparingDouble(Entrada::promedio).reversed()
                .thenComparing(en -> nombreCompleto(en.e()), collator));

        int ttl = entrds.size();
        double umbrlTrc = ttl == 0 ? 0 : entrds.get((int) Math.ceil(ttl / 3.0) - 1).promedio();
        double umbrlQnt = ttl == 0 ? 0 : entrds.get((int) Math.ceil(ttl / 5.0) - 1).promedio();
        long enTrc = entrds.stream().filter(en -> en.promedio() >= umbrlTrc).count();
        long enQnt = entrds.stream().filter(en -> en.promedio() >= umbrlQnt).count();

        List<List<String>> fls = new ArrayList<>();
        int pst = 0;
        Double antrr = null;
        for (int i = 0; i < entrds.size(); i++) {
            Entrada en = entrds.get(i);
            if (antrr == null || Double.compare(en.promedio(), antrr) != 0) {
                pst = i + 1;
            }
            antrr = en.promedio();
            if (lmtPsts != null && pst > lmtPsts) {
                break;
            }
            String mrt = en.promedio() >= umbrlQnt ? "Quinto superior" : (en.promedio() >= umbrlTrc ? "Tercio superior" : "");
            fls.add(List.of(String.valueOf(pst), matricula(en.e()), nombreCompleto(en.e()), String.valueOf(en.creditos()),
                    dos(en.promedio()), mrt, en.observacion()));
        }

        List<String> sbttl = new ArrayList<>(List.of("Programa: " + plan.getPrograma().getNombre(),
                "Plan: " + plan.getCodigo() + " - " + plan.getNombre()));
        sbttl.add(switch (ambt) {
            case CICLO_PERIODO -> "Ciclo: " + TituloCiclo.de(ciclo.getNumero()) + " | Periodo: " + periodo.getNombre();
            case CICLO_VIGENTE -> "Ciclo: " + TituloCiclo.de(ciclo.getNumero()) + " (notas vigentes de cualquier periodo)";
            case PLAN_ACUMULADO -> "General: promedio acumulado de todo el plan (notas vigentes)";
        });
        sbttl.add("Emitido: " + LocalDate.now().format(FECHA));
        sbttl.add("Estudiantes en el orden: " + ttl + (lmtPsts == null ? "" : " | Se muestran los primeros " + lmtPsts
                + " puestos (" + fls.size() + " estudiantes)"));
        if (ttl > 0) {
            sbttl.add("Tercio superior: promedio de " + dos(umbrlTrc) + " o mas (" + enTrc + " estudiantes) | "
                    + "Quinto superior: " + dos(umbrlQnt) + " o mas (" + enQnt + ")");
        }
        return new TablaReporte("ORDEN DE MERITO", sbttl, null,
                List.of("Puesto", "N\u00B0 Matricula", "Apellidos y nombres", "Creditos", "Promedio", "Merito", "Observacion"),
                List.of(7, 10, 26, 8, 9, 13, 36), List.of(true, false, false, true, true, false, false), fls,
                List.of("Los estudiantes con el mismo promedio (a 2 decimales) comparten el puesto; no se desempata por nombre. "
                        + "Tercio y quinto superior: el primer 1/3 y 1/5 de los estudiantes del orden, redondeado hacia arriba e "
                        + "incluyendo a los empatados en el corte. La observacion avisa de cursos desaprobados o notas por cargar; "
                        + "esos estudiantes igual entran al orden."), true);
    }

    public TablaReporte cuadroDeHonor(Long prdId, int psts) {
        sesionActual.requerirAutenticacion();
        if (psts < 1) {
            throw new ValidacionException("El numero de lugares debe ser al menos 1.");
        }
        PeriodoAcademico periodo = periodo(prdId);

        record Entrada(Estudiante e, double promedio, int creditos, Ciclo ciclo, String observacion) {
        }
        List<List<String>> fls = new ArrayList<>();
        Set<Integer> sccns = new java.util.HashSet<>();
        int espclddsConNts = 0;
        int rcncds = 0;
        for (ProgramaEstudio programa : programaRepository.findAll().stream()
                .sorted(Comparator.comparing(ProgramaEstudio::getNombre, collator)).toList()) {
            Map<Long, List<RegistroAcademico>> porEstdnt = new LinkedHashMap<>();
            for (PlanEstudio plan : planRepository.findByProgramaId(programa.getId())) {
                Set<Long> actvs = trayectoriaRepository.findByPlanEstudioIdAndEstado(plan.getId(), EstadoTrayectoria.ACTIVA)
                        .stream().map(Trayectoria::getId).collect(Collectors.toSet());
                for (RegistroAcademico r : registroRepository.findParaReporte(plan.getId(), prdId)) {
                    if (actvs.contains(r.getTrayectoria().getId())) {
                        porEstdnt.computeIfAbsent(r.getTrayectoria().getEstudiante().getId(), k -> new ArrayList<>()).add(r);
                    }
                }
            }
            List<Entrada> entrds = new ArrayList<>();
            for (List<RegistroAcademico> registros : porEstdnt.values()) {
                double ptj = 0;
                int creditos = 0;
                List<String> dsprbds = new ArrayList<>();
                Set<Long> conNt = new java.util.HashSet<>();
                Ciclo cclActl = null;
                for (RegistroAcademico r : registros) {
                    Ciclo ciclo = r.getPlanCurso().getCiclo();
                    if (cclActl == null || ciclo.getNumero() > cclActl.getNumero()) {
                        cclActl = ciclo;
                    }
                    Double nota = r.getNotaFinal();
                    if (nota == null) {
                        continue;
                    }
                    conNt.add(r.getPlanCurso().getId());
                    if (r.getSituacion().isCuentaParaPromedio()) {
                        ptj += nota * r.getPlanCurso().getCurso().getCreditos();
                        creditos += r.getPlanCurso().getCurso().getCreditos();
                    }
                    if (!ReglasAcademicas.aprobada(nota)) {
                        dsprbds.add(r.getPlanCurso().getCurso().getNombre() + " (" + num(nota) + ")");
                    }
                }
                if (creditos == 0 || cclActl == null) {
                    continue;
                }
                List<String> obsrvcns = new ArrayList<>();
                if (!dsprbds.isEmpty()) {
                    obsrvcns.add((dsprbds.size() == 1 ? "1 curso desaprobado: " : dsprbds.size() + " cursos desaprobados: ")
                            + String.join(", ", dsprbds));
                }
                long fltn = planCursoRepository.findByCicloIdOrderByOrdenAsc(cclActl.getId()).stream()
                        .filter(pc -> !conNt.contains(pc.getId())).count();
                if (fltn > 0) {
                    obsrvcns.add("Faltan notas de " + fltn + (fltn == 1 ? " curso" : " cursos"));
                }
                entrds.add(new Entrada(registros.get(0).getTrayectoria().getEstudiante(),
                        Math.round(ptj / creditos * 100) / 100.0, creditos, cclActl, String.join(". ", obsrvcns)));
            }
            entrds.sort(Comparator.comparingDouble(Entrada::promedio).reversed()
                    .thenComparing(en -> nombreCompleto(en.e()), collator));

            sccns.add(fls.size());
            fls.add(List.of("", "", programa.getNombre().toUpperCase(Locale.ROOT) + "  (" + entrds.size()
                    + (entrds.size() == 1 ? " estudiante con notas)" : " estudiantes con notas)"), "", "", "", ""));
            if (entrds.isEmpty()) {
                fls.add(List.of("", "", "Sin notas cargadas en este periodo", "", "", "", ""));
                continue;
            }
            espclddsConNts++;
            int pst = 0;
            Double antrr = null;
            for (int i = 0; i < entrds.size(); i++) {
                Entrada en = entrds.get(i);
                if (antrr == null || Double.compare(en.promedio(), antrr) != 0) {
                    pst = i + 1;
                }
                antrr = en.promedio();
                if (pst > psts) {
                    break;
                }
                rcncds++;
                fls.add(List.of(lugar(pst), matricula(en.e()), nombreCompleto(en.e()), TituloCiclo.de(en.ciclo().getNumero()),
                        String.valueOf(en.creditos()), dos(en.promedio()), en.observacion()));
            }
        }

        List<String> sbttl = new ArrayList<>(List.of("Periodo: " + periodo.getNombre(),
                "Emitido: " + LocalDate.now().format(FECHA),
                "Se muestran los primeros " + psts + " lugares de cada especialidad | Especialidades con notas: "
                        + espclddsConNts + " | Estudiantes reconocidos: " + rcncds));
        return new TablaReporte("CUADRO DE HONOR POR ESPECIALIDAD", sbttl, null,
                List.of("Lugar", "N° Matricula", "Apellidos y nombres", "Ciclo", "Creditos", "Promedio", "Observacion"),
                List.of(12, 10, 30, 12, 7, 8, 34), List.of(false, false, false, false, true, true, false), fls,
                List.of("El promedio es el ponderado por creditos de las notas del periodo; en cada especialidad compiten juntos "
                        + "todos los ciclos. Los estudiantes con el mismo promedio (a 2 decimales) comparten el lugar. Solo entran "
                        + "estudiantes con trayectoria activa. La observacion avisa de cursos desaprobados o notas por cargar; "
                        + "esos estudiantes igual entran al orden."), true, sccns);
    }

    private static String lugar(int pst) {
        return switch (pst) {
            case 1 -> "Primer lugar";
            case 2 -> "Segundo lugar";
            case 3 -> "Tercer lugar";
            default -> pst + "° lugar";
        };
    }

    private record Contexto(PlanEstudio plan, Ciclo ciclo, PeriodoAcademico periodo, List<PlanCurso> cursos,
                            List<RegistroAcademico> registros) {
    }

    private Contexto contexto(Long plnId, Long cclId, Long prdId) {
        sesionActual.requerirAutenticacion();
        PlanEstudio plan = plan(plnId);
        Ciclo ciclo = cicloRepository.findById(cclId)
                .filter(ci -> ci.getPlanEstudio().getId().equals(plnId))
                .orElseThrow(() -> new EntidadNoEncontradaException("Ciclo no encontrado en el plan: id=" + cclId));
        PeriodoAcademico periodo = periodo(prdId);
        List<PlanCurso> cursos = planCursoRepository.findByCicloIdOrderByOrdenAsc(cclId);
        List<RegistroAcademico> registros = registroRepository.findParaReporte(plnId, prdId).stream()
                .filter(r -> r.getPlanCurso().getCiclo().getId().equals(cclId)).toList();
        return new Contexto(plan, ciclo, periodo, cursos, registros);
    }

    private PlanEstudio plan(Long plnId) {
        return planRepository.findById(plnId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Plan de estudio no encontrado: id=" + plnId));
    }

    private PeriodoAcademico periodo(Long prdId) {
        return periodoRepository.findById(prdId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Periodo no encontrado: id=" + prdId));
    }

    private List<String> encabezado(Contexto c) {
        return new ArrayList<>(List.of("Programa: " + c.plan().getPrograma().getNombre(),
                "Plan: " + c.plan().getCodigo() + " - " + c.plan().getNombre(),
                "Ciclo: " + TituloCiclo.de(c.ciclo().getNumero()), "Periodo: " + c.periodo().getNombre(),
                "Emitido: " + LocalDate.now().format(FECHA)));
    }

    private Map<Estudiante, Map<Long, RegistroAcademico>> notasPorEstudiante(Contexto c) {
        Map<Estudiante, Map<Long, RegistroAcademico>> mp = new HashMap<>();
        for (RegistroAcademico r : c.registros()) {
            mp.computeIfAbsent(r.getTrayectoria().getEstudiante(), k -> new HashMap<>()).put(r.getPlanCurso().getId(), r);
        }
        return mp;
    }

    private List<Map.Entry<Estudiante, Map<Long, RegistroAcademico>>> estudiantesOrdenados(
            Map<Estudiante, Map<Long, RegistroAcademico>> mp) {
        return mp.entrySet().stream()
                .sorted(Comparator.comparing((Map.Entry<Estudiante, Map<Long, RegistroAcademico>> e) -> nombreCompleto(e.getKey()), collator))
                .toList();
    }

    private static String nombreCompleto(Estudiante e) {
        return nz(e.getApellidoPaterno()) + (e.getApellidoMaterno() == null || e.getApellidoMaterno().isBlank() ? ""
                : " " + e.getApellidoMaterno()) + ", " + nz(e.getNombres());
    }

    private static String matricula(Estudiante e) {
        return e.getDni() != null && !e.getDni().isBlank() ? e.getDni() : nz(e.getCodigo());
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    static String letra(String cdg, String nmbr) {
        return switch (cdg) {
            case EscalaLiteral.DESTACADO -> "D";
            case EscalaLiteral.LOGRADO -> "L";
            case EscalaLiteral.PROCESO -> "E";
            case EscalaLiteral.INICIO -> "I";
            case EscalaLiteral.PREVIO_INICIO -> "P";
            case "RETIRADO" -> "T";
            default -> nmbr == null || nmbr.isBlank() ? "" : nmbr.substring(0, 1).toUpperCase(Locale.ROOT);
        };
    }

    private static String num(double v) {
        return Math.rint(v) == v ? String.valueOf((long) v) : String.format(Locale.ROOT, "%.1f", v);
    }

    private static String dos(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }

    private static String porcentaje(double prt, double ttl) {
        return String.format(Locale.ROOT, "%.0f%%", 100.0 * prt / ttl);
    }
}
