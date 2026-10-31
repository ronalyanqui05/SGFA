package pe.edu.sgfa.auditoria.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import pe.edu.sgfa.common.domain.RyeIdentifiableEntity;
import pe.edu.sgfa.common.persistence.RyeInstantStringConverter;

@Entity
@Table(name = "auditoria")
public class RahpAuditoria extends RyeIdentifiableEntity {

    @Column(name = "usuario_id")
    @JdbcTypeCode(SqlTypes.INTEGER)
    private Long usuarioId;

    @Column(nullable = false)
    private String accion;

    private String entidad;

    @Column(name = "entidad_id")
    private String entidadId;

    private String detalle;

    @Column(nullable = false)
    @Convert(converter = RyeInstantStringConverter.class)
    private Instant fecha;

    protected RahpAuditoria() {
    }

    public RahpAuditoria(Long usuarioIdHnc, String accionHnc, String entidadHnc, String entidadIdHnc, String detalleHnc, Instant fechaHnc) {
        this.usuarioId = usuarioIdHnc;
        this.accion = accionHnc;
        this.entidad = entidadHnc;
        this.entidadId = entidadIdHnc;
        this.detalle = detalleHnc;
        this.fecha = fechaHnc;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getAccion() {
        return accion;
    }

    public String getEntidad() {
        return entidad;
    }

    public String getEntidadId() {
        return entidadId;
    }

    public String getDetalle() {
        return detalle;
    }

    public Instant getFecha() {
        return fecha;
    }
}
