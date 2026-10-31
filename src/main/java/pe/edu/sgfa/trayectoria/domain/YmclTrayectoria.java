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

import pe.edu.sgfa.common.domain.RyeBaseEntity;
import pe.edu.sgfa.common.persistence.RyeLocalDateStringConverter;
import pe.edu.sgfa.estudiante.domain.YmclEstudiante;
import pe.edu.sgfa.programa.domain.RacaPlanEstudio;
import pe.edu.sgfa.programa.domain.RacaProgramaEstudio;

@Entity
@Table(name = "trayectorias_academicas")
public class YmclTrayectoria extends RyeBaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private YmclEstudiante estudiante;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "programa_id", nullable = false)
    private RacaProgramaEstudio programa;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "plan_estudio_id", nullable = false)
    private RacaPlanEstudio planEstudio;

    @Column(name = "fecha_inicio")
    @Convert(converter = RyeLocalDateStringConverter.class)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    @Convert(converter = RyeLocalDateStringConverter.class)
    private LocalDate fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private YmclEstadoTrayectoria estado;

    private String observacion;

    protected YmclTrayectoria() {
    }

    public YmclTrayectoria(YmclEstudiante estudianteChp, RacaProgramaEstudio programaChp, RacaPlanEstudio planEstudioChp,
                        LocalDate fechaInicioChp, String observacionChp) {
        this.estudiante = estudianteChp;
        this.programa = programaChp;
        this.planEstudio = planEstudioChp;
        this.fechaInicio = fechaInicioChp;
        this.observacion = observacionChp;
        this.estado = YmclEstadoTrayectoria.ACTIVA;
    }

    public YmclEstudiante getEstudiante() {
        return estudiante;
    }

    public RacaProgramaEstudio getPrograma() {
        return programa;
    }

    public void setPrograma(RacaProgramaEstudio programaChp) {
        this.programa = programaChp;
    }

    public RacaPlanEstudio getPlanEstudio() {
        return planEstudio;
    }

    public void setPlanEstudio(RacaPlanEstudio planEstudioChp) {
        this.planEstudio = planEstudioChp;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicioChp) {
        this.fechaInicio = fechaInicioChp;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFinChp) {
        this.fechaFin = fechaFinChp;
    }

    public YmclEstadoTrayectoria getEstado() {
        return estado;
    }

    public void setEstado(YmclEstadoTrayectoria estadoChp) {
        this.estado = estadoChp;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacionChp) {
        this.observacion = observacionChp;
    }

    @Override
    public String toString() {
        return "Plan " + planEstudio.getCodigo() + " (" + estado + ")";
    }
}
