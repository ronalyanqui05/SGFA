package pe.edu.sgfa.programa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.sgfa.programa.domain.PlanEstudio;

public interface PlanEstudioRepository extends JpaRepository<PlanEstudio, Long> {

    List<PlanEstudio> findByProgramaId(Long prgrmId);

    Optional<PlanEstudio> findByProgramaIdAndCodigo(Long prgrmId, String cdg);

    boolean existsByProgramaIdAndCodigo(Long prgrmId, String cdg);

    boolean existsByProgramaIdAndCodigoAndIdNot(Long prgrmId, String cdg, Long id);
}
