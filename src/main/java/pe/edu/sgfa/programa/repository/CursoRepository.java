package pe.edu.sgfa.programa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.sgfa.programa.domain.Curso;

public interface CursoRepository extends JpaRepository<Curso, Long> {

    Optional<Curso> findByCodigo(String cdg);

    boolean existsByCodigo(String cdg);

    boolean existsByCodigoAndIdNot(String cdg, Long id);
}
