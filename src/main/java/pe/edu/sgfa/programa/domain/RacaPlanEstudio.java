package pe.edu.sgfa.programa.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import pe.edu.sgfa.common.domain.RyeBaseEntity;
import pe.edu.sgfa.common.persistence.RyeLocalDateStringConverter;

@Entity
@Table(name = "planes_estudio")
public class RacaPlanEstudio extends RyeBaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "programa_id", nullable = false)
    private RacaProgramaEstudio programa;

    @Column(nullable = false)
    private String codigo;

    @Column(nullable = false)
    private String nombre;

    @Column(name = "fecha_inicio")
    @Convert(converter = RyeLocalDateStringConverter.class)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    @Convert(converter = RyeLocalDateStringConverter.class)
    private LocalDate fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RacaEstadoPlanEstudio estado;

    protected RacaPlanEstudio() {
    }

    public RacaPlanEstudio(RacaProgramaEstudio programaCrr, String codigoCrr, String nombreCrr,
                        LocalDate fechaInicioCrr, LocalDate fechaFinCrr) {
        this.programa = programaCrr;
        this.codigo = codigoCrr;
        this.nombre = nombreCrr;
        this.fechaInicio = fechaInicioCrr;
        this.fechaFin = fechaFinCrr;
        this.estado = RacaEstadoPlanEstudio.ACTIVO;
    }

    public RacaProgramaEstudio getPrograma() {
        return programa;
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

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicioCrr) {
        this.fechaInicio = fechaInicioCrr;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFinCrr) {
        this.fechaFin = fechaFinCrr;
    }

    public RacaEstadoPlanEstudio getEstado() {
        return estado;
    }

    public void setEstado(RacaEstadoPlanEstudio estadoCrr) {
        this.estado = estadoCrr;
    }
}
