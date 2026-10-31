package pe.edu.sgfa.seed;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import pe.edu.sgfa.RyeSgfaApplication;
import pe.edu.sgfa.institucion.service.RacaInstitucionService;
import pe.edu.sgfa.programa.domain.RacaCiclo;
import pe.edu.sgfa.programa.domain.RacaCurso;
import pe.edu.sgfa.programa.domain.RacaPlanEstudio;
import pe.edu.sgfa.programa.domain.RacaProgramaEstudio;
import pe.edu.sgfa.programa.repository.RacaCicloRepository;
import pe.edu.sgfa.programa.repository.RacaCursoRepository;
import pe.edu.sgfa.programa.repository.RacaPlanCursoRepository;
import pe.edu.sgfa.programa.repository.RacaPlanEstudioRepository;
import pe.edu.sgfa.programa.repository.RacaProgramaEstudioRepository;
import pe.edu.sgfa.programa.service.RacaCicloService;
import pe.edu.sgfa.programa.service.RacaCursoService;
import pe.edu.sgfa.programa.service.RacaPlanCursoService;
import pe.edu.sgfa.programa.service.RacaPlanEstudioService;
import pe.edu.sgfa.programa.service.RacaProgramaEstudioService;
import pe.edu.sgfa.usuario.domain.RyeRolUsuario;
import pe.edu.sgfa.usuario.domain.RyeUsuario;
import pe.edu.sgfa.usuario.repository.RyeUsuarioRepository;
import pe.edu.sgfa.usuario.service.RyeSesionActual;

public class RyeSeedInicial {

    private static final String[] ROMANOS_YNQ = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    public static void main(String[] argsYnq) {
        ConfigurableApplicationContext ctxYnq = new SpringApplicationBuilder(RyeSgfaApplication.class)
                .web(WebApplicationType.NONE)
                .run(argsYnq);
        try {
            ryeEjecutar(ctxYnq);
        } finally {
            ctxYnq.close();
        }
    }

