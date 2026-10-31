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

import pe.edu.sgfa.common.exception.RyeEntidadNoEncontradaException;
import pe.edu.sgfa.common.exception.RyeValidacionException;
import pe.edu.sgfa.estudiante.domain.YmclEstudiante;
import pe.edu.sgfa.ficha.service.RacaTituloCiclo;
import pe.edu.sgfa.institucion.service.RacaInstitucionService;
import pe.edu.sgfa.nota.domain.YmclRegistroAcademico;
import pe.edu.sgfa.nota.repository.YmclRegistroAcademicoRepository;
import pe.edu.sgfa.nota.service.YmclEscalaLiteral;
import pe.edu.sgfa.periodo.domain.YmclPeriodoAcademico;
import pe.edu.sgfa.periodo.repository.YmclPeriodoAcademicoRepository;
import pe.edu.sgfa.programa.domain.RacaCiclo;
import pe.edu.sgfa.programa.domain.RacaPlanCurso;
import pe.edu.sgfa.programa.domain.RacaPlanEstudio;
import pe.edu.sgfa.programa.domain.RacaProgramaEstudio;
import pe.edu.sgfa.programa.repository.RacaCicloRepository;
import pe.edu.sgfa.programa.repository.RacaPlanCursoRepository;
import pe.edu.sgfa.programa.repository.RacaPlanEstudioRepository;
import pe.edu.sgfa.programa.repository.RacaProgramaEstudioRepository;
import pe.edu.sgfa.reporte.service.CphTablaReporte.CphGrupo;
import pe.edu.sgfa.trayectoria.domain.YmclEstadoTrayectoria;
import pe.edu.sgfa.trayectoria.domain.YmclTrayectoria;
import pe.edu.sgfa.trayectoria.repository.YmclTrayectoriaRepository;
import pe.edu.sgfa.usuario.service.RyeSesionActual;

@Service
@Transactional(readOnly = true)
public class CphReporteService {

    private static final DateTimeFormatter FECHA_PCH = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final RacaProgramaEstudioRepository programaRepositoryPch;
    private final RacaPlanEstudioRepository planRepositoryPch;
    private final RacaCicloRepository cicloRepositoryPch;
    private final YmclPeriodoAcademicoRepository periodoRepositoryPch;
    private final RacaPlanCursoRepository planCursoRepositoryPch;
    private final YmclRegistroAcademicoRepository registroRepositoryPch;
    private final YmclTrayectoriaRepository trayectoriaRepositoryPch;
    private final RacaInstitucionService institucionServicePch;
    private final RyeSesionActual sesionActualPch;
    private final Collator collatorPch = Collator.getInstance(Locale.forLanguageTag("es"));

    public CphReporteService(RacaProgramaEstudioRepository programaRepositoryPch, RacaPlanEstudioRepository planRepositoryPch,
                           RacaCicloRepository cicloRepositoryPch, YmclPeriodoAcademicoRepository periodoRepositoryPch,
                           RacaPlanCursoRepository planCursoRepositoryPch, YmclRegistroAcademicoRepository registroRepositoryPch,
                           YmclTrayectoriaRepository trayectoriaRepositoryPch, RacaInstitucionService institucionServicePch,
                           RyeSesionActual sesionActualPch) {
        this.programaRepositoryPch = programaRepositoryPch;
        this.planRepositoryPch = planRepositoryPch;
        this.cicloRepositoryPch = cicloRepositoryPch;
        this.periodoRepositoryPch = periodoRepositoryPch;
        this.planCursoRepositoryPch = planCursoRepositoryPch;
        this.registroRepositoryPch = registroRepositoryPch;
        this.trayectoriaRepositoryPch = trayectoriaRepositoryPch;
        this.institucionServicePch = institucionServicePch;
        this.sesionActualPch = sesionActualPch;
        this.collatorPch.setStrength(Collator.SECONDARY);
    }

    public List<RacaProgramaEstudio> cphListarProgramas() {
        sesionActualPch.ryeRequerirAutenticacion();
        return programaRepositoryPch.findAll().stream().sorted(Comparator.comparing(RacaProgramaEstudio::getNombre)).toList();
    }

    public List<RacaPlanEstudio> cphListarPlanes(Long programaIdPch) {
        sesionActualPch.ryeRequerirAutenticacion();
        return planRepositoryPch.findByProgramaId(programaIdPch);
    }

    public List<RacaCiclo> cphListarCiclos(Long planIdPch) {
        sesionActualPch.ryeRequerirAutenticacion();
        return cicloRepositoryPch.findByPlanEstudioIdOrderByOrdenAsc(planIdPch);
    }

    public List<YmclPeriodoAcademico> cphListarPeriodos() {
        sesionActualPch.ryeRequerirAutenticacion();
        return periodoRepositoryPch.findAll().stream()
                .sorted(Comparator.comparingInt(YmclPeriodoAcademico::getAnio).thenComparing(YmclPeriodoAcademico::getNombre).reversed())
                .toList();
    }

    public String cphNombreInstitucion() {
        return institucionServicePch.racaObtenerPrincipal().map(i -> i.getNombre()).orElse("");
    }

