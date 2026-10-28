package pe.edu.sgfa.programa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.sgfa.programa.domain.PlanCurso;

public interface PlanCursoRepository extends JpaRepository<PlanCurso, Long> {

    List<PlanCurso> findByCicloIdOrderByOrdenAsc(Long cclId);

    Optional<PlanCurso> findByCicloIdAndCursoId(Long cclId, Long crsId);

    boolean existsByPlanEstudioIdAndCicloIdAndCursoId(Long plnEstdId, Long cclId, Long crsId);
}
