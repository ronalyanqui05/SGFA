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

import pe.edu.sgfa.common.domain.BaseEntity;
import pe.edu.sgfa.common.persistence.LocalDateStringConverter;
import pe.edu.sgfa.trayectoria.domain.Trayectoria;
import pe.edu.sgfa.usuario.domain.Usuario;

@Entity
@Table(name = "fichas_generadas")
public class FichaGenerada extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String correlativo;

    @Column(name = "numero_secuencial", nullable = false)
    private int numeroSecuencial;

    @Column(nullable = false)
    private int anio;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "trayectoria_id", nullable = false)
    private Trayectoria trayectoria;

    @Column(name = "archivo_path", nullable = false)
    private String archivoPath;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "fecha_generacion", nullable = false)
    @Convert(converter = LocalDateStringConverter.class)
    private LocalDate fechaGeneracion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoFicha estado;

    private String observacion;

    protected FichaGenerada() {
    }

    public FichaGenerada(String correlativo, int numeroSecuencial, int anio, Trayectoria trayectoria,
                          String archivoPath, Usuario usuario) {
        this.correlativo = correlativo;
        this.numeroSecuencial = numeroSecuencial;
        this.anio = anio;
        this.trayectoria = trayectoria;
        this.archivoPath = archivoPath;
        this.usuario = usuario;
        this.fechaGeneracion = LocalDate.now();
        this.estado = EstadoFicha.GENERADA;
    }

    public void anular(String mtv) {
        this.estado = EstadoFicha.ANULADA;
        this.observacion = mtv;
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

    public Trayectoria getTrayectoria() {
        return trayectoria;
    }

    public String getArchivoPath() {
        return archivoPath;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public LocalDate getFechaGeneracion() {
        return fechaGeneracion;
    }

    public EstadoFicha getEstado() {
        return estado;
    }

    public String getObservacion() {
        return observacion;
    }
}