    public CphTablaReporte cphActaConsolidada(Long planIdPch, Long cicloIdPch, Long periodoIdPch) {
        CphContexto c = cphContexto(planIdPch, cicloIdPch, periodoIdPch);
        Map<YmclEstudiante, Map<Long, YmclRegistroAcademico>> porEstudiantePch = cphNotasPorEstudiante(c);

        List<String> columnasPch = new ArrayList<>(List.of("N°", "N° Matricula", "Apellido paterno", "Apellido materno", "Nombres"));
        List<Integer> anchosPch = new ArrayList<>(List.of(4, 10, 14, 14, 18));
        List<Boolean> numericasPch = new ArrayList<>(List.of(true, false, false, false, false));
        List<CphGrupo> gruposPch = new ArrayList<>(List.of(new CphGrupo("", 1), new CphGrupo("", 1), new CphGrupo("", 1), new CphGrupo("", 1),
                new CphGrupo("", 1)));
        for (RacaPlanCurso pc : c.cursos()) {
            gruposPch.add(new CphGrupo(pc.getCurso().getNombre() + " (" + pc.getCurso().getCreditos() + " cred.)", 3));
            columnasPch.addAll(List.of("Sit.", "Nota", "PTJ"));
            anchosPch.addAll(List.of(4, 5, 6));
            numericasPch.addAll(List.of(false, true, true));
        }
        gruposPch.add(new CphGrupo("TOTAL", 3));
        columnasPch.addAll(List.of("Cred.", "PTJ", "Prom."));
        anchosPch.addAll(List.of(5, 6, 6));
        numericasPch.addAll(List.of(true, true, true));

        List<List<String>> filasPch = new ArrayList<>();
        double[] sumaNotaPorCursoPch = new double[c.cursos().size()];
        int[] cantidadPorCursoPch = new int[c.cursos().size()];
        int numeroPch = 1;
        for (var entradaPch : cphEstudiantesOrdenados(porEstudiantePch)) {
            YmclEstudiante e = entradaPch.getKey();
            List<String> filaPch = new ArrayList<>(List.of(String.valueOf(numeroPch++), cphMatricula(e),
                    nz(e.getApellidoPaterno()), nz(e.getApellidoMaterno()), nz(e.getNombres())));
            double ptjTotalPch = 0;
            int credTotalPch = 0;
            for (int i = 0; i < c.cursos().size(); i++) {
                RacaPlanCurso pc = c.cursos().get(i);
                YmclRegistroAcademico r = entradaPch.getValue().get(pc.getId());
                if (r == null) {
                    filaPch.addAll(List.of("", "", ""));
                    continue;
                }
                filaPch.add(cphLetra(r.getSituacion().getCodigo(), r.getSituacion().getNombre()));
                if (r.getNotaFinal() == null) {
                    filaPch.addAll(List.of("", ""));
                    continue;
                }
                double ptjPch = r.getNotaFinal() * pc.getCurso().getCreditos();
                filaPch.add(num(r.getNotaFinal()));
                filaPch.add(num(ptjPch));
                sumaNotaPorCursoPch[i] += r.getNotaFinal();
                cantidadPorCursoPch[i]++;
                if (r.getSituacion().isCuentaParaPromedio()) {
                    ptjTotalPch += ptjPch;
                    credTotalPch += pc.getCurso().getCreditos();
                }
            }
            filaPch.add(credTotalPch == 0 ? "" : String.valueOf(credTotalPch));
            filaPch.add(credTotalPch == 0 ? "" : num(ptjTotalPch));
            filaPch.add(credTotalPch == 0 ? "-" : dos(ptjTotalPch / credTotalPch));
            filasPch.add(filaPch);
        }
        if (!filasPch.isEmpty()) {
            List<String> promedioPch = new ArrayList<>(List.of("", "", "PROMEDIO DEL CURSO", "", ""));
            for (int i = 0; i < c.cursos().size(); i++) {
                promedioPch.addAll(List.of("", cantidadPorCursoPch[i] == 0 ? "-" : dos(sumaNotaPorCursoPch[i] / cantidadPorCursoPch[i]), ""));
            }
            promedioPch.addAll(List.of("", "", ""));
            filasPch.add(promedioPch);
        }

        List<String> subtituloPch = cphEncabezado(c);
        subtituloPch.add("Estudiantes: " + porEstudiantePch.size());
        return new CphTablaReporte("ACTA CONSOLIDADA DE NOTAS", subtituloPch, gruposPch, columnasPch, anchosPch, numericasPch, filasPch,
                List.of("Sit.: D Destacado, L Logrado, E En proceso, I Inicio, P Previo al inicio, T Retirado. "
                        + "PTJ = nota x creditos. Prom. = promedio ponderado del ciclo."), true);
    }

    public CphTablaReporte cphResultadosPorCurso(Long planIdPch, Long cicloIdPch, Long periodoIdPch) {
        CphContexto c = cphContexto(planIdPch, cicloIdPch, periodoIdPch);
        Map<Long, List<YmclRegistroAcademico>> porCursoPch = c.registros().stream()
                .collect(Collectors.groupingBy(r -> r.getPlanCurso().getId()));

        List<String> columnasPch = List.of("N°", "Curso", "Cred.", "Evaluados", "Destacado", "Logrado", "En proceso", "Inicio",
                "Previo", "Aprobados", "Desaprobados", "% Aprob.", "Promedio", "Min.", "Max.");
        List<Integer> anchosPch = List.of(3, 30, 4, 6, 6, 6, 6, 5, 5, 6, 7, 6, 6, 4, 4);
        List<Boolean> numericasPch = List.of(true, false, true, true, true, true, true, true, true, true, true, true, true, true, true);

        List<List<String>> filasPch = new ArrayList<>();
        int[] totalesPch = new int[9];
        int n = 1;
        for (RacaPlanCurso pc : c.cursos()) {
            List<YmclRegistroAcademico> regsPch = porCursoPch.getOrDefault(pc.getId(), List.of());
            int[] contPch = new int[9];
            double sumaPch = 0;
            Double minPch = null;
            Double maxPch = null;
            for (YmclRegistroAcademico r : regsPch) {
                contPch[0]++;
                switch (r.getSituacion().getCodigo()) {
                    case YmclEscalaLiteral.DESTACADO -> contPch[1]++;
                    case YmclEscalaLiteral.LOGRADO -> contPch[2]++;
                    case YmclEscalaLiteral.PROCESO -> contPch[3]++;
                    case YmclEscalaLiteral.INICIO -> contPch[4]++;
                    case YmclEscalaLiteral.PREVIO_INICIO -> contPch[5]++;
                    default -> { }
                }
                Double notaPch = r.getNotaFinal();
                if (notaPch != null) {
                    contPch[8]++;
                    sumaPch += notaPch;
                    minPch = minPch == null ? notaPch : Math.min(minPch, notaPch);
                    maxPch = maxPch == null ? notaPch : Math.max(maxPch, notaPch);
                    if (CphReglasAcademicas.cphAprobada(notaPch)) {
                        contPch[6]++;
                    } else {
                        contPch[7]++;
                    }
                }
            }
            for (int i = 0; i < totalesPch.length; i++) {
                totalesPch[i] += contPch[i];
            }
            filasPch.add(List.of(String.valueOf(n++), pc.getCurso().getNombre(), String.valueOf(pc.getCurso().getCreditos()),
                    String.valueOf(contPch[0]), String.valueOf(contPch[1]), String.valueOf(contPch[2]), String.valueOf(contPch[3]),
                    String.valueOf(contPch[4]), String.valueOf(contPch[5]), String.valueOf(contPch[6]), String.valueOf(contPch[7]),
                    contPch[8] == 0 ? "-" : cphPorcentaje(contPch[6], contPch[8]), contPch[8] == 0 ? "-" : dos(sumaPch / contPch[8]),
                    minPch == null ? "-" : num(minPch), maxPch == null ? "-" : num(maxPch)));
        }
        filasPch.add(List.of("", "TOTAL", "", String.valueOf(totalesPch[0]), String.valueOf(totalesPch[1]), String.valueOf(totalesPch[2]),
                String.valueOf(totalesPch[3]), String.valueOf(totalesPch[4]), String.valueOf(totalesPch[5]), String.valueOf(totalesPch[6]),
                String.valueOf(totalesPch[7]), totalesPch[8] == 0 ? "-" : cphPorcentaje(totalesPch[6], totalesPch[8]), "", "", ""));

        return new CphTablaReporte("CUADRO DE RESULTADOS POR CURSO", cphEncabezado(c), null, columnasPch, anchosPch, numericasPch, filasPch,
                List.of("Aprobado = nota " + (int) CphReglasAcademicas.NOTA_MINIMA_APROBATORIA + " o mas. "
                        + "Evaluados = estudiantes con registro en el curso."), false);
    }

