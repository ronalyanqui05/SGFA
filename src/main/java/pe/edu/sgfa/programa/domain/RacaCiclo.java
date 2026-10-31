package pe.edu.sgfa.programa.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import pe.edu.sgfa.common.domain.RyeBaseEntity;

@Entity
@Table(name = "ciclos")
public class RacaCiclo extends RyeBaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "plan_estudio_id", nullable = false)
    private RacaPlanEstudio planEstudio;

    @Column(nullable = false)
    private int numero;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private int orden;

    protected RacaCiclo() {
    }

    public RacaCiclo(RacaPlanEstudio planEstudioCrr, int numeroCrr, String nombreCrr, int ordenCrr) {
        this.planEstudio = planEstudioCrr;
        this.numero = numeroCrr;
        this.nombre = nombreCrr;
        this.orden = ordenCrr;
    }

    public RacaPlanEstudio getPlanEstudio() {
        return planEstudio;
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numeroCrr) {
        this.numero = numeroCrr;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombreCrr) {
        this.nombre = nombreCrr;
    }

    public int getOrden() {
        return orden;
    }

    public void setOrden(int ordenCrr) {
        this.orden = ordenCrr;
    }
}
