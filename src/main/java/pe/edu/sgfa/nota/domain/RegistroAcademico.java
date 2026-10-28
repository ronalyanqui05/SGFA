package pe.edu.sgfa.nota.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import pe.edu.sgfa.common.domain.BaseEntity;
import pe.edu.sgfa.common.persistence.LocalDateStringConverter;
import pe.edu.sgfa.periodo.domain.PeriodoAcademico;
import pe.edu.sgfa.programa.domain.PlanCurso;
import pe.edu.sgfa.trayectoria.domain.Trayectoria;
import pe.edu.sgfa.usuario.domain.Usuario;

@Entity
@Table(name = "registros_academicos")
public class RegistroAcademico extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "trayectoria_id", nullable = false)
    private Trayectoria trayectoria;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "plan_curso_id", nullable = false)
    private PlanCurso planCurso;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "periodo_id", nullable = false)
    private PeriodoAcademico periodo;

    @Column(name = "nota_final")
    private Double notaFinal;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "situacion_id", nullable = false)
    private SituacionAcademica situacion;

    @Column(name = "fecha_registro", nullable = false)
    @Convert(converter = LocalDateStringConverter.class)
    private LocalDate fechaRegistro;

    private String observacion;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    protected RegistroAcademico() {
    }

    public RegistroAcademico(Trayectoria trayectoria, PlanCurso planCurso, PeriodoAcademico periodo,
                              Double notaFinal, SituacionAcademica situacion, String observacion, Usuario usuario) {
        this.trayectoria = trayectoria;
        this.planCurso = planCurso;
        this.periodo = periodo;
        this.notaFinal = notaFinal;
        this.situacion = situacion;
        this.observacion = observacion;
        this.usuario = usuario;
        this.fechaRegistro = LocalDate.now();
    }

    public Trayectoria getTrayectoria() {
        return trayectoria;
    }

    public PlanCurso getPlanCurso() {
        return planCurso;
    }

    public PeriodoAcademico getPeriodo() {
        return periodo;
    }

    public Double getNotaFinal() {
        return notaFinal;
    }

    public void setNotaFinal(Double notaFinal) {
        this.notaFinal = notaFinal;
    }

    public SituacionAcademica getSituacion() {
        return situacion;
    }

    public void setSituacion(SituacionAcademica situacion) {
        this.situacion = situacion;
    }

    public LocalDate getFechaRegistro() {
        return fechaRegistro;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }
}
