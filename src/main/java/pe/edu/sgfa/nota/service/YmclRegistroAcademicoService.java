package pe.edu.sgfa.nota.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.sgfa.auditoria.service.RahpAuditoriaService;
import pe.edu.sgfa.common.exception.RyeEntidadNoEncontradaException;
import pe.edu.sgfa.common.exception.RyeValidacionException;
import pe.edu.sgfa.nota.domain.YmclRegistroAcademico;
import pe.edu.sgfa.nota.domain.YmclSituacionAcademica;
import pe.edu.sgfa.nota.repository.YmclRegistroAcademicoRepository;
import pe.edu.sgfa.nota.repository.YmclSituacionAcademicaRepository;
import pe.edu.sgfa.periodo.domain.YmclPeriodoAcademico;
import pe.edu.sgfa.periodo.repository.YmclPeriodoAcademicoRepository;
import pe.edu.sgfa.programa.domain.RacaCiclo;
import pe.edu.sgfa.programa.domain.RacaPlanCurso;
import pe.edu.sgfa.programa.repository.RacaCicloRepository;
import pe.edu.sgfa.programa.repository.RacaPlanCursoRepository;
import pe.edu.sgfa.trayectoria.domain.YmclTrayectoria;
import pe.edu.sgfa.trayectoria.repository.YmclTrayectoriaRepository;
import pe.edu.sgfa.usuario.domain.RyeUsuario;
import pe.edu.sgfa.usuario.service.RyeSesionActual;

@Service
public class YmclRegistroAcademicoService {

    private final YmclRegistroAcademicoRepository registroAcademicoRepositoryChp;
    private final YmclTrayectoriaRepository trayectoriaRepositoryChp;
    private final RacaCicloRepository cicloRepositoryChp;
    private final RacaPlanCursoRepository planCursoRepositoryChp;
    private final YmclPeriodoAcademicoRepository periodoAcademicoRepositoryChp;
    private final YmclSituacionAcademicaRepository situacionAcademicaRepositoryChp;
    private final RyeSesionActual sesionActualChp;
    private final RahpAuditoriaService auditoriaServiceChp;

    public YmclRegistroAcademicoService(YmclRegistroAcademicoRepository registroAcademicoRepositoryChp,
                                     YmclTrayectoriaRepository trayectoriaRepositoryChp, RacaCicloRepository cicloRepositoryChp,
                                     RacaPlanCursoRepository planCursoRepositoryChp,
                                     YmclPeriodoAcademicoRepository periodoAcademicoRepositoryChp,
                                     YmclSituacionAcademicaRepository situacionAcademicaRepositoryChp,
                                     RyeSesionActual sesionActualChp, RahpAuditoriaService auditoriaServiceChp) {
        this.registroAcademicoRepositoryChp = registroAcademicoRepositoryChp;
        this.trayectoriaRepositoryChp = trayectoriaRepositoryChp;
        this.cicloRepositoryChp = cicloRepositoryChp;
        this.planCursoRepositoryChp = planCursoRepositoryChp;
        this.periodoAcademicoRepositoryChp = periodoAcademicoRepositoryChp;
        this.situacionAcademicaRepositoryChp = situacionAcademicaRepositoryChp;
        this.sesionActualChp = sesionActualChp;
        this.auditoriaServiceChp = auditoriaServiceChp;
    }

    public List<YmclSituacionAcademica> ymclListarSituaciones() {
        sesionActualChp.ryeRequerirAutenticacion();
        return situacionAcademicaRepositoryChp.findByActivoTrueOrderByNombreAsc();
    }

    public List<YmclFilaRegistro> ymclListarParaRegistro(Long trayectoriaIdChp, Long cicloIdChp, Long periodoIdChp) {
        sesionActualChp.ryeRequerirAutenticacion();
        YmclTrayectoria trayectoriaChp = ymclObtenerTrayectoriaOLanzar(trayectoriaIdChp);
        RacaCiclo cicloChp = ymclValidarCicloDelPlan(cicloIdChp, trayectoriaChp);

        List<RacaPlanCurso> planCursosChp = planCursoRepositoryChp.findByCicloIdOrderByOrdenAsc(cicloChp.getId());
        return planCursosChp.stream()
                .map(pc -> new YmclFilaRegistro(pc, registroAcademicoRepositoryChp
                        .findByTrayectoriaIdAndPlanCursoIdAndPeriodoId(trayectoriaIdChp, pc.getId(), periodoIdChp)
                        .orElse(null)))
                .toList();
    }

