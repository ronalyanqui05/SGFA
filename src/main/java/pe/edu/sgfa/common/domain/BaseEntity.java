package pe.edu.sgfa.common.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import pe.edu.sgfa.common.persistence.InstantStringConverter;

@MappedSuperclass
public abstract class BaseEntity extends IdentifiableEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    @Convert(converter = InstantStringConverter.class)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @Convert(converter = InstantStringConverter.class)
    private Instant updatedAt;

    @PrePersist
    protected void alCrear() {
        Instant ahr = Instant.now();
        createdAt = ahr;
        updatedAt = ahr;
    }

    @PreUpdate
    protected void alActualizar() {
        updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
