package pe.edu.sgfa.nota.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import pe.edu.sgfa.common.domain.BaseEntity;

@Entity
@Table(name = "situaciones_academicas")
public class SituacionAcademica extends BaseEntity {

    @Column(nullable = false)
    private String codigo;

    @Column(nullable = false)
    private String nombre;

    @Column(name = "cuenta_para_promedio", nullable = false)
    @JdbcTypeCode(SqlTypes.INTEGER)
    private boolean cuentaParaPromedio;

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.INTEGER)
    private boolean activo;

    protected SituacionAcademica() {
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public boolean isCuentaParaPromedio() {
        return cuentaParaPromedio;
    }

    public boolean isActivo() {
        return activo;
    }
}
