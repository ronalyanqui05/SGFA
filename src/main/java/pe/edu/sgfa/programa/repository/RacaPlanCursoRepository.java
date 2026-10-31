package pe.edu.sgfa.programa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.sgfa.programa.domain.RacaPlanCurso;

public interface RacaPlanCursoRepository extends JpaRepository<RacaPlanCurso, Long> {

    List<RacaPlanCurso> findByCicloIdOrderByOrdenAsc(Long cicloId);

    Optional<RacaPlanCurso> findByCicloIdAndCursoId(Long cicloId, Long cursoId);

    boolean existsByPlanEstudioIdAndCicloIdAndCursoId(Long planEstudioId, Long cicloId, Long cursoId);
}
