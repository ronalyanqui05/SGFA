package pe.edu.sgfa.usuario.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.sgfa.usuario.domain.RyeUsuario;

public interface RyeUsuarioRepository extends JpaRepository<RyeUsuario, Long> {

    Optional<RyeUsuario> findByUsername(String username);

    boolean existsByUsername(String username);
}
