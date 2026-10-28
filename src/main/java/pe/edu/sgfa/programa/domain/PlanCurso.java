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

@Entity
@Table(name = "plan_cursos")
public class PlanCurso extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "plan_estudio_id", nullable = false)
    private PlanEstudio planEstudio;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "ciclo_id", nullable = false)
    private Ciclo ciclo;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "curso_id", nullable = false)
    private Curso curso;

    @Column(nullable = false)
    private int orden;

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.INTEGER)
    private boolean obligatorio = true;

    protected PlanCurso() {
    }

    public PlanCurso(PlanEstudio planEstudio, Ciclo ciclo, Curso curso, int orden, boolean obligatorio) {
        this.planEstudio = planEstudio;
        this.ciclo = ciclo;
        this.curso = curso;
        this.orden = orden;
        this.obligatorio = obligatorio;
    }

    public PlanEstudio getPlanEstudio() {
        return planEstudio;
    }

    public Ciclo getCiclo() {
        return ciclo;
    }

    public Curso getCurso() {
        return curso;
    }

    public int getOrden() {
        return orden;
    }

    public void setOrden(int orden) {
        this.orden = orden;
    }

    public boolean isObligatorio() {
        return obligatorio;
    }

    public void setObligatorio(boolean obligatorio) {
        this.obligatorio = obligatorio;
    }
}
