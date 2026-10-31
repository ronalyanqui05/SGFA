package pe.edu.sgfa.programa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.sgfa.programa.domain.RacaCiclo;

public interface RacaCicloRepository extends JpaRepository<RacaCiclo, Long> {

    List<RacaCiclo> findByPlanEstudioIdOrderByOrdenAsc(Long planEstudioId);

    Optional<RacaCiclo> findByPlanEstudioIdAndNumero(Long planEstudioId, int numero);

    boolean existsByPlanEstudioIdAndNumero(Long planEstudioId, int numero);

    boolean existsByPlanEstudioIdAndNumeroAndIdNot(Long planEstudioId, int numero, Long id);
}
