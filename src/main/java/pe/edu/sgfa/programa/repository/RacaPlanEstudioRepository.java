package pe.edu.sgfa.programa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.sgfa.programa.domain.RacaPlanEstudio;

public interface RacaPlanEstudioRepository extends JpaRepository<RacaPlanEstudio, Long> {

    List<RacaPlanEstudio> findByProgramaId(Long programaId);

    Optional<RacaPlanEstudio> findByProgramaIdAndCodigo(Long programaId, String codigo);

    boolean existsByProgramaIdAndCodigo(Long programaId, String codigo);

    boolean existsByProgramaIdAndCodigoAndIdNot(Long programaId, String codigo, Long id);
}
