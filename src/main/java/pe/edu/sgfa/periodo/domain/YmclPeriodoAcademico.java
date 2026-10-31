package pe.edu.sgfa.periodo.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import pe.edu.sgfa.common.domain.RyeBaseEntity;
import pe.edu.sgfa.common.persistence.RyeLocalDateStringConverter;

@Entity
@Table(name = "periodos_academicos")
public class YmclPeriodoAcademico extends RyeBaseEntity {

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private int anio;

    private String tipo;

    @Column(name = "fecha_inicio")
    @Convert(converter = RyeLocalDateStringConverter.class)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    @Convert(converter = RyeLocalDateStringConverter.class)
    private LocalDate fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private YmclEstadoPeriodoAcademico estado;

    protected YmclPeriodoAcademico() {
    }

    public YmclPeriodoAcademico(String nombreChp, int anioChp, String tipoChp, LocalDate fechaInicioChp, LocalDate fechaFinChp) {
        this.nombre = nombreChp;
        this.anio = anioChp;
        this.tipo = tipoChp;
        this.fechaInicio = fechaInicioChp;
        this.fechaFin = fechaFinChp;
        this.estado = YmclEstadoPeriodoAcademico.ACTIVO;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombreChp) {
        this.nombre = nombreChp;
    }

    public int getAnio() {
        return anio;
    }

    public void setAnio(int anioChp) {
        this.anio = anioChp;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipoChp) {
        this.tipo = tipoChp;
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

    public YmclEstadoPeriodoAcademico getEstado() {
        return estado;
    }

    public void setEstado(YmclEstadoPeriodoAcademico estadoChp) {
        this.estado = estadoChp;
    }
}