    private static void ryeEjecutar(ConfigurableApplicationContext ctxYnq) {
        RyeUsuarioRepository usuarioRepositoryYnq = ctxYnq.getBean(RyeUsuarioRepository.class);
        RyeSesionActual sesionActualYnq = ctxYnq.getBean(RyeSesionActual.class);
        RyeUsuario adminYnq = usuarioRepositoryYnq.findAll().stream()
                .filter(u -> u.getRol() == RyeRolUsuario.ADMIN)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No existe un usuario ADMIN en la base de datos."));
        sesionActualYnq.ryeIniciar(adminYnq);
        System.out.println("Autenticado como: " + adminYnq.getUsername());

        RacaInstitucionService institucionServiceYnq = ctxYnq.getBean(RacaInstitucionService.class);
        if (institucionServiceYnq.racaObtenerPrincipal().isEmpty()) {
            institucionServiceYnq.racaGuardar("Escuela de Educacion Superior Pedagogica Publica \"Juliaca\"",
                    "EESPP Juliaca", null, null, null);
            System.out.println("Institucion creada.");
        } else {
            System.out.println("Institucion ya existia, no se modifica.");
        }

        RacaProgramaEstudioRepository programaEstudioRepositoryYnq = ctxYnq.getBean(RacaProgramaEstudioRepository.class);
        RacaProgramaEstudioService programaEstudioServiceYnq = ctxYnq.getBean(RacaProgramaEstudioService.class);
        RacaPlanEstudioRepository planEstudioRepositoryYnq = ctxYnq.getBean(RacaPlanEstudioRepository.class);
        RacaPlanEstudioService planEstudioServiceYnq = ctxYnq.getBean(RacaPlanEstudioService.class);
        RacaCicloRepository cicloRepositoryYnq = ctxYnq.getBean(RacaCicloRepository.class);
        RacaCicloService cicloServiceYnq = ctxYnq.getBean(RacaCicloService.class);
        RacaCursoRepository cursoRepositoryYnq = ctxYnq.getBean(RacaCursoRepository.class);
        RacaCursoService cursoServiceYnq = ctxYnq.getBean(RacaCursoService.class);
        RacaPlanCursoService planCursoServiceYnq = ctxYnq.getBean(RacaPlanCursoService.class);
        RacaPlanCursoRepository planCursoRepositoryYnq = ctxYnq.getBean(RacaPlanCursoRepository.class);

        record RyeProgDef(String codigo, String nombre) {
        }
        List<RyeProgDef> programasYnq = List.of(
                new RyeProgDef("EI", "Educacion Inicial"),
                new RyeProgDef("EP", "Educacion Primaria"),
                new RyeProgDef("SEC-COM", "Educacion Secundaria - Especialidad Comunicacion"),
                new RyeProgDef("SEC-MAT", "Educacion Secundaria - Especialidad Matematica"),
                new RyeProgDef("SEC-CYT", "Educacion Secundaria - Especialidad Ciencia y Tecnologia"),
                new RyeProgDef("SEC-CCS", "Educacion Secundaria - Especialidad Ciudadania y Ciencias Sociales"));

        Map<String, RacaProgramaEstudio> programasPorCodigoYnq = new LinkedHashMap<>();
        for (RyeProgDef pd : programasYnq) {
            RacaProgramaEstudio programaYnq = programaEstudioRepositoryYnq.findByCodigo(pd.codigo())
                    .orElseGet(() -> programaEstudioServiceYnq.racaCrear(pd.codigo(), pd.nombre(), null, 10));
            programasPorCodigoYnq.put(pd.codigo(), programaYnq);
            System.out.println("Programa: " + pd.codigo() + " - " + pd.nombre());
        }

        record RyePlanDef(String progCodigo, String planCodigo, String planNombre) {
        }
        List<RyePlanDef> planesYnq = List.of(
                new RyePlanDef("EI", "DCBN2019-EI", "DCBN 2019 - Educacion Inicial"),
                new RyePlanDef("EP", "DCBN2019-EP", "DCBN 2019 - Educacion Primaria"),
                new RyePlanDef("SEC-COM", "DCBN2020-COM", "DCBN 2020 - Secundaria Comunicacion"),
                new RyePlanDef("SEC-MAT", "DCBN2020-MAT", "DCBN 2020 - Secundaria Matematica"),
                new RyePlanDef("SEC-CYT", "DCBN2020-CYT", "DCBN 2020 - Secundaria Ciencia y Tecnologia"),
                new RyePlanDef("SEC-CCS", "DCBN2020-CCS", "DCBN 2020 - Secundaria Ciudadania y Ciencias Sociales"));

        Map<String, RacaPlanEstudio> planesPorCodigoYnq = new LinkedHashMap<>();
        for (RyePlanDef pd : planesYnq) {
            RacaProgramaEstudio programaYnq = programasPorCodigoYnq.get(pd.progCodigo());
            RacaPlanEstudio planYnq = planEstudioRepositoryYnq.findByProgramaIdAndCodigo(programaYnq.getId(), pd.planCodigo())
                    .orElseGet(() -> planEstudioServiceYnq.racaCrear(programaYnq.getId(), pd.planCodigo(), pd.planNombre(), null, null));
            planesPorCodigoYnq.put(pd.planCodigo(), planYnq);
            System.out.println("Plan: " + pd.planCodigo() + " - " + pd.planNombre());
        }

        Map<String, Map<Integer, RacaCiclo>> ciclosPorPlanYnq = new LinkedHashMap<>();
        for (RacaPlanEstudio planYnq : planesPorCodigoYnq.values()) {
            Map<Integer, RacaCiclo> ciclosDelPlanYnq = new LinkedHashMap<>();
            for (int numeroYnq = 1; numeroYnq <= 10; numeroYnq++) {
                int n = numeroYnq;
                RacaCiclo cicloYnq = cicloRepositoryYnq.findByPlanEstudioIdAndNumero(planYnq.getId(), n)
                        .orElseGet(() -> cicloServiceYnq.racaCrear(planYnq.getId(), n, "Ciclo " + ROMANOS_YNQ[n - 1], n));
                ciclosDelPlanYnq.put(numeroYnq, cicloYnq);
            }
            ciclosPorPlanYnq.put(planYnq.getCodigo(), ciclosDelPlanYnq);
            System.out.println("Ciclos I-X creados/verificados para plan " + planYnq.getCodigo());
        }

        Map<String, RacaCurso> cursosPorNombreYnq = new LinkedHashMap<>();
        cursoRepositoryYnq.findAll().forEach(c -> cursosPorNombreYnq.put(c.getNombre(), c));

        record RyeCursoDef(int ciclo, String nombre, int creditos, boolean obligatorio) {
            RyeCursoDef(int ciclo, String nombre, int creditos) {
                this(ciclo, nombre, creditos, true);
            }
        }

        List<RyeCursoDef> cursosMatematicaYnq = List.of(

                new RyeCursoDef(1, "Lectura y Escritura en la Educacion Superior", 3),
                new RyeCursoDef(1, "Resolucion de Problemas Matematicos I", 3),
                new RyeCursoDef(1, "Historia, Sociedad y Diversidad", 3),
                new RyeCursoDef(1, "Desarrollo Personal", 5),
                new RyeCursoDef(1, "Practica e Investigacion I", 5),
                new RyeCursoDef(1, "Educacion y Sociedad en el siglo XXI", 4),

                new RyeCursoDef(2, "Comunicacion Oral en la Educacion Superior", 3),
                new RyeCursoDef(2, "Resolucion de Problemas Matematicos II", 3),
                new RyeCursoDef(2, "Ciencia y Epistemologia", 3),
                new RyeCursoDef(2, "Practica e Investigacion II", 5),
                new RyeCursoDef(2, "Las Adolescencias: Desarrollo, Cambios e Identidades", 5),
                new RyeCursoDef(2, "Planificacion, Mediacion y Evaluacion de los Aprendizajes I", 5),

                new RyeCursoDef(3, "Arte, Creatividad y Aprendizaje", 3),
                new RyeCursoDef(3, "Ingles para Principiantes / Beginner English (A1)", 3),
                new RyeCursoDef(3, "Practica e Investigacion III", 5),
                new RyeCursoDef(3, "Adolescencia y Aprendizajes", 3),
                new RyeCursoDef(3, "Principios de la Didactica de la Matematica", 3),
                new RyeCursoDef(3, "Cognicion y sus Fundamentos para el Aprendizaje y la Ensenanza", 3),
                new RyeCursoDef(3, "Variaciones y sus Fundamentos para el Aprendizaje y la Ensenanza I", 3),

                new RyeCursoDef(4, "Deliberacion y Participacion", 3),
                new RyeCursoDef(4, "Ingles para Principiantes / Beginner English (A1)", 3),
                new RyeCursoDef(4, "Practica e Investigacion IV", 5),
                new RyeCursoDef(4, "Educacion Sexual Integral", 3),
                new RyeCursoDef(4, "Planificacion, Mediacion y Evaluacion de los Aprendizajes II", 3),
                new RyeCursoDef(4, "Gestion de Datos y sus Fundamentos para el Aprendizaje y la Ensenanza", 3),
                new RyeCursoDef(4, "Algebra como Herramienta Modeladora", 3),

                new RyeCursoDef(5, "Literatura y Sociedad en Contextos Diversos", 3),
                new RyeCursoDef(5, "Practica e Investigacion V", 5),
                new RyeCursoDef(5, "Culturas Escolares y Cambio Educativo", 3),
                new RyeCursoDef(5, "Forma, Movimiento y Localizacion y sus Fundamentos para el Aprendizaje y la Ensenanza I", 3),
                new RyeCursoDef(5, "Incertidumbre y sus Fundamentos para el Aprendizaje y la Ensenanza", 3),
                new RyeCursoDef(5, "Cantidad y sus Fundamentos para el Aprendizaje y la Ensenanza I", 3),
                new RyeCursoDef(5, "Electivo 1", 3, false));

        RacaPlanEstudio planMatematicaYnq = planesPorCodigoYnq.get("DCBN2020-MAT");
        Map<Integer, RacaCiclo> ciclosMatematicaYnq = ciclosPorPlanYnq.get("DCBN2020-MAT");
        Map<Integer, Integer> contadorPorCicloYnq = new LinkedHashMap<>();
        int creadosNuevosYnq = 0;
        int reutilizadosYnq = 0;
        int enlacesCreadosYnq = 0;

        for (RyeCursoDef cd : cursosMatematicaYnq) {
            RacaCurso cursoYnq = cursosPorNombreYnq.get(cd.nombre());
            if (cursoYnq == null) {
                int siguienteYnq = contadorPorCicloYnq.merge(cd.ciclo(), 1, Integer::sum);
                String codigoYnq = "MAT-C" + cd.ciclo() + "-" + String.format("%02d", siguienteYnq);
                cursoYnq = cursoServiceYnq.racaCrear(codigoYnq, cd.nombre(), cd.creditos(), null, null);
                cursosPorNombreYnq.put(cd.nombre(), cursoYnq);
                creadosNuevosYnq++;
            } else {
                reutilizadosYnq++;
            }

            RacaCiclo cicloYnq = ciclosMatematicaYnq.get(cd.ciclo());
            boolean yaAsignadoYnq = planCursoRepositoryYnq
                    .existsByPlanEstudioIdAndCicloIdAndCursoId(planMatematicaYnq.getId(), cicloYnq.getId(), cursoYnq.getId());
            if (!yaAsignadoYnq) {
                planCursoServiceYnq.racaAsignar(planMatematicaYnq.getId(), cicloYnq.getId(), cursoYnq.getId(), 1, cd.obligatorio());
                enlacesCreadosYnq++;
            }
        }

        System.out.println();
        System.out.println("=== RESUMEN ===");
        System.out.println("Cursos nuevos creados: " + creadosNuevosYnq);
        System.out.println("Cursos reutilizados (nombre repetido): " + reutilizadosYnq);
        System.out.println("Asignaciones curso-ciclo (plan_cursos) creadas: " + enlacesCreadosYnq);
        System.out.println("Listo.");
    }
}
