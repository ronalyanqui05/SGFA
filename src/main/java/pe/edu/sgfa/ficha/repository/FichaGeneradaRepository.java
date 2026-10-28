package pe.edu.sgfa.ficha.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.sgfa.ficha.domain.FichaGenerada;

public interface FichaGeneradaRepository extends JpaRepository<FichaGenerada, Long> {

    List<FichaGenerada> findByTrayectoriaIdOrderByFechaGeneracionDesc(Long tryctrId);

    Optional<FichaGenerada> findTopByAnioOrderByNumeroSecuencialDesc(int an);
}