    public CphTablaReporte cphRiesgoYPromocion(Long planIdPch, Long cicloIdPch, Long periodoIdPch) {
        CphContexto c = cphContexto(planIdPch, cicloIdPch, periodoIdPch);
        Map<YmclEstudiante, Map<Long, YmclRegistroAcademico>> porEstudiantePch = cphNotasPorEstudiante(c);
        int creditosCicloPch = c.cursos().stream().mapToInt(pc -> pc.getCurso().getCreditos()).sum();

        record CphFila(int orden, YmclEstudiante e, List<String> celdas) {
        }
        List<CphFila> filasPch = new ArrayList<>();
        for (var entradaPch : cphEstudiantesOrdenados(porEstudiantePch)) {
            Map<Long, YmclRegistroAcademico> notasPch = entradaPch.getValue();
            int aprobadosPch = 0;
            int faltantesPch = 0;
            int evaluadosPch = 0;
            double ptjPch = 0;
            int credConNotaPch = 0;
            List<String> desaprobadosPch = new ArrayList<>();
            List<String> sinNotaPch = new ArrayList<>();
            for (RacaPlanCurso pc : c.cursos()) {
                YmclRegistroAcademico r = notasPch.get(pc.getId());
                Double notaPch = r == null ? null : r.getNotaFinal();
                if (notaPch == null) {
                    faltantesPch += pc.getCurso().getCreditos();
                    sinNotaPch.add(pc.getCurso().getNombre());
                    continue;
                }
                evaluadosPch++;
                if (r.getSituacion().isCuentaParaPromedio()) {
                    ptjPch += notaPch * pc.getCurso().getCreditos();
                    credConNotaPch += pc.getCurso().getCreditos();
                }
                if (CphReglasAcademicas.cphAprobada(notaPch)) {
                    aprobadosPch += pc.getCurso().getCreditos();
                } else {
                    desaprobadosPch.add(pc.getCurso().getNombre() + " (" + num(notaPch) + ")");
                }
            }
            double pctPch = creditosCicloPch == 0 ? 0 : (double) aprobadosPch / creditosCicloPch;
            double potencialPch = creditosCicloPch == 0 ? 0 : (double) (aprobadosPch + faltantesPch) / creditosCicloPch;
            String promocionPch;
            String riesgoPch;
            int ordenPch;
            List<String> detallePch = new ArrayList<>();
            if (pctPch >= CphReglasAcademicas.PORCENTAJE_PROMOCION) {
                promocionPch = "PROMUEVE";
                riesgoPch = desaprobadosPch.isEmpty() ? "BAJO" : "MEDIO";
                ordenPch = desaprobadosPch.isEmpty() ? 3 : 2;
            } else if (potencialPch < CphReglasAcademicas.PORCENTAJE_PROMOCION) {
                promocionPch = "NO PROMUEVE";
                riesgoPch = "ALTO";
                ordenPch = 0;
                detallePch.add("Aun aprobando lo que falta no llega al 75% de creditos");
            } else {
                promocionPch = "POR DEFINIR";
                riesgoPch = "INCOMPLETO";
                ordenPch = 1;
            }
            if (!desaprobadosPch.isEmpty()) {
                detallePch.add("Desaprobado en: " + String.join(", ", desaprobadosPch));
            }
            if (!sinNotaPch.isEmpty()) {
                detallePch.add("Faltan notas: " + String.join(", ", sinNotaPch));
            }
            filasPch.add(new CphFila(ordenPch, entradaPch.getKey(), List.of("", cphMatricula(entradaPch.getKey()), cphNombreCompleto(entradaPch.getKey()),
                    evaluadosPch + "/" + c.cursos().size(), String.valueOf(creditosCicloPch), String.valueOf(aprobadosPch),
                    creditosCicloPch == 0 ? "-" : cphPorcentaje(aprobadosPch, creditosCicloPch),
                    credConNotaPch == 0 ? "-" : dos(ptjPch / credConNotaPch), promocionPch, riesgoPch, String.join(". ", detallePch))));
        }
        filasPch.sort(Comparator.comparingInt(CphFila::orden).thenComparing(f -> cphNombreCompleto(f.e()), collatorPch));
        List<List<String>> salidaPch = new ArrayList<>();
        int n = 1;
        for (CphFila f : filasPch) {
            List<String> celdasPch = new ArrayList<>(f.celdas());
            celdasPch.set(0, String.valueOf(n++));
            salidaPch.add(celdasPch);
        }
        List<String> subtituloPch = cphEncabezado(c);
        long altosPch = filasPch.stream().filter(f -> f.orden() == 0).count();
        long incompletosPch = filasPch.stream().filter(f -> f.orden() == 1).count();
        subtituloPch.add("Estudiantes: " + filasPch.size() + " | Riesgo alto: " + altosPch + " | Incompletos: " + incompletosPch);
        return new CphTablaReporte("ESTUDIANTES EN RIESGO Y PROMOCION", subtituloPch, null,
                List.of("N°", "N° Matricula", "Apellidos y nombres", "Evaluados", "Cred. ciclo", "Cred. aprob.", "% aprob.",
                        "Promedio", "Promocion", "Riesgo", "Detalle"),
                List.of(3, 9, 24, 6, 5, 5, 5, 6, 9, 8, 36),
                List.of(true, false, false, true, true, true, true, true, false, false, false), salidaPch,
                List.of("Se aprueba un curso con nota " + (int) CphReglasAcademicas.NOTA_MINIMA_APROBATORIA + " o mas. "
                        + "Se promueve con el 75% o mas de los creditos del ciclo aprobados. "
                        + "ALTO = no llega al 75% ni aprobando lo que falta; INCOMPLETO = faltan notas para decidir; "
                        + "MEDIO = promueve pero con cursos desaprobados; BAJO = todo en orden. Reporte interno."), false);
    }

