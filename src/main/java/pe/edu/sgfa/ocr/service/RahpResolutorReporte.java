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

import pe.edu.sgfa.common.exception.RyeValidacionException;
import pe.edu.sgfa.estudiante.domain.YmclEstudiante;
import pe.edu.sgfa.estudiante.repository.YmclEstudianteRepository;
import pe.edu.sgfa.estudiante.service.YmclEstudianteService;
import pe.edu.sgfa.nota.domain.YmclSituacionAcademica;
import pe.edu.sgfa.nota.service.YmclEscalaLiteral;
import pe.edu.sgfa.nota.service.YmclItemNota;
import pe.edu.sgfa.nota.service.YmclRegistroAcademicoService;
import pe.edu.sgfa.nota.service.YmclResultadoPromedio;
import pe.edu.sgfa.ocr.service.RahpReconocedorReporteCalificaciones.RahpCursoDetectado;
import pe.edu.sgfa.ocr.service.RahpReconocedorReporteCalificaciones.RahpReporteDetectado;
import pe.edu.sgfa.periodo.domain.YmclPeriodoAcademico;
import pe.edu.sgfa.periodo.service.YmclPeriodoAcademicoService;
import pe.edu.sgfa.programa.domain.RacaCiclo;
import pe.edu.sgfa.programa.domain.RacaEstadoPlanEstudio;
import pe.edu.sgfa.programa.domain.RacaPlanCurso;
import pe.edu.sgfa.programa.domain.RacaPlanEstudio;
import pe.edu.sgfa.programa.domain.RacaProgramaEstudio;
import pe.edu.sgfa.programa.repository.RacaPlanCursoRepository;
import pe.edu.sgfa.programa.service.RacaCicloService;
import pe.edu.sgfa.programa.service.RacaPlanEstudioService;
import pe.edu.sgfa.programa.service.RacaProgramaEstudioService;
import pe.edu.sgfa.trayectoria.domain.YmclTrayectoria;
import pe.edu.sgfa.trayectoria.service.YmclTrayectoriaService;

@Service
public class RahpResolutorReporte {

    private static final double TOLERANCIA_PROMEDIO_HNC = 0.015;

    private final RacaProgramaEstudioService programaEstudioServiceHnc;
    private final RacaPlanEstudioService planEstudioServiceHnc;
    private final RacaCicloService cicloServiceHnc;
    private final YmclPeriodoAcademicoService periodoAcademicoServiceHnc;
    private final RacaPlanCursoRepository planCursoRepositoryHnc;
    private final YmclRegistroAcademicoService registroAcademicoServiceHnc;
    private final YmclEstudianteService estudianteServiceHnc;
    private final YmclEstudianteRepository estudianteRepositoryHnc;
    private final YmclTrayectoriaService trayectoriaServiceHnc;

    public RahpResolutorReporte(RacaProgramaEstudioService programaEstudioServiceHnc, RacaPlanEstudioService planEstudioServiceHnc,
                             RacaCicloService cicloServiceHnc, YmclPeriodoAcademicoService periodoAcademicoServiceHnc,
                             RacaPlanCursoRepository planCursoRepositoryHnc, YmclRegistroAcademicoService registroAcademicoServiceHnc,
                             YmclEstudianteService estudianteServiceHnc, YmclEstudianteRepository estudianteRepositoryHnc,
                             YmclTrayectoriaService trayectoriaServiceHnc) {
        this.programaEstudioServiceHnc = programaEstudioServiceHnc;
        this.planEstudioServiceHnc = planEstudioServiceHnc;
        this.cicloServiceHnc = cicloServiceHnc;
        this.periodoAcademicoServiceHnc = periodoAcademicoServiceHnc;
        this.planCursoRepositoryHnc = planCursoRepositoryHnc;
        this.registroAcademicoServiceHnc = registroAcademicoServiceHnc;
        this.estudianteServiceHnc = estudianteServiceHnc;
        this.estudianteRepositoryHnc = estudianteRepositoryHnc;
        this.trayectoriaServiceHnc = trayectoriaServiceHnc;
    }

