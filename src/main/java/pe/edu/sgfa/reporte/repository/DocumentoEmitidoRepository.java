package pe.edu.sgfa.reporte.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.sgfa.reporte.domain.DocumentoEmitido;
import pe.edu.sgfa.reporte.domain.TipoDocumento;

public interface DocumentoEmitidoRepository extends JpaRepository<DocumentoEmitido, Long> {

    Optional<DocumentoEmitido> findTopByTipoAndAnioOrderByNumeroSecuencialDesc(TipoDocumento tp, int an);

    List<DocumentoEmitido> findAllByOrderByIdDesc();

    Optional<DocumentoEmitido> findByCorrelativo(String crrltv);
}