    public CphTablaReporte cphCoberturaPorCiclo(Long planIdPch, Long periodoIdPch) {
        RacaPlanEstudio planPch = cphPlan(planIdPch);
        YmclPeriodoAcademico periodoPch = cphPeriodo(periodoIdPch);
        sesionActualPch.ryeRequerirAutenticacion();
        Map<Long, List<YmclRegistroAcademico>> porCicloPch = registroRepositoryPch.findParaReporte(planIdPch, periodoIdPch).stream()
                .collect(Collectors.groupingBy(r -> r.getPlanCurso().getCiclo().getId()));

        List<List<String>> filasPch = new ArrayList<>();
        for (RacaCiclo cicloPch : cicloRepositoryPch.findByPlanEstudioIdOrderByOrdenAsc(planIdPch)) {
            int cursosPch = planCursoRepositoryPch.findByCicloIdOrderByOrdenAsc(cicloPch.getId()).size();
            List<YmclRegistroAcademico> regsPch = porCicloPch.getOrDefault(cicloPch.getId(), List.of());
            long estudiantesPch = regsPch.stream().map(r -> r.getTrayectoria().getId()).distinct().count();
            long esperadasPch = estudiantesPch * cursosPch;
            long cargadasPch = regsPch.stream().filter(r -> r.getNotaFinal() != null).count();
            long faltanPch = Math.max(0, esperadasPch - cargadasPch);
            String estadoPch = regsPch.isEmpty() ? "Sin notas" : (faltanPch == 0 ? "Completo" : "Parcial");
            filasPch.add(List.of(RacaTituloCiclo.de(cicloPch.getNumero()), String.valueOf(cursosPch), String.valueOf(estudiantesPch),
                    String.valueOf(esperadasPch), String.valueOf(cargadasPch), String.valueOf(faltanPch),
                    esperadasPch == 0 ? "-" : cphPorcentaje(cargadasPch, esperadasPch), estadoPch));
        }
        return new CphTablaReporte("COBERTURA DE NOTAS POR CICLO", List.of("Programa: " + planPch.getPrograma().getNombre(),
                "Plan: " + planPch.getCodigo() + " - " + planPch.getNombre(), "Periodo: " + periodoPch.getNombre(),
                "Emitido: " + LocalDate.now().format(FECHA_PCH)), null,
                List.of("Ciclo", "Cursos", "Estudiantes con notas", "Notas esperadas", "Notas cargadas", "Faltan", "% cobertura", "Estado"),
                List.of(14, 6, 10, 9, 9, 7, 8, 9), List.of(false, true, true, true, true, true, true, false), filasPch,
                List.of("Esperadas = estudiantes que ya tienen alguna nota del ciclo x cursos del ciclo. "
                        + "Un ciclo \"Sin notas\" puede ser que aun no se cargo o que nadie lo cursa en el periodo."), false);
    }