    public enum RahpEstado {

        LISTO,

        REVISAR
    }

    public record RahpCursoPropuesto(String nombreDetectado, Double nota, String situacionCodigo, RacaPlanCurso planCurso) {
    }

    public record RahpPropuesta(String matricula, String apellidoPaterno, String apellidoMaterno, String nombres,
                             YmclEstudiante estudiante, RacaProgramaEstudio programa, RacaPlanEstudio plan, RacaCiclo ciclo,
                             YmclPeriodoAcademico periodo, List<RahpCursoPropuesto> cursos, Double promedioImpreso,
                             Double promedioCalculado, List<String> avisos, List<String> notas) {

        public RahpEstado rahpEstado() {
            return avisos.isEmpty() ? RahpEstado.LISTO : RahpEstado.REVISAR;
        }

        public boolean rahpEsNuevo() {
            return estudiante == null;
        }

        public long rahpCursosEmparejados() {
            return cursos.stream().filter(c -> c.planCurso() != null).count();
        }

        RahpPropuesta rahpConAviso(String avisoHnc) {
            List<String> nuevosHnc = new ArrayList<>(avisos);
            nuevosHnc.add(avisoHnc);
            return new RahpPropuesta(matricula, apellidoPaterno, apellidoMaterno, nombres, estudiante, programa, plan, ciclo,
                    periodo, cursos, promedioImpreso, promedioCalculado, List.copyOf(nuevosHnc), notas);
        }
    }

    public record RahpResultadoGuardado(YmclEstudiante estudiante, boolean estudianteNuevo, int notasGuardadas,
                                     Double promedioCalculado) {
    }

    public RahpPropuesta rahpProponer(RahpReporteDetectado r) {
        return rahpProponer(r, null);
    }

