package pe.edu.sgfa.institucion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import pe.edu.sgfa.common.domain.BaseEntity;

@Entity
@Table(name = "instituciones")
public class Institucion extends BaseEntity {

    @Column(nullable = false)
    private String nombre;

    private String siglas;

    private String direccion;

    private String telefono;

    private String correo;

    @Column(name = "logo_path")
    private String logoPath;

    @Column(name = "fondo_login_path")
    private String fondoLoginPath;

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.INTEGER)
    private boolean activo = true;

    protected Institucion() {
    }

    public Institucion(String nombre) {
        this.nombre = nombre;
        this.activo = true;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getSiglas() {
        return siglas;
    }

    public void setSiglas(String siglas) {
        this.siglas = siglas;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getLogoPath() {
        return logoPath;
    }

    public void setLogoPath(String logoPath) {
        this.logoPath = logoPath;
    }

    public String getFondoLoginPath() {
        return fondoLoginPath;
    }

    public void setFondoLoginPath(String fondoLoginPath) {
        this.fondoLoginPath = fondoLoginPath;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
