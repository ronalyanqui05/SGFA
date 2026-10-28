package pe.edu.sgfa.auditoria.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import pe.edu.sgfa.common.domain.IdentifiableEntity;
import pe.edu.sgfa.common.persistence.InstantStringConverter;

@Entity
@Table(name = "auditoria")
public class Auditoria extends IdentifiableEntity {

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
    @Convert(converter = InstantStringConverter.class)
    private Instant fecha;

    protected Auditoria() {
    }

    public Auditoria(Long usuarioId, String accion, String entidad, String entidadId, String detalle, Instant fecha) {
        this.usuarioId = usuarioId;
        this.accion = accion;
        this.entidad = entidad;
        this.entidadId = entidadId;
        this.detalle = detalle;
        this.fecha = fecha;
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
