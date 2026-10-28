package pe.edu.sgfa.programa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.sgfa.programa.domain.ProgramaEstudio;

public interface ProgramaEstudioRepository extends JpaRepository<ProgramaEstudio, Long> {

    Optional<ProgramaEstudio> findByCodigo(String cdg);

    boolean existsByInstitucionIdAndCodigo(Long insttcnId, String cdg);

    boolean existsByInstitucionIdAndCodigoAndIdNot(Long insttcnId, String cdg, Long id);
}
