package pe.edu.sgfa.institucion.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.sgfa.institucion.domain.Institucion;

public interface InstitucionRepository extends JpaRepository<Institucion, Long> {
}
