package pe.edu.sgfa.ficha.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.sgfa.ficha.domain.RacaFichaGenerada;

public interface RacaFichaGeneradaRepository extends JpaRepository<RacaFichaGenerada, Long> {

    List<RacaFichaGenerada> findByTrayectoriaIdOrderByFechaGeneracionDesc(Long trayectoriaId);

    Optional<RacaFichaGenerada> findTopByAnioOrderByNumeroSecuencialDesc(int anio);
}
