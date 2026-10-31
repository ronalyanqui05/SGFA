package pe.edu.sgfa.nota.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import pe.edu.sgfa.common.domain.RyeBaseEntity;
import pe.edu.sgfa.common.persistence.RyeLocalDateStringConverter;
import pe.edu.sgfa.periodo.domain.YmclPeriodoAcademico;
import pe.edu.sgfa.programa.domain.RacaPlanCurso;
import pe.edu.sgfa.trayectoria.domain.YmclTrayectoria;
import pe.edu.sgfa.usuario.domain.RyeUsuario;

@Entity
@Table(name = "registros_academicos")
public class YmclRegistroAcademico extends RyeBaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "trayectoria_id", nullable = false)
    private YmclTrayectoria trayectoria;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "plan_curso_id", nullable = false)
    private RacaPlanCurso planCurso;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "periodo_id", nullable = false)
    private YmclPeriodoAcademico periodo;

    @Column(name = "nota_final")
    private Double notaFinal;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "situacion_id", nullable = false)
    private YmclSituacionAcademica situacion;

    @Column(name = "fecha_registro", nullable = false)
    @Convert(converter = RyeLocalDateStringConverter.class)
    private LocalDate fechaRegistro;

    private String observacion;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id")
    private RyeUsuario usuario;

    protected YmclRegistroAcademico() {
    }

    public YmclRegistroAcademico(YmclTrayectoria trayectoriaChp, RacaPlanCurso planCursoChp, YmclPeriodoAcademico periodoChp,
                              Double notaFinalChp, YmclSituacionAcademica situacionChp, String observacionChp, RyeUsuario usuarioChp) {
        this.trayectoria = trayectoriaChp;
        this.planCurso = planCursoChp;
        this.periodo = periodoChp;
        this.notaFinal = notaFinalChp;
        this.situacion = situacionChp;
        this.observacion = observacionChp;
        this.usuario = usuarioChp;
        this.fechaRegistro = LocalDate.now();
    }

    public YmclTrayectoria getTrayectoria() {
        return trayectoria;
    }

    public RacaPlanCurso getPlanCurso() {
        return planCurso;
    }

    public YmclPeriodoAcademico getPeriodo() {
        return periodo;
    }

    public Double getNotaFinal() {
        return notaFinal;
    }

    public void setNotaFinal(Double notaFinalChp) {
        this.notaFinal = notaFinalChp;
    }

    public YmclSituacionAcademica getSituacion() {
        return situacion;
    }

    public void setSituacion(YmclSituacionAcademica situacionChp) {
        this.situacion = situacionChp;
    }

    public LocalDate getFechaRegistro() {
        return fechaRegistro;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacionChp) {
        this.observacion = observacionChp;
    }

    public RyeUsuario getUsuario() {
        return usuario;
    }

    public void setUsuario(RyeUsuario usuarioChp) {
        this.usuario = usuarioChp;
    }
}
