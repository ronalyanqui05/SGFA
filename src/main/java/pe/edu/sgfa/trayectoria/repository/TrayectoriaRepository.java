package pe.edu.sgfa.trayectoria.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.sgfa.trayectoria.domain.EstadoTrayectoria;
import pe.edu.sgfa.trayectoria.domain.Trayectoria;

public interface TrayectoriaRepository extends JpaRepository<Trayectoria, Long> {

    List<Trayectoria> findByEstudianteIdOrderByFechaInicioDesc(Long estdntId);

    Optional<Trayectoria> findByEstudianteIdAndEstado(Long estdntId, EstadoTrayectoria estd);

    List<Trayectoria> findByPlanEstudioIdAndEstado(Long plnEstdId, EstadoTrayectoria estd);

    boolean existsByPlanEstudioId(Long plnEstdId);
}