    public CphTablaReporte cphPendientes(Long planIdPch, Long periodoIdPch, Long cicloIdPch) {
        RacaPlanEstudio planPch = cphPlan(planIdPch);
        YmclPeriodoAcademico periodoPch = cphPeriodo(periodoIdPch);
        sesionActualPch.ryeRequerirAutenticacion();
        List<YmclRegistroAcademico> registrosPch = registroRepositoryPch.findParaReporte(planIdPch, periodoIdPch);

        List<RacaCiclo> ciclosPch = cicloRepositoryPch.findByPlanEstudioIdOrderByOrdenAsc(planIdPch).stream()
                .filter(ci -> cicloIdPch == null || ci.getId().equals(cicloIdPch)).toList();
        record CphPendiente(int ciclo, YmclEstudiante e, List<String> celdas) {
        }
        List<CphPendiente> pendientesPch = new ArrayList<>();
        for (RacaCiclo cicloPch : ciclosPch) {
            List<RacaPlanCurso> cursosPch = planCursoRepositoryPch.findByCicloIdOrderByOrdenAsc(cicloPch.getId());
            Map<YmclEstudiante, List<YmclRegistroAcademico>> porEstudiantePch = registrosPch.stream()
                    .filter(r -> r.getPlanCurso().getCiclo().getId().equals(cicloPch.getId()))
                    .collect(Collectors.groupingBy(r -> r.getTrayectoria().getEstudiante()));
            for (var entradaPch : porEstudiantePch.entrySet()) {
                Set<Long> conNotaPch = entradaPch.getValue().stream().filter(r -> r.getNotaFinal() != null)
                        .map(r -> r.getPlanCurso().getId()).collect(Collectors.toSet());
                List<String> faltanPch = cursosPch.stream().filter(pc -> !conNotaPch.contains(pc.getId()))
                        .map(pc -> pc.getCurso().getNombre()).toList();
                if (!faltanPch.isEmpty()) {
                    pendientesPch.add(new CphPendiente(cicloPch.getNumero(), entradaPch.getKey(), List.of("", cphMatricula(entradaPch.getKey()),
                            cphNombreCompleto(entradaPch.getKey()), RacaTituloCiclo.de(cicloPch.getNumero()),
                            conNotaPch.size() + "/" + cursosPch.size(), String.join("; ", faltanPch))));
                }
            }
        }
        pendientesPch.sort(Comparator.comparingInt(CphPendiente::ciclo).thenComparing(p -> cphNombreCompleto(p.e()), collatorPch));

        List<List<String>> filasPch = new ArrayList<>();
        int n = 1;
        for (CphPendiente p : pendientesPch) {
            List<String> celdasPch = new ArrayList<>(p.celdas());
            celdasPch.set(0, String.valueOf(n++));
            filasPch.add(celdasPch);
        }
        long incompletosPch = filasPch.size();
        long sinNingunaPch = 0;
        if (cicloIdPch == null) {
            Set<Long> conRegistrosPch = registrosPch.stream().map(r -> r.getTrayectoria().getId()).collect(Collectors.toSet());
            List<YmclTrayectoria> sinNotasPch = trayectoriaRepositoryPch.findByPlanEstudioIdAndEstado(planIdPch, YmclEstadoTrayectoria.ACTIVA).stream()
                    .filter(t -> !conRegistrosPch.contains(t.getId()))
                    .sorted(Comparator.comparing((YmclTrayectoria t) -> cphNombreCompleto(t.getEstudiante()), collatorPch)).toList();
            for (YmclTrayectoria t : sinNotasPch) {
                filasPch.add(List.of(String.valueOf(n++), cphMatricula(t.getEstudiante()), cphNombreCompleto(t.getEstudiante()), "-", "0/-",
                        "Sin ninguna nota en el periodo"));
            }
            sinNingunaPch = sinNotasPch.size();
        }
        List<String> subtituloPch = new ArrayList<>(List.of("Programa: " + planPch.getPrograma().getNombre(),
                "Plan: " + planPch.getCodigo() + " - " + planPch.getNombre(), "Periodo: " + periodoPch.getNombre()));
        if (cicloIdPch != null && !ciclosPch.isEmpty()) {
            subtituloPch.add("Ciclo: " + RacaTituloCiclo.de(ciclosPch.get(0).getNumero()));
        }
        subtituloPch.add("Con notas incompletas: " + incompletosPch + (cicloIdPch == null ? " | Activos sin ninguna nota: " + sinNingunaPch : ""));
        return new CphTablaReporte("PENDIENTES DE CARGAR", subtituloPch, null,
                List.of("N°", "N° Matricula", "Apellidos y nombres", "Ciclo", "Cursos con nota", "Cursos sin nota"),
                List.of(3, 9, 24, 12, 8, 50), List.of(true, false, false, false, true, false), filasPch,
                List.of("Incluye a quienes ya tienen alguna nota del ciclo pero les faltan cursos y, al ver todos los ciclos, "
                        + "a los estudiantes activos del plan sin ninguna nota en el periodo."), false);
    }

    public enum CphAmbitoMerito {

        CICLO_PERIODO,

        CICLO_VIGENTE,

        PLAN_ACUMULADO
    }

