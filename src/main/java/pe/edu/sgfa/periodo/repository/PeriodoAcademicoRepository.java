package pe.edu.sgfa.periodo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.sgfa.periodo.domain.EstadoPeriodoAcademico;
import pe.edu.sgfa.periodo.domain.PeriodoAcademico;

public interface PeriodoAcademicoRepository extends JpaRepository<PeriodoAcademico, Long> {

    Optional<PeriodoAcademico> findByNombre(String nmbr);

    boolean existsByNombre(String nmbr);

    boolean existsByNombreAndIdNot(String nmbr, Long id);

    List<PeriodoAcademico> findByEstadoOrderByAnioDescNombreDesc(EstadoPeriodoAcademico estd);
}
