package pe.edu.sgfa.estudiante.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.edu.sgfa.estudiante.domain.EstadoEstudiante;
import pe.edu.sgfa.estudiante.domain.Estudiante;

public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {

    List<Estudiante> findByEstadoOrderByApellidoPaternoAscApellidoMaternoAscNombresAsc(EstadoEstudiante estado);

    List<Estudiante> findAllByOrderByApellidoPaternoAscApellidoMaternoAscNombresAsc();

    Optional<Estudiante> findByCodigo(String codigo);

    Optional<Estudiante> findByDni(String dni);

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, Long id);

    boolean existsByDni(String dni);

    boolean existsByDniAndIdNot(String dni, Long id);

    long countByEstado(EstadoEstudiante estado);

    @Query("SELECT e FROM Estudiante e WHERE "
            + "LOWER(e.nombres) LIKE LOWER(CONCAT('%', :termino, '%')) OR "
            + "LOWER(e.apellidoPaterno) LIKE LOWER(CONCAT('%', :termino, '%')) OR "
            + "LOWER(e.apellidoMaterno) LIKE LOWER(CONCAT('%', :termino, '%')) OR "
            + "LOWER(e.codigo) LIKE LOWER(CONCAT('%', :termino, '%')) OR "
            + "LOWER(e.dni) LIKE LOWER(CONCAT('%', :termino, '%')) "
            + "ORDER BY e.apellidoPaterno, e.apellidoMaterno, e.nombres")
    List<Estudiante> buscar(@Param("termino") String termino);
}
