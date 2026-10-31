package pe.edu.sgfa.programa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.sgfa.programa.domain.RacaProgramaEstudio;

public interface RacaProgramaEstudioRepository extends JpaRepository<RacaProgramaEstudio, Long> {

    Optional<RacaProgramaEstudio> findByCodigo(String codigo);

    boolean existsByInstitucionIdAndCodigo(Long institucionId, String codigo);

    boolean existsByInstitucionIdAndCodigoAndIdNot(Long institucionId, String codigo, Long id);
}