    public CphTablaReporte cphOrdenMerito(Long planIdPch, CphAmbitoMerito ambitoPch, Long cicloIdPch, Long periodoIdPch, Integer limitePuestosPch) {
        sesionActualPch.ryeRequerirAutenticacion();
        RacaPlanEstudio planPch = cphPlan(planIdPch);
        RacaCiclo cicloPch = null;
        YmclPeriodoAcademico periodoPch = null;
        if (ambitoPch != CphAmbitoMerito.PLAN_ACUMULADO) {
            cicloPch = cicloRepositoryPch.findById(cicloIdPch).filter(ci -> ci.getPlanEstudio().getId().equals(planIdPch))
                    .orElseThrow(() -> new RyeEntidadNoEncontradaException("Ciclo no encontrado en el plan: id=" + cicloIdPch));
        }
        if (ambitoPch == CphAmbitoMerito.CICLO_PERIODO) {
            periodoPch = cphPeriodo(periodoIdPch);
        }
        List<RacaPlanCurso> cursosDelCicloPch = cicloPch == null ? List.of() : planCursoRepositoryPch.findByCicloIdOrderByOrdenAsc(cicloPch.getId());
        final Long cicloFiltroPch = cicloPch == null ? null : cicloPch.getId();

        List<YmclRegistroAcademico> registrosPch;
        if (ambitoPch == CphAmbitoMerito.CICLO_PERIODO) {
            registrosPch = registroRepositoryPch.findParaReporte(planIdPch, periodoPch.getId()).stream()
                    .filter(r -> r.getPlanCurso().getCiclo().getId().equals(cicloFiltroPch)).toList();
        } else {

            Map<String, YmclRegistroAcademico> vigentesPch = new HashMap<>();
            for (YmclRegistroAcademico r : registroRepositoryPch.findTodosDelPlan(planIdPch)) {
                if (cicloFiltroPch != null && !r.getPlanCurso().getCiclo().getId().equals(cicloFiltroPch)) {
                    continue;
                }
                vigentesPch.merge(r.getTrayectoria().getId() + "|" + r.getPlanCurso().getId(), r,
                        (a, b) -> a.getId() > b.getId() ? a : b);
            }
            registrosPch = List.copyOf(vigentesPch.values());
        }

        Set<Long> activasPch = trayectoriaRepositoryPch.findByPlanEstudioIdAndEstado(planIdPch, YmclEstadoTrayectoria.ACTIVA).stream()
                .map(YmclTrayectoria::getId).collect(Collectors.toSet());
        Map<YmclEstudiante, List<YmclRegistroAcademico>> porEstudiantePch = registrosPch.stream()
                .filter(r -> activasPch.contains(r.getTrayectoria().getId()))
                .collect(Collectors.groupingBy(r -> r.getTrayectoria().getEstudiante()));

        record CphEntrada(YmclEstudiante e, double promedio, int creditos, String observacion) {
        }
        List<CphEntrada> entradasPch = new ArrayList<>();
        for (var entradaPch : porEstudiantePch.entrySet()) {
            double ptjPch = 0;
            int creditosPch = 0;
            List<String> desaprobadosPch = new ArrayList<>();
            Set<Long> conNotaPch = new java.util.HashSet<>();
            for (YmclRegistroAcademico r : entradaPch.getValue()) {
                Double notaPch = r.getNotaFinal();
                if (notaPch == null) {
                    continue;
                }
                conNotaPch.add(r.getPlanCurso().getId());
                if (r.getSituacion().isCuentaParaPromedio()) {
                    ptjPch += notaPch * r.getPlanCurso().getCurso().getCreditos();
                    creditosPch += r.getPlanCurso().getCurso().getCreditos();
                }
                if (!CphReglasAcademicas.cphAprobada(notaPch)) {
                    desaprobadosPch.add(r.getPlanCurso().getCurso().getNombre() + " (" + num(notaPch) + ")");
                }
            }
            if (creditosPch == 0) {
                continue;
            }
            List<String> observacionesPch = new ArrayList<>();
            if (!desaprobadosPch.isEmpty()) {
                observacionesPch.add((desaprobadosPch.size() == 1 ? "1 curso desaprobado: " : desaprobadosPch.size() + " cursos desaprobados: ")
                        + String.join(", ", desaprobadosPch));
            }
            long faltanPch = cursosDelCicloPch.stream().filter(pc -> !conNotaPch.contains(pc.getId())).count();
            if (faltanPch > 0) {
                observacionesPch.add("Faltan notas de " + faltanPch + (faltanPch == 1 ? " curso" : " cursos"));
            }
            entradasPch.add(new CphEntrada(entradaPch.getKey(), Math.round(ptjPch / creditosPch * 100) / 100.0, creditosPch,
                    String.join(". ", observacionesPch)));
        }
        entradasPch.sort(Comparator.comparingDouble(CphEntrada::promedio).reversed()
                .thenComparing(en -> cphNombreCompleto(en.e()), collatorPch));

        int totalPch = entradasPch.size();
        double umbralTercioPch = totalPch == 0 ? 0 : entradasPch.get((int) Math.ceil(totalPch / 3.0) - 1).promedio();
        double umbralQuintoPch = totalPch == 0 ? 0 : entradasPch.get((int) Math.ceil(totalPch / 5.0) - 1).promedio();
        long enTercioPch = entradasPch.stream().filter(en -> en.promedio() >= umbralTercioPch).count();
        long enQuintoPch = entradasPch.stream().filter(en -> en.promedio() >= umbralQuintoPch).count();

        List<List<String>> filasPch = new ArrayList<>();
        int puestoPch = 0;
        Double anteriorPch = null;
        for (int i = 0; i < entradasPch.size(); i++) {
            CphEntrada en = entradasPch.get(i);
            if (anteriorPch == null || Double.compare(en.promedio(), anteriorPch) != 0) {
                puestoPch = i + 1;
            }
            anteriorPch = en.promedio();
            if (limitePuestosPch != null && puestoPch > limitePuestosPch) {
                break;
            }
            String meritoPch = en.promedio() >= umbralQuintoPch ? "Quinto superior" : (en.promedio() >= umbralTercioPch ? "Tercio superior" : "");
            filasPch.add(List.of(String.valueOf(puestoPch), cphMatricula(en.e()), cphNombreCompleto(en.e()), String.valueOf(en.creditos()),
                    dos(en.promedio()), meritoPch, en.observacion()));
        }

        List<String> subtituloPch = new ArrayList<>(List.of("Programa: " + planPch.getPrograma().getNombre(),
                "Plan: " + planPch.getCodigo() + " - " + planPch.getNombre()));
        subtituloPch.add(switch (ambitoPch) {
            case CICLO_PERIODO -> "Ciclo: " + RacaTituloCiclo.de(cicloPch.getNumero()) + " | Periodo: " + periodoPch.getNombre();
            case CICLO_VIGENTE -> "Ciclo: " + RacaTituloCiclo.de(cicloPch.getNumero()) + " (notas vigentes de cualquier periodo)";
            case PLAN_ACUMULADO -> "General: promedio acumulado de todo el plan (notas vigentes)";
        });
        subtituloPch.add("Emitido: " + LocalDate.now().format(FECHA_PCH));
        subtituloPch.add("Estudiantes en el orden: " + totalPch + (limitePuestosPch == null ? "" : " | Se muestran los primeros " + limitePuestosPch
                + " puestos (" + filasPch.size() + " estudiantes)"));
        if (totalPch > 0) {
            subtituloPch.add("Tercio superior: promedio de " + dos(umbralTercioPch) + " o mas (" + enTercioPch + " estudiantes) | "
                    + "Quinto superior: " + dos(umbralQuintoPch) + " o mas (" + enQuintoPch + ")");
        }
        return new CphTablaReporte("ORDEN DE MERITO", subtituloPch, null,
                List.of("Puesto", "N\u00B0 Matricula", "Apellidos y nombres", "Creditos", "Promedio", "Merito", "Observacion"),
                List.of(7, 10, 26, 8, 9, 13, 36), List.of(true, false, false, true, true, false, false), filasPch,
                List.of("Los estudiantes con el mismo promedio (a 2 decimales) comparten el puesto; no se desempata por nombre. "
                        + "Tercio y quinto superior: el primer 1/3 y 1/5 de los estudiantes del orden, redondeado hacia arriba e "
                        + "incluyendo a los empatados en el corte. La observacion avisa de cursos desaprobados o notas por cargar; "
                        + "esos estudiantes igual entran al orden."), true);
    }

