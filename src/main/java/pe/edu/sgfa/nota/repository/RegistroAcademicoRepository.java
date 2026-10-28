package pe.edu.sgfa.nota.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.edu.sgfa.nota.domain.RegistroAcademico;

public interface RegistroAcademicoRepository extends JpaRepository<RegistroAcademico, Long> {

    Optional<RegistroAcademico> findByTrayectoriaIdAndPlanCursoIdAndPeriodoId(
            Long trayectoriaId, Long planCursoId, Long periodoId);

    List<RegistroAcademico> findByTrayectoriaIdAndPeriodoId(Long trayectoriaId, Long periodoId);

    Optional<RegistroAcademico> findFirstByTrayectoriaIdAndPlanCursoIdOrderByIdDesc(
            Long trayectoriaId, Long planCursoId);

    @Query("select r from RegistroAcademico r join fetch r.trayectoria t join fetch t.estudiante "
            + "join fetch r.planCurso pc join fetch pc.curso join fetch pc.ciclo join fetch r.situacion "
            + "where r.periodo.id = :periodoId and t.planEstudio.id = :planId")
    List<RegistroAcademico> findParaReporte(@Param("planId") Long planId, @Param("periodoId") Long periodoId);

    @Query("select r from RegistroAcademico r join fetch r.trayectoria t join fetch t.estudiante "
            + "join fetch r.planCurso pc join fetch pc.curso join fetch pc.ciclo join fetch r.situacion "
            + "where t.planEstudio.id = :planId")
    List<RegistroAcademico> findTodosDelPlan(@Param("planId") Long planId);

    boolean existsByTrayectoriaId(Long trayectoriaId);

    long deleteByTrayectoriaId(Long trayectoriaId);
}
