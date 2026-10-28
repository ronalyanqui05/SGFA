package pe.edu.sgfa.reporte.domain;

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
import pe.edu.sgfa.usuario.domain.Usuario;

@Entity
@Table(name = "documentos_emitidos")
public class DocumentoEmitido extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoDocumento tipo;

    @Column(nullable = false, unique = true)
    private String correlativo;

    @Column(name = "numero_secuencial", nullable = false)
    private int numeroSecuencial;

    @Column(nullable = false)
    private int anio;

    @Column(nullable = false)
    private String titulo;

    private String descripcion;

    @Column(name = "codigo_verificacion", nullable = false)
    private String codigoVerificacion;

    @Column(name = "archivo_path", nullable = false)
    private String archivoPath;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "fecha_emision", nullable = false)
    @Convert(converter = LocalDateStringConverter.class)
    private LocalDate fechaEmision;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoDocumento estado;

    private String observacion;

    protected DocumentoEmitido() {
    }

    public DocumentoEmitido(TipoDocumento tipo, String correlativo, int numeroSecuencial, int anio, String titulo,
                             String descripcion, String codigoVerificacion, String archivoPath, Usuario usuario) {
        this.tipo = tipo;
        this.correlativo = correlativo;
        this.numeroSecuencial = numeroSecuencial;
        this.anio = anio;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.codigoVerificacion = codigoVerificacion;
        this.archivoPath = archivoPath;
        this.usuario = usuario;
        this.fechaEmision = LocalDate.now();
        this.estado = EstadoDocumento.EMITIDO;
    }

    public void anular(String mtv) {
        this.estado = EstadoDocumento.ANULADO;
        this.observacion = mtv;
    }

    public TipoDocumento getTipo() {
        return tipo;
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

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getCodigoVerificacion() {
        return codigoVerificacion;
    }

    public String getArchivoPath() {
        return archivoPath;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public LocalDate getFechaEmision() {
        return fechaEmision;
    }

    public EstadoDocumento getEstado() {
        return estado;
    }

    public String getObservacion() {
        return observacion;
    }
}
