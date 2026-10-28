package pe.edu.sgfa.trayectoria.domain;

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
import pe.edu.sgfa.estudiante.domain.Estudiante;
import pe.edu.sgfa.programa.domain.PlanEstudio;
import pe.edu.sgfa.programa.domain.ProgramaEstudio;

@Entity
@Table(name = "trayectorias_academicas")
public class Trayectoria extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "programa_id", nullable = false)
    private ProgramaEstudio programa;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "plan_estudio_id", nullable = false)
    private PlanEstudio planEstudio;

    @Column(name = "fecha_inicio")
    @Convert(converter = LocalDateStringConverter.class)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    @Convert(converter = LocalDateStringConverter.class)
    private LocalDate fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoTrayectoria estado;

    private String observacion;

    protected Trayectoria() {
    }

    public Trayectoria(Estudiante estudiante, ProgramaEstudio programa, PlanEstudio planEstudio,
                        LocalDate fechaInicio, String observacion) {
        this.estudiante = estudiante;
        this.programa = programa;
        this.planEstudio = planEstudio;
        this.fechaInicio = fechaInicio;
        this.observacion = observacion;
        this.estado = EstadoTrayectoria.ACTIVA;
    }

    public Estudiante getEstudiante() {
        return estudiante;
    }

    public ProgramaEstudio getPrograma() {
        return programa;
    }

    public void setPrograma(ProgramaEstudio programa) {
        this.programa = programa;
    }

    public PlanEstudio getPlanEstudio() {
        return planEstudio;
    }

    public void setPlanEstudio(PlanEstudio planEstudio) {
        this.planEstudio = planEstudio;
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

    public EstadoTrayectoria getEstado() {
        return estado;
    }

    public void setEstado(EstadoTrayectoria estado) {
        this.estado = estado;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    @Override
    public String toString() {
        return "Plan " + planEstudio.getCodigo() + " (" + estado + ")";
    }
}
