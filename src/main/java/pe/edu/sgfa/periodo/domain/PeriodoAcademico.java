package pe.edu.sgfa.periodo.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import pe.edu.sgfa.common.domain.BaseEntity;
import pe.edu.sgfa.common.persistence.LocalDateStringConverter;

@Entity
@Table(name = "periodos_academicos")
public class PeriodoAcademico extends BaseEntity {

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private int anio;

    private String tipo;

    @Column(name = "fecha_inicio")
    @Convert(converter = LocalDateStringConverter.class)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    @Convert(converter = LocalDateStringConverter.class)
    private LocalDate fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPeriodoAcademico estado;

    protected PeriodoAcademico() {
    }

    public PeriodoAcademico(String nombre, int anio, String tipo, LocalDate fechaInicio, LocalDate fechaFin) {
        this.nombre = nombre;
        this.anio = anio;
        this.tipo = tipo;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.estado = EstadoPeriodoAcademico.ACTIVO;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getAnio() {
        return anio;
    }

    public void setAnio(int anio) {
        this.anio = anio;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public EstadoPeriodoAcademico getEstado() {
        return estado;
    }

    public void setEstado(EstadoPeriodoAcademico estado) {
        this.estado = estado;
    }
}