    public CphTablaReporte cphCuadroDeHonor(Long periodoIdPch, int puestosPch) {
        sesionActualPch.ryeRequerirAutenticacion();
        if (puestosPch < 1) {
            throw new RyeValidacionException("El numero de lugares debe ser al menos 1.");
        }
        YmclPeriodoAcademico periodoPch = cphPeriodo(periodoIdPch);

        record Entrada(YmclEstudiante e, double promedio, int creditos, RacaCiclo ciclo, String observacion) {
        }
        List<List<String>> filasPch = new ArrayList<>();
        Set<Integer> seccionesPch = new java.util.HashSet<>();
        int especialidadesConNotasPch = 0;
        int reconocidosPch = 0;
        for (RacaProgramaEstudio programaPch : programaRepositoryPch.findAll().stream()
                .sorted(Comparator.comparing(RacaProgramaEstudio::getNombre, collatorPch)).toList()) {
            Map<Long, List<YmclRegistroAcademico>> porEstudiantePch = new LinkedHashMap<>();
            for (RacaPlanEstudio planPch : planRepositoryPch.findByProgramaId(programaPch.getId())) {
                Set<Long> activasPch = trayectoriaRepositoryPch.findByPlanEstudioIdAndEstado(planPch.getId(), YmclEstadoTrayectoria.ACTIVA)
                        .stream().map(YmclTrayectoria::getId).collect(Collectors.toSet());
                for (YmclRegistroAcademico r : registroRepositoryPch.findParaReporte(planPch.getId(), periodoIdPch)) {
                    if (activasPch.contains(r.getTrayectoria().getId())) {
                        porEstudiantePch.computeIfAbsent(r.getTrayectoria().getEstudiante().getId(), k -> new ArrayList<>()).add(r);
                    }
                }
            }
            List<Entrada> entradasPch = new ArrayList<>();
            for (List<YmclRegistroAcademico> registrosPch : porEstudiantePch.values()) {
                double ptjPch = 0;
                int creditosPch = 0;
                List<String> desaprobadosPch = new ArrayList<>();
                Set<Long> conNotaPch = new java.util.HashSet<>();
                RacaCiclo cicloActualPch = null;
                for (YmclRegistroAcademico r : registrosPch) {
                    RacaCiclo cicloPch = r.getPlanCurso().getCiclo();
                    if (cicloActualPch == null || cicloPch.getNumero() > cicloActualPch.getNumero()) {
                        cicloActualPch = cicloPch;
                    }
                    Double notaPch = r.getNotaFinal();
                    if (notaPch == null) {
                        continue;
                    }
                    conNotaPch.add(r.getPlanCurso().getId());
                    if (r.getSituacion().isCuentaParaPromedio()) {
                        ptjPch += notaPch * r.getPlanCurso().getCurso().getCreditos();
                        creditosPch += r.getPlanCurso().getCurso().getCreditos();
                    }
                    if (!CphReglasAcademicas.cphAprobada(notaPch)) {
                        desaprobadosPch.add(r.getPlanCurso().getCurso().getNombre() + " (" + num(notaPch) + ")");
                    }
                }
                if (creditosPch == 0 || cicloActualPch == null) {
                    continue;
                }
                List<String> observacionesPch = new ArrayList<>();
                if (!desaprobadosPch.isEmpty()) {
                    observacionesPch.add((desaprobadosPch.size() == 1 ? "1 curso desaprobado: " : desaprobadosPch.size() + " cursos desaprobados: ")
                            + String.join(", ", desaprobadosPch));
                }
                long faltanPch = planCursoRepositoryPch.findByCicloIdOrderByOrdenAsc(cicloActualPch.getId()).stream()
                        .filter(pc -> !conNotaPch.contains(pc.getId())).count();
                if (faltanPch > 0) {
                    observacionesPch.add("Faltan notas de " + faltanPch + (faltanPch == 1 ? " curso" : " cursos"));
                }
                entradasPch.add(new Entrada(registrosPch.get(0).getTrayectoria().getEstudiante(),
                        Math.round(ptjPch / creditosPch * 100) / 100.0, creditosPch, cicloActualPch, String.join(". ", observacionesPch)));
            }
            entradasPch.sort(Comparator.comparingDouble(Entrada::promedio).reversed()
                    .thenComparing(en -> cphNombreCompleto(en.e()), collatorPch));

            seccionesPch.add(filasPch.size());
            filasPch.add(List.of("", "", programaPch.getNombre().toUpperCase(Locale.ROOT) + "  (" + entradasPch.size()
                    + (entradasPch.size() == 1 ? " estudiante con notas)" : " estudiantes con notas)"), "", "", "", ""));
            if (entradasPch.isEmpty()) {
                filasPch.add(List.of("", "", "Sin notas cargadas en este periodo", "", "", "", ""));
                continue;
            }
            especialidadesConNotasPch++;
            int puestoPch = 0;
            Double anteriorPch = null;
            for (int i = 0; i < entradasPch.size(); i++) {
                Entrada en = entradasPch.get(i);
                if (anteriorPch == null || Double.compare(en.promedio(), anteriorPch) != 0) {
                    puestoPch = i + 1;
                }
                anteriorPch = en.promedio();
                if (puestoPch > puestosPch) {
                    break;
                }
                reconocidosPch++;
                filasPch.add(List.of(lugar(puestoPch), cphMatricula(en.e()), cphNombreCompleto(en.e()), RacaTituloCiclo.de(en.ciclo().getNumero()),
                        String.valueOf(en.creditos()), dos(en.promedio()), en.observacion()));
            }
        }

        List<String> subtituloPch = new ArrayList<>(List.of("Periodo: " + periodoPch.getNombre(),
                "Emitido: " + LocalDate.now().format(FECHA_PCH),
                "Se muestran los primeros " + puestosPch + " lugares de cada especialidad | Especialidades con notas: "
                        + especialidadesConNotasPch + " | Estudiantes reconocidos: " + reconocidosPch));
        return new CphTablaReporte("CUADRO DE HONOR POR ESPECIALIDAD", subtituloPch, null,
                List.of("Lugar", "N° Matricula", "Apellidos y nombres", "Ciclo", "Creditos", "Promedio", "Observacion"),
                List.of(12, 10, 30, 12, 7, 8, 34), List.of(false, false, false, false, true, true, false), filasPch,
                List.of("El promedio es el ponderado por creditos de las notas del periodo; en cada especialidad compiten juntos "
                        + "todos los ciclos. Los estudiantes con el mismo promedio (a 2 decimales) comparten el lugar. Solo entran "
                        + "estudiantes con trayectoria activa. La observacion avisa de cursos desaprobados o notas por cargar; "
                        + "esos estudiantes igual entran al orden."), true, seccionesPch);
    }

