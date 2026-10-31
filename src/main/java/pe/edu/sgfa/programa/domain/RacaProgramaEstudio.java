package pe.edu.sgfa.programa.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import pe.edu.sgfa.common.domain.RyeBaseEntity;
import pe.edu.sgfa.institucion.domain.RacaInstitucion;

@Entity
@Table(name = "programas_estudio")
public class RacaProgramaEstudio extends RyeBaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "institucion_id", nullable = false)
    private RacaInstitucion institucion;

    @Column(nullable = false)
    private String codigo;

    @Column(nullable = false)
    private String nombre;

    private String descripcion;

    @Column(name = "duracion_ciclos", nullable = false)
    private int duracionCiclos;

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.INTEGER)
    private boolean activo = true;

    protected RacaProgramaEstudio() {
    }

    public RacaProgramaEstudio(RacaInstitucion institucionCrr, String codigoCrr, String nombreCrr, String descripcionCrr,
                            int duracionCiclosCrr) {
        this.institucion = institucionCrr;
        this.codigo = codigoCrr;
        this.nombre = nombreCrr;
        this.descripcion = descripcionCrr;
        this.duracionCiclos = duracionCiclosCrr;
        this.activo = true;
    }

    public RacaInstitucion getInstitucion() {
        return institucion;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigoCrr) {
        this.codigo = codigoCrr;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombreCrr) {
        this.nombre = nombreCrr;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcionCrr) {
        this.descripcion = descripcionCrr;
    }

    public int getDuracionCiclos() {
        return duracionCiclos;
    }

    public void setDuracionCiclos(int duracionCiclosCrr) {
        this.duracionCiclos = duracionCiclosCrr;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activoCrr) {
        this.activo = activoCrr;
    }
}