    private RahpPropuesta rahpProponer(RahpReporteDetectado r, YmclPeriodoAcademico periodoPorDefectoHnc) {
        List<String> avisosHnc = new ArrayList<>(r.advertencias());
        List<String> notasHnc = new ArrayList<>();

        Optional<YmclEstudiante> existenteHnc = rahpBuscarEstudiante(r.matricula());
        String paternoHnc = r.apellidoPaterno();
        String maternoHnc = r.apellidoMaterno();
        String nombresHnc = r.nombres();
        if (existenteHnc.isPresent()) {
            YmclEstudiante e = existenteHnc.get();

            if (paternoHnc != null && RahpEmparejadorNombres.rahpSimilitud(paternoHnc, e.getApellidoPaterno()) < 0.6) {
                avisosHnc.add("La matricula " + r.matricula() + " es de " + e.getApellidoPaterno() + " "
                        + rahpVacioSiNulo(e.getApellidoMaterno()) + ", pero el reporte dice " + paternoHnc + ": revise la matricula.");
            }
            paternoHnc = e.getApellidoPaterno();
            maternoHnc = e.getApellidoMaterno();
            nombresHnc = e.getNombres();
        } else if (r.matricula() != null) {
            if (paternoHnc == null || paternoHnc.isBlank() || nombresHnc == null || nombresHnc.isBlank()) {
                avisosHnc.add("Estudiante nuevo: faltan el apellido paterno o los nombres.");
            }
        }

        RacaProgramaEstudio programaHnc = null;
        RacaPlanEstudio planHnc = null;
        Optional<YmclTrayectoria> trayectoriaHnc = existenteHnc.flatMap(e -> trayectoriaServiceHnc.ymclObtenerActiva(e.getId())
                .or(() -> trayectoriaServiceHnc.ymclListarPorEstudiante(e.getId()).stream().findFirst()));
        if (trayectoriaHnc.isPresent()) {
            programaHnc = trayectoriaHnc.get().getPrograma();
            planHnc = trayectoriaHnc.get().getPlanEstudio();
        } else {
            programaHnc = rahpProgramaPorTexto(r.textoCompleto());
            if (programaHnc == null) {
                avisosHnc.add("No se identifico el programa de estudios.");
            } else {
                List<RacaPlanEstudio> activosHnc = planEstudioServiceHnc.racaListarPorPrograma(programaHnc.getId()).stream()
                        .filter(p -> p.getEstado() == RacaEstadoPlanEstudio.ACTIVO).toList();
                if (activosHnc.size() == 1) {
                    planHnc = activosHnc.get(0);
                } else {
                    avisosHnc.add("El programa " + programaHnc.getNombre() + " tiene "
                            + (activosHnc.isEmpty() ? "ningun plan activo" : "varios planes activos") + ": elija el plan.");
                }
            }
        }

        RacaCiclo cicloHnc = null;
        if (planHnc != null) {
            List<RacaCiclo> ciclosDelPlanHnc = cicloServiceHnc.racaListarPorPlan(planHnc.getId());
            if (r.ciclo() != null) {
                cicloHnc = ciclosDelPlanHnc.stream().filter(c -> c.getNumero() == r.ciclo()).findFirst().orElse(null);
                if (cicloHnc == null) {
                    avisosHnc.add("El plan " + planHnc.getCodigo() + " no tiene el ciclo " + r.ciclo() + ".");
                }
            } else {

                cicloHnc = rahpInferirCiclo(ciclosDelPlanHnc, r.cursos());
                if (cicloHnc != null) {
                    avisosHnc.remove("No se reconocio el ciclo.");
                    notasHnc.add("El ciclo no se leyo del encabezado: se dedujo de los cursos (" + cicloHnc.getNombre() + ").");
                }
            }
        }

        YmclPeriodoAcademico periodoHnc = null;
        if (r.periodo() != null) {
            periodoHnc = periodoAcademicoServiceHnc.ymclListar().stream()
                    .filter(p -> p.getNombre().replaceAll("\\s", "").equalsIgnoreCase(r.periodo())).findFirst().orElse(null);
            if (periodoHnc == null) {
                avisosHnc.add("El periodo " + r.periodo() + " no existe en Catalogos.");
            }
        } else if (periodoPorDefectoHnc != null) {
            periodoHnc = periodoPorDefectoHnc;
            avisosHnc.remove("No se reconocio el periodo academico.");
            notasHnc.add("El periodo no se leyo de la hoja: se uso el del resto del lote (" + periodoHnc.getNombre() + ").");
        }

        List<RahpCursoPropuesto> cursosHnc = rahpEmparejarCursos(r.cursos(), cicloHnc, avisosHnc);
        Double calculadoHnc = rahpPromedioDe(cursosHnc);
        if (r.promedioReporte() != null && calculadoHnc != null
                && Math.abs(r.promedioReporte() - calculadoHnc) > TOLERANCIA_PROMEDIO_HNC) {
            avisosHnc.add(String.format("El promedio impreso (%.2f) no coincide con el calculado (%.2f): probablemente "
                    + "una nota o un curso se leyo mal.", r.promedioReporte(), calculadoHnc));
        }
        if (r.promedioReporte() == null && !cursosHnc.isEmpty()) {
            avisosHnc.add("No se leyo el promedio impreso: no hay con que verificar las notas.");
        }
        if (r.matricula() != null && r.matricula().length() == 7 && existenteHnc.isEmpty()) {
            avisosHnc.add("La matricula leida tiene 7 digitos (" + r.matricula() + "): si se perdio un digito, corrijala.");
        }
        if (cursosHnc.isEmpty() && r.advertencias().stream().noneMatch(a -> a.contains("tabla"))) {
            avisosHnc.add("No se leyo ningun curso.");
        }

        return new RahpPropuesta(r.matricula(), paternoHnc, maternoHnc, nombresHnc, existenteHnc.orElse(null), programaHnc, planHnc, cicloHnc,
                periodoHnc, cursosHnc, r.promedioReporte(), calculadoHnc, List.copyOf(avisosHnc), List.copyOf(notasHnc));
    }