    private static String lugar(int puestoPch) {
        return switch (puestoPch) {
            case 1 -> "Primer lugar";
            case 2 -> "Segundo lugar";
            case 3 -> "Tercer lugar";
            default -> puestoPch + "° lugar";
        };
    }

    private record CphContexto(RacaPlanEstudio plan, RacaCiclo ciclo, YmclPeriodoAcademico periodo, List<RacaPlanCurso> cursos,
                            List<YmclRegistroAcademico> registros) {
    }

    private CphContexto cphContexto(Long planIdPch, Long cicloIdPch, Long periodoIdPch) {
        sesionActualPch.ryeRequerirAutenticacion();
        RacaPlanEstudio planPch = cphPlan(planIdPch);
        RacaCiclo cicloPch = cicloRepositoryPch.findById(cicloIdPch)
                .filter(ci -> ci.getPlanEstudio().getId().equals(planIdPch))
                .orElseThrow(() -> new RyeEntidadNoEncontradaException("Ciclo no encontrado en el plan: id=" + cicloIdPch));
        YmclPeriodoAcademico periodoPch = cphPeriodo(periodoIdPch);
        List<RacaPlanCurso> cursosPch = planCursoRepositoryPch.findByCicloIdOrderByOrdenAsc(cicloIdPch);
        List<YmclRegistroAcademico> registrosPch = registroRepositoryPch.findParaReporte(planIdPch, periodoIdPch).stream()
                .filter(r -> r.getPlanCurso().getCiclo().getId().equals(cicloIdPch)).toList();
        return new CphContexto(planPch, cicloPch, periodoPch, cursosPch, registrosPch);
    }

    private RacaPlanEstudio cphPlan(Long planIdPch) {
        return planRepositoryPch.findById(planIdPch)
                .orElseThrow(() -> new RyeEntidadNoEncontradaException("Plan de estudio no encontrado: id=" + planIdPch));
    }

    private YmclPeriodoAcademico cphPeriodo(Long periodoIdPch) {
        return periodoRepositoryPch.findById(periodoIdPch)
                .orElseThrow(() -> new RyeEntidadNoEncontradaException("Periodo no encontrado: id=" + periodoIdPch));
    }

    private List<String> cphEncabezado(CphContexto c) {
        return new ArrayList<>(List.of("Programa: " + c.plan().getPrograma().getNombre(),
                "Plan: " + c.plan().getCodigo() + " - " + c.plan().getNombre(),
                "Ciclo: " + RacaTituloCiclo.de(c.ciclo().getNumero()), "Periodo: " + c.periodo().getNombre(),
                "Emitido: " + LocalDate.now().format(FECHA_PCH)));
    }

    private Map<YmclEstudiante, Map<Long, YmclRegistroAcademico>> cphNotasPorEstudiante(CphContexto c) {
        Map<YmclEstudiante, Map<Long, YmclRegistroAcademico>> mapaPch = new HashMap<>();
        for (YmclRegistroAcademico r : c.registros()) {
            mapaPch.computeIfAbsent(r.getTrayectoria().getEstudiante(), k -> new HashMap<>()).put(r.getPlanCurso().getId(), r);
        }
        return mapaPch;
    }

    private List<Map.Entry<YmclEstudiante, Map<Long, YmclRegistroAcademico>>> cphEstudiantesOrdenados(
            Map<YmclEstudiante, Map<Long, YmclRegistroAcademico>> mapaPch) {
        return mapaPch.entrySet().stream()
                .sorted(Comparator.comparing((Map.Entry<YmclEstudiante, Map<Long, YmclRegistroAcademico>> e) -> cphNombreCompleto(e.getKey()), collatorPch))
                .toList();
    }

    private static String cphNombreCompleto(YmclEstudiante e) {
        return nz(e.getApellidoPaterno()) + (e.getApellidoMaterno() == null || e.getApellidoMaterno().isBlank() ? ""
                : " " + e.getApellidoMaterno()) + ", " + nz(e.getNombres());
    }

    private static String cphMatricula(YmclEstudiante e) {
        return e.getDni() != null && !e.getDni().isBlank() ? e.getDni() : nz(e.getCodigo());
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    static String cphLetra(String codigoPch, String nombrePch) {
        return switch (codigoPch) {
            case YmclEscalaLiteral.DESTACADO -> "D";
            case YmclEscalaLiteral.LOGRADO -> "L";
            case YmclEscalaLiteral.PROCESO -> "E";
            case YmclEscalaLiteral.INICIO -> "I";
            case YmclEscalaLiteral.PREVIO_INICIO -> "P";
            case "RETIRADO" -> "T";
            default -> nombrePch == null || nombrePch.isBlank() ? "" : nombrePch.substring(0, 1).toUpperCase(Locale.ROOT);
        };
    }

    private static String num(double v) {
        return Math.rint(v) == v ? String.valueOf((long) v) : String.format(Locale.ROOT, "%.1f", v);
    }

    private static String dos(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }

    private static String cphPorcentaje(double partePch, double totalPch) {
        return String.format(Locale.ROOT, "%.0f%%", 100.0 * partePch / totalPch);
    }
}
