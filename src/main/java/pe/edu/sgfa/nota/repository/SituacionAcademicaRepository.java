package pe.edu.sgfa.nota.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.sgfa.nota.domain.SituacionAcademica;

public interface SituacionAcademicaRepository extends JpaRepository<SituacionAcademica, Long> {

    List<SituacionAcademica> findByActivoTrueOrderByNombreAsc();

    Optional<SituacionAcademica> findByCodigo(String cdg);
}
