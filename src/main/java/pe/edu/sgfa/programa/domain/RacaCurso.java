package pe.edu.sgfa.programa.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import pe.edu.sgfa.common.domain.RyeBaseEntity;

@Entity
@Table(name = "cursos")
public class RacaCurso extends RyeBaseEntity {

    @Column(nullable = false)
    private String codigo;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private int creditos;

    private Integer horas;

    private String tipo;

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.INTEGER)
    private boolean activo = true;

    protected RacaCurso() {
    }

    public RacaCurso(String codigoCrr, String nombreCrr, int creditosCrr, Integer horasCrr, String tipoCrr) {
        this.codigo = codigoCrr;
        this.nombre = nombreCrr;
        this.creditos = creditosCrr;
        this.horas = horasCrr;
        this.tipo = tipoCrr;
        this.activo = true;
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

    public int getCreditos() {
        return creditos;
    }

    public void setCreditos(int creditosCrr) {
        this.creditos = creditosCrr;
    }

    public Integer getHoras() {
        return horas;
    }

    public void setHoras(Integer horasCrr) {
        this.horas = horasCrr;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipoCrr) {
        this.tipo = tipoCrr;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activoCrr) {
        this.activo = activoCrr;
    }
}
