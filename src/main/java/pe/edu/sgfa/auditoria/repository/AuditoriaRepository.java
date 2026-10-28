package pe.edu.sgfa.auditoria.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.edu.sgfa.auditoria.domain.Auditoria;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    @Query("SELECT a FROM Auditoria a WHERE "
            + "(:desde IS NULL OR a.fecha >= :desde) AND "
            + "(:hasta IS NULL OR a.fecha <= :hasta) AND "
            + "(:usuarioId IS NULL OR a.usuarioId = :usuarioId) AND "
            + "(:accion IS NULL OR a.accion = :accion) AND "
            + "(:entidad IS NULL OR a.entidad = :entidad)")
    Page<Auditoria> buscar(@Param("desde") Instant desde, @Param("hasta") Instant hasta,
                            @Param("usuarioId") Long usuarioId, @Param("accion") String accion,
                            @Param("entidad") String entidad, Pageable pageable);

    @Query("SELECT DISTINCT a.accion FROM Auditoria a ORDER BY a.accion")
    List<String> listarAcciones();

    @Query("SELECT DISTINCT a.entidad FROM Auditoria a WHERE a.entidad IS NOT NULL ORDER BY a.entidad")
    List<String> listarEntidades();
}
