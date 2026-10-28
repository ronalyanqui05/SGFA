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

import pe.edu.sgfa.common.domain.BaseEntity;
import pe.edu.sgfa.common.persistence.LocalDateStringConverter;

@Entity
@Table(name = "planes_estudio")
public class PlanEstudio extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "programa_id", nullable = false)
    private ProgramaEstudio programa;

    @Column(nullable = false)
    private String codigo;

    @Column(nullable = false)
    private String nombre;

    @Column(name = "fecha_inicio")
    @Convert(converter = LocalDateStringConverter.class)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    @Convert(converter = LocalDateStringConverter.class)
    private LocalDate fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPlanEstudio estado;

    protected PlanEstudio() {
    }

    public PlanEstudio(ProgramaEstudio programa, String codigo, String nombre,
                        LocalDate fechaInicio, LocalDate fechaFin) {
        this.programa = programa;
        this.codigo = codigo;
        this.nombre = nombre;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.estado = EstadoPlanEstudio.ACTIVO;
    }

    public ProgramaEstudio getPrograma() {
        return programa;
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

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public EstadoPlanEstudio getEstado() {
        return estado;
    }

    public void setEstado(EstadoPlanEstudio estado) {
        this.estado = estado;
    }
}