    @Transactional
    public void ymclGuardarMasivo(Long trayectoriaIdChp, Long periodoIdChp, List<YmclItemNota> itemsChp) {
        sesionActualChp.ryeRequerirAutenticacion();
        YmclTrayectoria trayectoriaChp = ymclObtenerTrayectoriaOLanzar(trayectoriaIdChp);
        YmclPeriodoAcademico periodoChp = periodoAcademicoRepositoryChp.findById(periodoIdChp)
                .orElseThrow(() -> new RyeEntidadNoEncontradaException("Periodo no encontrado: id=" + periodoIdChp));

        for (YmclItemNota itemChp : itemsChp) {
            if (itemChp.situacionId() == null) {
                continue;
            }
            ymclGuardarUno(trayectoriaChp, periodoChp, itemChp);
        }
    }

    public List<YmclFilaRegistro> ymclListarVigentesPorCiclo(Long trayectoriaIdChp, Long cicloIdChp) {
        sesionActualChp.ryeRequerirAutenticacion();
        YmclTrayectoria trayectoriaChp = ymclObtenerTrayectoriaOLanzar(trayectoriaIdChp);
        RacaCiclo cicloChp = ymclValidarCicloDelPlan(cicloIdChp, trayectoriaChp);

        List<RacaPlanCurso> planCursosChp = planCursoRepositoryChp.findByCicloIdOrderByOrdenAsc(cicloChp.getId());
        return planCursosChp.stream()
                .map(pc -> new YmclFilaRegistro(pc, registroAcademicoRepositoryChp
                        .findFirstByTrayectoriaIdAndPlanCursoIdOrderByIdDesc(trayectoriaIdChp, pc.getId())
                        .orElse(null)))
                .toList();
    }

    public YmclResultadoPromedio ymclCalcularPromedioCiclo(Long trayectoriaIdChp, Long cicloIdChp) {
        return ymclCalcularPromedioDeFilas(ymclListarVigentesPorCiclo(trayectoriaIdChp, cicloIdChp));
    }

    public YmclResultadoPromedio ymclCalcularPromedioGeneral(Long trayectoriaIdChp) {
        sesionActualChp.ryeRequerirAutenticacion();
        YmclTrayectoria trayectoriaChp = ymclObtenerTrayectoriaOLanzar(trayectoriaIdChp);
        List<RacaCiclo> ciclosChp = cicloRepositoryChp.findByPlanEstudioIdOrderByOrdenAsc(trayectoriaChp.getPlanEstudio().getId());
        List<YmclFilaRegistro> todasLasFilasChp = ciclosChp.stream()
                .flatMap(cicloChp -> ymclListarVigentesPorCiclo(trayectoriaIdChp, cicloChp.getId()).stream())
                .toList();
        return ymclCalcularPromedioDeFilas(todasLasFilasChp);
    }

    public YmclResultadoPromedio ymclCalcularPromedio(Long trayectoriaIdChp, Long cicloIdChp, Long periodoIdChp) {
        return ymclCalcularPromedioDeFilas(ymclListarParaRegistro(trayectoriaIdChp, cicloIdChp, periodoIdChp));
    }

    private YmclResultadoPromedio ymclCalcularPromedioDeFilas(List<YmclFilaRegistro> filasChp) {
        double sumaPonderadaChp = 0;
        int totalCreditosChp = 0;
        for (YmclFilaRegistro filaChp : filasChp) {
            YmclRegistroAcademico registroChp = filaChp.registro();
            if (registroChp != null && registroChp.getNotaFinal() != null && registroChp.getSituacion().isCuentaParaPromedio()) {
                int creditosChp = filaChp.planCurso().getCurso().getCreditos();
                sumaPonderadaChp += registroChp.getNotaFinal() * creditosChp;
                totalCreditosChp += creditosChp;
            }
        }
        Double promedioChp = totalCreditosChp == 0 ? null : sumaPonderadaChp / totalCreditosChp;
        return new YmclResultadoPromedio(sumaPonderadaChp, totalCreditosChp, promedioChp);
    }