    private RacaCiclo rahpInferirCiclo(List<RacaCiclo> ciclosHnc, List<RahpCursoDetectado> detectadosHnc) {
        if (detectadosHnc.isEmpty()) {
            return null;
        }
        RacaCiclo mejorHnc = null;
        long mejorPuntajeHnc = 0;
        long segundoHnc = 0;
        for (RacaCiclo cicloHnc : ciclosHnc) {
            List<RacaPlanCurso> delCicloHnc = planCursoRepositoryHnc.findByCicloIdOrderByOrdenAsc(cicloHnc.getId());
            long puntajeHnc = detectadosHnc.stream()
                    .filter(c -> RahpEmparejadorNombres.rahpMejorCoincidencia(c.nombre(), delCicloHnc, pc -> pc.getCurso().getNombre())
                            .isPresent()).count();
            if (puntajeHnc > mejorPuntajeHnc) {
                segundoHnc = mejorPuntajeHnc;
                mejorPuntajeHnc = puntajeHnc;
                mejorHnc = cicloHnc;
            } else if (puntajeHnc > segundoHnc) {
                segundoHnc = puntajeHnc;
            }
        }
        boolean convincenteHnc = mejorPuntajeHnc >= Math.max(2, (detectadosHnc.size() + 1) / 2) && mejorPuntajeHnc > segundoHnc;
        return convincenteHnc ? mejorHnc : null;
    }

