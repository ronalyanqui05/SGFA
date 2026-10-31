package pe.edu.sgfa.institucion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import pe.edu.sgfa.common.domain.RyeBaseEntity;

@Entity
@Table(name = "instituciones")
public class RacaInstitucion extends RyeBaseEntity {

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

    protected RacaInstitucion() {
    }

    public RacaInstitucion(String nombreCrr) {
        this.nombre = nombreCrr;
        this.activo = true;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombreCrr) {
        this.nombre = nombreCrr;
    }

    public String getSiglas() {
        return siglas;
    }

    public void setSiglas(String siglasCrr) {
        this.siglas = siglasCrr;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccionCrr) {
        this.direccion = direccionCrr;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefonoCrr) {
        this.telefono = telefonoCrr;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correoCrr) {
        this.correo = correoCrr;
    }

    public String getLogoPath() {
        return logoPath;
    }

    public void setLogoPath(String logoPathCrr) {
        this.logoPath = logoPathCrr;
    }

    public String getFondoLoginPath() {
        return fondoLoginPath;
    }

    public void setFondoLoginPath(String fondoLoginPathCrr) {
        this.fondoLoginPath = fondoLoginPathCrr;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activoCrr) {
        this.activo = activoCrr;
    }
}
