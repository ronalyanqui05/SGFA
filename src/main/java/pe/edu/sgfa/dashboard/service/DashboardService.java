package pe.edu.sgfa.dashboard.service;

import java.util.List;

import org.springframework.stereotype.Service;

import pe.edu.sgfa.estudiante.domain.EstadoEstudiante;
import pe.edu.sgfa.estudiante.repository.EstudianteRepository;
import pe.edu.sgfa.periodo.domain.EstadoPeriodoAcademico;
import pe.edu.sgfa.periodo.domain.PeriodoAcademico;
import pe.edu.sgfa.periodo.repository.PeriodoAcademicoRepository;
import pe.edu.sgfa.programa.repository.PlanEstudioRepository;
import pe.edu.sgfa.programa.repository.ProgramaEstudioRepository;
import pe.edu.sgfa.usuario.service.SesionActual;

@Service
public class DashboardService {

    private final EstudianteRepository estudianteRepository;
    private final ProgramaEstudioRepository programaEstudioRepository;
    private final PlanEstudioRepository planEstudioRepository;
    private final PeriodoAcademicoRepository periodoAcademicoRepository;
    private final SesionActual sesionActual;

    public DashboardService(EstudianteRepository estudianteRepository,
                             ProgramaEstudioRepository programaEstudioRepository,
                             PlanEstudioRepository planEstudioRepository,
                             PeriodoAcademicoRepository periodoAcademicoRepository, SesionActual sesionActual) {
        this.estudianteRepository = estudianteRepository;
        this.programaEstudioRepository = programaEstudioRepository;
        this.planEstudioRepository = planEstudioRepository;
        this.periodoAcademicoRepository = periodoAcademicoRepository;
        this.sesionActual = sesionActual;
    }

    public ResumenDashboard obtenerResumen() {
        sesionActual.requerirAutenticacion();

        long ttlEstdnts = estudianteRepository.count();
        long estdntsActvs = estudianteRepository.countByEstado(EstadoEstudiante.ACTIVO);
        long ttlPrgrms = programaEstudioRepository.count();
        long ttlPlns = planEstudioRepository.count();

        List<PeriodoAcademico> actvs = periodoAcademicoRepository
                .findByEstadoOrderByAnioDescNombreDesc(EstadoPeriodoAcademico.ACTIVO);
        String prdActl = actvs.isEmpty() ? null : actvs.get(0).getNombre();

        return new ResumenDashboard(ttlEstdnts, estdntsActvs, ttlPrgrms, ttlPlns, prdActl);
    }
}
