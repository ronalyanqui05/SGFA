package pe.edu.sgfa.programa.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import pe.edu.sgfa.common.domain.BaseEntity;
import pe.edu.sgfa.institucion.domain.Institucion;

@Entity
@Table(name = "programas_estudio")
public class ProgramaEstudio extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "institucion_id", nullable = false)
    private Institucion institucion;

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

    protected ProgramaEstudio() {
    }

    public ProgramaEstudio(Institucion institucion, String codigo, String nombre, String descripcion,
                            int duracionCiclos) {
        this.institucion = institucion;
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.duracionCiclos = duracionCiclos;
        this.activo = true;
    }

    public Institucion getInstitucion() {
        return institucion;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public int getDuracionCiclos() {
        return duracionCiclos;
    }

    public void setDuracionCiclos(int duracionCiclos) {
        this.duracionCiclos = duracionCiclos;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
