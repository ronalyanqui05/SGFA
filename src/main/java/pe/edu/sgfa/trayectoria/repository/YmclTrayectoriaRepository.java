package pe.edu.sgfa.trayectoria.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.sgfa.trayectoria.domain.YmclEstadoTrayectoria;
import pe.edu.sgfa.trayectoria.domain.YmclTrayectoria;

public interface YmclTrayectoriaRepository extends JpaRepository<YmclTrayectoria, Long> {

    List<YmclTrayectoria> findByEstudianteIdOrderByFechaInicioDesc(Long estudianteId);

    Optional<YmclTrayectoria> findByEstudianteIdAndEstado(Long estudianteId, YmclEstadoTrayectoria estado);

    List<YmclTrayectoria> findByPlanEstudioIdAndEstado(Long planEstudioId, YmclEstadoTrayectoria estado);

    boolean existsByPlanEstudioId(Long planEstudioId);
}
