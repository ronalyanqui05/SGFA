package pe.edu.sgfa.dashboard.service;

import java.util.List;

import org.springframework.stereotype.Service;

import pe.edu.sgfa.estudiante.domain.YmclEstadoEstudiante;
import pe.edu.sgfa.estudiante.repository.YmclEstudianteRepository;
import pe.edu.sgfa.periodo.domain.YmclEstadoPeriodoAcademico;
import pe.edu.sgfa.periodo.domain.YmclPeriodoAcademico;
import pe.edu.sgfa.periodo.repository.YmclPeriodoAcademicoRepository;
import pe.edu.sgfa.programa.repository.RacaPlanEstudioRepository;
import pe.edu.sgfa.programa.repository.RacaProgramaEstudioRepository;
import pe.edu.sgfa.usuario.service.RyeSesionActual;

@Service
public class RyeDashboardService {

    private final YmclEstudianteRepository estudianteRepositoryYnq;
    private final RacaProgramaEstudioRepository programaEstudioRepositoryYnq;
    private final RacaPlanEstudioRepository planEstudioRepositoryYnq;
    private final YmclPeriodoAcademicoRepository periodoAcademicoRepositoryYnq;
    private final RyeSesionActual sesionActualYnq;

    public RyeDashboardService(YmclEstudianteRepository estudianteRepositoryYnq,
                             RacaProgramaEstudioRepository programaEstudioRepositoryYnq,
                             RacaPlanEstudioRepository planEstudioRepositoryYnq,
                             YmclPeriodoAcademicoRepository periodoAcademicoRepositoryYnq, RyeSesionActual sesionActualYnq) {
        this.estudianteRepositoryYnq = estudianteRepositoryYnq;
        this.programaEstudioRepositoryYnq = programaEstudioRepositoryYnq;
        this.planEstudioRepositoryYnq = planEstudioRepositoryYnq;
        this.periodoAcademicoRepositoryYnq = periodoAcademicoRepositoryYnq;
        this.sesionActualYnq = sesionActualYnq;
    }

    public RyeResumenDashboard ryeObtenerResumen() {
        sesionActualYnq.ryeRequerirAutenticacion();

        long totalEstudiantesYnq = estudianteRepositoryYnq.count();
        long estudiantesActivosYnq = estudianteRepositoryYnq.countByEstado(YmclEstadoEstudiante.ACTIVO);
        long totalProgramasYnq = programaEstudioRepositoryYnq.count();
        long totalPlanesYnq = planEstudioRepositoryYnq.count();

        List<YmclPeriodoAcademico> activosYnq = periodoAcademicoRepositoryYnq
                .findByEstadoOrderByAnioDescNombreDesc(YmclEstadoPeriodoAcademico.ACTIVO);
        String periodoActualYnq = activosYnq.isEmpty() ? null : activosYnq.get(0).getNombre();

        return new RyeResumenDashboard(totalEstudiantesYnq, estudiantesActivosYnq, totalProgramasYnq, totalPlanesYnq, periodoActualYnq);
    }
}
