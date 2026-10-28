package pe.edu.sgfa.programa.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import pe.edu.sgfa.common.domain.BaseEntity;

@Entity
@Table(name = "ciclos")
public class Ciclo extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "plan_estudio_id", nullable = false)
    private PlanEstudio planEstudio;

    @Column(nullable = false)
    private int numero;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private int orden;

    protected Ciclo() {
    }

    public Ciclo(PlanEstudio planEstudio, int numero, String nombre, int orden) {
        this.planEstudio = planEstudio;
        this.numero = numero;
        this.nombre = nombre;
        this.orden = orden;
    }

    public PlanEstudio getPlanEstudio() {
        return planEstudio;
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getOrden() {
        return orden;
    }

    public void setOrden(int orden) {
        this.orden = orden;
    }
}
