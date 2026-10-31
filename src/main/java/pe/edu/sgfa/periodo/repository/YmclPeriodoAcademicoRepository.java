package pe.edu.sgfa.periodo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.sgfa.periodo.domain.YmclEstadoPeriodoAcademico;
import pe.edu.sgfa.periodo.domain.YmclPeriodoAcademico;

public interface YmclPeriodoAcademicoRepository extends JpaRepository<YmclPeriodoAcademico, Long> {

    Optional<YmclPeriodoAcademico> findByNombre(String nombre);

    boolean existsByNombre(String nombre);

    boolean existsByNombreAndIdNot(String nombre, Long id);

    List<YmclPeriodoAcademico> findByEstadoOrderByAnioDescNombreDesc(YmclEstadoPeriodoAcademico estado);
}
