package pe.edu.sgfa.programa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.sgfa.programa.domain.RacaCurso;

public interface RacaCursoRepository extends JpaRepository<RacaCurso, Long> {

    Optional<RacaCurso> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, Long id);
}
