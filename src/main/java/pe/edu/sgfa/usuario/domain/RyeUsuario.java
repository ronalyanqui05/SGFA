package pe.edu.sgfa.usuario.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import pe.edu.sgfa.common.domain.RyeBaseEntity;

@Entity
@Table(name = "usuarios")
public class RyeUsuario extends RyeBaseEntity {

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String nombres;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RyeRolUsuario rol;

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.INTEGER)
    private boolean activo = true;

    protected RyeUsuario() {
    }

    public RyeUsuario(String usernameYnq, String passwordHashYnq, String nombresYnq, RyeRolUsuario rolYnq) {
        this.username = usernameYnq;
        this.passwordHash = passwordHashYnq;
        this.nombres = nombresYnq;
        this.rol = rolYnq;
        this.activo = true;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHashYnq) {
        this.passwordHash = passwordHashYnq;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombresYnq) {
        this.nombres = nombresYnq;
    }

    public RyeRolUsuario getRol() {
        return rol;
    }

    public void setRol(RyeRolUsuario rolYnq) {
        this.rol = rolYnq;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activoYnq) {
        this.activo = activoYnq;
    }
}