    public List<RahpPropuesta> rahpProponerTodos(List<RahpReporteDetectado> reportesHnc) {
        List<RahpPropuesta> propuestasHnc = new ArrayList<>(reportesHnc.stream().map(this::rahpProponer).toList());

        Map<Long, Long> frecuenciaHnc = propuestasHnc.stream().map(RahpPropuesta::periodo).filter(java.util.Objects::nonNull)
                .collect(Collectors.groupingBy(YmclPeriodoAcademico::getId, Collectors.counting()));
        var comunHnc = frecuenciaHnc.entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);
        if (comunHnc != null && comunHnc.getValue() >= 2 && comunHnc.getValue() * 10 >= propuestasHnc.size() * 6L) {
            YmclPeriodoAcademico periodoComunHnc = propuestasHnc.stream().map(RahpPropuesta::periodo).filter(java.util.Objects::nonNull)
                    .filter(p -> p.getId().equals(comunHnc.getKey())).findFirst().orElseThrow();
            for (int i = 0; i < propuestasHnc.size(); i++) {
                if (propuestasHnc.get(i).periodo() == null && reportesHnc.get(i).periodo() == null) {
                    propuestasHnc.set(i, rahpProponer(reportesHnc.get(i), periodoComunHnc));
                }
            }
        }
        Map<String, List<Integer>> porClaveHnc = new HashMap<>();
        for (int i = 0; i < propuestasHnc.size(); i++) {
            RahpPropuesta p = propuestasHnc.get(i);
            if (p.matricula() != null && p.ciclo() != null && p.periodo() != null) {
                porClaveHnc.computeIfAbsent(p.matricula() + "|" + p.ciclo().getId() + "|" + p.periodo().getId(),
                        k -> new ArrayList<>()).add(i);
            }
        }
        for (List<Integer> indicesHnc : porClaveHnc.values()) {
            if (indicesHnc.size() > 1) {
                for (int i : indicesHnc) {
                    propuestasHnc.set(i, propuestasHnc.get(i).rahpConAviso("Otra hoja del lote trae el mismo estudiante, ciclo y periodo."));
                }
            }
        }
        return propuestasHnc;
    }

    private List<RahpCursoPropuesto> rahpEmparejarCursos(List<RahpCursoDetectado> detectadosHnc, RacaCiclo cicloHnc, List<String> avisosHnc) {
        List<RacaPlanCurso> delCicloHnc = cicloHnc == null ? List.of() : planCursoRepositoryHnc.findByCicloIdOrderByOrdenAsc(cicloHnc.getId());
        Set<Long> usadosHnc = new HashSet<>();
        List<RahpCursoPropuesto> resultadoHnc = new ArrayList<>();
        for (RahpCursoDetectado c : detectadosHnc) {
            RacaPlanCurso planCursoHnc = delCicloHnc.isEmpty() ? null
                    : RahpEmparejadorNombres.rahpMejorCoincidencia(c.nombre(), delCicloHnc, pc -> pc.getCurso().getNombre()).orElse(null);
            if (planCursoHnc != null && !usadosHnc.add(planCursoHnc.getId())) {
                avisosHnc.add("\"" + c.nombre() + "\" se emparejo con un curso que ya tiene otra fila.");
                planCursoHnc = null;
            } else if (planCursoHnc == null && cicloHnc != null) {
                avisosHnc.add("\"" + c.nombre() + "\": no se identifico el curso del plan.");
            }
            if (c.nota() == null) {
                avisosHnc.add("\"" + c.nombre() + "\": sin nota.");
            } else if (c.nota() < 0 || c.nota() > 20) {
                avisosHnc.add("\"" + c.nombre() + "\": nota fuera de 0-20 (" + c.nota() + ").");
            } else if (c.situacion() != null && rahpContradice(c.situacion(), YmclEscalaLiteral.ymclCodigoPara(c.nota()))) {
                avisosHnc.add("\"" + c.nombre() + "\": el reporte dice " + c.situacion() + " pero la nota " + c.nota()
                        + " corresponde a " + YmclEscalaLiteral.ymclCodigoPara(c.nota()) + ".");
            }
            String situacionHnc = c.situacion() != null ? c.situacion()
                    : (c.nota() == null ? null : YmclEscalaLiteral.ymclCodigoPara(c.nota()));
            resultadoHnc.add(new RahpCursoPropuesto(c.nombre(), c.nota(), situacionHnc, planCursoHnc));
        }
        return resultadoHnc;
    }

    private static final List<String> NIVELES_HNC = List.of(YmclEscalaLiteral.PREVIO_INICIO, YmclEscalaLiteral.INICIO,
            YmclEscalaLiteral.PROCESO, YmclEscalaLiteral.LOGRADO, YmclEscalaLiteral.DESTACADO);

    private static boolean rahpContradice(String leidaHnc, String segunNotaHnc) {
        int a = NIVELES_HNC.indexOf(leidaHnc);
        int b = NIVELES_HNC.indexOf(segunNotaHnc);
        return a >= 0 && b >= 0 && Math.abs(a - b) > 1;
    }

    private static Double rahpPromedioDe(List<RahpCursoPropuesto> cursosHnc) {
        double sumaHnc = 0;
        int creditosHnc = 0;
        for (RahpCursoPropuesto c : cursosHnc) {
            if (c.planCurso() != null && c.nota() != null) {
                int cr = c.planCurso().getCurso().getCreditos();
                sumaHnc += c.nota() * cr;
                creditosHnc += cr;
            }
        }
        return creditosHnc == 0 ? null : sumaHnc / creditosHnc;
    }

    private RacaProgramaEstudio rahpProgramaPorTexto(String textoHnc) {
        RacaProgramaEstudio mejorHnc = null;
        double mejorPuntajeHnc = 0;
        for (RacaProgramaEstudio programaHnc : programaEstudioServiceHnc.racaListar()) {
            double puntajeHnc = RahpEmparejadorNombres.rahpCobertura(programaHnc.getNombre(), textoHnc);
            if (puntajeHnc > mejorPuntajeHnc) {
                mejorPuntajeHnc = puntajeHnc;
                mejorHnc = programaHnc;
            }
        }
        return mejorPuntajeHnc >= 0.8 ? mejorHnc : null;
    }

    private Optional<YmclEstudiante> rahpBuscarEstudiante(String matriculaHnc) {
        if (matriculaHnc == null || matriculaHnc.isBlank()) {
            return Optional.empty();
        }
        return estudianteRepositoryHnc.findByDni(matriculaHnc.trim()).or(() -> estudianteRepositoryHnc.findByCodigo(matriculaHnc.trim()));
    }

    public RahpResultadoGuardado rahpGuardar(RahpPropuesta p) {
        if (p.matricula() == null || p.matricula().isBlank() || p.programa() == null || p.plan() == null
                || p.ciclo() == null || p.periodo() == null) {
            throw new RyeValidacionException("Faltan datos para guardar (matricula, programa, plan, ciclo o periodo).");
        }
        Map<String, YmclSituacionAcademica> situacionesHnc = registroAcademicoServiceHnc.ymclListarSituaciones().stream()
                .collect(Collectors.toMap(YmclSituacionAcademica::getCodigo, Function.identity(), (a, b) -> a));
        List<YmclItemNota> itemsHnc = new ArrayList<>();
        for (RahpCursoPropuesto c : p.cursos()) {
            if (c.planCurso() == null || c.nota() == null) {
                continue;
            }
            YmclSituacionAcademica situacionHnc = situacionesHnc.get(c.situacionCodigo() != null ? c.situacionCodigo()
                    : YmclEscalaLiteral.ymclCodigoPara(c.nota()));
            if (situacionHnc == null) {
                throw new RyeValidacionException("Falta la escala literal en la base de datos (situaciones DESTACADO/LOGRADO/...).");
            }
            itemsHnc.add(new YmclItemNota(c.planCurso().getId(), c.nota(), situacionHnc.getId(), null));
        }
        if (itemsHnc.isEmpty()) {
            throw new RyeValidacionException("No hay ninguna nota emparejada con un curso del plan.");
        }

        boolean nuevoHnc = p.estudiante() == null && rahpBuscarEstudiante(p.matricula()).isEmpty();
        YmclEstudiante estudianteHnc = p.estudiante() != null ? p.estudiante()
                : rahpBuscarEstudiante(p.matricula()).orElseGet(() -> rahpCrearEstudiante(p));
        YmclTrayectoria trayectoriaHnc = trayectoriaServiceHnc.ymclListarPorEstudiante(estudianteHnc.getId()).stream()
                .filter(t -> t.getPlanEstudio().getId().equals(p.plan().getId()))
                .findFirst()
                .orElseGet(() -> trayectoriaServiceHnc.ymclIniciar(estudianteHnc.getId(), p.programa().getId(), p.plan().getId(),
                        null, "Importado desde Reporte de calificaciones (OCR)"));
        registroAcademicoServiceHnc.ymclGuardarMasivo(trayectoriaHnc.getId(), p.periodo().getId(), itemsHnc);

        YmclResultadoPromedio promedioHnc = registroAcademicoServiceHnc.ymclCalcularPromedio(trayectoriaHnc.getId(), p.ciclo().getId(),
                p.periodo().getId());
        return new RahpResultadoGuardado(estudianteHnc, nuevoHnc, itemsHnc.size(), promedioHnc.promedio());
    }

    private YmclEstudiante rahpCrearEstudiante(RahpPropuesta p) {
        if (p.apellidoPaterno() == null || p.apellidoPaterno().isBlank() || p.nombres() == null || p.nombres().isBlank()) {
            throw new RyeValidacionException("Estudiante nuevo: faltan el apellido paterno o los nombres.");
        }
        String maternoHnc = p.apellidoMaterno() == null || p.apellidoMaterno().isBlank() ? null : p.apellidoMaterno().trim();
        return estudianteServiceHnc.ymclCrear(p.matricula().trim(), p.matricula().trim(), p.nombres().trim(),
                p.apellidoPaterno().trim(), maternoHnc, null, null, null, null, null);
    }

    private static String rahpVacioSiNulo(String textoHnc) {
        return textoHnc == null ? "" : textoHnc;
    }
}
