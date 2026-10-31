package pe.edu.sgfa.nota.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.sgfa.nota.domain.YmclSituacionAcademica;

public interface YmclSituacionAcademicaRepository extends JpaRepository<YmclSituacionAcademica, Long> {

    List<YmclSituacionAcademica> findByActivoTrueOrderByNombreAsc();

    Optional<YmclSituacionAcademica> findByCodigo(String codigo);
}
