package pe.edu.sgfa.reporte.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.sgfa.reporte.domain.CphDocumentoEmitido;
import pe.edu.sgfa.reporte.domain.CphTipoDocumento;

public interface CphDocumentoEmitidoRepository extends JpaRepository<CphDocumentoEmitido, Long> {

    Optional<CphDocumentoEmitido> findTopByTipoAndAnioOrderByNumeroSecuencialDesc(CphTipoDocumento tipo, int anio);

    List<CphDocumentoEmitido> findAllByOrderByIdDesc();

    Optional<CphDocumentoEmitido> findByCorrelativo(String correlativo);
}
