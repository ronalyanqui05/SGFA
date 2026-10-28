package pe.edu.sgfa.nota.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.sgfa.auditoria.service.AuditoriaService;
import pe.edu.sgfa.common.exception.EntidadNoEncontradaException;
import pe.edu.sgfa.common.exception.ValidacionException;
import pe.edu.sgfa.nota.domain.RegistroAcademico;
import pe.edu.sgfa.nota.domain.SituacionAcademica;
import pe.edu.sgfa.nota.repository.RegistroAcademicoRepository;
import pe.edu.sgfa.nota.repository.SituacionAcademicaRepository;
import pe.edu.sgfa.periodo.domain.PeriodoAcademico;
import pe.edu.sgfa.periodo.repository.PeriodoAcademicoRepository;
import pe.edu.sgfa.programa.domain.Ciclo;
import pe.edu.sgfa.programa.domain.PlanCurso;
import pe.edu.sgfa.programa.repository.CicloRepository;
import pe.edu.sgfa.programa.repository.PlanCursoRepository;
import pe.edu.sgfa.trayectoria.domain.Trayectoria;
import pe.edu.sgfa.trayectoria.repository.TrayectoriaRepository;
import pe.edu.sgfa.usuario.domain.Usuario;
import pe.edu.sgfa.usuario.service.SesionActual;

@Service
public class RegistroAcademicoService {

    private final RegistroAcademicoRepository registroAcademicoRepository;
    private final TrayectoriaRepository trayectoriaRepository;
    private final CicloRepository cicloRepository;
    private final PlanCursoRepository planCursoRepository;
    private final PeriodoAcademicoRepository periodoAcademicoRepository;
    private final SituacionAcademicaRepository situacionAcademicaRepository;
    private final SesionActual sesionActual;
    private final AuditoriaService auditoriaService;

    public RegistroAcademicoService(RegistroAcademicoRepository registroAcademicoRepository,
                                     TrayectoriaRepository trayectoriaRepository, CicloRepository cicloRepository,
                                     PlanCursoRepository planCursoRepository,
                                     PeriodoAcademicoRepository periodoAcademicoRepository,
                                     SituacionAcademicaRepository situacionAcademicaRepository,
                                     SesionActual sesionActual, AuditoriaService auditoriaService) {
        this.registroAcademicoRepository = registroAcademicoRepository;
        this.trayectoriaRepository = trayectoriaRepository;
        this.cicloRepository = cicloRepository;
        this.planCursoRepository = planCursoRepository;
        this.periodoAcademicoRepository = periodoAcademicoRepository;
        this.situacionAcademicaRepository = situacionAcademicaRepository;
        this.sesionActual = sesionActual;
        this.auditoriaService = auditoriaService;
    }

    public List<SituacionAcademica> listarSituaciones() {
        sesionActual.requerirAutenticacion();
        return situacionAcademicaRepository.findByActivoTrueOrderByNombreAsc();
    }

    public List<FilaRegistro> listarParaRegistro(Long tryctrId, Long cclId, Long prdId) {
        sesionActual.requerirAutenticacion();
        Trayectoria trayectoria = obtenerTrayectoriaOLanzar(tryctrId);
        Ciclo ccl = validarCicloDelPlan(cclId, trayectoria);

        List<PlanCurso> plnCrss = planCursoRepository.findByCicloIdOrderByOrdenAsc(ccl.getId());
        return plnCrss.stream()
                .map(pc -> new FilaRegistro(pc, registroAcademicoRepository
                        .findByTrayectoriaIdAndPlanCursoIdAndPeriodoId(tryctrId, pc.getId(), prdId)
                        .orElse(null)))
                .toList();
    }

