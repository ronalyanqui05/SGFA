package pe.edu.sgfa.programa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.sgfa.programa.domain.Ciclo;

public interface CicloRepository extends JpaRepository<Ciclo, Long> {

    List<Ciclo> findByPlanEstudioIdOrderByOrdenAsc(Long plnEstdId);

    Optional<Ciclo> findByPlanEstudioIdAndNumero(Long plnEstdId, int nmr);

    boolean existsByPlanEstudioIdAndNumero(Long plnEstdId, int nmr);

    boolean existsByPlanEstudioIdAndNumeroAndIdNot(Long plnEstdId, int nmr, Long id);
}
