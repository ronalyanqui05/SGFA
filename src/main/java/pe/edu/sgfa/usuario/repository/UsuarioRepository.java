package pe.edu.sgfa.usuario.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.sgfa.usuario.domain.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String usrnm);

    boolean existsByUsername(String usrnm);
}