    private void ymclGuardarUno(YmclTrayectoria trayectoriaChp, YmclPeriodoAcademico periodoChp, YmclItemNota itemChp) {
        RacaPlanCurso planCursoChp = planCursoRepositoryChp.findById(itemChp.planCursoId())
                .orElseThrow(() -> new RyeEntidadNoEncontradaException("Curso del plan no encontrado: id=" + itemChp.planCursoId()));
        if (!planCursoChp.getPlanEstudio().getId().equals(trayectoriaChp.getPlanEstudio().getId())) {
            throw new RyeValidacionException("El curso seleccionado no pertenece al plan de estudio de la trayectoria.");
        }
        ymclValidarNota(itemChp.notaFinal());
        YmclSituacionAcademica situacionChp = situacionAcademicaRepositoryChp.findById(itemChp.situacionId())
                .orElseThrow(() -> new RyeEntidadNoEncontradaException("Situacion no encontrada: id=" + itemChp.situacionId()));

        RyeUsuario actorChp = sesionActualChp.ryeObtenerUsuarioActual().orElse(null);
        Optional<YmclRegistroAcademico> existenteChp = registroAcademicoRepositoryChp
                .findByTrayectoriaIdAndPlanCursoIdAndPeriodoId(trayectoriaChp.getId(), planCursoChp.getId(), periodoChp.getId());

        if (existenteChp.isPresent()) {
            YmclRegistroAcademico registroChp = existenteChp.get();
            Double notaAnteriorChp = registroChp.getNotaFinal();
            YmclSituacionAcademica situacionAnteriorChp = registroChp.getSituacion();
            boolean cambioChp = !Objects.equals(notaAnteriorChp, itemChp.notaFinal())
                    || !situacionAnteriorChp.getId().equals(situacionChp.getId());
            if (!cambioChp) {
                return;
            }
            registroChp.setNotaFinal(itemChp.notaFinal());
            registroChp.setSituacion(situacionChp);
            registroChp.setObservacion(itemChp.observacion());
            registroChp.setUsuario(actorChp);
            auditoriaServiceChp.rahpRegistrar(actorChp, "EDITAR_NOTA", "registros_academicos", registroChp.getId().toString(),
                    "curso=" + planCursoChp.getCurso().getCodigo()
                            + ", nota_anterior=" + notaAnteriorChp + ", nota_nueva=" + itemChp.notaFinal()
                            + ", situacion_anterior=" + situacionAnteriorChp.getCodigo()
                            + ", situacion_nueva=" + situacionChp.getCodigo());
        } else {
            YmclRegistroAcademico nuevoChp = new YmclRegistroAcademico(trayectoriaChp, planCursoChp, periodoChp, itemChp.notaFinal(),
                    situacionChp, itemChp.observacion(), actorChp);
            registroAcademicoRepositoryChp.save(nuevoChp);
            auditoriaServiceChp.rahpRegistrar(actorChp, "CREAR_NOTA", "registros_academicos", nuevoChp.getId().toString(),
                    "curso=" + planCursoChp.getCurso().getCodigo() + ", nota=" + itemChp.notaFinal()
                            + ", situacion=" + situacionChp.getCodigo());
        }
    }

    private RacaCiclo ymclValidarCicloDelPlan(Long cicloIdChp, YmclTrayectoria trayectoriaChp) {
        RacaCiclo cicloChp = cicloRepositoryChp.findById(cicloIdChp)
                .orElseThrow(() -> new RyeEntidadNoEncontradaException("Ciclo no encontrado: id=" + cicloIdChp));
        if (!cicloChp.getPlanEstudio().getId().equals(trayectoriaChp.getPlanEstudio().getId())) {
            throw new RyeValidacionException("El ciclo seleccionado no pertenece al plan de estudio de la trayectoria.");
        }
        return cicloChp;
    }

    private YmclTrayectoria ymclObtenerTrayectoriaOLanzar(Long trayectoriaIdChp) {
        return trayectoriaRepositoryChp.findById(trayectoriaIdChp)
                .orElseThrow(() -> new RyeEntidadNoEncontradaException("Trayectoria no encontrada: id=" + trayectoriaIdChp));
    }

    private void ymclValidarNota(Double notaChp) {
        if (notaChp != null && (notaChp < 0 || notaChp > 20)) {
            throw new RyeValidacionException("La nota debe estar entre 0 y 20.");
        }
    }
}