    @Transactional
    public void guardarMasivo(Long tryctrId, Long prdId, List<ItemNota> itms) {
        sesionActual.requerirAutenticacion();
        Trayectoria trayectoria = obtenerTrayectoriaOLanzar(tryctrId);
        PeriodoAcademico periodo = periodoAcademicoRepository.findById(prdId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Periodo no encontrado: id=" + prdId));

        for (ItemNota itm : itms) {
            if (itm.situacionId() == null) {
                continue;
            }
            guardarUno(trayectoria, periodo, itm);
        }
    }

    public List<FilaRegistro> listarVigentesPorCiclo(Long tryctrId, Long cclId) {
        sesionActual.requerirAutenticacion();
        Trayectoria trayectoria = obtenerTrayectoriaOLanzar(tryctrId);
        Ciclo ccl = validarCicloDelPlan(cclId, trayectoria);

        List<PlanCurso> plnCrss = planCursoRepository.findByCicloIdOrderByOrdenAsc(ccl.getId());
        return plnCrss.stream()
                .map(pc -> new FilaRegistro(pc, registroAcademicoRepository
                        .findFirstByTrayectoriaIdAndPlanCursoIdOrderByIdDesc(tryctrId, pc.getId())
                        .orElse(null)))
                .toList();
    }

    public ResultadoPromedio calcularPromedioCiclo(Long tryctrId, Long cclId) {
        return calcularPromedioDeFilas(listarVigentesPorCiclo(tryctrId, cclId));
    }

    public ResultadoPromedio calcularPromedioGeneral(Long tryctrId) {
        sesionActual.requerirAutenticacion();
        Trayectoria trayectoria = obtenerTrayectoriaOLanzar(tryctrId);
        List<Ciclo> ccls = cicloRepository.findByPlanEstudioIdOrderByOrdenAsc(trayectoria.getPlanEstudio().getId());
        List<FilaRegistro> tdsLasFls = ccls.stream()
                .flatMap(ccl -> listarVigentesPorCiclo(tryctrId, ccl.getId()).stream())
                .toList();
        return calcularPromedioDeFilas(tdsLasFls);
    }

    public ResultadoPromedio calcularPromedio(Long tryctrId, Long cclId, Long prdId) {
        return calcularPromedioDeFilas(listarParaRegistro(tryctrId, cclId, prdId));
    }

    private ResultadoPromedio calcularPromedioDeFilas(List<FilaRegistro> fls) {
        double smPndrd = 0;
        int ttlCrdts = 0;
        for (FilaRegistro fl : fls) {
            RegistroAcademico registro = fl.registro();
            if (registro != null && registro.getNotaFinal() != null && registro.getSituacion().isCuentaParaPromedio()) {
                int crdts = fl.planCurso().getCurso().getCreditos();
                smPndrd += registro.getNotaFinal() * crdts;
                ttlCrdts += crdts;
            }
        }
        Double prmd = ttlCrdts == 0 ? null : smPndrd / ttlCrdts;
        return new ResultadoPromedio(smPndrd, ttlCrdts, prmd);
    }

    private void guardarUno(Trayectoria trayectoria, PeriodoAcademico periodo, ItemNota itm) {
        PlanCurso planCurso = planCursoRepository.findById(itm.planCursoId())
                .orElseThrow(() -> new EntidadNoEncontradaException("Curso del plan no encontrado: id=" + itm.planCursoId()));
        if (!planCurso.getPlanEstudio().getId().equals(trayectoria.getPlanEstudio().getId())) {
            throw new ValidacionException("El curso seleccionado no pertenece al plan de estudio de la trayectoria.");
        }
        validarNota(itm.notaFinal());
        SituacionAcademica stcn = situacionAcademicaRepository.findById(itm.situacionId())
                .orElseThrow(() -> new EntidadNoEncontradaException("Situacion no encontrada: id=" + itm.situacionId()));

        Usuario actr = sesionActual.obtenerUsuarioActual().orElse(null);
        Optional<RegistroAcademico> exstnt = registroAcademicoRepository
                .findByTrayectoriaIdAndPlanCursoIdAndPeriodoId(trayectoria.getId(), planCurso.getId(), periodo.getId());

        if (exstnt.isPresent()) {
            RegistroAcademico registro = exstnt.get();
            Double ntAntrr = registro.getNotaFinal();
            SituacionAcademica stcnAntrr = registro.getSituacion();
            boolean cmb = !Objects.equals(ntAntrr, itm.notaFinal())
                    || !stcnAntrr.getId().equals(stcn.getId());
            if (!cmb) {
                return;
            }
            registro.setNotaFinal(itm.notaFinal());
            registro.setSituacion(stcn);
            registro.setObservacion(itm.observacion());
            registro.setUsuario(actr);
            auditoriaService.registrar(actr, "EDITAR_NOTA", "registros_academicos", registro.getId().toString(),
                    "curso=" + planCurso.getCurso().getCodigo()
                            + ", nota_anterior=" + ntAntrr + ", nota_nueva=" + itm.notaFinal()
                            + ", situacion_anterior=" + stcnAntrr.getCodigo()
                            + ", situacion_nueva=" + stcn.getCodigo());
        } else {
            RegistroAcademico nv = new RegistroAcademico(trayectoria, planCurso, periodo, itm.notaFinal(),
                    stcn, itm.observacion(), actr);
            registroAcademicoRepository.save(nv);
            auditoriaService.registrar(actr, "CREAR_NOTA", "registros_academicos", nv.getId().toString(),
                    "curso=" + planCurso.getCurso().getCodigo() + ", nota=" + itm.notaFinal()
                            + ", situacion=" + stcn.getCodigo());
        }
    }

    private Ciclo validarCicloDelPlan(Long cclId, Trayectoria trayectoria) {
        Ciclo ccl = cicloRepository.findById(cclId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Ciclo no encontrado: id=" + cclId));
        if (!ccl.getPlanEstudio().getId().equals(trayectoria.getPlanEstudio().getId())) {
            throw new ValidacionException("El ciclo seleccionado no pertenece al plan de estudio de la trayectoria.");
        }
        return ccl;
    }

    private Trayectoria obtenerTrayectoriaOLanzar(Long tryctrId) {
        return trayectoriaRepository.findById(tryctrId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Trayectoria no encontrada: id=" + tryctrId));
    }

    private void validarNota(Double nota) {
        if (nota != null && (nota < 0 || nota > 20)) {
            throw new ValidacionException("La nota debe estar entre 0 y 20.");
        }
    }
}
