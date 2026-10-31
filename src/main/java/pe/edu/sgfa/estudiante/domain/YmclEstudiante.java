package pe.edu.sgfa.estudiante.domain;

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
@Table(name = "estudiantes")
public class YmclEstudiante extends RyeBaseEntity {

    private String codigo;

    private String dni;

    @Column(nullable = false)
    private String nombres;

    @Column(name = "apellido_paterno", nullable = false)
    private String apellidoPaterno;

    @Column(name = "apellido_materno")
    private String apellidoMaterno;

    @Column(name = "fecha_nacimiento")
    @Convert(converter = RyeLocalDateStringConverter.class)
    private LocalDate fechaNacimiento;

    @Enumerated(EnumType.STRING)
    private YmclSexo sexo;

    private String direccion;

    private String telefono;

    private String correo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private YmclEstadoEstudiante estado;

    @Column(name = "fecha_registro", nullable = false)
    @Convert(converter = RyeLocalDateStringConverter.class)
    private LocalDate fechaRegistro;

    protected YmclEstudiante() {
    }

    public YmclEstudiante(String codigoChp, String dniChp, String nombresChp, String apellidoPaternoChp, String apellidoMaternoChp,
                       LocalDate fechaNacimientoChp, YmclSexo sexoChp, String direccionChp, String telefonoChp, String correoChp) {
        this.codigo = codigoChp;
        this.dni = dniChp;
        this.nombres = nombresChp;
        this.apellidoPaterno = apellidoPaternoChp;
        this.apellidoMaterno = apellidoMaternoChp;
        this.fechaNacimiento = fechaNacimientoChp;
        this.sexo = sexoChp;
        this.direccion = direccionChp;
        this.telefono = telefonoChp;
        this.correo = correoChp;
        this.estado = YmclEstadoEstudiante.ACTIVO;
        this.fechaRegistro = LocalDate.now();
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigoChp) {
        this.codigo = codigoChp;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dniChp) {
        this.dni = dniChp;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombresChp) {
        this.nombres = nombresChp;
    }

    public String getApellidoPaterno() {
        return apellidoPaterno;
    }

    public void setApellidoPaterno(String apellidoPaternoChp) {
        this.apellidoPaterno = apellidoPaternoChp;
    }

    public String getApellidoMaterno() {
        return apellidoMaterno;
    }

    public void setApellidoMaterno(String apellidoMaternoChp) {
        this.apellidoMaterno = apellidoMaternoChp;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimientoChp) {
        this.fechaNacimiento = fechaNacimientoChp;
    }

    public YmclSexo getSexo() {
        return sexo;
    }

    public void setSexo(YmclSexo sexoChp) {
        this.sexo = sexoChp;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccionChp) {
        this.direccion = direccionChp;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefonoChp) {
        this.telefono = telefonoChp;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correoChp) {
        this.correo = correoChp;
    }

    public YmclEstadoEstudiante getEstado() {
        return estado;
    }

    public void setEstado(YmclEstadoEstudiante estadoChp) {
        this.estado = estadoChp;
    }

    public LocalDate getFechaRegistro() {
        return fechaRegistro;
    }
}
