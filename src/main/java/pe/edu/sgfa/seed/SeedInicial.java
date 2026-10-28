package pe.edu.sgfa.seed;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import pe.edu.sgfa.SgfaApplication;
import pe.edu.sgfa.institucion.service.InstitucionService;
import pe.edu.sgfa.programa.domain.Ciclo;
import pe.edu.sgfa.programa.domain.Curso;
import pe.edu.sgfa.programa.domain.PlanEstudio;
import pe.edu.sgfa.programa.domain.ProgramaEstudio;
import pe.edu.sgfa.programa.repository.CicloRepository;
import pe.edu.sgfa.programa.repository.CursoRepository;
import pe.edu.sgfa.programa.repository.PlanCursoRepository;
import pe.edu.sgfa.programa.repository.PlanEstudioRepository;
import pe.edu.sgfa.programa.repository.ProgramaEstudioRepository;
import pe.edu.sgfa.programa.service.CicloService;
import pe.edu.sgfa.programa.service.CursoService;
import pe.edu.sgfa.programa.service.PlanCursoService;
import pe.edu.sgfa.programa.service.PlanEstudioService;
import pe.edu.sgfa.programa.service.ProgramaEstudioService;
import pe.edu.sgfa.usuario.domain.RolUsuario;
import pe.edu.sgfa.usuario.domain.Usuario;
import pe.edu.sgfa.usuario.repository.UsuarioRepository;
import pe.edu.sgfa.usuario.service.SesionActual;

public class SeedInicial {

    private static final String[] ROMANOS = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    public static void main(String[] argsr) {
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(SgfaApplication.class)
                .web(WebApplicationType.NONE)
                .run(argsr);
        try {
            ejecutar(ctx);
        } finally {
            ctx.close();
        }
    }

