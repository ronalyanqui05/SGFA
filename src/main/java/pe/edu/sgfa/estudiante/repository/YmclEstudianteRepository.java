package pe.edu.sgfa.estudiante.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.edu.sgfa.estudiante.domain.YmclEstadoEstudiante;
import pe.edu.sgfa.estudiante.domain.YmclEstudiante;

public interface YmclEstudianteRepository extends JpaRepository<YmclEstudiante, Long> {

    List<YmclEstudiante> findByEstadoOrderByApellidoPaternoAscApellidoMaternoAscNombresAsc(YmclEstadoEstudiante estado);

    List<YmclEstudiante> findAllByOrderByApellidoPaternoAscApellidoMaternoAscNombresAsc();

    Optional<YmclEstudiante> findByCodigo(String codigo);

    Optional<YmclEstudiante> findByDni(String dni);

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, Long id);

    boolean existsByDni(String dni);

    boolean existsByDniAndIdNot(String dni, Long id);

    long countByEstado(YmclEstadoEstudiante estado);

    @Query("SELECT e FROM YmclEstudiante e WHERE "
            + "LOWER(e.nombres) LIKE LOWER(CONCAT('%', :termino, '%')) OR "
            + "LOWER(e.apellidoPaterno) LIKE LOWER(CONCAT('%', :termino, '%')) OR "
            + "LOWER(e.apellidoMaterno) LIKE LOWER(CONCAT('%', :termino, '%')) OR "
            + "LOWER(e.codigo) LIKE LOWER(CONCAT('%', :termino, '%')) OR "
            + "LOWER(e.dni) LIKE LOWER(CONCAT('%', :termino, '%')) "
            + "ORDER BY e.apellidoPaterno, e.apellidoMaterno, e.nombres")
    List<YmclEstudiante> buscar(@Param("termino") String termino);
}
