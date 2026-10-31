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

import pe.edu.sgfa.common.domain.RyeBaseEntity;
import pe.edu.sgfa.common.persistence.RyeLocalDateStringConverter;
import pe.edu.sgfa.usuario.domain.RyeUsuario;

@Entity
@Table(name = "documentos_emitidos")
public class CphDocumentoEmitido extends RyeBaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CphTipoDocumento tipo;

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
    private RyeUsuario usuario;

    @Column(name = "fecha_emision", nullable = false)
    @Convert(converter = RyeLocalDateStringConverter.class)
    private LocalDate fechaEmision;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CphEstadoDocumento estado;

    private String observacion;

    protected CphDocumentoEmitido() {
    }

    public CphDocumentoEmitido(CphTipoDocumento tipoPch, String correlativoPch, int numeroSecuencialPch, int anioPch, String tituloPch,
                             String descripcionPch, String codigoVerificacionPch, String archivoPathPch, RyeUsuario usuarioPch) {
        this.tipo = tipoPch;
        this.correlativo = correlativoPch;
        this.numeroSecuencial = numeroSecuencialPch;
        this.anio = anioPch;
        this.titulo = tituloPch;
        this.descripcion = descripcionPch;
        this.codigoVerificacion = codigoVerificacionPch;
        this.archivoPath = archivoPathPch;
        this.usuario = usuarioPch;
        this.fechaEmision = LocalDate.now();
        this.estado = CphEstadoDocumento.EMITIDO;
    }

    public void cphAnular(String motivoPch) {
        this.estado = CphEstadoDocumento.ANULADO;
        this.observacion = motivoPch;
    }

    public CphTipoDocumento getTipo() {
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

    public RyeUsuario getUsuario() {
        return usuario;
    }

    public LocalDate getFechaEmision() {
        return fechaEmision;
    }

    public CphEstadoDocumento getEstado() {
        return estado;
    }

    public String getObservacion() {
        return observacion;
    }
}
