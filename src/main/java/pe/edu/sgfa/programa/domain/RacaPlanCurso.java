package pe.edu.sgfa.programa.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import pe.edu.sgfa.common.domain.RyeBaseEntity;

@Entity
@Table(name = "plan_cursos")
public class RacaPlanCurso extends RyeBaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "plan_estudio_id", nullable = false)
    private RacaPlanEstudio planEstudio;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "ciclo_id", nullable = false)
    private RacaCiclo ciclo;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "curso_id", nullable = false)
    private RacaCurso curso;

    @Column(nullable = false)
    private int orden;

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.INTEGER)
    private boolean obligatorio = true;

    protected RacaPlanCurso() {
    }

    public RacaPlanCurso(RacaPlanEstudio planEstudioCrr, RacaCiclo cicloCrr, RacaCurso cursoCrr, int ordenCrr, boolean obligatorioCrr) {
        this.planEstudio = planEstudioCrr;
        this.ciclo = cicloCrr;
        this.curso = cursoCrr;
        this.orden = ordenCrr;
        this.obligatorio = obligatorioCrr;
    }

    public RacaPlanEstudio getPlanEstudio() {
        return planEstudio;
    }

    public RacaCiclo getCiclo() {
        return ciclo;
    }

    public RacaCurso getCurso() {
        return curso;
    }

    public int getOrden() {
        return orden;
    }

    public void setOrden(int ordenCrr) {
        this.orden = ordenCrr;
    }

    public boolean isObligatorio() {
        return obligatorio;
    }

    public void setObligatorio(boolean obligatorioCrr) {
        this.obligatorio = obligatorioCrr;
    }
}
