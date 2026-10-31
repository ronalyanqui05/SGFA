package pe.edu.sgfa.ficha.domain;

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
import pe.edu.sgfa.trayectoria.domain.YmclTrayectoria;
import pe.edu.sgfa.usuario.domain.RyeUsuario;

@Entity
@Table(name = "fichas_generadas")
public class RacaFichaGenerada extends RyeBaseEntity {

    @Column(nullable = false, unique = true)
    private String correlativo;

    @Column(name = "numero_secuencial", nullable = false)
    private int numeroSecuencial;

    @Column(nullable = false)
    private int anio;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "trayectoria_id", nullable = false)
    private YmclTrayectoria trayectoria;

    @Column(name = "archivo_path", nullable = false)
    private String archivoPath;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id")
    private RyeUsuario usuario;

    @Column(name = "fecha_generacion", nullable = false)
    @Convert(converter = RyeLocalDateStringConverter.class)
    private LocalDate fechaGeneracion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RacaEstadoFicha estado;

    private String observacion;

    protected RacaFichaGenerada() {
    }

    public RacaFichaGenerada(String correlativoCrr, int numeroSecuencialCrr, int anioCrr, YmclTrayectoria trayectoriaCrr,
                          String archivoPathCrr, RyeUsuario usuarioCrr) {
        this.correlativo = correlativoCrr;
        this.numeroSecuencial = numeroSecuencialCrr;
        this.anio = anioCrr;
        this.trayectoria = trayectoriaCrr;
        this.archivoPath = archivoPathCrr;
        this.usuario = usuarioCrr;
        this.fechaGeneracion = LocalDate.now();
        this.estado = RacaEstadoFicha.GENERADA;
    }

    public void racaAnular(String motivoCrr) {
        this.estado = RacaEstadoFicha.ANULADA;
        this.observacion = motivoCrr;
    }

    public String getCorrelativo() {
        return correlativo;
    }

    public int getNumeroSecuencial() {
        return numeroSecuencial;
    }

    public int getAnio() {
        return anio;
    }

    public YmclTrayectoria getTrayectoria() {
        return trayectoria;
    }

    public String getArchivoPath() {
        return archivoPath;
    }

    public RyeUsuario getUsuario() {
        return usuario;
    }

    public LocalDate getFechaGeneracion() {
        return fechaGeneracion;
    }

    public RacaEstadoFicha getEstado() {
        return estado;
    }

    public String getObservacion() {
        return observacion;
    }
}