    private static void ejecutar(ConfigurableApplicationContext ctx) {
        UsuarioRepository usrRpstry = ctx.getBean(UsuarioRepository.class);
        SesionActual ssnActl = ctx.getBean(SesionActual.class);
        Usuario admn = usrRpstry.findAll().stream()
                .filter(u -> u.getRol() == RolUsuario.ADMIN)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No existe un usuario ADMIN en la base de datos."));
        ssnActl.iniciar(admn);
        System.out.println("Autenticado como: " + admn.getUsername());

        InstitucionService insttcnSrvc = ctx.getBean(InstitucionService.class);
        if (insttcnSrvc.obtenerPrincipal().isEmpty()) {
            insttcnSrvc.guardar("Escuela de Educacion Superior Pedagogica Publica \"Juliaca\"",
                    "EESPP Juliaca", null, null, null);
            System.out.println("Institucion creada.");
        } else {
            System.out.println("Institucion ya existia, no se modifica.");
        }

        ProgramaEstudioRepository prgrmEstdRpstry = ctx.getBean(ProgramaEstudioRepository.class);
        ProgramaEstudioService prgrmEstdSrvc = ctx.getBean(ProgramaEstudioService.class);
        PlanEstudioRepository plnEstdRpstry = ctx.getBean(PlanEstudioRepository.class);
        PlanEstudioService plnEstdSrvc = ctx.getBean(PlanEstudioService.class);
        CicloRepository cclRpstry = ctx.getBean(CicloRepository.class);
        CicloService cclSrvc = ctx.getBean(CicloService.class);
        CursoRepository crsRpstry = ctx.getBean(CursoRepository.class);
        CursoService crsSrvc = ctx.getBean(CursoService.class);
        PlanCursoService plnCrsSrvc = ctx.getBean(PlanCursoService.class);
        PlanCursoRepository plnCrsRpstry = ctx.getBean(PlanCursoRepository.class);

        record ProgDef(String codigo, String nombre) {
        }
        List<ProgDef> prgrms = List.of(
                new ProgDef("EI", "Educacion Inicial"),
                new ProgDef("EP", "Educacion Primaria"),
                new ProgDef("SEC-COM", "Educacion Secundaria - Especialidad Comunicacion"),
                new ProgDef("SEC-MAT", "Educacion Secundaria - Especialidad Matematica"),
                new ProgDef("SEC-CYT", "Educacion Secundaria - Especialidad Ciencia y Tecnologia"),
                new ProgDef("SEC-CCS", "Educacion Secundaria - Especialidad Ciudadania y Ciencias Sociales"));

        Map<String, ProgramaEstudio> prgrmsPorCdg = new LinkedHashMap<>();
        for (ProgDef pd : prgrms) {
            ProgramaEstudio programa = prgrmEstdRpstry.findByCodigo(pd.codigo())
                    .orElseGet(() -> prgrmEstdSrvc.crear(pd.codigo(), pd.nombre(), null, 10));
            prgrmsPorCdg.put(pd.codigo(), programa);
            System.out.println("Programa: " + pd.codigo() + " - " + pd.nombre());
        }

        record PlanDef(String progCodigo, String planCodigo, String planNombre) {
        }
        List<PlanDef> plns = List.of(
                new PlanDef("EI", "DCBN2019-EI", "DCBN 2019 - Educacion Inicial"),
                new PlanDef("EP", "DCBN2019-EP", "DCBN 2019 - Educacion Primaria"),
                new PlanDef("SEC-COM", "DCBN2020-COM", "DCBN 2020 - Secundaria Comunicacion"),
                new PlanDef("SEC-MAT", "DCBN2020-MAT", "DCBN 2020 - Secundaria Matematica"),
                new PlanDef("SEC-CYT", "DCBN2020-CYT", "DCBN 2020 - Secundaria Ciencia y Tecnologia"),
                new PlanDef("SEC-CCS", "DCBN2020-CCS", "DCBN 2020 - Secundaria Ciudadania y Ciencias Sociales"));

        Map<String, PlanEstudio> plnsPorCdg = new LinkedHashMap<>();
        for (PlanDef pd : plns) {
            ProgramaEstudio programa = prgrmsPorCdg.get(pd.progCodigo());
            PlanEstudio pln = plnEstdRpstry.findByProgramaIdAndCodigo(programa.getId(), pd.planCodigo())
                    .orElseGet(() -> plnEstdSrvc.crear(programa.getId(), pd.planCodigo(), pd.planNombre(), null, null));
            plnsPorCdg.put(pd.planCodigo(), pln);
            System.out.println("Plan: " + pd.planCodigo() + " - " + pd.planNombre());
        }

        Map<String, Map<Integer, Ciclo>> cclsPorPln = new LinkedHashMap<>();
        for (PlanEstudio pln : plnsPorCdg.values()) {
            Map<Integer, Ciclo> cclsDelPln = new LinkedHashMap<>();
            for (int nmr = 1; nmr <= 10; nmr++) {
                int n = nmr;
                Ciclo ciclo = cclRpstry.findByPlanEstudioIdAndNumero(pln.getId(), n)
                        .orElseGet(() -> cclSrvc.crear(pln.getId(), n, "Ciclo " + ROMANOS[n - 1], n));
                cclsDelPln.put(nmr, ciclo);
            }
            cclsPorPln.put(pln.getCodigo(), cclsDelPln);
            System.out.println("Ciclos I-X creados/verificados para plan " + pln.getCodigo());
        }

        Map<String, Curso> crssPorNmbr = new LinkedHashMap<>();
        crsRpstry.findAll().forEach(c -> crssPorNmbr.put(c.getNombre(), c));

        record CursoDef(int ciclo, String nombre, int creditos, boolean obligatorio) {
            CursoDef(int ciclo, String nombre, int creditos) {
                this(ciclo, nombre, creditos, true);
            }
        }

        List<CursoDef> crssMtmtc = List.of(

                new CursoDef(1, "Lectura y Escritura en la Educacion Superior", 3),
                new CursoDef(1, "Resolucion de Problemas Matematicos I", 3),
                new CursoDef(1, "Historia, Sociedad y Diversidad", 3),
                new CursoDef(1, "Desarrollo Personal", 5),
                new CursoDef(1, "Practica e Investigacion I", 5),
                new CursoDef(1, "Educacion y Sociedad en el siglo XXI", 4),

                new CursoDef(2, "Comunicacion Oral en la Educacion Superior", 3),
                new CursoDef(2, "Resolucion de Problemas Matematicos II", 3),
                new CursoDef(2, "Ciencia y Epistemologia", 3),
                new CursoDef(2, "Practica e Investigacion II", 5),
                new CursoDef(2, "Las Adolescencias: Desarrollo, Cambios e Identidades", 5),
                new CursoDef(2, "Planificacion, Mediacion y Evaluacion de los Aprendizajes I", 5),

                new CursoDef(3, "Arte, Creatividad y Aprendizaje", 3),
                new CursoDef(3, "Ingles para Principiantes / Beginner English (A1)", 3),
                new CursoDef(3, "Practica e Investigacion III", 5),
                new CursoDef(3, "Adolescencia y Aprendizajes", 3),
                new CursoDef(3, "Principios de la Didactica de la Matematica", 3),
                new CursoDef(3, "Cognicion y sus Fundamentos para el Aprendizaje y la Ensenanza", 3),
                new CursoDef(3, "Variaciones y sus Fundamentos para el Aprendizaje y la Ensenanza I", 3),

                new CursoDef(4, "Deliberacion y Participacion", 3),
                new CursoDef(4, "Ingles para Principiantes / Beginner English (A1)", 3),
                new CursoDef(4, "Practica e Investigacion IV", 5),
                new CursoDef(4, "Educacion Sexual Integral", 3),
                new CursoDef(4, "Planificacion, Mediacion y Evaluacion de los Aprendizajes II", 3),
                new CursoDef(4, "Gestion de Datos y sus Fundamentos para el Aprendizaje y la Ensenanza", 3),
                new CursoDef(4, "Algebra como Herramienta Modeladora", 3),

                new CursoDef(5, "Literatura y Sociedad en Contextos Diversos", 3),
                new CursoDef(5, "Practica e Investigacion V", 5),
                new CursoDef(5, "Culturas Escolares y Cambio Educativo", 3),
                new CursoDef(5, "Forma, Movimiento y Localizacion y sus Fundamentos para el Aprendizaje y la Ensenanza I", 3),
                new CursoDef(5, "Incertidumbre y sus Fundamentos para el Aprendizaje y la Ensenanza", 3),
                new CursoDef(5, "Cantidad y sus Fundamentos para el Aprendizaje y la Ensenanza I", 3),
                new CursoDef(5, "Electivo 1", 3, false));

        PlanEstudio plnMtmtc = plnsPorCdg.get("DCBN2020-MAT");
        Map<Integer, Ciclo> cclsMtmtc = cclsPorPln.get("DCBN2020-MAT");
        Map<Integer, Integer> cntdrPorCcl = new LinkedHashMap<>();
        int crdsNvs = 0;
        int rtlzds = 0;
        int enlcsCrds = 0;

        for (CursoDef cd : crssMtmtc) {
            Curso crs = crssPorNmbr.get(cd.nombre());
            if (crs == null) {
                int sgnt = cntdrPorCcl.merge(cd.ciclo(), 1, Integer::sum);
                String codigo = "MAT-C" + cd.ciclo() + "-" + String.format("%02d", sgnt);
                crs = crsSrvc.crear(codigo, cd.nombre(), cd.creditos(), null, null);
                crssPorNmbr.put(cd.nombre(), crs);
                crdsNvs++;
            } else {
                rtlzds++;
            }

            Ciclo ciclo = cclsMtmtc.get(cd.ciclo());
            boolean yaAsgnd = plnCrsRpstry
                    .existsByPlanEstudioIdAndCicloIdAndCursoId(plnMtmtc.getId(), ciclo.getId(), crs.getId());
            if (!yaAsgnd) {
                plnCrsSrvc.asignar(plnMtmtc.getId(), ciclo.getId(), crs.getId(), 1, cd.obligatorio());
                enlcsCrds++;
            }
        }

        System.out.println();
        System.out.println("=== RESUMEN ===");
        System.out.println("Cursos nuevos creados: " + crdsNvs);
        System.out.println("Cursos reutilizados (nombre repetido): " + rtlzds);
        System.out.println("Asignaciones curso-ciclo (plan_cursos) creadas: " + enlcsCrds);
        System.out.println("Listo.");
    }
}
